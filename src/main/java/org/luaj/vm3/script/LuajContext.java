/*******************************************************************************
* Copyright (c) 2013 LuaJ. All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:
*
* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.
* 
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
******************************************************************************/
package org.luaj.vm3.script;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Writer;

import javax.script.ScriptContext;
import javax.script.SimpleScriptContext;

import org.luaj.vm3.Globals;
import org.luaj.vm3.lib.jse.JsePlatform;

/** 
 * Context for LuaScriptEngine execution which maintains its own Globals, 
 * and manages the input and output redirection.
 */
public class LuajContext extends SimpleScriptContext implements ScriptContext {

	/** Globals for this context instance. */
	public final Globals globals;

	/** The initial value of globals.STDIN */
	private final InputStream stdin;
	/** The initial value of globals.STDOUT */
	private final PrintStream stdout;
	/** The initial value of globals.STDERR */
	private final PrintStream stderr;
	
	/** Construct a LuajContext with its own globals which may
	 * be debug globals depending on the value of the system
	 * property 'org.luaj.debug'
	 * <p>
	 * If the system property 'org.luaj.debug' is set, the globals
	 * created will be a debug globals that includes the debug 
	 * library.  This may provide better stack traces, but may 
	 * have negative impact on performance.
	 */
	public LuajContext() {
		this("true".equals(System.getProperty("org.luaj.debug")),
			"true".equals(System.getProperty("org.luaj.luajc")));
	}

	/** Construct a LuajContext with its own globals, which
	 * which optionally are debug globals, and optionally use the
	 * luajc direct lua to java bytecode compiler.
	 * <p>
	 * If createDebugGlobals is set, the globals
	 * created will be a debug globals that includes the debug 
	 * library.  This may provide better stack traces, but may 
	 * have negative impact on performance.
	 * @param createDebugGlobals true to create debug globals, 
	 * false for standard globals.
	 * @param useLuaJCCompiler true to use the luajc compiler, 
	 * reqwuires bcel to be on the class path.
	 */
	public LuajContext(boolean createDebugGlobals, boolean useLuaJCCompiler) {
		globals = createDebugGlobals?
    		JsePlatform.debugGlobals():
    		JsePlatform.standardGlobals();
    	stdin = globals.STDIN;
    	stdout = globals.STDOUT;
    	stderr = globals.STDERR;
	}
	
	@Override
	public void setErrorWriter(Writer writer) {
		globals.STDERR = writer != null?
				new PrintStream(new WriterOutputStream(writer)):
				stderr;
	}

	@Override
	public void setReader(Reader reader) {
		globals.STDIN = reader != null?
				new ReaderInputStream(reader):
				stdin;
	}

	@Override
	public void setWriter(Writer writer) {
		globals.STDOUT = writer != null?
				new PrintStream(new WriterOutputStream(writer), true):
				stdout;
	}

	static final class WriterOutputStream extends OutputStream {
		final Writer w;
		WriterOutputStream(Writer w) {
			this.w = w;
		}
		public void write(int b) throws IOException {
			w.write(new String(new byte[] {(byte)b}));
		}
		public void write(byte[] b, int o, int l) throws IOException {
			w.write(new String(b, o, l));
		}
		public void write(byte[] b) throws IOException {
			w.write(new String(b));
		}
		public void close() throws IOException {
			w.close();
		}
		public void flush() throws IOException {
			w.flush();
		}
	}
	
	static final class ReaderInputStream extends InputStream {
		final Reader r;
		ReaderInputStream(Reader r) {
			this.r = r;
		}
		public int read() throws IOException {
			return r.read();
		}
	}
}
