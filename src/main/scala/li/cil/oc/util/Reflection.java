package li.cil.oc.util;

import javax.annotation.Nullable;
import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class Reflection {
    @Nullable
    public static Class<?> getClass(final String name) {
        try {
            return Class.forName(name);
        } catch (final ClassNotFoundException ignored) {
            return null;
        }
    }

    @Nullable
    public static Object get(final Object instance, final String fieldName) {
        try {
            final Field field = instance.getClass().getField(fieldName);
            return field.get(instance);
        } catch (final IllegalAccessException | NoSuchFieldException ignored) {
            return null;
        }
    }

    public static void set(final Object instance, final String fieldName, final Object value) {
        try {
            final Field field = instance.getClass().getField(fieldName);
            field.set(instance, value);
        } catch (final IllegalAccessException | NoSuchFieldException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T invoke(final Object instance, final String methodName, final Object... args) throws Throwable {
        try {
            outer:
            for (final Method method : instance.getClass().getMethods()) {
                if (method.getName().equals(methodName) && method.getParameterTypes().length == args.length) {
                    final Class<?>[] argTypes = method.getParameterTypes();
                    for (int i = 0; i < argTypes.length; ++i) {
                        final Class<?> have = argTypes[i];
                        final Class<?> given = args[i].getClass();
                        // Fail if not assignable and not assignable to primitive.
                        if (!have.isAssignableFrom(given) && (!have.isPrimitive()
                                || (!(byte.class.equals(have) && Byte.class.equals(given))
                                && !(short.class.equals(have) && Short.class.equals(given))
                                && !(int.class.equals(have) && Integer.class.equals(given))
                                && !(long.class.equals(have) && Long.class.equals(given))
                                && !(float.class.equals(have) && Float.class.equals(given))
                                && !(double.class.equals(have) && Double.class.equals(given))
                                && !(boolean.class.equals(have) && Boolean.class.equals(given))
                                && !(char.class.equals(have) && Character.class.equals(given))))) {
                            continue outer;
                        }
                    }
                    return (T) method.invoke(instance, args);
                }
            }
            return null;
        } catch (final InvocationTargetException e) {
            throw e.getCause();
        } catch (final IllegalAccessException | ClassCastException e) {
            return null;
        }
    }

    @Nullable
    public static <T> T tryInvoke(final Object instance, final String methodName, final Object... args) {
        try {
            return invoke(instance, methodName, args);
        } catch (final Throwable ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T getStaticMethod(final String name, final Class<T> fInterface) {
        try {
            final Method fMethod = fInterface.getDeclaredMethods()[0];

            final int nameSplit = name.lastIndexOf('.');
            final String className = name.substring(0, nameSplit);
            final String methodName = name.substring(nameSplit + 1);
            final Class<?> clazz = Class.forName(className);

            final MethodHandles.Lookup caller = MethodHandles.lookup();
            final MethodType methodType = MethodType.methodType(fMethod.getReturnType(), fMethod.getParameterTypes());
            final MethodHandle methodHandle = caller.findStatic(clazz, methodName, methodType);
            final CallSite site = LambdaMetafactory.metafactory(caller,
                    fMethod.getName(),
                    MethodType.methodType(fInterface),
                    methodType,
                    methodHandle,
                    methodType);
            final MethodHandle factory = site.getTarget();
            return (T) factory.invoke();
        } catch (final Throwable t) {
            throw new IllegalArgumentException("Failed resolving method '" + name + "' to functional interface '" + fInterface.getSimpleName() + "'.", t);
        }
    }

    // ----------------------------------------------------------------------- //

    private Reflection() {
    }
}
