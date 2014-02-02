/*
 * $Id: JavaModule.java 76 2012-01-06 01:25:52Z andre@naef.com $
 * See LICENSE.txt for license terms.
 */

package com.naef.jnlua;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.naef.jnlua.JavaReflector.Metamethod;

/**
 * Provides the Java module for Lua. The Java module contains Java functions for
 * using Java in Lua.
 */
public class JavaModule {
	// -- Static
	private static final JavaModule INSTANCE = new JavaModule();
	private static final Map<String, Class<?>> PRIMITIVE_TYPES = new HashMap<String, Class<?>>();
	static {
		PRIMITIVE_TYPES.put("boolean", Boolean.TYPE);
		PRIMITIVE_TYPES.put("byte", Byte.TYPE);
		PRIMITIVE_TYPES.put("char", Character.TYPE);
		PRIMITIVE_TYPES.put("double", Double.TYPE);
		PRIMITIVE_TYPES.put("float", Float.TYPE);
		PRIMITIVE_TYPES.put("int", Integer.TYPE);
		PRIMITIVE_TYPES.put("long", Long.TYPE);
		PRIMITIVE_TYPES.put("short", Short.TYPE);
		PRIMITIVE_TYPES.put("void", Void.TYPE);
	}

	// -- State
	private final NamedJavaFunction[] functions = { new Require(), new New(),
			new InstanceOf(), new Cast(), new Proxy(), new Pairs(),
			new IPairs(), new ToTable(), new Elements(), new Fields(),
			new Methods(), new Properties() };

	// -- Static methods
	/**
	 * Returns the instance of the Java module.
	 * 
	 * @return the instance
	 */
	public static JavaModule getInstance() {
		return INSTANCE;
	}

	// -- Construction
	/**
	 * Singleton.
	 */
	private JavaModule() {
	}

	// -- Operations
	/**
	 * Opens this module in a Lua state. The method is invoked by
	 * {@link LuaState#openLibs()} or by
	 * {@link LuaState#openLib(com.naef.jnlua.LuaState.Library)} if
	 * {@link LuaState.Library#JAVA} is passed. The module is pushed onto the
	 * stack.
	 * 
	 * @param luaState
	 *            the Lua state to open in
	 */
	public void open(LuaState luaState) {
		luaState.register("java", functions, true);
	}

	/**
	 * Returns a table-like Lua value for the specified map. The returned value
	 * corresponds to the return value of the <code>totable()</code> function
	 * provided by this Java module.
	 * 
	 * @param map
	 *            the map
	 * @return the table-like Lua value
	 */
	public TypedJavaObject toTable(Map<?, ?> map) {
		return ToTable.toTable(map);
	}

	/**
	 * Returns a table-like Lua value for the specified list. The returned value
	 * corresponds to the return value of the <code>totable()</code> function
	 * provided by this Java module.
	 * 
	 * @param list
	 *            the list
	 * @return the table-like Lua value
	 */
	public TypedJavaObject toTable(List<?> list) {
		return ToTable.toTable(list);
	}

