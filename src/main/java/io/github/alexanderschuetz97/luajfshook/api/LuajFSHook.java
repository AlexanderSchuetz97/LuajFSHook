//
// Copyright Alexander Schütz, 2022
//
// This file is part of LuajFSHook.
//
// LuajFSHook is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// LuajFSHook is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// A copy of the GNU Lesser General Public License should be provided
// in the COPYING & COPYING.LESSER files in top level directory of LuajFSHook.
// If not, see <https://www.gnu.org/licenses/>.
//
package io.github.alexanderschuetz97.luajfshook.api;

import io.github.alexanderschuetz97.luajfshook.impl.DefaultLuaFileSystemHandler;
import io.github.alexanderschuetz97.luajfshook.impl.FsAwareJseIoLib;
import io.github.alexanderschuetz97.luajfshook.impl.FsAwareOs_execute;
import io.github.alexanderschuetz97.luajfshook.impl.FsAwareOs_remove;
import io.github.alexanderschuetz97.luajfshook.impl.FsAwareOs_rename;
import io.github.alexanderschuetz97.luajfshook.impl.FsAwareOs_tmpname;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.PackageLib;
import org.luaj.vm2.lib.jse.JseBaseLib;
import org.luaj.vm2.lib.jse.JseOsLib;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Util class to load the LuajFSHook into a lua {@link Globals} environment.
 * Notes:
 *
 * LuajFSHook will load {@link JseBaseLib}, {@link PackageLib}, {@link JseOsLib} if they are not already loaded.
 * reloading any of these libs or reloading the {@link org.luaj.vm2.lib.IoLib} will undo the LuajFSHook.
 * Load them before calling {@link #install(Globals)}.
 *
 * processes created by os.execute & io.popen are started in the work directory as determined by the
 * {@link LuaFileSystemHandler#getWorkDirectory()} only if the work directory's
 * {@link LuaPath#toSystemPath()} does not return null. In addition to that processes are free to
 * break out of any potential 'sandboxes' that may be created by a custom {@link LuaFileSystemHandler}.
 * If you intend to sandbox lua, you will have to overwrite or remove those 2 functions
 * after the call to {@link #install(Globals, LuaFileSystemHandler, Executor)} returns.
 */
public class LuajFSHook {

    private static Executor DEFAULT;

    /**
     * Gets (and creates if this is the first call) a new Executor service.
     */
    public synchronized static Executor getDefaultExecutor() {
        if (DEFAULT == null) {
            DEFAULT = Executors.newCachedThreadPool();
        }

        return DEFAULT;
    }

    /**
     * Load the LuajFSHook into a lua {@link Globals} environment.
     *
     * @param executor executor that is used by os.execute to copy bytes to stdout/stderr. null -> getDefaultExecutor()
     * @param globals the globals. null -> {@link NullPointerException}
     * @param fileSystemHandler the fs handler to use. null -> {@link DefaultLuaFileSystemHandler}
     * @return true if the installation was successful, false if another LuaFileSystemHandler is already installed.
     */
    public static boolean install(Globals globals, LuaFileSystemHandler fileSystemHandler, Executor executor) {
        if (globals.finder instanceof LuaFileSystemHandler) {
            return false;
        }

        if (fileSystemHandler == null) {
            fileSystemHandler = new DefaultLuaFileSystemHandler();
        }

        if (executor == null) {
            executor = getDefaultExecutor();
        }

        if (globals.baselib == null) {
            globals.load(new JseBaseLib());
        }

        if (globals.get("package") == null) {
            globals.load(new PackageLib());
        }

        LuaValue loaded = globals.get("package").get("loaded");
        LuaValue os = loaded.get("os");
        if (os.isnil()) {
            globals.load(new JseOsLib());
            os = loaded.get("os");
        }

        globals.finder = fileSystemHandler;

        globals.load(new FsAwareJseIoLib());
        os.set("remove", new FsAwareOs_remove(fileSystemHandler));
        os.set("rename", new FsAwareOs_rename(fileSystemHandler));
        os.set("tmpname", new FsAwareOs_tmpname(fileSystemHandler));
        os.set("execute", new FsAwareOs_execute(globals, fileSystemHandler, executor));

        return true;
    }

    /**
     * Load the LuajFSHook into a lua {@link Globals} environment.
     *
     * @param globals the globals. null -> {@link NullPointerException}
     * @param fileSystemHandler the fs handler to use. null -> {@link DefaultLuaFileSystemHandler}
     * @return true if the installation was successful, false if another LuaFileSystemHandler is already installed.
     */
    public static boolean install(Globals globals, LuaFileSystemHandler fileSystemHandler) {
        return install(globals, fileSystemHandler, getDefaultExecutor());
    }

    /**
     * Load the LuajFSHook into a lua {@link Globals} environment.
     *
     * @param globals the globals. null -> {@link NullPointerException}
     * @param executor executor that is used by os.execute to copy bytes to stdout/stderr. null -> getDefaultExecutor()
     * @return true if the installation was successful, false if another LuaFileSystemHandler is already installed.
     */
    public static boolean install(Globals globals, Executor executor) {
        return install(globals, new DefaultLuaFileSystemHandler(), getDefaultExecutor());
    }

    /**
     * Load the LuajFSHook into a lua {@link Globals} environment.
     *
     * @param globals the globals. null -> {@link NullPointerException}
     * @return true if the installation was successful, false if another LuaFileSystemHandler is already installed.
     */
    public static boolean install(Globals globals) {
        return install(globals, new DefaultLuaFileSystemHandler(), getDefaultExecutor());
    }

    /**
     * Utility function that may call {@link #install(Globals)} and then returns the newly/already installed {@link LuaFileSystemHandler}.
     * If this function for some reason fails it throws {@link LuaError}.
     *
     * This function is intended to be used inside other libraries that use the file system.
     *
     * @param globals the globals
     * @return the installed LuaFileSystemHandler
     */
    public static LuaFileSystemHandler getOrInstall(Globals globals) {
        if (globals.finder instanceof LuaFileSystemHandler) {
            return (LuaFileSystemHandler) globals.finder;
        }

        if (!LuajFSHook.install(globals)) {
            throw new LuaError("failed to install LuajFSHook");
        }

        return (LuaFileSystemHandler) globals.finder;
    }

    /**
     * This function returns the installed LuaFileSystemHandler.
     *
     * If no LuaFileSystemHandler is installed then this method will return null.
     */
    public static LuaFileSystemHandler get(Globals globals) {
        if (globals.finder instanceof LuaFileSystemHandler) {
            return (LuaFileSystemHandler) globals.finder;
        }

        return null;
    }


}
