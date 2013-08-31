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
}