	// -- Private methods
	/**
	 * Loads a type. The named type is a primitive type or a class.
	 */
	private static Class<?> loadType(LuaState luaState, String typeName) {
		Class<?> clazz;
		if ((clazz = PRIMITIVE_TYPES.get(typeName)) != null) {
			return clazz;
		}
		try {
			clazz = luaState.getClassLoader().loadClass(typeName);
			return clazz;
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	// -- Nested types
	/**
	 * Imports a Java class into the Lua namespace. Returns the class and a
	 * status code. The status code indicates if the class was stored in the Lua
	 * namespace. Primitive types and classes without a package are not stored
	 * in the Lua namespace.
	 */
	private static class Require implements NamedJavaFunction {
		// -- JavaFunction methods
		@Override
		public int invoke(LuaState luaState) {
			// Check arguments
			String className = luaState.checkString(1);
			boolean doImport = luaState.toBoolean(2);

			// Load
			Class<?> clazz = loadType(luaState, className);
			luaState.pushJavaObject(clazz);

			// Import
			if (doImport) {
				luaState.rawGet(LuaState.REGISTRYINDEX, LuaState.RIDX_GLOBALS);
				String name = clazz.getName();
				int index = name.indexOf('.');
				while (index >= 0) {
					String part = name.substring(0, index);
					luaState.getField(-1, part);
					if (!luaState.isTable(-1)) {
						luaState.pop(1);
						luaState.newTable();
						luaState.pushValue(-1);
						luaState.setField(-3, part);
					}
					luaState.remove(-2);
					name = name.substring(index + 1);
					index = name.indexOf('.');
				}
				luaState.pushValue(-2);
				luaState.setField(-2, name);
				luaState.pop(1);
			}
			luaState.pushBoolean(doImport);

			// Return
			return 2;
		}

		@Override
		public String getName() {
			return "require";
		}
	}

	/**
	 * Creates and returns a new Java object or array thereof. The first
	 * argument designates the type to instantiate, either as a class or a
	 * string. The remaining arguments are the dimensions.
	 */
	private static class New implements NamedJavaFunction {
		// -- JavaFunction methods
		@Override
		public int invoke(LuaState luaState) {
			// Find class
			Class<?> clazz;
			if (luaState.isJavaObject(1, Class.class)) {
				clazz = luaState.checkJavaObject(1, Class.class);
			} else {
				String className = luaState.checkString(1);
				clazz = loadType(luaState, className);
			}

			// Instantiate
			Object object;
			int dimensionCount = luaState.getTop() - 1;
			switch (dimensionCount) {
			case 0:
				try {
					object = clazz.newInstance();
				} catch (InstantiationException e) {
					throw new RuntimeException(e);
				} catch (IllegalAccessException e) {
					throw new RuntimeException(e);
				}
				break;
			case 1:
				object = Array.newInstance(clazz, luaState.checkInteger(2));
				break;
			default:
				int[] dimensions = new int[dimensionCount];
				for (int i = 0; i < dimensionCount; i++) {
					dimensions[i] = luaState.checkInteger(i + 2);
				}
				object = Array.newInstance(clazz, dimensions);
			}

			// Return
			luaState.pushJavaObject(object);
			return 1;
		}

		@Override
		public String getName() {
			return "new";
		}
	}

	/**
	 * Returns whether an object is an instance of a type. The object is given
	 * as the first argument. the type is given as the second argument, either
	 * as a class or as a type name.
	 */
	private static class InstanceOf implements NamedJavaFunction {
		// -- JavaFunction methods
		@Override
		public int invoke(LuaState luaState) {
			// Get the object
			Object object = luaState.checkJavaObject(1, Object.class);

			// Find class
			Class<?> clazz;
			if (luaState.isJavaObject(2, Class.class)) {
				clazz = luaState.checkJavaObject(2, Class.class);
			} else {
				String className = luaState.checkString(2);
				clazz = loadType(luaState, className);
			}

			// Type check
			luaState.pushBoolean(clazz.isInstance(object));
			return 1;
		}

		@Override
		public String getName() {
			return "instanceof";
		}
	}

	/**
	 * Creates a typed Java object.
	 */
	private static class Cast implements NamedJavaFunction {
		// -- NamedJavaFunction methods
		@Override
		public int invoke(LuaState luaState) {
			// Find class
			final Class<?> clazz;
			if (luaState.isJavaObject(2, Class.class)) {
				clazz = luaState.checkJavaObject(2, Class.class);
			} else {
				String className = luaState.checkString(2);
				clazz = loadType(luaState, className);
			}

			// Get the object
			final Object object = luaState.checkJavaObject(1, clazz);

			// Push result
			luaState.pushJavaObject(new TypedJavaObject() {
				@Override
				public Object getObject() {
					return object;
				}

				@Override
				public Class<?> getType() {
					return clazz;
				}

				@Override
				public boolean isStrong() {
					return false;
				}
			});
			return 1;
		}

		@Override
		public String getName() {
			return "cast";
		}
	}

	/**
	 * Creates a dynamic proxy object the implements a set of Java interfaces in
	 * Lua.
	 */
	private static class Proxy implements NamedJavaFunction {
		// -- JavaFunction methods
		@Override
		public int invoke(LuaState luaState) {
			// Check table
			luaState.checkType(1, LuaType.TABLE);

			// Get interfaces
			int interfaceCount = luaState.getTop() - 1;
			luaState.checkArg(2, interfaceCount > 0, "no interface specified");
			Class<?>[] interfaces = new Class<?>[interfaceCount];
			for (int i = 0; i < interfaceCount; i++) {
				if (luaState.isJavaObject(i + 2, Class.class)) {
					interfaces[i] = luaState
							.checkJavaObject(i + 2, Class.class);
				} else {
					String interfaceName = luaState.checkString(i + 2);
					interfaces[i] = loadType(luaState, interfaceName);
				}
			}

			// Create proxy
			luaState.pushJavaObjectRaw(luaState.getProxy(1, interfaces));
			return 1;
		}

		@Override
		public String getName() {
			return "proxy";
		}
	}

	/**
	 * Provides the pairs iterator from the Java reflector.
	 */
	private static class Pairs implements NamedJavaFunction {
		// -- NamedJavaFunction methods
		@Override
		public int invoke(LuaState luaState) {
			luaState.checkArg(
					1,
					luaState.isJavaObjectRaw(1),
					String.format("Java object expected, got %s",
							luaState.typeName(1)));
			JavaFunction metamethod = luaState.getMetamethod(
					luaState.toJavaObjectRaw(1), Metamethod.PAIRS);
			return metamethod.invoke(luaState);
		}

		@Override
		public String getName() {
			return "pairs";
		}
	}

	/**
	 * Provides the ipairs iterator from the Java reflector.
	 */
	private static class IPairs implements NamedJavaFunction {
		// -- NamedJavaFunction methods
		@Override
		public int invoke(LuaState luaState) {
			luaState.checkArg(
					1,
					luaState.isJavaObjectRaw(1),
					String.format("Java object expected, got %s",
							luaState.typeName(1)));
			JavaFunction metamethod = luaState.getMetamethod(
					luaState.toJavaObjectRaw(1), Metamethod.IPAIRS);
			return metamethod.invoke(luaState);
		}

		@Override
		public String getName() {
			return "ipairs";
		}
	}

	/**
	 * Provides a wrapper object for table-like map and list access from Lua.
	 */
	private static class ToTable implements NamedJavaFunction {
		// -- Static methods
		/**
		 * Returns a table-like Lua value for the specified map.
		 */
		@SuppressWarnings("unchecked")
		public static TypedJavaObject toTable(Map<?, ?> map) {
			return new LuaMap((Map<Object, Object>) map);
		}

		/**
		 * Returns a table-list Lua value for the specified list.
		 */
		@SuppressWarnings("unchecked")
		public static TypedJavaObject toTable(List<?> list) {
			return new LuaList((List<Object>) list);
		}

		// -- JavaFunction methods
		@SuppressWarnings("unchecked")
		@Override
		public int invoke(LuaState luaState) {
			if (luaState.isJavaObject(1, Map.class)) {
				Map<Object, Object> map = luaState.toJavaObject(1, Map.class);
				luaState.pushJavaObject(new LuaMap(map));
			} else if (luaState.isJavaObject(1, List.class)) {
				List<Object> list = luaState.toJavaObject(1, List.class);
				luaState.pushJavaObject(new LuaList(list));
			} else {
				luaState.checkArg(
						1,
						false,
						String.format("expected map or list, got %s",
								luaState.typeName(1)));
			}
			return 1;
		}

		@Override
		public String getName() {
			return "totable";
		}

		// -- Member types
		/**
		 * Provides table-like access in Lua to a Java map.
		 */
		private static class LuaMap implements JavaReflector, TypedJavaObject {
			// -- Static
			private static final JavaFunction INDEX = new Index();
			private static final JavaFunction NEW_INDEX = new NewIndex();

			// -- State
			private Map<Object, Object> map;

			// -- Construction
			/**
			 * Creates a new instance.
			 */
			public LuaMap(Map<Object, Object> map) {
				this.map = map;
			}

			// -- Properties
			/**
			 * Returns the map.
			 */
			public Map<Object, Object> getMap() {
				return map;
			}

			// -- JavaReflector methods
			@Override
			public JavaFunction getMetamethod(Metamethod metamethod) {
				switch (metamethod) {
				case INDEX:
					return INDEX;
				case NEWINDEX:
					return NEW_INDEX;
				default:
					return null;
				}
			}

			// -- TypedJavaObject methods
			@Override
			public Object getObject() {
				return map;
			}

			@Override
			public Class<?> getType() {
				return Map.class;
			}

			@Override
			public boolean isStrong() {
				return true;
			}

			// -- Member types
			/**
			 * __index implementation for maps.
			 */
			private static class Index implements JavaFunction {
				// -- JavaFunction methods
				@Override
				public int invoke(LuaState luaState) {
					LuaMap luaMap = (LuaMap) luaState.toJavaObjectRaw(1);
					Object key = luaState.toJavaObject(2, Object.class);
					if (key == null) {
						throw new LuaRuntimeException(String.format(
								"attempt to read map with %s accessor",
								luaState.typeName(2)));
					}
					luaState.pushJavaObject(luaMap.getMap().get(key));
					return 1;
				}
			}

			/**
			 * __newindex implementation for maps.
			 */
			private static class NewIndex implements JavaFunction {
				// -- JavaFunction methods
				@Override
				public int invoke(LuaState luaState) {
					LuaMap luaMap = (LuaMap) luaState.toJavaObjectRaw(1);
					Object key = luaState.toJavaObject(2, Object.class);
					if (key == null) {
						throw new LuaRuntimeException(String.format(
								"attempt to write map with %s accessor",
								luaState.typeName(2)));
					}
					Object value = luaState.toJavaObject(3, Object.class);
					if (value != null) {
						luaMap.getMap().put(key, value);
					} else {
						luaMap.getMap().remove(key);
					}
					return 0;
				}
			}
		}

		/**
		 * Provides table-like access in Lua to a Java list.
		 */
		private static class LuaList implements JavaReflector, TypedJavaObject {
			// -- Static
			private static final JavaFunction INDEX = new Index();
			private static final JavaFunction NEW_INDEX = new NewIndex();
			private static final JavaFunction LENGTH = new Length();

			// -- State
			private List<Object> list;

			// -- Construction
			/**
			 * Creates a new instance.
			 */
			public LuaList(List<Object> list) {
				this.list = list;
			}

			// -- Properties
			/**
			 * Returns the map.
			 */
			public List<Object> getList() {
				return list;
			}

			// -- JavaReflector methods
			@Override
			public JavaFunction getMetamethod(Metamethod metamethod) {
				switch (metamethod) {
				case INDEX:
					return INDEX;
				case NEWINDEX:
					return NEW_INDEX;
				case LEN:
					return LENGTH;
				default:
					return null;
				}
			}

			// -- TypedJavaObject methods
			@Override
			public Object getObject() {
				return list;
			}

			@Override
			public Class<?> getType() {
				return List.class;
			}

			@Override
			public boolean isStrong() {
				return true;
			}

			// -- Member types
			/**
			 * __index implementation for lists.
			 */
			private static class Index implements JavaFunction {
				// -- JavaFunction methods
				@Override
				public int invoke(LuaState luaState) {
					LuaList luaList = (LuaList) luaState.toJavaObjectRaw(1);
					if (!luaState.isNumber(2)) {
						throw new LuaRuntimeException(String.format(
								"attempt to read list with %s accessor",
								luaState.typeName(2)));
					}
					int index = luaState.toInteger(2);
					luaState.pushJavaObject(luaList.getList().get(index - 1));
					return 1;
				}
			}

			/**
			 * __newindex implementation for lists.
			 */
			private static class NewIndex implements JavaFunction {
				// -- JavaFunction methods
				@Override
				public int invoke(LuaState luaState) {
					LuaList luaList = (LuaList) luaState.toJavaObjectRaw(1);
					if (!luaState.isNumber(2)) {
						throw new LuaRuntimeException(String.format(
								"attempt to write list with %s accessor",
								luaState.typeName(2)));
					}
					int index = luaState.toInteger(2);
					Object value = luaState.toJavaObject(3, Object.class);
					if (value != null) {
						int size = luaList.getList().size();
						if (index - 1 != size) {
							luaList.getList().set(index - 1, value);
						} else {
							luaList.getList().add(value);
						}
					} else {
						luaList.getList().remove(index - 1);
					}
					return 0;
				}
			}

			/**
			 * __len implementation for lists.
			 */
			private static class Length implements JavaFunction {
				// -- JavaFunction methods
				@Override
				public int invoke(LuaState luaState) {
					LuaList luaList = (LuaList) luaState.toJavaObjectRaw(1);
					luaState.pushInteger(luaList.getList().size());
					return 1;
				}
			}
		}
	}

	/**
	 * Provides an iterator for Iterable objects.
	 */
	private static class Elements implements NamedJavaFunction {
		// -- NamedJavaFunction methods
		@Override
		public int invoke(LuaState luaState) {
			Iterable<?> iterable = luaState.checkJavaObject(1, Iterable.class);
			luaState.pushJavaObject(new ElementIterator(iterable.iterator()));
			luaState.pushJavaObject(iterable);
			luaState.pushNil();
			return 3;
		}

		@Override
		public String getName() {
			return "elements";
		}

		// -- Member types
		private static class ElementIterator implements JavaFunction {
			// -- State
			private Iterator<?> iterator;

			// -- Construction
			/**
			 * Creates a new instance.
			 */
			public ElementIterator(Iterator<?> iterator) {
				this.iterator = iterator;
			}

			// -- JavaFunction methods
			@Override
			public int invoke(LuaState luaState) {
				if (iterator.hasNext()) {
					luaState.pushJavaObject(iterator.next());
				} else {
					luaState.pushNil();
				}
				return 1;
			}
		}
	}

	/**
	 * Provides an iterator for Java object fields.
	 */
	private static class Fields implements NamedJavaFunction {
		// -- NamedJavaFunction methods
		@Override
		public int invoke(LuaState luaState) {
			luaState.checkArg(
					1,
					luaState.isJavaObjectRaw(1),
					String.format("expected Java object, got %s",
							luaState.typeName(1)));
			JavaFunction metamethod = luaState.getMetamethod(
					luaState.toJavaObjectRaw(1), Metamethod.JAVAFIELDS);
			return metamethod.invoke(luaState);
		}

		@Override
		public String getName() {
			return "fields";
		}
	}

	/**
	 * Provides an iterator for Java methods.
	 */
	private static class Methods implements NamedJavaFunction {
		// -- NamedJavaFunction methods
		@Override
		public int invoke(LuaState luaState) {
			luaState.checkArg(
					1,
					luaState.isJavaObjectRaw(1),
					String.format("expected Java object, got %s",
							luaState.typeName(1)));
			JavaFunction metamethod = luaState.getMetamethod(
					luaState.toJavaObjectRaw(1), Metamethod.JAVAMETHODS);
			return metamethod.invoke(luaState);
		}

		@Override
		public String getName() {
			return "methods";
		}
	}

	/**
	 * Provides an iterator for Java object properties.
	 */
	private static class Properties implements NamedJavaFunction {
		// -- NamedJavaFunction methods
		@Override
		public int invoke(LuaState luaState) {
			luaState.checkArg(
					1,
					luaState.isJavaObjectRaw(1),
					String.format("expected Java object, got %s",
							luaState.typeName(1)));
			JavaFunction metamethod = luaState.getMetamethod(
					luaState.toJavaObjectRaw(1), Metamethod.JAVAPROPERTIES);
			return metamethod.invoke(luaState);
		}

		@Override
		public String getName() {
			return "properties";
		}
	}
}
