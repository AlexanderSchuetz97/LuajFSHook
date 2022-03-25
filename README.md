# LuajFSHook
Hook for Luaj's JseIoLib and other file related functions for more control over how luaj interacts with the file system.
<br>LuajFSHook provides an API that can be used to 
1. Improve interoperability between lua libraries that use the file system
2. To more easily Sanbox/Jail Luaj's IO lib without having to reimplement the IO Library
3. Implement a virtual file system to be used by lua that is completely unrelated to the real filesystem.

## License
LuajFSHook is released under the GNU Lesser General Public License Version 3. <br>
A copy of the GNU Lesser General Public License Version 3 can be found in the COPYING & COPYING.LESSER files.<br>

## Requirements
* Java 7 or newer
* LuaJ 3.0.1

## Usage:
Maven:
````
<dependency>
  <groupId>io.github.alexanderschuetz97</groupId>
  <artifactId>luajfshook</artifactId>
  <version>1.0</version>
</dependency>
````

In Java:
````
Globals globals = JsePlatform.standardGlobals();
//This will overwrite Globals.finder!
LuajFSHook.install(globals);
//This may be used by your library
LuaFileSystemHandler handler = LuajFSHook.get(globals);

//Example: Lua has a different workdir compared to the jvm

//First we create the "subfolder" which will serve as our new work dir.
new File(new File("."), "luaworkdir").mkdir();

//We get the current work dir that lua uses. This will be equal to "."
LuaPath workDir = handler.getWorkDir().child("luaworkdir");
//Now we change the work dir for lua to the folder we made earlier
handler.setWorkDirectory(workDir.child("luaworkdir"));

//Now we run our lua script the provided example will just create a file.
globals.load(new InputStreamReader(new FileInputStream("test.lua")), "test.lua").call();
//The lua script should have created the file "./luaworkdir/test.txt" 
````
In test.lua:
````
local io = require('io')
local file = io.open("test.txt", "w+")
file:write("Hello World")
file:close()
````
