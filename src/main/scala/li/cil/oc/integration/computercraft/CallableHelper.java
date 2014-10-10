package li.cil.oc.integration.computercraft;

import com.google.common.collect.Iterables;
import li.cil.oc.api.machine.Arguments;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

public final class CallableHelper {
    private final List<String> _methods;

    public CallableHelper(final String[] methods) {
        _methods = Arrays.asList(methods);
    }

    public int methodIndex(final String method) throws NoSuchMethodException {
        final int index = _methods.indexOf(method);
        if (index < 0) {
            throw new NoSuchMethodException();
        }
        return index;
    }

    public Object[] convertArguments(final Arguments args) throws UnsupportedEncodingException {
        final Object[] argArray = Iterables.toArray(args, Object.class);
        for (int i = 0; i < argArray.length; ++i) {
            if (argArray[i] instanceof byte[]) {
                argArray[i] = new String((byte[]) argArray[i], "UTF-8");
            }
        }
        return argArray;
    }
}
