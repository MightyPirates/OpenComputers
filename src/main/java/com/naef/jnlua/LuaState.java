/*
 * $Id: LuaState.java 157 2012-10-05 23:00:17Z andre@naef.com $
 * See LICENSE.txt for license terms.
 */

package com.naef.jnlua;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

import com.naef.jnlua.JavaReflector.Metamethod;

/**
 * JNLua core class representing a Lua instance.
 * 
 * <p>
 * The class performs extensive checking on all arguments and its state.
 * Specifically, the following exceptions are thrown under the indicated
 * conditions:
 * </p>
 * 
 * <table class="doc">
 * <tr>
 * <th>Exception</th>
 * <th>When</th>
 * </tr>
 * <tr>
 * <td>{@link java.lang.NullPointerException}</td>
 * <td>if an argument is <code>null</code> and the API does not explicitly
 * specify that the argument may be <code>null</code></td>
 * </tr>
 * <tr>
 * <td>{@link java.lang.IllegalStateException}</td>
 * <td>if the Lua state is closed and the API does not explicitly specify that
 * the method may be invoked on a closed Lua state</td>
 * </tr>
 * <tr>
 * <td>{@link java.lang.IllegalArgumentException}</td>
 * <td>if a stack index refers to a non-valid stack location and the API does
 * not explicitly specify that the stack index may be non-valid</td>
 * </tr>
 * <tr>
 * <td>{@link java.lang.IllegalArgumentException}</td>
 * <td>if a stack index refers to an stack location with value type that is
 * different from the value type explicitly specified in the API</td>
 * </tr>
 * <tr>
 * <td>{@link java.lang.IllegalArgumentException}</td>
 * <td>if a count is negative or out of range and the API does not explicitly
 * specify that the count may be negative or out of range</td>
 * </tr>
 * <tr>
 * <td>{@link com.naef.jnlua.LuaRuntimeException}</td>
 * <td>if a Lua runtime error occurs</td>
 * </tr>
 * <tr>
 * <td>{@link com.naef.jnlua.LuaSyntaxException}</td>
 * <td>if the syntax of a Lua chunk is incorrect</td>
 * </tr>
 * <tr>
 * <td>{@link com.naef.jnlua.LuaMemoryAllocationException}</td>
 * <td>if the Lua memory allocator runs out of memory or if a JNI allocation
 * fails</td>
 * </tr>
 * <tr>
 * <td>{@link com.naef.jnlua.LuaGcMetamethodException}</td>
 * <td>if an error occurs running a <code>__gc</code> metamethod during garbage
 * collection</td>
 * </tr>
 * <tr>
 * <td>{@link com.naef.jnlua.LuaMessageHandlerException}</td>
 * <td>if an error occurs running the message handler of a protected call</td>
 * </tr>
 * </table>
 */
public class LuaState {
	// -- Static
	/**
	 * Multiple returns pseudo return value count.
	 */
	public static final int MULTRET = -1;

	/**
	 * Registry pseudo-index.
	 */
	public static final int REGISTRYINDEX;

	/**
	 * OK status.
	 * 
	 * @since JNLua 1.0.0
	 */
	public static final int OK = 0;

	/**
	 * Status indicating that a thread is suspended.
	 */
	public static final int YIELD = 1;

	/**
	 * Registry index of the main thread.
	 * 
	 * @since JNLua 1.0.0
	 */
	public static final int RIDX_MAINTHREAD = 1;

	/**
	 * Registry index of the global environment.
	 * 
	 * @since JNLua 1.0.0
	 */
	public static final int RIDX_GLOBALS = 2;

	/**
	 * The JNLua version. The format is &lt;major&gt;.&lt;minor&gt;.
	 */
	public static final String VERSION = "1.0";

	/**
	 * The Lua version. The format is &lt;major&gt;.&lt;minor&gt;.
	 */
	public static final String LUA_VERSION;

	static {
		NativeSupport.getInstance().getLoader().load();
		REGISTRYINDEX = lua_registryindex();
		LUA_VERSION = lua_version();
	}

	/**
	 * The API version.
	 */
	private static final int APIVERSION = 3;

	// -- State
	/**
	 * Whether the <code>lua_State</code> on the JNI side is owned by the Java
	 * state and must be closed when the Java state closes.
	 */
	private boolean ownState;

	/**
	 * The <code>lua_State</code> pointer on the JNI side. <code>0</code>
	 * implies that this Lua state is closed. The field is modified exclusively
	 * on the JNI side and must not be touched on the Java side.
	 */
	private long luaState;

	/**
	 * The <code>lua_State</code> pointer on the JNI side for the running
	 * coroutine. This field is modified exclusively on the JNI side and must
	 * not be touched on the Java side.
	 */
	private long luaThread;

	/**
	 * The yield flag. This field is modified from both the JNI side and Java
	 * side and signals a pending yield.
	 */
	private boolean yield;

	/**
	 * The maximum amount of memory the may be used by the Lua state, in bytes.
	 * This can be adjusted to limit the amount of memory a state may use. If
	 * it is reduced while a VM is active this can very quickly lead to out of
	 * memory errors.
	 */
	private int luaMemoryTotal;

	/**
	 * The amount of memory currently used by the Lua state, in bytes. This is
	 * set from the JNI side and must not be modified from the Java side.
	 */
	private int luaMemoryUsed;
	
	/**
	 * Ensures proper finalization of this Lua state.
	 */
	private Object finalizeGuardian;

	/**
	 * The class loader for dynamically loading classes.
	 */
	private ClassLoader classLoader;

	/**
	 * Reflects Java objects.
	 */
	private JavaReflector javaReflector;

	/**
	 * Converts between Lua types and Java types.
	 */
	private Converter converter;

	/**
	 * Set of Lua proxy phantom references for pre-mortem cleanup.
	 */
	private Set<LuaValueProxyRef> proxySet = new HashSet<LuaValueProxyRef>();

	/**
	 * Reference queue for pre-mortem cleanup.
	 */
	private ReferenceQueue<LuaValueProxyImpl> proxyQueue = new ReferenceQueue<LuaValueProxyImpl>();

	// -- Construction
	/**
	 * Creates a new instance. The class loader of this Lua state is set to the
	 * context class loader of the calling thread. The Java reflector and the
	 * converter are initialized with the default implementations. The Lua
	 * state may allocate as much memory as it wants.
	 * 
	 * @see #getClassLoader()
	 * @see #setClassLoader(ClassLoader)
	 * @see #getJavaReflector()
	 * @see #setJavaReflector(JavaReflector)
	 * @see #getConverter()
	 * @see #setConverter(Converter)
	 */
	public LuaState() {
		this(0L, 0);
	}

	/**
	 * Creates a new instance. The class loader of this Lua state is set to the
	 * context class loader of the calling thread. The Java reflector and the
	 * converter are initialized with the default implementations. The Lua
	 * state may allocate only as much memory as specified. This is enforced
	 * by a custom allocator that is only used if a maximum memory is given.
	 * 
	 * @param memory
	 *            the maximum amount of memory this Lua state may use, in bytes
	 * @see #getClassLoader()
	 * @see #setClassLoader(ClassLoader)
	 * @see #getJavaReflector()
	 * @see #setJavaReflector(JavaReflector)
	 * @see #getConverter()
	 * @see #setConverter(Converter)
	 */
	public LuaState(int memory) {
		this(0L, validateMemory(memory));
	}

	/**
	 * Creates a new instance.
	 */
	private LuaState(long luaState, int memory) {
		ownState = luaState == 0L;
		luaMemoryTotal = memory;
		lua_newstate(APIVERSION, luaState);
		check();

		// Create a finalize guardian
		finalizeGuardian = new Object() {
			@Override
			public void finalize() {
				synchronized (LuaState.this) {
					closeInternal();
				}
			}
		};

		// Add metamethods
		for (int i = 0; i < JavaReflector.Metamethod.values().length; i++) {
			final JavaReflector.Metamethod metamethod = JavaReflector.Metamethod
					.values()[i];
			lua_pushjavafunction(new JavaFunction() {
				@Override
				public int invoke(LuaState luaState) {
					JavaFunction javaFunction = getMetamethod(
							luaState.toJavaObjectRaw(1), metamethod);
					if (javaFunction != null) {
						return javaFunction.invoke(LuaState.this);
					} else {
						throw new UnsupportedOperationException(
								metamethod.getMetamethodName());
					}
				}
			});
			lua_setfield(-2, metamethod.getMetamethodName());
		}
		lua_pop(1);

		// Set fields
		classLoader = Thread.currentThread().getContextClassLoader();
		javaReflector = DefaultJavaReflector.getInstance();
		converter = DefaultConverter.getInstance();
	}

	// -- Properties
	/**
	 * Returns the class loader of this Lua state. The class loader is used for
	 * dynamically loading classes.
	 * 
	 * <p>
	 * The method may be invoked on a closed Lua state.
	 * </p>
	 * 
	 * @return the class loader
	 */
	public synchronized ClassLoader getClassLoader() {
		return classLoader;
	}

