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
import io.github.alexanderschuetz97.luajfshook.api.LuajFSHook;
import io.github.alexanderschuetz97.luajfshook.impl.DefaultLuaFileSystemHandler;
import org.junit.Assert;
import org.junit.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.lib.jse.JsePlatform;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class LuajFSHookTest {

    @Test
    public void testLoadFileAndDofile() {
        Globals gl = JsePlatform.standardGlobals();
        TestHandler th = new TestHandler();

        Assert.assertTrue(LuajFSHook.install(gl, th));
        gl.load("loadfile('beepboop')").call();
        Assert.assertEquals("beepboop", th.resource);
        try {
            gl.load("dofile('beepboop2')").call();
            Assert.fail();
        } catch (LuaError luaError) {
            //EXPECTED
        }
        Assert.assertEquals("beepboop2", th.resource);
    }

    @Test
    public void testExecutePWD() throws IOException, InterruptedException {
        DefaultLuaFileSystemHandler th = new DefaultLuaFileSystemHandler();
        th.setWorkDirectory(th.resolvePath("/tmp"));
        Globals gl = JsePlatform.standardGlobals();
        ExecutorService ex = Executors.newCachedThreadPool();

        Assert.assertTrue(LuajFSHook.install(gl, th, ex));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        gl.STDOUT = new PrintStream(baos);
        gl.load("os.execute('pwd')").call();
        ex.shutdown();
        Assert.assertTrue(ex.awaitTermination(5000, TimeUnit.MILLISECONDS));
        Assert.assertEquals("/tmp\n", baos.toString());
    }


    class TestHandler extends DefaultLuaFileSystemHandler {

        private String resource;

        @Override
        public InputStream findResource(String filename) {
            resource = filename;
            return null;
        }
    }
}
