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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Default implementation of LuaFileSystemHandler that just delegates to the default FileSystem and RandomAccessFile
 */
public class DefaultLuaFileSystemHandler implements LuaFileSystemHandler {

    private volatile DefaultLuaPath workDirectory;

    private static  final Set<FileVisitOption> FOLLOW_LINKS = EnumSet.of(FileVisitOption.FOLLOW_LINKS);

    private static final Set<FileVisitOption> DONT_FOLLOW_LINKS = EnumSet.noneOf(FileVisitOption.class);

    public DefaultLuaFileSystemHandler() {
        File wd = new File(".").getAbsoluteFile();

        try {
            wd = wd.getCanonicalFile();
        } catch (IOException e) {
            //DC
        }

        workDirectory = new DefaultLuaPath(wd.toPath());
    }

    @Override
    public LuaPath resolvePath(String filename) {
        return workDirectory.child(filename);
    }

    @Override
    public LuaPath relativePath(String path) {
        return new DefaultLuaPath(Paths.get(Objects.requireNonNull(path)));
    }

    @Override
    public LuaPath resolveSysPath(Path path) {
        return new DefaultLuaPath(Objects.requireNonNull(path));
    }

    @Override
    public LuaPath getWorkDirectory() {
        return workDirectory;
    }

    @Override
    public LuaPath tmpFile(String prefix, String suffix) throws IOException {
        return new DefaultLuaPath(Files.createTempFile(prefix == null ? ".luaj" : prefix,suffix == null ? "bin" : suffix ));
    }

    @Override
    public LuaPath tmpDir() throws IOException {
        String prop = System.getProperty("java.io.tmpdir");
        if (prop == null) {
            LuaPath t = tmpFile(null, null);
            t.delete();
            LuaPath tmpDir = t.parent();

            if (tmpDir == null) {
                throw new IOException("Unable to find tmpdir");
            }

            return tmpDir;
        }

        return new DefaultLuaPath(Paths.get(prop));
    }

    @Override
    public synchronized void setWorkDirectory(LuaPath file) throws NotDirectoryException, IOException {
        if (!file.isAbsolute()) {
            file = file.absolutePath();
        }

        if (!file.isDir()) {
            throw new NotDirectoryException(file.toString());
        }

        file = file.canon();


        this.workDirectory = (DefaultLuaPath) file;
    }

    @Override
    public InputStream findResource(String filename) {
        LuaPath luaPath = resolvePath(filename);

        try {
            return luaPath.openInput();
        } catch (IOException e) {
            return null;
        }
    }

    protected static class DefaultLuaPath implements LuaPath {

        protected final Path delegate;

        public DefaultLuaPath(Path delegate) {
            this.delegate = delegate;
        }

        @Override
        public String name() {
            Path fn = delegate.getFileName();
            if (fn == null) {
                fn = delegate.getRoot();
            }
            return fn.toString();
        }

        @Override
        public boolean isAbsolute() {
            return delegate.isAbsolute();
        }

        @Override
        public LuaPath absolutePath() {
            if (isAbsolute()) {
                return this;
            }
            return new DefaultLuaPath(delegate.toAbsolutePath());
        }

        @Override
        public LuaPath realPath() throws IOException {
            return new DefaultLuaPath(delegate.toRealPath());
        }

        @Override
        public LuaPath canon() throws IOException {
            return new DefaultLuaPath(delegate.toRealPath(LinkOption.NOFOLLOW_LINKS));
        }

        @Override
        public LuaPath child(String name) {
            return new DefaultLuaPath(delegate.resolve(name));
        }

        @Override
        public LuaPath relative(LuaPath other) {
            return new DefaultLuaPath(delegate.relativize(other.toSystemPath()));
        }

        @Override
        public void link(LuaPath to) throws IOException {
            Path syspath = to.toSystemPath();
            if (syspath == null) {
                throw new IOException("no syspath in target");
            }

            Files.createLink(delegate, syspath);
        }

        @Override
        public void symlink(LuaPath to) throws IOException {
            Path syspath = to.toSystemPath();
            if (syspath == null) {
                throw new IOException("no syspath in target");
            }

            Files.createSymbolicLink(delegate, syspath);
        }

        @Override
        public LuaPath child(LuaPath other) {
            if (other.isAbsolute()) {
                return other;
            }

            Path syspath = other.toSystemPath();
            if (syspath == null) {
                throw new IllegalArgumentException("no syspath");
            }

            return new DefaultLuaPath(delegate.resolve(syspath));
        }

        @Override
        public BasicFileAttributes attributes() throws FileNotFoundException, IOException {
            return Files.readAttributes(delegate, BasicFileAttributes.class);
        }