	/**
	 * Sets the class loader of this Lua state. The class loader is used for
	 * dynamically loading classes.
	 * 
	 * <p>
	 * The method may be invoked on a closed Lua state.
	 * </p>
	 * 
	 * @param classLoader
	 *            the class loader to set
	 */
	public synchronized void setClassLoader(ClassLoader classLoader) {
		if (classLoader == null) {
			throw new NullPointerException();
		}
		this.classLoader = classLoader;
	}

	/**
	 * Returns the Java reflector of this Lua state.
	 * 
	 * <p>
	 * The method may be invoked on a closed Lua state.
	 * </p>
	 * 
	 * @return the Java reflector converter
	 */
	public synchronized JavaReflector getJavaReflector() {
		return javaReflector;
	}

	/**
	 * Sets the Java reflector of this Lua state.
	 * 
	 * <p>
	 * The method may be invoked on a closed Lua state.
	 * </p>
	 * 
	 * @param javaReflector
	 *            the Java reflector
	 */
	public synchronized void setJavaReflector(JavaReflector javaReflector) {
		if (javaReflector == null) {
			throw new NullPointerException();
		}
		this.javaReflector = javaReflector;
	}

	/**
	 * Returns a metamethod for a specified object. If the object implements the
	 * {@link com.naef.jnlua.JavaReflector} interface, the metamethod is first
	 * queried from the object. If the object provides the requested metamethod,
	 * that metamethod is returned. Otherwise, the method returns the metamethod
	 * provided by the Java reflector configured in this Lua state.
	 * 
	 * <p>
	 * Clients requiring access to metamethods should go by this method to
	 * ensure consistent class-by-class overriding of the Java reflector.
	 * </p>
	 * 
	 * @param obj
	 *            the object, or <code>null</code>
	 * @return the Java reflector
	 */
	public synchronized JavaFunction getMetamethod(Object obj,
			Metamethod metamethod) {
		if (obj != null && obj instanceof JavaReflector) {
			JavaFunction javaFunction = ((JavaReflector) obj)
					.getMetamethod(metamethod);
			if (javaFunction != null) {
				return javaFunction;
			}
		}
		return javaReflector.getMetamethod(metamethod);
	}

	/**
	 * Returns the converter of this Lua state.
	 * 
	 * <p>
	 * The method may be invoked on a closed Lua state.
	 * </p>
	 * 
	 * @return the converter
	 */
	public synchronized Converter getConverter() {
		return converter;
	}

	/**
	 * Sets the converter of this Lua state.
	 * 
	 * <p>
	 * The method may be invoked on a closed Lua state.
	 * </p>
	 * 
	 * @param converter
	 *            the converter
	 */
	public synchronized void setConverter(Converter converter) {
		if (converter == null) {
			throw new NullPointerException();
		}
		this.converter = converter;
	}

	// -- Memory
	/**
	 * Returns the maximum memory consumption of this Lua state. This is the
	 * maximum raw memory Lua may allocate for this state, in bytes.
	 * 
	 * @return the maximum memory consumption
	 */
	public synchronized int getTotalMemory() {
		return luaMemoryTotal;
	}

	/**
	 * Sets the maximum amount of memory this Lua state may allocate. This is
	 * the size of the raw memory the Lua library may allocate for this tate,
	 * in bytes. Note that you can only set the maximum memory consumption for
	 * states that were created to enforce a maximum memory consumption.
	 * 
	 * @param value
	 *            the new maximum memory size this state may allocate
	 */
	public synchronized void setTotalMemory(int value) {
		if (luaMemoryTotal < 1) {
			throw new IllegalStateException("cannot set maximum memory for this state");
		}
		luaMemoryTotal = validateMemory(value);
	}

	/**
	 * Returns the current amount of unused memory by this Lua state. This is
	 * the size of the total available memory minus the raw memory currently
	 * allocated by this state, in bytes.
	 * 
	 * This is guaranteed to be less or equal to {@link #getTotalMemory()} and
	 * larger or equal to zero.
	 * 
	 * This only returns something not zero if a maximum memory consumption is
	 * enforced by this state. Otherwise it will always return zero. 
	 * 
	 * @return the current memory consumption
	 */
	public synchronized int getFreeMemory() {
		// This is the reason we use free amount instead of used amount: if we
		// lower the max memory we can get below used memory, which would be
		// weird; so we just say free memory is zero, which is more intuitive
		// and true at the same time.
		return Math.max(0, luaMemoryTotal - luaMemoryUsed);
	}

	// -- Life cycle
	/**
	 * Returns whether this Lua state is open.
	 * 
	 * <p>
	 * The method may be invoked on a closed Lua state.
	 * </p>
	 * 
	 * @return whether this Lua state is open
	 */
	public final synchronized boolean isOpen() {
		return isOpenInternal();
	}

	/**
	 * Closes this Lua state and releases all resources.
	 * 
	 * <p>
	 * The method may be invoked on a closed Lua state and has no effect in that
	 * case.
	 * </p>
	 */
	public synchronized void close() {
		closeInternal();
	}

	/**
	 * Performs a garbage collection operation. Please see the Lua Reference
	 * Manual for an explanation of the actions, arguments and return values.
	 * 
	 * @param what
	 *            the operation to perform
	 * @param data
	 *            the argument required by some operations
	 * @return a return value depending on the GC operation performed
	 */
	public synchronized int gc(GcAction what, int data) {
		check();
		return lua_gc(what.ordinal(), data);
	}

	// -- Registration
	/**
	 * Opens the specified library in this Lua state. The library is pushed onto
	 * the stack.
	 * 
	 * @param library
	 *            the library
	 */
	public synchronized void openLib(Library library) {
		check();
		library.open(this);
	}

	/**
	 * Opens the Lua standard libraries and the JNLua Java module in this Lua
	 * state.
	 * 
	 * <p>
	 * The method opens all libraries defined by the {@link Library}
	 * enumeration.
	 * </p>
	 */
	public synchronized void openLibs() {
		check();
		for (Library library : Library.values()) {
			library.open(this);
			pop(1);
		}
	}

	/**
	 * Registers a named Java function as a global variable.
	 * 
	 * @param namedJavaFunction
	 *            the Java function to register
	 */
	public synchronized void register(NamedJavaFunction namedJavaFunction) {
		check();
		String name = namedJavaFunction.getName();
		if (name == null) {
			throw new IllegalArgumentException("anonymous function");
		}
		pushJavaFunction(namedJavaFunction);
		setGlobal(name);
	}

	/**
	 * Registers a module and pushes the module on the stack. Optionally, a
	 * module can be registered globally. As of Lua 5.2, modules are <i>not</i>
	 * expected to set global variables anymore.
	 * 
	 * @param moduleName
	 *            the module name
	 * @param namedJavaFunctions
	 *            the Java functions of the module
	 * @param global
	 *            whether to register the module globally
	 */
	public synchronized void register(String moduleName,
			NamedJavaFunction[] namedJavaFunctions, boolean global) {
		check();
		/*
		 * The following code corresponds to luaL_requiref() and must be kept in
		 * sync. The original code cannot be called due to the necessity of
		 * pushing each C function with an individual closure.
		 */
		newTable(0, namedJavaFunctions.length);
		for (int i = 0; i < namedJavaFunctions.length; i++) {
			String name = namedJavaFunctions[i].getName();
			if (name == null) {
				throw new IllegalArgumentException(String.format(
						"anonymous function at index %d", i));
			}
			pushJavaFunction(namedJavaFunctions[i]);
			setField(-2, name);
		}
		lua_getsubtable(REGISTRYINDEX, "_LOADED");
		pushValue(-2);
		setField(-2, moduleName);
		pop(1);
		if (global) {
			rawGet(REGISTRYINDEX, RIDX_GLOBALS);
			pushValue(-2);
			setField(-2, moduleName);
			pop(1);
		}
	}

	// -- Load and dump
	/**
	 * Loads a Lua chunk from an input stream and pushes it on the stack as a
	 * function. Depending on the value of mode, the the Lua chunk can either be
	 * a pre-compiled binary chunk or a UTF-8 encoded text chunk.
	 * 
	 * @param inputStream
	 *            the input stream
	 * @param chunkName
	 *            the name of the chunk for use in error messages
	 * @param mode
	 *            <code>"b"</code> to accept binary, <code>"t"</code> to accept
	 *            text, or <code>"bt"</code> to accept both
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public synchronized void load(InputStream inputStream, String chunkName,
			String mode) throws IOException {
		check();
		lua_load(inputStream, chunkName, mode);
	}

	/**
	 * Loads a Lua chunk from a string and pushes it on the stack as a function.
	 * The string must contain a source chunk.
	 * 
	 * @param chunk
	 *            the Lua source chunk
	 * @param chunkName
	 *            the name of the chunk for use in error messages
	 */
	public synchronized void load(String chunk, String chunkName) {
		check();
		try {
			load(new ByteArrayInputStream(chunk.getBytes("UTF-8")), chunkName,
					"t");
		} catch (IOException e) {
			throw new LuaMemoryAllocationException(e.getMessage(), e);
		}
	}

