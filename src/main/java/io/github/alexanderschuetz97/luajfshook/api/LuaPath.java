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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.InvalidPathException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;

/**
 * Interface for a virtual lua path and operations that can be performed on a path.
 */
public interface LuaPath {

    /**
     * File or directory name of the element referred to by this LuaPath
     */
    String name();

    /**
     * returns true if this LuaPath is absolute.
     */
    boolean isAbsolute();

    /**
     * returns the absolute path of this LuaPath.
     * Note that this call may return different results when this is not absolute and the workDirectory of the FileSystem changes.
     */
    LuaPath absolutePath();

    /**
     * returns the real path (symlinks resolved) of this LuaPath.
     * May return "this" in case the fs does not support symlinks.
     */
    LuaPath realPath() throws IOException;

    /**
     * constructs a canon path. I.e without ".." inside of it
     * Example a/b/../d -> a/d
     */
    LuaPath canon() throws IOException;

    /**
     * creates a child path of this path
     * @param name name of the child
     * @return a child path to this path.
     */
    LuaPath child(String name) throws InvalidPathException;

    /**
     * Constructs a relative path from this path to other.
     * Example this is a/b and other is a/b/c/d then this would return c/d as relative path.
     */
    LuaPath relative(LuaPath other);

    /**
     * Places a hard link to the target.
     * The link is placed AT the location referred to by THIS path.
     */
    void link(LuaPath to) throws IOException;

    /**
     * Places a hard link to the target.
     * The link is placed AT the location referred to by THIS path.
     */
    void symlink(LuaPath to) throws IOException;

    /**
     * resolves the other path against this path.
     * If other is absolute then other is directly returned
     *
     */
    LuaPath child(LuaPath other);

    /**
     * returns the attributes of file/dir referred to by this path.
     */
    BasicFileAttributes attributes() throws FileNotFoundException, IOException;

    /**
     * returns the attributes of file/dir referred to by this path and does not follow symlinks.
     */
    BasicFileAttributes linkAttributes() throws FileNotFoundException, IOException;

    /**
     * Some values may be ignored or this method may be noop if the fs does not support this.
     */
    void setFileTimes(FileTime lastModifiedTime, FileTime lastAccessTime, FileTime createTime) throws IOException;

    /**
     * returns the relative path of this LuaPath.
     */
    String path();

    /**
     * Returns the parent of this LuaPath or null if this LuaPath refers to the root.
     */
    LuaPath parent();

    /**
     * copies the file referred to by this path to the target path.
     * The target path is created if it does not exists.
     * If the target already is a file then it is replaced.
     */
    void copyFile(LuaPath target) throws IOException;

    /**
     * move the file referred to by this path to the target path.
     * If the target already is a file then it is replaced.
     */
    void moveFile(LuaPath target) throws IOException;

    /**
     * moves the file or directory referred to by this path to the target path.
     * If the target already is a file then it is replaced.
     * If the target already is a directory then it must be empty.
     */
    void move(LuaPath tar) throws IOException;

    /**
     * Does this path refer to something that exists?
     */
    boolean exists();

    /**
     * Does this path refer to a directory
     */
    boolean isDir();

    /**
     * is it a file?
     */
    boolean isFile();

    /**
     * returns true if this Path refers to a link.
     */
    boolean isĹink();

    /**
     * Lists all children of this path
     */
    List<LuaPath> list() throws NotDirectoryException, IOException;

    interface LuaFileVisitor {
        /**
         * called before a directory is entered.
         */
        FileVisitResult preVisitDirectory(LuaPath dir) throws IOException;

        /**
         * called when a file or directory is visited.
         * Note: directories are only visited when they are NOT entered because the maximum depth has been reached.
         */
        FileVisitResult visitFile(LuaPath dir) throws IOException;

        /**
         * called when the directory is left.
         */
        FileVisitResult postVisitDirectory(LuaPath dir) throws IOException;
    }

    /**
     * walks the file tree and calls the given visitor
     */
    void walkFileTree(int depth, boolean followLinks, LuaFileVisitor visitor) throws IOException;

    /**
     * Creates a new empty file.
     */
    void createNewFile() throws FileAlreadyExistsException, IOException, NotDirectoryException;

    /**
     * Creates a directory
     *
     * @throws FileAlreadyExistsException if the file already exists
     * @throws IOException if an io error occurs
     * @throws NotDirectoryException if parent does not exist or is a file
     */
    void mkdir() throws FileAlreadyExistsException, IOException, NotDirectoryException;

    /**
     * Creates this path as a folder as well as any necesarry parent folders that do not exist.
     *
     * @throws FileAlreadyExistsException if the file already exists
     * @throws IOException if an io error occurs
     * @throws NotDirectoryException if any parent is a file
     */
    void mkdirs() throws FileAlreadyExistsException, IOException, NotDirectoryException;

    /**
     * returns a system path that this lua path refers to.
     * This may return null if this LuaPath does not have a System path representation
     */
    Path toSystemPath();

    /**
     * Opens the file referred to by this path. r, rw or w
     * @throws IOException if an io error occurs
     */
    LuaRandomAccessFile open(String mode) throws IOException;

    /**
     * Opens the file referred to by this LuaPath as an InputStream.
     * @throws IOException if an io error occurs
     * @throws FileNotFoundException if the file does not exist
     */
    InputStream openInput() throws FileNotFoundException, IOException;

    /**
     * If the file does not exists it is created.
     */
    OutputStream openOutput(boolean append) throws IOException;

    /**
     * Returns the file size for the file that this lua path refers to
     */
    long size() throws IOException;

    /**
     * Deletes the file or directory that this lua path refers to.
     * Depending on the FileSystem directories may have to be empty for deletion to succeed.
     */
    void delete() throws IOException;

    /**
     * Deletes the file when the JVM exits. There is no way to undo this.
     */
    void deleteOnExit();
}
