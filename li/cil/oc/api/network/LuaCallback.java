package li.cil.oc.api.network;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used for methods in an {@link Environment} to mark
 * them for exposure to Lua.
 * <p/>
 * Any method exposed like this can be enumerated and called from a computer
 * that can see the node of the environment.
 * <p/>
 * Note that methods annotated with this interface must have the following
 * signature:
 * <pre>
 *     Object[] f(Computer computer, Arguments arguments);
 * </pre>
 *
 * @see Context
 * @see Arguments
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LuaCallback {
    /**
     * The name under which to make the callback available in Lua.
     *
     * @return the name of the function.
     */
    String value() default "";

    /**
     * Whether this function may be called asynchronously, i.e. directly from
     * the computer's executor thread.
     * <p/>
     * You will have to ensure anything your callback does is thread safe when
     * setting this to <tt>true</tt>. Use this for minor lookups, for example.
     * This is mainly intended to allow functions to perform faster than when
     * called synchronously (where the call takes at least one server tick).
     */
    boolean asynchronous() default false;
}
