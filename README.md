OpenComputers
=============
OpenComputers is a Minecraft mod that adds programmable computers and robots to the game. Computers and robots are programmed in Lua and are fully persistent, meaning programs will continue running across reloads. To achieve this the mod comes with a native library. This limits the platforms with full support to those the library is available for, which at this time are: Windows, Linux and MacOS. On systems the native library is not available for, the mod will fall back to using LuaJ, which means computers will *not* persist and won't be limited in how much memory they use.

For more information on the mod, please [see the wiki](https://github.com/MightyPirates/OpenComputers/wiki). If you still have questions, visit the [community forums](http://oc.cil.li/).

Contributing
------------
If you'd like to contribute, the easiest way to do so is to provide a translation. See [`assets/opencomputers/lang`](https://github.com/MightyPirates/OpenComputers/tree/master/src/main/resources/assets/opencomputers/lang) for already existing translations. New translations should be based on the English localization, since it will usually be the most up-to-date.

If you'd like to contribute code, please have a look at the [code conventions](https://github.com/MightyPirates/OpenComputers/wiki/CodeConventions). If you plan to make a big contribution, use common sense and ask first and talk things through with me.

You can also implement your own item and block components using [the public API](https://github.com/MightyPirates/OpenComputers/tree/master/src/main/java/li/cil/oc/api), which unlike the rest of the mod is in plain Java, so you don't have to know or learn Scala.

If you encounter any bugs, please report them [in the issue tracker](https://github.com/MightyPirates/OpenComputers/issues?state=open), if they have not already been reported. If you report a crash, always provide your log file.

If you wish to discuss or suggest a new feature, the [forums](http://oc.cil.li//index.php?/forum/22-feedback-and-suggestions/) are a good place for that.

Building
========
Java
----
You'll need a Forge development environment set up with support for Scala. There are no dependencies other than the bundled APIs. Compile it like any other mod.

Natives
-------
You'll usually not have to go through this, since the compiled library is in the repository for the more common operating systems (Windows, Linux and MacOS). This is here for reference, and in case you don't trust precompiled binaries.

First clone [Eris](https://github.com/fnuecke/eris). Build it using the provided makefile or manually.

For example, for Windows I used the command line compiler that comes with VS2012 with the following script:
```cmd
cl.exe /nologo /c /O2 /MT *.c
del lua.obj
del luac.obj
lib.exe /nologo /LTCG /OUT:lua52.lib *.obj 
del *.obj
```

For Linux use the makefile using the linux target.

For MacOS use the macosx target.


Next, clone the [Eris Branch of JNLua](https://github.com/fnuecke/jnlua/tree/eris). Build it using the library built in step one. Copy the library generated in the first step to the `src\main\c` folder, as well as the includes to an `include` subfolder. For Linux and MacOS I named the library according to its format, i.e. `liblua.32.a` and `liblua.64.a`. For Windows I just replaced the file as necessary, because I suck at batch scripting.

For example, for Windows I again used the command line compiler of VS2012:
```cmd
cl.exe /nologo /c /O2 /MT jnlua.c /Iinclude
link.exe /nologo /OUT:native.dll /DLL jnlua.obj /LIBPATH:. lua52.lib 
del native.exp
del native.lib
del jnlua.obj
```
Then rename the `native.dll` according to its format (i.e. native.32.dll or native.64.dll).

For Linux I used the following script:
```sh
FLAGS="-fno-strict-aliasing -fPIC -O2 -Wall -DNDEBUG -D_REENTRANT -DLUA_USE_LINUX -s"
ARCH=32
JDK_DIR="/usr/lib/jvm/java-6-openjdk"
INCLUDES="-I$JDK_DIR/include -I$JDK_DIR/include/linux -Iinclude"
gcc -c $FLAGS -m$ARCH $INCLUDES jnlua.c -ojnlua.o
gcc -m$ARCH -shared -Wl,-soname=native.$ARCH.so -onative.$ARCH.so jnlua.o liblua.$ARCH.a
strip --strip-unneeded native.$ARCH.so
```

For MacOS I used a slightly modified version of the Linux script, which I don't have handy anymore because I don't have the Mac Mini I built it on here, but it should've been *similar* to this:
```sh
FLAGS="-fno-strict-aliasing -fPIC -O2 -Wall -DNDEBUG -D_REENTRANT -DLUA_USE_MACOSX -s"
ARCH=32
JDK_DIR="System/Library/Frameworks/JavaVM.framework/Headers"
INCLUDES="-I$JDK_DIR -I$JDK_DIR/linux -Iinclude"
gcc $FLAGS -m$ARCH $INCLUDES jnlua.c liblua.$ARCH.a
gcc -m$ARCH -shared -onative.$ARCH.dylib jnlua.o
```

Perform these steps twice, once for the 32 bit and once for the 64 bit version of the library. Adjust scripts as necessary (e.g. Lua makefile).
