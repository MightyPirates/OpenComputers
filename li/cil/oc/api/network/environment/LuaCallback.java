package li.cil.oc.api.network.environment;

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
}
