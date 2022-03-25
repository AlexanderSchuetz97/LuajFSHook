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
import org.luaj.vm2.lib.OsLib;
import org.luaj.vm2.lib.VarArgFunction;

import java.io.IOException;
import java.util.Objects;

public class FsAwareOs_tmpname extends VarArgFunction {

    protected static volatile long COUNTER = System.currentTimeMillis();

    protected final LuaFileSystemHandler handler;

    public FsAwareOs_tmpname(LuaFileSystemHandler handler) {
        this.handler = Objects.requireNonNull(handler);
    }

    @Override
    public Varargs invoke(Varargs args) {
        try {
            return valueOf(handler.tmpFile(".luaj", "tmp").toString());
        } catch (IOException e) {
            synchronized ( FsAwareOs_tmpname.class ) {
                return valueOf(".luaj"+(COUNTER++)+"tmp");
            }
        }
    }
}
