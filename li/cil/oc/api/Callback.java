package li.cil.oc.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used by {@see IDriver}s to mark methods that are part of
 * the API they expose to computers.
 * 
 * If the driver provides an API, for each method annotated like this an entry
 * in the API table will be generated. The actual entry will be a wrapper that
 * takes care of parameter transformation and all the Lua protocol stuff. This
 * requires fixed parameters and return types, however, i.e. one parameter will
 * always have to be one specific type (which is not necessary in Lua since it
 * is a dynamic language).
 * 
 * Any type is supported as long as it can be converted by JNLua's {@see
 * com.naef.jnlua.DefaultConverter}. However, so as not to break persistence
 * (i.e. saving a computer's state and seamlessly resume after reloading) only
 * simple types such as booleans, numbers and strings and table types such as
 * <tt>Map</tt>,<tt>List</tt> and arrays should be passed along.
 * 
 * If you wish more flexibility in how you handle parameters, define the
 * callback with the following signature:
 * 
 * <pre>
 * Object[] callback(IComputerContext, Object[])
 * </pre>
 * 
 * In this case the first argument is the computer from which the function was
 * called, the second is the array of objects passed along from Lua, which may
 * be an arbitrary number of arbitrarily typed object (although still only basic
 * types as described above are supported). The returned array must also contain
 * only basic types as described above and represents the values returned as
 * results to Lua as with automatically wrapped functions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Callback {
  /**
   * The name under which the method will be available in Lua.
   * 
   * If this is not specified, the method's name itself will be used.
   */
  String name() default "";
}