	/**
	 * Dumps the function on top of the stack as a pre-compiled binary chunk
	 * into an output stream.
	 * 
	 * @param outputStream
	 *            the output stream
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public synchronized void dump(OutputStream outputStream) throws IOException {
		check();
		lua_dump(outputStream);
	}

	// -- Call
	/**
	 * Calls a Lua function. The function to call and the specified number of
	 * arguments are on the stack. After the call, the specified number of
	 * returns values are on stack. If the number of return values has been
	 * specified as {@link #MULTRET}, the number of values on the stack
	 * corresponds the to number of values actually returned by the called
	 * function.
	 * 
	 * @param argCount
	 *            the number of arguments
	 * @param returnCount
	 *            the number of return values, or {@link #MULTRET} to accept all
	 *            values returned by the function
	 */
	public synchronized void call(int argCount, int returnCount) {
		check();
		lua_pcall(argCount, returnCount);
	}

	// -- Globals
	/**
	 * Pushes the value of a global variable on the stack.
	 * 
	 * @param name
	 *            the global variable name
	 */
	public synchronized void getGlobal(String name) {
		check();
		lua_getglobal(name);
	}

	/**
	 * Sets the value on top of the stack as a global variable and pops the
	 * value from the stack.
	 * 
	 * @param name
	 *            the global variable name
	 */
	public synchronized void setGlobal(String name)
			throws LuaMemoryAllocationException, LuaRuntimeException {
		check();
		lua_setglobal(name);
	}

	// -- Stack push
	/**
	 * Pushes a boolean value on the stack.
	 * 
	 * @param b
	 *            the boolean value to push
	 */
	public synchronized void pushBoolean(boolean b) {
		check();
		lua_pushboolean(b ? 1 : 0);
	}

	/**
	 * Pushes a byte array value as a string value on the stack.
	 * 
	 * @param b
	 *            the byte array to push
	 */
	public synchronized void pushByteArray(byte[] b) {
		check();
		lua_pushbytearray(b);
	}

	/**
	 * Pushes an integer value as a number value on the stack.
	 * 
	 * @param n
	 *            the integer value to push
	 */
	public synchronized void pushInteger(int n) {
		check();
		lua_pushinteger(n);
	}

	/**
	 * Pushes a Java function on the stack.
	 * 
	 * @param javaFunction
	 *            the function to push
	 */
	public synchronized void pushJavaFunction(JavaFunction javaFunction) {
		check();
		lua_pushjavafunction(javaFunction);
	}

	/**
	 * Pushes a Java object on the stack with conversion. The object is
	 * processed the by the configured converter.
	 * 
	 * @param object
	 *            the Java object
	 * @see #getConverter()
	 * @see #setConverter(Converter)
	 */
	public synchronized void pushJavaObject(Object object) {
		check();
		converter.convertJavaObject(this, object);
	}

	/**
	 * Pushes a Java object on the stack. The object is pushed "as is", i.e.
	 * without conversion.
	 * 
	 * <p>
	 * If you require to push a Lua value that represents the Java object, then
	 * invoke <code>pushJavaObject(object)</code>.
	 * </p>
	 * 
	 * <p>
	 * You cannot push <code>null</code> without conversion since
	 * <code>null</code> is not a Java object. The converter converts
	 * <code>null</code> to <code>nil</code>.
	 * </p>
	 * 
	 * @param object
	 *            the Java object
	 * @see #pushJavaObject(Object)
	 */
	public synchronized void pushJavaObjectRaw(Object object) {
		check();
		lua_pushjavaobject(object);
	}

	/**
	 * Pushes a nil value on the stack.
	 */
	public synchronized void pushNil() {
		check();
		lua_pushnil();
	}

	/**
	 * Pushes a number value on the stack.
	 * 
	 * @param n
	 *            the number to push
	 */
	public synchronized void pushNumber(double n) {
		check();
		lua_pushnumber(n);
	}

	/**
	 * Pushes a string value on the stack.
	 * 
	 * @param s
	 *            the string value to push
	 */
	public synchronized void pushString(String s) {
		check();
		lua_pushstring(s);
	}

	// -- Stack type test
	/**
	 * Returns whether the value at the specified stack index is a boolean.
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return whether the value is a boolean
	 */
	public synchronized boolean isBoolean(int index) {
		check();
		return lua_isboolean(index) != 0;
	}

	/**
	 * Returns whether the value at the specified stack index is a C function.
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return whether the value is a function
	 */
	public synchronized boolean isCFunction(int index) {
		check();
		return lua_iscfunction(index) != 0;
	}

	/**
	 * Returns whether the value at the specified stack index is a function
	 * (either a C function, a Java function or a Lua function.)
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return whether the value is a function
	 */
	public synchronized boolean isFunction(int index) {
		check();
		return lua_isfunction(index) != 0;
	}

	/**
	 * Returns whether the value at the specified stack index is a Java
	 * function.
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return whether the value is a function
	 */
	public synchronized boolean isJavaFunction(int index) {
		check();
		return lua_isjavafunction(index) != 0;
	}

	/**
	 * Returns whether the value at the specified stack index is convertible to
	 * a Java object of the specified type. The conversion is checked by the
	 * configured converter.
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return whether the value is convertible to a Java object of the
	 *         specified type
	 * @see #setConverter(Converter)
	 * @see #getConverter()
	 */
	public synchronized boolean isJavaObject(int index, Class<?> type) {
		check();
		return converter.getTypeDistance(this, index, type) != Integer.MAX_VALUE;
	}

	/**
	 * Returns whether the value at the specified stack index is a Java object.
	 * 
	 * <p>
	 * Note that the method does not perform conversion. If you want to check if
	 * a value <i>is convertible to</i> a Java object, then invoke <code>
	 * isJavaObject(index, Object.class)</code>.
	 * </p>
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return whether the value is a Java object
	 * @see #isJavaObject(int, Class)
	 */
	public synchronized boolean isJavaObjectRaw(int index) {
		check();
		return lua_isjavaobject(index) != 0;
	}

	/**
	 * Returns whether the value at the specified stack index is
	 * <code>nil</code>.
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return whether the value is <code>nil</code>
	 */
	public synchronized boolean isNil(int index) {
		check();
		return lua_isnil(index) != 0;
	}

	/**
	 * Returns whether the specified stack index is non-valid.
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return whether the stack index is non-valid
	 */
	public synchronized boolean isNone(int index) {
		check();
		return lua_isnone(index) != 0;
	}

	/**
	 * Returns whether the specified stack index is non-valid or its value is
	 * <code>nil</code>.
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return whether the stack index is non-valid or its value is
	 *         <code>nil</code>
	 */
	public synchronized boolean isNoneOrNil(int index) {
		check();
		return lua_isnoneornil(index) != 0;
	}

	/**
	 * Returns whether the value at the specified stack index is a number or a
	 * string convertible to a number.
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return whether the value is a number or a string convertible to a number
	 */
	public synchronized boolean isNumber(int index) {
		check();
		return lua_isnumber(index) != 0;
	}

	/**
	 * Returns whether the value at the specified stack index is a string or a
	 * number (which is always convertible to a string.)
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return whether the value is a string or a number
	 */
	public synchronized boolean isString(int index) {
		check();
		return lua_isstring(index) != 0;
	}

	/**
	 * Returns whether the value at the specified stack index is a table.
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return whether the value is a table
	 */
	public synchronized boolean isTable(int index) {
		check();
		return lua_istable(index) != 0;
	}

	/**
	 * Returns whether the value at the specified stack index is a thread.
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return whether the value is a thread
	 */
	public synchronized boolean isThread(int index) {
		check();
		return lua_isthread(index) != 0;
	}

	// -- Stack query
	/**
	 * Compares the values at two specified stack indexes for the specified
	 * operator according to Lua semantics.
	 * 
	 * <p>
	 * Any stack index may be non-valid in which case the method returns
	 * <code>false</code>.
	 * </p>
	 * 
	 * @param index1
	 *            the first stack index
	 * @param index2
	 *            the second stack index
	 * @param operator
	 *            the operator
	 * @return the result of the comparison
	 * @since JNLua 1.0.0
	 */
	public synchronized boolean compare(int index1, int index2,
			RelOperator operator) {
		check();
		return lua_compare(index1, index2, operator.ordinal()) != 0;
	}

	/**
	 * Returns whether the values at two specified stack indexes are equal
	 * according to Lua semantics.
	 * 
	 * @param index1
	 *            the first stack index
	 * @param index2
	 *            the second stack index
	 * @return whether the values are equal
	 * @deprecated instead use {@link #compare(int, int, RelOperator)}
	 */
	public synchronized boolean equal(int index1, int index2) {
		return compare(index1, index2, RelOperator.EQ);
	}

	/**
	 * Returns the length of the value at the specified stack index. Please see
	 * the Lua Reference Manual for the definition of the raw length of a value.
	 * 
	 * @param index
	 *            the stack index
	 * @return the length
	 * @deprecated instead use {@link #rawLen(int)}
	 */
	public synchronized int length(int index) {
		return rawLen(index);
	}