        @Override
        public BasicFileAttributes linkAttributes() throws FileNotFoundException, IOException {
            return Files.readAttributes(delegate, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        }

        @Override
        public void setFileTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException {
            Files.getFileAttributeView(delegate, BasicFileAttributeView.class).setTimes(lastModifiedTime, lastAccessTime, createTime);
        }

        @Override
        public String path() {
            return delegate.toString();
        }

        @Override
        public LuaPath parent() {
            Path parent = delegate.getParent();
            if (parent == null) {
                return null;
            }
            return new DefaultLuaPath(parent);
        }

        @Override
        public void copyFile(final LuaPath target) throws IOException {
            if (this.isDir()) {
                throw new IOException("cannot copy directory");
            }

            Path syspath = target.toSystemPath();
            if (syspath != null) {
                try {
                    Files.copy(delegate, syspath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                } catch (UnsupportedOperationException e) {
                    try {
                        Files.copy(delegate, syspath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (UnsupportedOperationException e2) {
                        throw new IOException(e2);
                    }
                }
                return;
            }

            byte[] buf = new byte[512];
            try (OutputStream outputStream = target.openOutput(false)) {
                try(InputStream inputStream = this.openInput()) {
                    int i = 0;
                    while(i != -1) {
                        i = inputStream.read(buf);
                        if (i > 0) {
                            outputStream.write(buf, 0, i);
                        }
                    }
                }
            }
        }

        @Override
        public void moveFile(LuaPath target) throws IOException {
            if (this.isDir()) {
                throw new IOException("cannot move directory");
            }

            Path syspath = target.toSystemPath();
            if (syspath != null) {
                try {
                    Files.move(delegate, syspath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
                } catch (UnsupportedOperationException e) {
                    try {
                        Files.move(delegate, syspath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (UnsupportedOperationException e2) {
                        throw new IOException(e2);
                    }

                }
                return;
            }

            byte[] buf = new byte[512];
            try (OutputStream outputStream = target.openOutput(false)) {
                try(InputStream inputStream = this.openInput()) {
                    int i = 0;
                    while(i != -1) {
                        i = inputStream.read(buf);
                        if (i > 0) {
                            outputStream.write(buf, 0, i);
                        }
                    }
                }
            }

            delete();
        }

        @Override
        public void move(LuaPath tar) throws IOException {
            Path syspath = tar.toSystemPath();
            if (syspath == null) {
                throw new IOException("no syspath in target");
            }

            Files.move(delegate, syspath, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
        }

        @Override
        public boolean exists() {
            return Files.exists(delegate);
        }

        @Override
        public boolean isDir() {
            return Files.isDirectory(delegate);
        }

        @Override
        public boolean isFile() {
            return Files.isRegularFile(delegate);
        }

        @Override
        public boolean isĹink() {
            return Files.isSymbolicLink(delegate);
        }

        @Override
        public List<LuaPath> list() throws NotDirectoryException, IOException {
            if (!isDir()) {
                throw new NotDirectoryException(delegate.toString());
            }

            final List<LuaPath> children = new LinkedList<>();
            Files.walkFileTree(delegate, FOLLOW_LINKS,1, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (Files.isSameFile(delegate, dir)) {
                        return FileVisitResult.CONTINUE;
                    }

                    children.add(new DefaultLuaPath(dir));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    children.add(new DefaultLuaPath(file));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });

            return children;
        }

        @Override
        public void walkFileTree(int depth, boolean followLinks, final LuaFileVisitor visitor) throws IOException {
            Files.walkFileTree(delegate, followLinks ? FOLLOW_LINKS : DONT_FOLLOW_LINKS , depth, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    return visitor.preVisitDirectory(new DefaultLuaPath(dir));
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    return visitor.visitFile(new DefaultLuaPath(file));
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    return visitor.postVisitDirectory(new DefaultLuaPath(dir));
                }
            });
        }

        @Override
        public void createNewFile() throws FileAlreadyExistsException, IOException, NotDirectoryException {
            Files.createFile(delegate);
        }

        @Override
        public void mkdir() throws FileAlreadyExistsException, IOException, NotDirectoryException {
            Files.createDirectory(delegate);
        }

        @Override
        public void mkdirs() throws FileAlreadyExistsException, IOException, NotDirectoryException {
            Files.createDirectories(delegate);
        }

        @Override
        public Path toSystemPath() {
            return delegate.toAbsolutePath();
        }

        @Override
        public LuaRandomAccessFile open(String mode) throws IOException {
            return new DefaultLuaRandomAccessFile(new RandomAccessFile(delegate.toAbsolutePath().toString(), mode), this);
        }

        @Override
        public InputStream openInput() throws IOException {
            if (isDir()) {
                throw new IOException("cant open directory for reading");
            }

            return new FileInputStream(delegate.toAbsolutePath().toString());
        }

        @Override
        public OutputStream openOutput(boolean append) throws IOException {
            if (!exists()) {
                LuaPath parent = parent();
                if (!parent.isDir()) {
                    parent.mkdirs();
                }

                createNewFile();
            }

            if (isDir()) {
                throw new IOException("cant open directory for writing");
            }

            return new FileOutputStream(delegate.toAbsolutePath().toString(), append);
        }

        @Override
        public long size() throws IOException {
            return Files.size(delegate);
        }

        @Override
        public void delete() throws IOException {
            Files.delete(delegate);
        }

        @Override
        public void deleteOnExit() {
            delegate.toFile().deleteOnExit();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DefaultLuaPath that = (DefaultLuaPath) o;

            return delegate.equals(that.delegate);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }


}
