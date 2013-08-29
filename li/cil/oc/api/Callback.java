package li.cil.oc.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used by components to mark methods that are part of the
 * API they expose to computers.
 * 
 * If the component provides an API, each method annotated like this an entry in
 * the API table will be generated. The actual entry will be a wrapper that
 * takes care of parameter transformation and all the Lua protocol stuff. This
 * does limit the types you can use for parameters and return types, however.
 * 
 * Supported parameter types are: Boolean, Integer, Double and String. These map
 * to boolean, number, number, and string respectively. If a function parameter
 * is of an unsupported type its value will always be the type's default value.
 * 
 * Supported return types are: Boolean, Integer, Double, String and Array. These
 * map to the same types as parameter types, with the exception of arrays which
 * are interpreted as tuples, meaning they will be dissolved into multiple
 * return values. If a return value type is of an unsupported type it will be
 * ignored.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Callback {
  /**
   * The name under which the method will be available in Lua.
   */
  String name();

  /**
   * Whether this API function is called synchronized with the server thread.
   * 
   * Each computer runs its Lua state using a separate thread, to avoid slowing
   * down the game or lock it up completely. This means that when a state calls
   * a driver's API function this code is run in such a worker thread that is
   * not synchronized with the server thread (i.e. the main game thread). The
   * driver must therefore take care to avoid threading issues. For convenience
   * a driver can request that an API function is only called in sync with the
   * server thread. In that case the computer's thread will stop and wait for
   * the server thread to come along. The server thread will see that the
   * computer thread is waiting for it and perform it's API call for it, then
   * let it loose again and continue with what it was doing.
   */
  boolean synchronize() default false;
}