	/**
	 * Returns whether a value at a first stack index is less than the value at
	 * a second stack index according to Lua semantics.
	 * 
	 * @param index1
	 *            the first stack index
	 * @param index2
	 *            the second stack index
	 * @return whether the value at the first index is less than the value at
	 *         the second index
	 * @deprecated instead use {@link #compare(int, int, RelOperator)}
	 */
	public synchronized boolean lessThan(int index1, int index2)
			throws LuaMemoryAllocationException, LuaRuntimeException {
		return compare(index1, index2, RelOperator.LT);
	}

	/**
	 * Bypassing metatable logic, returns whether the values at two specified
	 * stack indexes are equal according to Lua semantics.
	 * 
	 * <p>
	 * Any stack index may be non-valid in which case the method returns
	 * <code>false</code>.
	 * </p>
	 * 
	 * @param index1
	 *            the first stack index
	 * @param index2
	 *            the second stack index
	 * @return whether the values are equal
	 */
	public synchronized boolean rawEqual(int index1, int index2) {
		check();
		return lua_rawequal(index1, index2) != 0;
	}

	/**
	 * Bypassing metatable logic, returns the length of the value at the
	 * specified stack index. Please see the Lua Reference Manual for the
	 * definition of the raw length of a value.
	 * 
	 * @param index
	 *            the stack index
	 * @return the length
	 * @since JNLua 1.0.0
	 */
	public synchronized int rawLen(int index) {
		check();
		return lua_rawlen(index);
	}

	/**
	 * Returns the boolean representation of the value at the specified stack
	 * index. The boolean representation is <code>true</code> for all values
	 * except <code>false</code> and <code>nil</code>. The method also returns
	 * <code>false</code> if the index is non-valid.
	 * 
	 * @param index
	 *            the stack index
	 * @return the boolean representation of the value
	 */
	public synchronized boolean toBoolean(int index) {
		check();
		return lua_toboolean(index) != 0;
	}

	/**
	 * Returns the byte array representation of the value at the specified stack
	 * index. The value must be a string or a number. If the value is a number,
	 * it is in place converted to a string. Otherwise, the method returns
	 * <code>null</code>.
	 * 
	 * @param index
	 *            the stack index
	 * @return the byte array representation of the value
	 */
	public synchronized byte[] toByteArray(int index) {
		check();
		return lua_tobytearray(index);
	}

	/**
	 * Returns the integer representation of the value at the specified stack
	 * index. The value must be a number or a string convertible to a number.
	 * Otherwise, the method returns <code>0</code>.
	 * 
	 * @param index
	 *            the stack index
	 * @return the integer representation, or <code>0</code>
	 */
	public synchronized int toInteger(int index) {
		check();
		return lua_tointeger(index);
	}

	/**
	 * Returns the integer representation of the value at the specified stack
	 * index. The value must be a number or a string convertible to a number.
	 * Otherwise, the method returns <code>null</code>.
	 * 
	 * @param index
	 *            the stack index
	 * @return the integer representation, or <code>null</code>
	 * @since JNLua 1.0.2
	 */
	public synchronized Integer toIntegerX(int index) {
		check();
		return lua_tointegerx(index);
	}

	/**
	 * Returns the Java function of the value at the specified stack index. If
	 * the value is not a Java function, the method returns <code>null</code>.
	 * 
	 * @param index
	 *            the stack index
	 * @return the Java function, or <code>null</code>
	 */
	public synchronized JavaFunction toJavaFunction(int index) {
		check();
		return lua_tojavafunction(index);
	}

	/**
	 * Returns a Java object of the specified type representing the value at the
	 * specified stack index. The value must be convertible to a Java object of
	 * the specified type. The conversion is executed by the configured
	 * converter.
	 * 
	 * @param index
	 *            the stack index
	 * @param type
	 *            the Java type to convert to
	 * @return the object
	 * @throws ClassCastException
	 *             if the conversion is not supported by the converter
	 * @see #getConverter()
	 * @see #setConverter(Converter)
	 */
	public synchronized <T> T toJavaObject(int index, Class<T> type) {
		check();
		return converter.convertLuaValue(this, index, type);
	}

	/**
	 * Returns the Java object of the value at the specified stack index. If the
	 * value is not a Java object, the method returns <code>null</code>.
	 * 
	 * <p>
	 * Note that the method does not convert values to Java objects. If you
	 * require <i>any</i> Java object that represents the value at the specified
	 * index, then invoke <code>toJavaObject(index, Object.class)</code>.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return the Java object, or <code>null</code>
	 * @see #toJavaObject(int, Class)
	 */
	public synchronized Object toJavaObjectRaw(int index) {
		check();
		return lua_tojavaobject(index);
	}

	/**
	 * Returns the number representation of the value at the specified stack
	 * index. The value must be a number or a string convertible to a number.
	 * Otherwise, the method returns <code>0.0</code>.
	 * 
	 * @param index
	 *            the stack index
	 * @return the number representation, or <code>0.0</code>
	 */
	public synchronized double toNumber(int index) {
		check();
		return lua_tonumber(index);
	}

	/**
	 * Returns the number representation of the value at the specified stack
	 * index. The value must be a number or a string convertible to a number.
	 * Otherwise, the method returns <code>null</code>.
	 * 
	 * @param index
	 *            the stack index
	 * @return the number representation, or <code>null</code>
	 * @since JNLua 1.0.2
	 */
	public synchronized Double toNumberX(int index) {
		check();
		return lua_tonumberx(index);
	}

	/**
	 * Returns the pointer representation of the value at the specified stack
	 * index. The value must be a table, thread, function or userdata (such as a
	 * Java object.) Otherwise, the method returns <code>0L</code>. Different
	 * values return different pointers. Other than that, the returned value has
	 * no portable significance.
	 * 
	 * @param index
	 *            the stack index
	 * @return the pointer representation, or <code>0L</code> if none
	 */
	public synchronized long toPointer(int index) {
		check();
		return lua_topointer(index);
	}

	/**
	 * Returns the string representation of the value at the specified stack
	 * index. The value must be a string or a number. If the value is a number,
	 * it is in place converted to a string. Otherwise, the method returns
	 * <code>null</code>.
	 * 
	 * @param index
	 *            the stack index
	 * @return the string representation, or <code>null</code>
	 */
	public synchronized String toString(int index) {
		check();
		return lua_tostring(index);
	}

	/**
	 * Returns the type of the value at the specified stack index.
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return the type, or <code>null</code> if the stack index is non-valid
	 */
	public synchronized LuaType type(int index) {
		check();
		int type = lua_type(index);
		return type >= 0 ? LuaType.values()[type] : null;
	}

	/**
	 * Returns the name of the type at the specified stack index. The type name
	 * is the display text for the Lua type except for Java objects where the
	 * type name is the canonical class name.
	 * 
	 * <p>
	 * The stack index may be non-valid in which case the method returns the
	 * string <code>"none"</code>.
	 * </p>
	 * 
	 * @param index
	 *            the index
	 * @return the type name
	 * @see LuaType#displayText()
	 * @see Class#getCanonicalName()
	 */
	public synchronized String typeName(int index) {
		check();
		LuaType type = type(index);
		if (type == null) {
			return "none";
		}
		switch (type) {
		case USERDATA:
			if (isJavaObjectRaw(index)) {
				Object object = toJavaObjectRaw(index);
				Class<?> clazz;
				if (object instanceof Class<?>) {
					clazz = (Class<?>) object;
				} else {
					clazz = object.getClass();
				}
				return clazz.getCanonicalName();
			}
			break;
		}
		return type.displayText();
	}

	// -- Stack operation
	/**
	 * Returns the absolute stack index of the specified index.
	 * 
	 * <p>
	 * The stack index may be non-valid.
	 * </p>
	 * 
	 * @param index
	 *            the stack index
	 * @return the absolute stack index
	 * @since JNLua 1.0.0
	 */
	public synchronized int absIndex(int index) {
		check();
		return lua_absindex(index);
	}

	/**
	 * Performs an arithmetic operation with values on top of the stack using
	 * Lua semantics.
	 * 
	 * @param operator
	 *            the operator to apply
	 * @since JNLua 1.0.0
	 */
	public synchronized void arith(ArithOperator operator) {
		check();
		lua_arith(operator.ordinal());
	}

	/**
	 * Concatenates the specified number values on top of the stack and replaces
	 * them with the concatenated value.
	 * 
	 * @param n
	 *            the number of values to concatenate
	 */
	public synchronized void concat(int n) {
		check();
		lua_concat(n);
	}

	/**
	 * Copies a value at a specified index to another index, replacing the value
	 * at that index.
	 * 
	 * @param fromIndex
	 *            the index to copy from
	 * @param toIndex
	 *            the index to copy to
	 * @since JNLua 1.0.0
	 */
	public synchronized void copy(int fromIndex, int toIndex) {
		check();
		lua_copy(fromIndex, toIndex);
	}

	/**
	 * Returns the number of values on the stack.
	 * 
	 * @return the number of values on the tack
	 */
	public synchronized int getTop() {
		check();
		return lua_gettop();
	}

