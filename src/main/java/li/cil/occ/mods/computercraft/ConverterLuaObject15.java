package li.cil.occ.mods.computercraft;

import dan200.computer.api.ILuaObject;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.prefab.AbstractValue;

import java.util.Map;

public final class ConverterLuaObject15 implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof ILuaObject) {
            output.put("value", new LuaObjectValue((ILuaObject) value));
        }
    }

    public static final class LuaObjectValue extends AbstractValue implements ManagedPeripheral {
        private final ILuaObject value;

        protected final CallableHelper helper;

        // For loading when values were saved in a computer state.
        public LuaObjectValue() {
            value = null;
            helper = null;
        }

        public LuaObjectValue(final ILuaObject value) {
            this.value = value;
            helper = new CallableHelper(value.getMethodNames());
        }

        @Override
        public String[] methods() {
            if (value != null)
                return value.getMethodNames();
            return new String[0]; // Loaded userdata, missing context.
        }

        @Override
        public Object[] invoke(final String method, final Context context, final Arguments args) throws Exception {
            if (value != null) {
                final int index = helper.methodIndex(method);
                final Object[] argArray = helper.convertArguments(args);
                return value.callMethod(DriverPeripheral15.Environment.UnsupportedLuaContext.instance(), index, argArray);
            }
            return new Object[]{null, "ComputerCraft userdata cannot be persisted"};
        }
    }
}
