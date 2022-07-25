package li.cil.oc.integration.computercraft;

import dan200.computercraft.api.lua.IDynamicLuaObject;
import dan200.computercraft.api.lua.ObjectArguments;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedPeripheral;
import li.cil.oc.api.prefab.AbstractValue;

import java.util.Map;

public final class ConverterLuaObject implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof IDynamicLuaObject) {
            output.put("value", new LuaObjectValue((IDynamicLuaObject) value));
        }
    }

    public static final class LuaObjectValue extends AbstractValue implements ManagedPeripheral {
        private final IDynamicLuaObject value;

        protected final CallableHelper helper;

        // For loading when values were saved in a computer state.
        public LuaObjectValue() {
            value = null;
            helper = null;
        }

        public LuaObjectValue(final IDynamicLuaObject value) {
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
                final Object[] argArray = CallableHelper.convertArguments(args);
                return value.callMethod(DriverPeripheral.Environment.UnsupportedLuaContext.instance(), index, new ObjectArguments(argArray)).getResult();
            }
            return new Object[]{null, "ComputerCraft userdata cannot be persisted"};
        }
    }
}
