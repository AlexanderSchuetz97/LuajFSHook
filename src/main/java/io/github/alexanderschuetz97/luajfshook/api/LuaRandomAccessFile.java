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

import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.channels.FileChannel;

/**
 * interface to provide {@link java.io.RandomAccessFile} like access to a file backed by virtual lua path.
 * The backing behind an instance may be a RandomAccessFile or something else.
 */
public interface LuaRandomAccessFile {

    /**
     * Returns the file descriptor of this file.
     * This may be null if this LuaRandomAccessFile is not associated with an operating system file descriptor.
     */
    FileDescriptor getFileDescriptor() throws IOException;

    /**
     * Returns the file channel of this file.
     * This may be null if this LuaRandomAccessFile is not associated with an operating system FileChannel.
     */
    FileChannel getFileChannel();

    /**
     * Returns an absolute path of this LuaRandomAccessFile.
     */
    LuaPath getPath();

    void setPosition(long position) throws IOException;

    long getPosition() throws IOException;

    long size() throws IOException;

    int read() throws IOException;

    int read(byte[] buf, int off, int len) throws IOException;

    void write(int b) throws IOException;

    void write(byte[] buf, int off, int len) throws IOException;

    void setSize(long i) throws IOException;

    void close() throws IOException;
}
