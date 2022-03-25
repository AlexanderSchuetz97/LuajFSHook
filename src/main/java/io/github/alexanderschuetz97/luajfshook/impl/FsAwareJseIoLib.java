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
import io.github.alexanderschuetz97.luajfshook.api.LuaRandomAccessFile;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.jse.JseIoLib;

import java.io.EOFException;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * Provides a implementation of JseIoLib that should behave exactly the same as the standard JseIoLib however it uses a {@link LuaFileSystemHandler}
 * to determine paths to files.
 */
public class FsAwareJseIoLib extends JseIoLib {



    protected LuaFileSystemHandler handler;

    //Fix a bug/inconsistency in IoLib, in c lua io.output does not overwrite stdout. In JseIoLib it does...
    protected File stderr;
    protected File stdout;
    protected File stdin;

    protected static final LuaValue STDIN       = valueOf("stdin");
    protected static final LuaValue STDOUT      = valueOf("stdout");
    protected static final LuaValue STDERR      = valueOf("stderr");

    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        Globals globals = env.checkglobals();
        if (!(globals.finder instanceof LuaFileSystemHandler)) {
            throw new LuaError("globals.finder is not instanceof LuaFileSystemHandler");
        }
        handler = (LuaFileSystemHandler) globals.finder;
        LuaValue iotab = super.call(modname, env);

        try {
            stderr = wrapStderr();
            stdout = wrapStdout();
            stdin = wrapStdin();
        } catch (IOException e) {
            throw new LuaError(e);
        }

        return iotab;
    }

    /**
     * meta method __INDEX on the IO table
     */
    public Varargs _io_index(LuaValue v) {
        return v.equals(STDOUT)? stdout:
                v.equals(STDIN)?  stdin:
                        v.equals(STDERR)? stderr: NIL;
    }

    protected File openFile(String filename, boolean readMode, boolean appendMode, boolean updateMode, boolean binaryMode ) throws IOException {
        LuaPath path = handler.resolvePath(filename);
        LuaRandomAccessFile f = path.open(readMode? "r": "rw");
        if (!appendMode && !readMode) {
            f.setSize(0);
        } else if (appendMode) {
            f.setPosition(f.size());
        }

        return new RandomAccessFileFile(f);
    }

    protected File openProgram(String prog, String mode) throws IOException {


        Path sysPath = handler.getWorkDirectory().toSystemPath();
        java.io.File wd = sysPath != null ? sysPath.toFile() : new java.io.File(".");

        //This is pretty bad ngl... I would like to improve this to make
        //it not be like this but can not due to api...
        final Process p = Runtime.getRuntime().exec(prog, null, wd);
        return "w".equals(mode)?
                new OutputStreamFile( p.getOutputStream() ):
                new InputStreamFile( p.getInputStream() );
    }

    protected File tmpFile() throws IOException {
        LuaPath path = handler.tmpFile(".luaj", "bin");
        return new RandomAccessFileFile(path.open("rw"));
    }

    protected class OutputStreamFile extends File {

        private final OutputStream outputStream;
        private boolean closed = false;
        private boolean flushAfterWrite = false;

        public OutputStreamFile(OutputStream outputStream) {
            this.outputStream = outputStream;
        }

        @Override
        public void write(LuaString string) throws IOException {
            outputStream.write( string.m_bytes, string.m_offset, string.m_length );
            if (flushAfterWrite) {
                flush();
            }
        }

        @Override
        public void flush() throws IOException {
            outputStream.flush();
        }

        @Override
        public boolean isstdfile() {
            return true;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            outputStream.close();

        }

        @Override
        public boolean isclosed() {
            return closed;
        }

        @Override
        public int seek(String option, int bytecount) throws IOException {
            throw new LuaError("not implemented");
        }

        @Override
        public void setvbuf(String mode, int size) {
            flushAfterWrite = "no".equals(mode);
        }

        @Override
        public int remaining() throws IOException {
            return -1;
        }

        @Override
        public int peek() throws IOException, EOFException {
            throw new LuaError("not implemented");
        }

        @Override
        public int read() throws IOException, EOFException {
            throw new LuaError("not implemented");
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            throw new LuaError("not implemented");
        }
    }

    protected class InputStreamFile extends File {

        private final InputStream inputStream;
        private boolean closed = false;

        public InputStreamFile(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void write(LuaString string) throws IOException {
            throw new LuaError("not implemented");
        }

        @Override
        public void flush() throws IOException {

        }

        @Override
        public boolean isstdfile() {
            return true;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            inputStream.close();

        }

        @Override
        public boolean isclosed() {
            return closed;
        }

        @Override
        public int seek(String option, int bytecount) throws IOException {
            throw new LuaError("not implemented");
        }

        @Override
        public void setvbuf(String mode, int size) {

        }

        @Override
        public int remaining() throws IOException {
            return -1;
        }

        @Override
        public int peek() throws IOException, EOFException {
            inputStream.mark(1);
            int c = inputStream.read();
            inputStream.reset();
            return c;
        }

        @Override
        public int read() throws IOException, EOFException {
            return inputStream.read();
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            return inputStream.read(bytes, offset, length);
        }
    }

    protected class RandomAccessFileFile extends File {

        private final LuaRandomAccessFile file;
        private boolean closed = false;

        public RandomAccessFileFile(LuaRandomAccessFile file) throws IOException {
            this.file = file;
        }

        @Override
        public boolean isuserdata() {
            return true;
        }

        @Override
        public boolean isuserdata(Class c) {
            if (c.isInstance(file)) {
                return true;
            }

            return false;
        }

        @Override
        public Object touserdata() {
            return file;
        }

        @Override
        public Object touserdata(Class c) {
            if (!c.isInstance(file)) {
                return null;
            }

            return file;
        }

        @Override
        public Object checkuserdata() {
            return file;
        }

        @Override
        public Object checkuserdata(Class c) {

            if (c.isInstance(file)) {
                return file;
            }

            return typerror(c.getName());
        }

        @Override
        public void write(LuaString string) throws IOException {
            file.write(string.m_bytes, string.m_offset, string.m_length);
        }

        @Override
        public void flush() throws IOException {
            FileDescriptor fd = file.getFileDescriptor();
            if (fd != null) {
                fd.sync();
            }
        }

        @Override
        public boolean isstdfile() {
            return false;
        }

        @Override
        public void close() throws IOException {
            closed = true;
            file.close();
        }

        @Override
        public boolean isclosed() {
            return closed;
        }

        @Override
        public int seek(String option, int bytecount) throws IOException {
            switch (option) {
                case ("set"):
                    file.setPosition(bytecount);
                    break;
                case ("end"):
                    file.setPosition(file.size()+bytecount);
                    break;
                default:
                    file.setPosition(file.getPosition()+bytecount);
                    break;
            }

            return (int) file.getPosition();
        }

        @Override
        public void setvbuf(String mode, int size) {

        }

        @Override
        public int remaining() throws IOException {
            return (int) (file.size()-file.getPosition());
        }

        @Override
        public int peek() throws IOException, EOFException {
            long fp = file.getPosition();
            int c = file.read();
            file.setPosition(fp);
            return c;
        }

        @Override
        public int read() throws IOException, EOFException {
            return file.read();
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            return file.read(bytes, offset, length);
        }
    }
}
