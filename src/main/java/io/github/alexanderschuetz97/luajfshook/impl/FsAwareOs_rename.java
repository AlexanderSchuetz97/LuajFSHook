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
package io.github.alexanderschuetz97.luajfshook.impl;

import io.github.alexanderschuetz97.luajfshook.api.LuaFileSystemHandler;
import io.github.alexanderschuetz97.luajfshook.api.LuaPath;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.io.IOException;
import java.util.Objects;

public class FsAwareOs_rename extends VarArgFunction {

    protected static final LuaValue FAILED_TO_DELETE = LuaValue.valueOf( "Failed to delete");
    protected static final LuaValue NO_SUCH_FILE_OR_DIRECTORY = LuaValue.valueOf("No such file or directory");


    protected final LuaFileSystemHandler handler;

    public FsAwareOs_rename(LuaFileSystemHandler handler) {
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public Varargs invoke(Varargs args) {
        String oldname = args.checkjstring(1);
        String newname = args.checkjstring(2);

        LuaPath old = handler.resolvePath(oldname);
        if (!old.exists()) {
            return NO_SUCH_FILE_OR_DIRECTORY;
        }

        try {
            old.moveFile(handler.resolvePath(newname));
        } catch (IOException e) {
            //not quite the error message I expected, but it is what JseOsLib does
            return FAILED_TO_DELETE;
        }
        ;


        return super.invoke(args);
    }
}
