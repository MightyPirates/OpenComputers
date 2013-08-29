/*
 * $Id: CompiledLuaScript.java 38 2012-01-04 22:44:15Z andre@naef.com $
 * See LICENSE.txt for license terms.
 */

package com.naef.jnlua.script;

import java.io.ByteArrayInputStream;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Compiled script implementation conforming to JSR 223: Scripting for the Java
 * Platform.
 */
class CompiledLuaScript extends CompiledScript {
	// -- State
	private LuaScriptEngine engine;
	private byte[] script;

	// -- Construction
	/**
	 * Creates a new instance.
	 */
	public CompiledLuaScript(LuaScriptEngine engine, byte[] script) {
		this.engine = engine;
		this.script = script;
	}

	// -- CompiledScript methods
	@Override
	public Object eval(ScriptContext context) throws ScriptException {
		synchronized (engine.getLuaState()) {
			engine.loadChunk(new ByteArrayInputStream(script), context, "b");
			return engine.callChunk(context);
		}
	}

	@Override
	public ScriptEngine getEngine() {
		return engine;
	}
}
