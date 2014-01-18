/*******************************************************************************
* Copyright (c) 2008 LuaJ. All rights reserved.
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
package org.luaj.vm2.script;

import java.util.Arrays;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

/**
 * Jsr 223 scripting engine factory
 */
public class LuaScriptEngineFactory implements ScriptEngineFactory {
    
 	private static final String [] EXTENSIONS = {
 		"lua",
 		".lua",
 	};
    
    private static final String [] MIMETYPES = {
        "text/lua",
        "application/lua"
    };
    
    private static final String [] NAMES = {
        "lua", 
        "luaj",
    };
    
    private static final ThreadLocal<ScriptEngine> engines 
		= new ThreadLocal<ScriptEngine>();
    private List<String> extensions;
    private List<String> mimeTypes;
    private List<String> names;

    
    public LuaScriptEngineFactory() {
        extensions = Arrays.asList(EXTENSIONS);
        mimeTypes = Arrays.asList(MIMETYPES);
        names = Arrays.asList(NAMES);
    }
    
    public String getEngineName() {
        return getScriptEngine().get(ScriptEngine.ENGINE).toString();
    }
    
    public String getEngineVersion() {
        return getScriptEngine().get(ScriptEngine.ENGINE_VERSION).toString();
    }
    
    public List<String> getExtensions() {
        return extensions;
    }
    
    public List<String> getMimeTypes() {
        return mimeTypes;
    }
    
    public List<String> getNames() {
        return names;
    }
    
    public String getLanguageName() {
        return getScriptEngine().get(ScriptEngine.LANGUAGE).toString();
    }
    
    public String getLanguageVersion() {
        return getScriptEngine().get(ScriptEngine.LANGUAGE_VERSION).toString();
    }
    
    public Object getParameter(String key) {
        return getScriptEngine().get(key).toString();
    }
    
    public String getMethodCallSyntax(String obj, String m, String... args)  {
        StringBuffer sb = new StringBuffer();
        sb.append(obj + ":" + m + "(");
        int len = args.length;
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(args[i]);
        }
        sb.append(")");
        return sb.toString();
    }
    
    public String getOutputStatement(String toDisplay) {
        return "print(" + toDisplay + ")";
    }
    
    public String getProgram(String ... statements) {
        StringBuffer sb = new StringBuffer();
        int len = statements.length;
        for (int i = 0; i < len; i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(statements[i]);
        }
        return sb.toString();
    }
    
    public ScriptEngine getScriptEngine() {
    	ScriptEngine eng = engines.get();
    	if ( eng == null ) {
    		eng = new LuaScriptEngine();
	        engines.set(eng);
    	}
		return eng;
    }
}
