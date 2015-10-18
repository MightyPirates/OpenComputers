package li.cil.oc.api.machine;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Methods annotated with this are used to compute the "cost" to a callback.
 * <p/>
 * This is used exclusively for direct calls, i.e. methods with {@link Callback#direct} set to <tt>true</tt>.
 * <p/>
 * Unlike {@link Callback#limit}, which is a remnant of the dark ages, this must return the actual value
 * to remove from the calling machine's "call budget" for the current tick, not a (now) pseudo number of
 * allowed calls per tick.
 * <p/>
 * Therefore, the signature of methods annotated with this must be:
 * <pre>
 *     double f(Context context, Arguments arguments);
 * </pre>
 * <p/>
 * Note that if the returned value is larger than the remaining call budget, the call is still performed.
 * This is to avoid large values leading to blocking calls on systems with a total call budget smaller
 * than the returned cost value.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CallbackCost {
    /**
     * The name of the method the cost computation applies to.
     * <p/>
     * Note that this is expected to be the name of the target method, as specified in the {@link Callback}
     * annotation, which is not necessarily equal to the actual name of the method.
     */
    String value();
}