	/**
	 * Pushes the length of the value at the specified stack index on the stack.
	 * The value pushed by the method corresponds to the Lua <code>#</code>
	 * operator.
	 * 
	 * @param index
	 *            the index for which to push the length
	 * @since JNLua 1.0.0
	 */
	public synchronized void len(int index) {
		check();
		lua_len(index);
	}

	/**
	 * Pops the value on top of the stack inserting it at the specified index
	 * and moving up elements above that index.
	 * 
	 * @param index
	 *            the stack index
	 */
	public synchronized void insert(int index) {
		check();
		lua_insert(index);
	}

	/**
	 * Pops values from the stack.
	 * 
	 * @param count
	 *            the number of values to pop
	 */
	public synchronized void pop(int count) {
		check();
		lua_pop(count);
	}

	/**
	 * Pushes the value at the specified index on top of the stack.
	 * 
	 * @param index
	 *            the stack index
	 */
	public synchronized void pushValue(int index) {
		check();
		lua_pushvalue(index);
	}

	/**
	 * Removes the value at the specified stack index moving down elements above
	 * that index.
	 * 
	 * @param index
	 *            the stack index
	 */
	public synchronized void remove(int index) {
		check();
		lua_remove(index);
	}

	/**
	 * Replaces the value at the specified index with the value popped from the
	 * top of the stack.
	 * 
	 * @param index
	 *            the stack index
	 */
	public synchronized void replace(int index) {
		check();
		lua_replace(index);
	}

	/**
	 * Sets the specified index as the new top of the stack.
	 * 
	 * <p>
	 * The new top of the stack may be above the current top of the stack. In
	 * this case, new values are set to <code>nil</code>.
	 * </p>
	 * 
	 * @param index
	 *            the index of the new top of the stack
	 */
	public synchronized void setTop(int index) {
		check();
		lua_settop(index);
	}

	// -- Table
	/**
	 * Pushes on the stack the value indexed by the key on top of the stack in
	 * the table at the specified index. The key is replaced by the value from
	 * the table.
	 * 
	 * @param index
	 *            the stack index containing the table
	 */
	public synchronized void getTable(int index) {
		check();
		lua_gettable(index);
	}

	/**
	 * Pushes on the stack the value indexed by the specified string key in the
	 * table at the specified index.
	 * 
	 * @param index
	 *            the stack index containing the table
	 * @param key
	 *            the string key
	 */
	public synchronized void getField(int index, String key) {
		check();
		lua_getfield(index, key);
	}

	/**
	 * Creates a new table and pushes it on the stack.
	 */
	public synchronized void newTable() {
		check();
		lua_newtable();
	}

	/**
	 * Creates a new table with pre-allocated space for a number of array
	 * elements and record elements and pushes it on the stack.
	 * 
	 * @param arrayCount
	 *            the number of array elements
	 * @param recordCount
	 *            the number of record elements
	 */
	public synchronized void newTable(int arrayCount, int recordCount) {
		check();
		lua_createtable(arrayCount, recordCount);
	}

	/**
	 * Pops a key from the stack and pushes on the stack the next key and its
	 * value in the table at the specified index. If there is no next key, the
	 * key is popped but nothing is pushed. The method returns whether there is
	 * a next key.
	 * 
	 * @param index
	 *            the stack index containing the table
	 * @return whether there is a next key
	 */
	public synchronized boolean next(int index) {
		check();
		return lua_next(index) != 0;
	}

	/**
	 * Bypassing metatable logic, pushes on the stack the value indexed by the
	 * key on top of the stack in the table at the specified index. The key is
	 * replaced by the value from the table.
	 * 
	 * @param index
	 *            the stack index containing the table
	 */
	public synchronized void rawGet(int index) {
		check();
		lua_rawget(index);
	}

	/**
	 * Bypassing metatable logic, pushes on the stack the value indexed by the
	 * specified integer key in the table at the specified index.
	 * 
	 * @param index
	 *            the stack index containing the table
	 * @param key
	 *            the integer key
	 */
	public synchronized void rawGet(int index, int key) {
		check();
		lua_rawgeti(index, key);
	}

	/**
	 * Bypassing metatable logic, sets the value on top of the stack in the
	 * table at the specified index using the value on the second highest stack
	 * position as the key. Both the value and the key are popped from the
	 * stack.
	 * 
	 * @param index
	 *            the stack index containing the table
	 */
	public synchronized void rawSet(int index) {
		check();
		lua_rawset(index);
	}

	/**
	 * Bypassing metatable logic, sets the value on top of the stack in the
	 * table at the specified index using the specified integer key. The value
	 * is popped from the stack.
	 * 
	 * @param index
	 *            the stack index containing the table
	 * @param key
	 *            the integer key
	 */
	public synchronized void rawSet(int index, int key) {
		check();
		lua_rawseti(index, key);
	}

	/**
	 * Sets the value on top of the stack in the table at the specified index
	 * using the value on the second highest stack position as the key. Both the
	 * value and the key are popped from the stack.
	 * 
	 * @param index
	 *            the stack index containing the table
	 */
	public synchronized void setTable(int index) {
		check();
		lua_settable(index);
	}

	/**
	 * Sets the value on top of the stack in the table at the specified index
	 * using the specified string key. The value is popped from the stack.
	 * 
	 * @param index
	 *            the stack index containing the table
	 * @param key
	 *            the string key
	 */
	public synchronized void setField(int index, String key) {
		check();
		lua_setfield(index, key);
	}

	// -- Metatable
	/**
	 * Pushes on the stack the value of the named field in the metatable of the
	 * value at the specified index and returns <code>true</code>. If the value
	 * does not have a metatable or if the metatable does not contain the named
	 * field, nothing is pushed and the method returns <code>false</code>.
	 * 
	 * @param index
	 *            the stack index containing the value to get the metafield from
	 * @param key
	 *            the string key
	 * @return whether the metafield was pushed on the stack
	 */
	public synchronized boolean getMetafield(int index, String key) {
		check();
		return lua_getmetafield(index, key) != 0;
	}

	/**
	 * Pushes on the stack the metatable of the value at the specified index. If
	 * the value does not have a metatable, the method returns
	 * <code>false</code> and nothing is pushed.
	 * 
	 * @param index
	 *            the stack index containing the value to get the metatable from
	 * @return whether the metatable was pushed on the stack
	 */
	public synchronized boolean getMetatable(int index) {
		check();
		return lua_getmetatable(index) != 0;
	}

	/**
	 * Sets the value on top of the stack as the metatable of the value at the
	 * specified index. The metatable to be set is popped from the stack
	 * regardless whether it can be set or not.
	 * 
	 * @param index
	 *            the stack index containing the value to set the metatable for
	 */
	public synchronized void setMetatable(int index) {
		check();
		lua_setmetatable(index);
	}

	// -- Thread
	/**
	 * Pops the start function of a new Lua thread from the stack and creates
	 * the new thread with that start function. The new thread is pushed on the
	 * stack.
	 */
	public synchronized void newThread() {
		check();
		lua_newthread();
	}

	/**
	 * Resumes the thread at the specified stack index, popping the specified
	 * number of arguments from the top of the stack and passing them to the
	 * resumed thread. The method returns the number of values pushed on the
	 * stack as the return values of the resumed thread.
	 * 
	 * @param index
	 *            the stack index containing the thread
	 * @param argCount
	 *            the number of arguments to pass
	 * @return the number of values returned by the thread
	 */
	public synchronized int resume(int index, int argCount) {
		check();
		return lua_resume(index, argCount);
	}

	/**
	 * Returns the status of the thread at the specified stack index. If the
	 * thread is in initial state of has finished its execution, the method
	 * returns <code>0</code>. If the thread has yielded, the method returns
	 * {@link #YIELD}. Other return values indicate errors for which an
	 * exception has been thrown.
	 * 
	 * @param index
	 *            the index
	 * @return the status
	 */
	public synchronized int status(int index) {
		check();
		return lua_status(index);
	}

	/**
	 * Yields the running thread, popping the specified number of values from
	 * the top of the stack and passing them as return values to the thread
	 * which has resumed the running thread. The method must be used exclusively
	 * at the exit point of Java functions, i.e.
	 * <code>return luaState.yield(n)</code>.
	 * 
	 * @param returnCount
	 *            the number of results to pass
	 * @return the return value of the Java function
	 */
	public synchronized int yield(int returnCount) {
		check();
		yield = true;
		return returnCount;
	}

	// -- Reference
	/**
	 * Stores the value on top of the stack in the table at the specified index
	 * and returns the integer key of the value in that table as a reference.
	 * The value is popped from the stack.
	 * 
	 * @param index
	 *            the stack index containing the table where to store the value
	 * @return the reference integer key
	 * @see #unref(int, int)
	 */
	public synchronized int ref(int index) {
		check();
		return lua_ref(index);

	}

