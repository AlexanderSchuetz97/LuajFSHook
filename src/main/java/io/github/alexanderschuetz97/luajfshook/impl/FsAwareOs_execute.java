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
import org.luaj.vm2.Globals;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.Executor;

import static org.luaj.vm2.lib.jse.JseOsLib.EXEC_ERROR;
import static org.luaj.vm2.lib.jse.JseOsLib.EXEC_INTERRUPTED;
import static org.luaj.vm2.lib.jse.JseOsLib.EXEC_IOEXCEPTION;

public class FsAwareOs_execute extends VarArgFunction {

    protected final LuaFileSystemHandler handler;
    protected final Globals globals;
    protected final Executor executor;

    public FsAwareOs_execute(Globals globals, LuaFileSystemHandler handler, Executor executor) {
        this.handler = Objects.requireNonNull(handler);
        this.globals = Objects.requireNonNull(globals);
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public Varargs invoke(Varargs args) {
        String command = args.optjstring(1, null);
        Path syspath = handler.getWorkDirectory().toSystemPath();
        File f = syspath == null ? new File(".") : syspath.toFile();

        int exitValue;
        try {
            Process process = Runtime.getRuntime().exec(command, null, f);

            if (globals.STDERR != null) {
                executor.execute(new RedirectIORunnable( process.getErrorStream(), globals.STDERR));
            }

            if (globals.STDOUT != null) {
                executor.execute(new RedirectIORunnable(process.getInputStream(), globals.STDOUT));
            }

            exitValue = process.waitFor();
        } catch (IOException ioe) {
            exitValue = EXEC_IOEXCEPTION;
        } catch (InterruptedException e) {
            exitValue = EXEC_INTERRUPTED;
        } catch (Throwable t) {
            exitValue = EXEC_ERROR;
        }
        if (exitValue == 0)
            return varargsOf(TRUE, valueOf("exit"), ZERO);
        return varargsOf(NIL, valueOf("signal"), valueOf(exitValue));
    }

    protected static class RedirectIORunnable implements Runnable {
        private final InputStream src;
        private final OutputStream out;

        public RedirectIORunnable(InputStream src, OutputStream out) {
            this.src = src;
            this.out = out;
        }

        @Override
        public void run() {
            byte[] bytes = new byte[1024];
            int i = 0;
            try {
                while(i != -1) {
                    i = src.read(bytes);
                    if (i > 0) {
                        out.write(bytes, 0, i);
                    }
                }
            } catch (IOException e) {
                //DC
            } finally {
                try {
                    src.close();
                } catch (IOException ex) {
                    //DC.
                }
            }
        }

    }
}
