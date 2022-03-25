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

import org.luaj.vm2.lib.ResourceFinder;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

/**
 * Interface for a file system handler that manages virtual {@link LuaPath} instances
 */
public interface LuaFileSystemHandler extends ResourceFinder {

    /**
     * Resolve a path in the current work directory.
     * If the path is determined to be absolute then the returned File is not affected by the work directory.
     * If it is relative then it is dependent on the work directory.
     *
     * @param path abstract or relative path.
     * @throws NullPointerException if filename is null
     * @return an absolute Path that is resolved to the work directory
     */
    LuaPath resolvePath(String path) throws InvalidPathException;

    /**
     * returns a relative path for the given path name
     * @throws NullPointerException if filename is null
     */
    LuaPath relativePath(String path) throws InvalidPathException;

    /**
     * resolves a system path to a lua path. This may return null if the path is outside of the fs.
     * The returned LuaPath is if not null always absolute.
     *
     * If the input path is relative it must be treated as relative to the JVM Process work directory.
     */
    LuaPath resolveSysPath(Path path);

    /**
     * returns the current work directory.
     */
    LuaPath getWorkDirectory();


    /**
     * Creates a temporary file with the prefix/suffix in the fs. (both can be null for default values)
     */
    LuaPath tmpFile(String prefix, String suffix) throws IOException;

    /**
     * Returns the temporary folder where temporary files should be put.
     */
    LuaPath tmpDir() throws IOException;

    /**
     * changes the current work directory to the given file.
     *
     * @throws NullPointerException if file is null
     * @throws NotDirectoryException if the given file does not point ot a directory
     * @throws IOException if anything else goes wrong when trying to set the work dir to the given path.
     */
    void setWorkDirectory(LuaPath path) throws NotDirectoryException, IOException;
}