	/**
	 * Removes a previously created reference from the table at the specified
	 * index. The value is removed from the table and its integer key of the
	 * reference is freed for reuse.
	 * 
	 * @param index
	 *            the stack index containing the table where the value was
	 *            stored
	 * @param reference
	 *            the reference integer key
	 * @see #ref(int)
	 */
	public synchronized void unref(int index, int reference) {
		check();
		lua_unref(index, reference);
	}

	// -- Optimization
	/**
	 * Counts the number of entries in a table.
	 * 
	 * <p>
	 * The method provides optimized performance over a Java implementation of
	 * the same functionality due to the reduced number of JNI transitions.
	 * </p>
	 * 
	 * @param index
	 *            the stack index containing the table
	 * @return the number of entries in the table
	 */
	public synchronized int tableSize(int index) {
		check();
		return lua_tablesize(index);
	}

	/**
	 * Moves the specified number of sequential elements in a table used as an
	 * array from one index to another.
	 * 
	 * <p>
	 * The method provides optimized performance over a Java implementation of
	 * the same functionality due to the reduced number of JNI transitions.
	 * </p>
	 * 
	 * @param index
	 *            the stack index containing the table
	 * @param from
	 *            the index to move from
	 * @param to
	 *            the index to move to
	 * @param count
	 *            the number of elements to move
	 */
	public synchronized void tableMove(int index, int from, int to, int count) {
		check();
		lua_tablemove(index, from, to, count);
	}

	// -- Argument checking
	/**
	 * Checks if a condition is true for the specified function argument. If
	 * not, the method throws a Lua runtime exception with the specified error
	 * message.
	 * 
	 * @param index
	 *            the argument index
	 * @param condition
	 *            the condition
	 * @param msg
	 *            the error message
	 */
	public synchronized void checkArg(int index, boolean condition, String msg) {
		check();
		if (!condition) {
			throw getArgException(index, msg);
		}
	}

	/**
	 * Checks if the value of the specified function argument is a string or a
	 * number. If so, the argument value is returned as a byte array. Otherwise,
	 * the method throws a Lua runtime exception with a descriptive error
	 * message.
	 * 
	 * @param index
	 *            the argument index
	 * @return the byte array value
	 */
	public synchronized byte[] checkByteArray(int index) {
		check();
		if (!isString(index)) {
			throw getArgTypeException(index, LuaType.STRING);
		}
		return toByteArray(index);
	}

	/**
	 * Checks if the value of the specified function argument is a string or a
	 * number. If so, the argument value is returned as a byte array. If the
	 * value of the specified argument is undefined or <code>nil</code>, the
	 * method returns the specified default value. Otherwise, the method throws
	 * a Lua runtime exception with a descriptive error message.
	 * 
	 * @param index
	 *            the argument index
	 * @param d
	 *            the default value
	 * @return the string value, or the default value
	 */
	public synchronized byte[] checkByteArray(int index, byte[] d) {
		check();
		if (isNoneOrNil(index)) {
			return d;
		}
		return checkByteArray(index);
	}

	/**
	 * Checks if the value of the specified function argument is a string or a
	 * number matching the name of one of the specified enum values. If so, the
	 * argument value is returned as an enum value. Otherwise, the method throws
	 * a Lua runtime exception with a descriptive error message.
	 * 
	 * @param index
	 *            the argument index
	 * @param values
	 *            the enum values
	 * @return the string value
	 * @since JNLua 1.0.0
	 */
	public synchronized <T extends Enum<T>> T checkEnum(int index, T[] values) {
		check();
		return checkEnum(index, values, null);
	}

	/**
	 * Checks if the value of the specified function argument is a string or a
	 * number matching one of the specified enum values. If so, the argument
	 * value is returned as an enum value. If the specified stack index is
	 * non-valid or if its value is <code>nil</code>, the method returns the
	 * specified default value. Otherwise, the method throws a Lua runtime
	 * exception with a descriptive error message.
	 * 
	 * @param index
	 *            the argument index
	 * @param values
	 *            the enum values
	 * @param d
	 *            the default value
	 * @return the string value, or the default value
	 * @since JNLua 1.0.0
	 */
	public synchronized <T extends Enum<T>> T checkEnum(int index, T[] values,
			T d) {
		check();
		String s = d != null ? checkString(index, d.name())
				: checkString(index);
		for (int i = 0; i < values.length; i++) {
			if (values[i].name().equals(s)) {
				return values[i];
			}
		}
		throw getArgException(index, String.format("invalid option '%s'", s));
	}

  /**
   * Checks if the value of the specified function argument is a boolean. If
   * so, the argument value is returned as a boolean. Otherwise, the method
   * throws a Lua runtime exception with a descriptive error message.
   * 
   * @param index
   *            the argument index
   * @return the integer value
   */
  public synchronized boolean checkBoolean(int index) {
    check();
    if (!isBoolean(index)) {
      throw getArgTypeException(index, LuaType.BOOLEAN);
    }
    return toBoolean(index);
  }

	/**
	 * Checks if the value of the specified function argument is a number or a
	 * string convertible to a number. If so, the argument value is returned as
	 * an integer. Otherwise, the method throws a Lua runtime exception with a
	 * descriptive error message.
	 * 
	 * @param index
	 *            the argument index
	 * @return the integer value
	 */
	public synchronized int checkInteger(int index) {
		check();
		Integer integer = toIntegerX(index);
		if (integer == null) {
			throw getArgTypeException(index, LuaType.NUMBER);
		}
		return integer.intValue();
	}

	/**
	 * Checks if the value of the specified function argument is a number or a
	 * string convertible to a number. If so, the argument value is returned as
	 * an integer. If the specified stack index is non-valid or if its value is
	 * <code>nil</code>, the method returns the specified default value.
	 * Otherwise, the method throws a Lua runtime exception with a descriptive
	 * error message.
	 * 
	 * @param index
	 *            the argument index
	 * @param d
	 *            the default value
	 * @return the integer value, or the default value
	 */
	public synchronized int checkInteger(int index, int d) {
		check();
		if (isNoneOrNil(index)) {
			return d;
		}
		return checkInteger(index);
	}

	/**
	 * Checks if the value of the specified function argument is convertible to
	 * a Java object of the specified type. If so, the argument value is
	 * returned as a Java object of the specified type. Otherwise, the method
	 * throws a Lua runtime exception with a descriptive error message.
	 * 
	 * <p>
	 * Note that the converter converts <code>nil</code> to <code>null</code>.
	 * Therefore, the method may return <code>null</code> if the value is
	 * <code>nil</code>.
	 * </p>
	 * 
	 * @param index
	 *            the argument index
	 * @param clazz
	 *            the expected type
	 * @return the Java object, or <code>null</code>
	 */
	public synchronized <T> T checkJavaObject(int index, Class<T> clazz) {
		check();
		if (!isJavaObject(index, clazz)) {
			checkArg(
					index,
					false,
					String.format("%s expected, got %s",
							clazz.getCanonicalName(), typeName(index)));
		}
		return toJavaObject(index, clazz);
	}

	/**
	 * Checks if the value of the specified function argument is convertible to
	 * a Java object of the specified type. If so, the argument value is
	 * returned as a Java object of the specified type. If the specified stack
	 * index is non-valid or if its value is <code>nil</code>, the method
	 * returns the specified default value. Otherwise, the method throws a Lua
	 * runtime exception with a descriptive error message.
	 * 
	 * @param index
	 *            the argument index
	 * @param clazz
	 *            the expected class
	 * @param d
	 *            the default value
	 * @return the Java object, or the default value
	 */
	public synchronized <T> T checkJavaObject(int index, Class<T> clazz, T d) {
		check();
		if (isNoneOrNil(index)) {
			return d;
		}
		return checkJavaObject(index, clazz);
	}

	/**
	 * Checks if the value of the specified function argument is a number or a
	 * string convertible to a number. If so, the argument value is returned as
	 * a number. Otherwise, the method throws a Lua runtime exception with a
	 * descriptive error message.
	 * 
	 * @param index
	 *            the argument index
	 * @return the number value
	 */
	public synchronized double checkNumber(int index) {
		check();
		Double number = toNumberX(index);
		if (number == null) {
			throw getArgTypeException(index, LuaType.NUMBER);
		}
		return number.doubleValue();
	}

	/**
	 * Checks if the value of the specified function argument is a number or a
	 * string convertible to a number. If so, the argument value is returned as
	 * a number. If the specified stack index is non-valid or if its value is
	 * <code>nil</code>, the method returns the specified default value.
	 * Otherwise, the method throws a Lua runtime exception with a descriptive
	 * error message.
	 * 
	 * @param index
	 *            the argument index
	 * @param d
	 *            the default value
	 * @return the number value, or the default value
	 */
	public synchronized double checkNumber(int index, double d) {
		check();
		if (isNoneOrNil(index)) {
			return d;
		}
		return checkNumber(index);
	}

	/**
	 * Checks if the value of the specified function argument is a string or a
	 * number matching one of the specified options. If so, the index position
	 * of the matched option is returned. Otherwise, the method throws a Lua
	 * runtime exception with a descriptive error message.
	 * 
	 * @param index
	 *            the argument index
	 * @param options
	 *            the options
	 * @return the index position of the matched option
	 */
	public synchronized int checkOption(int index, String[] options) {
		check();
		return checkOption(index, options, null);
	}

