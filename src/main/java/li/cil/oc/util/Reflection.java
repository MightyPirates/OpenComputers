package li.cil.oc.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class Reflection {
    private Reflection() {
    }

    public static Class<?> getClass(final String name) {
        try {
            return Class.forName(name);
        } catch (final ClassNotFoundException ignored) {
            return null;
        }
    }

    public static Object get(final Object instance, final String fieldName) {
        try {
            final Field field = instance.getClass().getField(fieldName);
            return field.get(instance);
        } catch (final IllegalAccessException ignored) {
            return null;
        } catch (final NoSuchFieldException ignored) {
            return null;
        }
    }

    public static void set(final Object instance, final String fieldName, final Object value) {
        try {
            final Field field = instance.getClass().getField(fieldName);
            field.set(instance, value);
        } catch (final IllegalAccessException ignored) {
        } catch (final NoSuchFieldException ignored) {
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T invoke(final Object instance, final String methodName, final Object... args) throws Throwable {
        try {
            outer:
            for (Method method : instance.getClass().getMethods()) {
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
        } catch (InvocationTargetException e) {
            throw e.getCause();
        } catch (IllegalAccessException e) {
            return null;
        } catch (ClassCastException e) {
            return null;
        }
    }

    public static <T> T tryInvoke(final Object instance, final String methodName, final Object... args) {
        try {
            return invoke(instance, methodName, args);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
