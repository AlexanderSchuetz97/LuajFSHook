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

import io.github.alexanderschuetz97.luajfshook.api.LuaPath;
import io.github.alexanderschuetz97.luajfshook.api.LuaRandomAccessFile;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;

public class DefaultLuaRandomAccessFile implements LuaRandomAccessFile {

    protected final RandomAccessFile delegate;
    protected final LuaPath creator;

    public DefaultLuaRandomAccessFile(RandomAccessFile delegate, LuaPath creator) {
        this.delegate = delegate;
        this.creator = creator;
    }


    @Override
    public FileDescriptor getFileDescriptor() throws IOException {
        return delegate.getFD();
    }

    @Override
    public FileChannel getFileChannel() {
        return delegate.getChannel();
    }

    @Override
    public LuaPath getPath() {
        return creator;
    }

    @Override
    public void setPosition(long position) throws IOException {
        delegate.seek(position);
    }

    @Override
    public long getPosition() throws IOException {
        return delegate.getFilePointer();
    }

    @Override
    public long size() throws IOException {
        return delegate.length();
    }

    @Override
    public int read() throws IOException {
        return delegate.read();
    }

    @Override
    public int read(byte[] buf, int off, int len) throws IOException {
        return delegate.read(buf, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        delegate.write(b);
    }

    @Override
    public void write(byte[] buf, int off, int len) throws IOException {
        delegate.write(buf, off, len);
    }

    @Override
    public void setSize(long i) throws IOException {
        delegate.setLength(i);
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

}