	/**
	 * Checks if the value of the specified function argument is a string or a
	 * number matching one of the specified options. If so, the index position
	 * of the matched option is returned. If the specified stack index is
	 * non-valid or if its value is <code>nil</code>, the method matches the
	 * specified default value. If no match is found, the method throws a Lua
	 * runtime exception with a descriptive error message.
	 * 
	 * @param index
	 *            the argument index
	 * @param options
	 *            the options
	 * @param d
	 *            the default value
	 * @return the index position of the matched option
	 */
	public synchronized int checkOption(int index, String[] options, String d) {
		check();
		String s = d != null ? checkString(index, d) : checkString(index);
		for (int i = 0; i < options.length; i++) {
			if (options[i].equals(s)) {
				return i;
			}
		}
		throw getArgException(index, String.format("invalid option '%s'", s));
	}

	/**
	 * Checks if the value of the specified function argument is a string or a
	 * number. If so, the argument value is returned as a string. Otherwise, the
	 * method throws a Lua runtime exception with a descriptive error message.
	 * 
	 * @param index
	 *            the argument index
	 * @return the string value
	 */
	public synchronized String checkString(int index) {
		check();
		if (!isString(index)) {
			throw getArgTypeException(index, LuaType.STRING);
		}
		return toString(index);
	}

	/**
	 * Checks if the value of the specified function argument is a string or a
	 * number. If so, the argument value is returned as a string. If the
	 * specified stack index is non-valid or if its value is <code>nil</code>,
	 * the method returns the specified default value. Otherwise, the method
	 * throws a Lua runtime exception with a descriptive error message.
	 * 
	 * @param index
	 *            the argument index
	 * @param d
	 *            the default value
	 * @return the string value, or the default value
	 */
	public synchronized String checkString(int index, String d) {
		check();
		if (isNoneOrNil(index)) {
			return d;
		}
		return checkString(index);
	}

	/**
	 * Checks if the value of the specified function argument is of the
	 * specified type. If not, the method throws a Lua runtime exception with a
	 * descriptive error message.
	 * 
	 * @param index
	 *            the argument index
	 * @param type
	 *            the type
	 */
	public synchronized void checkType(int index, LuaType type) {
		check();
		if (type(index) != type) {
			throw getArgTypeException(index, type);
		}
	}

	// -- Proxy
	/**
	 * Returns a proxy object for the Lua value at the specified index.
	 * 
	 * @param index
	 *            the stack index containing the Lua value
	 * @return the Lua value proxy
	 */
	public synchronized LuaValueProxy getProxy(int index) {
		check();
		pushValue(index);
		return new LuaValueProxyImpl(ref(REGISTRYINDEX));
	}

	/**
	 * Returns a proxy object implementing the specified interface in Lua. The
	 * table at the specified stack index contains the method names from the
	 * interface as keys and the Lua functions implementing the interface
	 * methods as values. The returned object always implements the
	 * {@link LuaValueProxy} interface in addition to the specified interface.
	 * 
	 * @param index
	 *            the stack index containing the table
	 * @param interfaze
	 *            the interface
	 * @return the proxy object
	 */
	@SuppressWarnings("unchecked")
	public synchronized <T> T getProxy(int index, Class<T> interfaze) {
		check();
		return (T) getProxy(index, new Class<?>[] { interfaze });
	}

	/**
	 * Returns a proxy object implementing the specified list of interfaces in
	 * Lua. The table at the specified stack index contains the method names
	 * from the interfaces as keys and the Lua functions implementing the
	 * interface methods as values. The returned object always implements the
	 * {@link LuaValueProxy} interface in addition to the specified interfaces.
	 * 
	 * @param index
	 *            the stack index containing the table
	 * @param interfaces
	 *            the interfaces
	 * @return the proxy object
	 */
	public synchronized LuaValueProxy getProxy(int index, Class<?>[] interfaces) {
		check();
		pushValue(index);
		if (!isTable(index)) {
			throw new IllegalArgumentException(String.format(
					"index %d is not a table", index));
		}
		Class<?>[] allInterfaces = new Class<?>[interfaces.length + 1];
		System.arraycopy(interfaces, 0, allInterfaces, 0, interfaces.length);
		allInterfaces[allInterfaces.length - 1] = LuaValueProxy.class;
		int reference = ref(REGISTRYINDEX);
		try {
			Object proxy = Proxy.newProxyInstance(classLoader, allInterfaces,
					new LuaInvocationHandler(reference));
			reference = -1;
			return (LuaValueProxy) proxy;
		} finally {
			if (reference >= 0) {
				unref(REGISTRYINDEX, reference);
			}
		}
	}

	// -- Private methods
	/**
	 * Returns whether this Lua state is open.
	 */
	private boolean isOpenInternal() {
		return luaState != 0L;
	}

	/**
	 * Closes this Lua state.
	 */
	private void closeInternal() {
		if (isOpenInternal()) {
			lua_close(ownState);
			if (isOpenInternal()) {
				throw new IllegalStateException("cannot close");
			}
		}
	}

	/**
	 * Checks this Lua state.
	 */
	private void check() {
		// Check open
		if (!isOpenInternal()) {
			throw new IllegalStateException("Lua state is closed");
		}

		// Check proxy queue
		LuaValueProxyRef luaValueProxyRef;
		while ((luaValueProxyRef = (LuaValueProxyRef) proxyQueue.poll()) != null) {
			proxySet.remove(luaValueProxyRef);
			lua_unref(REGISTRYINDEX, luaValueProxyRef.getReference());
		}
	}

	/**
	 * Creates a Lua runtime exception to indicate an argument type error.
	 */
	private LuaRuntimeException getArgTypeException(int index, LuaType type) {
		return getArgException(index,
				String.format("%s expected, got %s", type.toString()
						.toLowerCase(), type(index).toString().toLowerCase()));
	}

	/**
	 * Creates a Lua runtime exception to indicate an argument error.
	 * 
	 * @param extraMsg
	 * @return
	 */
	private LuaRuntimeException getArgException(int index, String extraMsg) {
		check();

		// Get execution point
		String name = null, nameWhat = null;
		LuaDebug luaDebug = lua_getstack(0);
		if (luaDebug != null) {
			lua_getinfo("n", luaDebug);
			name = luaDebug.getName();
			nameWhat = luaDebug.getNameWhat();
		}

		// Adjust for methods
		if ("method".equals(nameWhat)) {
			index--;
		}

		// Format message
		String msg;
		String argument = index > 0 ? String.format("argument #%d", index)
				: "self argument";
		if (name != null) {
			msg = String
					.format("bad %s to '%s' (%s)", argument, name, extraMsg);
		} else {
			msg = String.format("bad %s (%s)", argument, extraMsg);
		}
		return new LuaRuntimeException(msg);
	}

	/**
	 * Validates a value specified as the new maximum allowed memory use. This
	 * is used in particular to validate values passed to the constructor.
	 * 
	 * @param value
	 *            the value to validate
	 * @return the value itself
	 */
	private static int validateMemory(int value) {
		if (value < 1) {
			throw new IllegalArgumentException("Maximum memory must be larger than zero.");
		}
		return value;
	}

	// -- Native methods
	private static native int lua_registryindex();

	private static native String lua_version();

	private native void lua_newstate(int apiversion, long luaState);

	private native void lua_close(boolean ownState);

	private native int lua_gc(int what, int data);

	private native void lua_openlib(int lib);

	private native void lua_load(InputStream inputStream, String chunkname,
			String mode) throws IOException;

	private native void lua_dump(OutputStream outputStream) throws IOException;

	private native void lua_pcall(int nargs, int nresults);

	private native void lua_getglobal(String name);

	private native void lua_setglobal(String name);

	private native void lua_pushboolean(int b);

	private native void lua_pushbytearray(byte[] b);
	
	private native void lua_pushinteger(int n);

	private native void lua_pushjavafunction(JavaFunction f);

	private native void lua_pushjavaobject(Object object);

	private native void lua_pushnil();

	private native void lua_pushnumber(double n);

	private native void lua_pushstring(String s);

	private native int lua_isboolean(int index);

	private native int lua_iscfunction(int index);

	private native int lua_isfunction(int index);

	private native int lua_isjavafunction(int index);

	private native int lua_isjavaobject(int index);

	private native int lua_isnil(int index);

	private native int lua_isnone(int index);

	private native int lua_isnoneornil(int index);

	private native int lua_isnumber(int index);

	private native int lua_isstring(int index);

	private native int lua_istable(int index);

	private native int lua_isthread(int index);

	private native int lua_compare(int index1, int index2, int operator);

	private native int lua_rawequal(int index1, int index2);

	private native int lua_rawlen(int index);

	private native int lua_toboolean(int index);

	private native byte[] lua_tobytearray(int index);
	
	private native int lua_tointeger(int index);

	private native Integer lua_tointegerx(int index);

	private native JavaFunction lua_tojavafunction(int index);

	private native Object lua_tojavaobject(int index);

	private native double lua_tonumber(int index);

	private native Double lua_tonumberx(int index);

	private native long lua_topointer(int index);

	private native String lua_tostring(int index);

	private native int lua_type(int index);

	private native int lua_absindex(int index);

	private native int lua_arith(int operator);

	private native void lua_concat(int n);

	private native int lua_copy(int fromIndex, int toIndex);

	private native int lua_gettop();

	private native void lua_len(int index);

	private native void lua_insert(int index);

	private native void lua_pop(int n);

	private native void lua_pushvalue(int index);

	private native void lua_remove(int index);

	private native void lua_replace(int index);

	private native void lua_settop(int index);

	private native void lua_createtable(int narr, int nrec);

	private native int lua_getsubtable(int idx, String fname);

	private native void lua_gettable(int index);

	private native void lua_getfield(int index, String k);

	private native void lua_newtable();

	private native int lua_next(int index);

	private native void lua_rawget(int index);

	private native void lua_rawgeti(int index, int n);

	private native void lua_rawset(int index);

	private native void lua_rawseti(int index, int n);

	private native void lua_settable(int index);

	private native void lua_setfield(int index, String k);

	private native int lua_getmetatable(int index);

	private native void lua_setmetatable(int index);

	private native int lua_getmetafield(int index, String k);

	private native void lua_newthread();

	private native int lua_resume(int index, int nargs);

	private native int lua_status(int index);

	private native int lua_ref(int index);

	private native void lua_unref(int index, int ref);

	private native LuaDebug lua_getstack(int level);

	private native int lua_getinfo(String what, LuaDebug ar);

	private native int lua_tablesize(int index);

	private native void lua_tablemove(int index, int from, int to, int count);

	// -- Enumerated types
	/**
	 * Represents a Lua library.
	 */
	public enum Library {
		/*
		 * The order of the libraries follows the definition in linit.c.
		 */
		/**
		 * The base library.
		 */
		BASE,

		/**
		 * The package library.
		 */
		PACKAGE,

		/**
		 * The coroutine library.
		 * 
		 * @since JNLua 1.0.0
		 */
		COROUTINE,

		/**
		 * The table library.
		 */
		TABLE,

		/**
		 * The IO library.
		 */
		IO,

		/**
		 * The OS library.
		 */
		OS,

		/**
		 * The string library.
		 */
		STRING,

		/**
//		 * The bit32 library. * 
		 * @since JNLua 1.0.0
		 */
		BIT32,

		/**
		 * The math library.
		 */
		MATH,

		/**
		 * The debug library.
		 */
		DEBUG,

    /**
     * The persistence library.
     */
    ERIS,

		/**
		 * The Java library.
		 */
		JAVA {
			@Override
			void open(LuaState luaState) {
				JavaModule.getInstance().open(luaState);
			}
		};

		// -- Methods
		/**
		 * Opens this library.
		 */
		void open(LuaState luaState) {
			luaState.lua_openlib(ordinal());
		}
	}

	/**
	 * Represents a Lua garbage collector action. Please see the Lua Reference
	 * Manual for an explanation of these actions.
	 */
	public enum GcAction {
		/**
		 * Stop.
		 */
		STOP,

		/**
		 * Restart.
		 */
		RESTART,

		/**
		 * Collect.
		 */
		COLLECT,

		/**
		 * Count memory in kilobytes.
		 */
		COUNT,

		/**
		 * Count reminder in bytes.
		 */
		COUNTB,

		/**
		 * Step.
		 */
		STEP,

		/**
		 * Set pause.
		 */
		SETPAUSE,

		/**
		 * Set step multiplier.
		 */
		SETSTEPMUL,

		/**
		 * Undocumented.
		 * 
		 * @since JNLua 1.0.0
		 */
		SETMAJORINC,

		/**
		 * Returns whether the collector is running (i.e. not stopped).
		 * 
		 * @since JNLua 1.0.0
		 */
		ISRUNNING,

		/**
		 * Changes the collector to the generational mode.
		 * 
		 * @since JNLua 1.0.0
		 */
		GEN,

		/**
		 * Changes the collector to the incremental mode.
		 * 
		 * @since JNLua 1.0.0
		 */
		INC
	}

	/**
	 * Represents a Lua arithmetic operator. Please see the Lua Reference Manual
	 * for an explanation of these operators.
	 * 
	 * @since JNLua 1.0.0
	 */
	public enum ArithOperator {
		/**
		 * Addition operator.
		 */
		ADD,

		/**
		 * Subtraction operator.
		 */
		SUB,

		/**
		 * Multiplication operator.
		 */
		MUL,

		/**
		 * Division operator.
		 */
		DIV,

		/**
		 * Modulo operator.
		 */
		MOD,

		/**
		 * Exponentiation operator.
		 */
		POW,

		/**
		 * Mathematical negation operator.
		 */
		UNM
	}

	/**
	 * Represents a Lua relational operator. Please see the Lua Reference Manual
	 * for an explanation of these operators.
	 * 
	 * @since JNLua 1.0.0
	 */
	public enum RelOperator {
		/**
		 * Equality operator.
		 */
		EQ,

		/**
		 * Less than operator.
		 */
		LT,

		/**
		 * Less or equal operator.
		 */
		LE
	}

	// -- Nested types
	/**
	 * Phantom reference to a Lua value proxy for pre-mortem cleanup.
	 */
	private static class LuaValueProxyRef extends
			PhantomReference<LuaValueProxyImpl> {
		// -- State
		private int reference;

		// --Construction
		/**
		 * Creates a new instance.
		 */
		public LuaValueProxyRef(LuaValueProxyImpl luaProxyImpl, int reference) {
			super(luaProxyImpl, luaProxyImpl.getLuaState().proxyQueue);
			this.reference = reference;
		}

		// -- Properties
		/**
		 * Returns the reference.
		 */
		public int getReference() {
			return reference;
		}
	}

	/**
	 * Lua value proxy implementation.
	 */
	private class LuaValueProxyImpl implements LuaValueProxy {
		// -- State
		private int reference;

		// -- Construction
		/**
		 * Creates a new instance.
		 */
		public LuaValueProxyImpl(int reference) {
			this.reference = reference;
			proxySet.add(new LuaValueProxyRef(this, reference));
		}

		// -- LuaProxy methods
		@Override
		public LuaState getLuaState() {
			return LuaState.this;
		}

		@Override
		public void pushValue() {
			synchronized (LuaState.this) {
				rawGet(REGISTRYINDEX, reference);
			}
		}
	}

	/**
	 * Invocation handler for implementing Java interfaces in Lua.
	 */
	private class LuaInvocationHandler extends LuaValueProxyImpl implements
			InvocationHandler {
		// -- Construction
		/**
		 * Creates a new instance.
		 */
		public LuaInvocationHandler(int reference) {
			super(reference);
		}

		// -- InvocationHandler methods
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			// Handle LuaProxy methods
			if (method.getDeclaringClass() == LuaValueProxy.class) {
				return method.invoke(this, args);
			}

			// Handle Lua calls
			synchronized (LuaState.this) {
				pushValue();
				getField(-1, method.getName());
				if (!isFunction(-1)) {
					pop(2);
					throw new UnsupportedOperationException(method.getName());
				}
				insert(-2);
				int argCount = args != null ? args.length : 0;
				for (int i = 0; i < argCount; i++) {
					pushJavaObject(args[i]);
				}
				int retCount = method.getReturnType() != Void.TYPE ? 1 : 0;
				call(argCount + 1, retCount);
				try {
					return retCount == 1 ? LuaState.this.toJavaObject(-1,
							method.getReturnType()) : null;
				} finally {
					if (retCount == 1) {
						pop(1);
					}
				}
			}
		}
	}

	/**
	 * Lua debug structure.
	 */
	private static class LuaDebug {
		/**
		 * The <code>lua_Debug</code> pointer on the JNI side. <code>0</code>
		 * implies that the activation record has been freed. The field is
		 * modified exclusively on the JNI side and must not be touched on the
		 * Java side.
		 */
		private long luaDebug;

		/**
		 * Ensures proper finalization of this Lua debug structure.
		 */
		private Object finalizeGuardian;

		/**
		 * Creates a new instance.
		 */
		private LuaDebug(long luaDebug, boolean ownDebug) {
			this.luaDebug = luaDebug;
			if (ownDebug) {
				finalizeGuardian = new Object() {
					@Override
					public void finalize() {
						synchronized (LuaDebug.this) {
							lua_debugfree();
						}
					}
				};
			}
		}

		// -- Properties
		/**
		 * Returns a reasonable name for the function given by this activation
		 * record, or <code>null</code> if none is found.
		 */
		public String getName() {
			return lua_debugname();
		}

		/**
		 * Explains the name of the function given by this activation record.
		 */
		public String getNameWhat() {
			return lua_debugnamewhat();
		}

		// -- Native methods
		private native void lua_debugfree();

		private native String lua_debugname();

		private native String lua_debugnamewhat();
	}
}
