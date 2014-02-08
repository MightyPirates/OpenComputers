package li.cil.oc.api.prefab;

import com.google.common.collect.Iterables;
import dan200.computer.api.*;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.Node;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of the <tt>ManagedPeripheral</tt> interface for simple
 * wrapping of ComputerCraft peripherals.
 */
public class ManagedPeripheral extends ManagedEnvironment implements li.cil.oc.api.network.ManagedPeripheral {
    protected final IPeripheral peripheral;

    protected final List<String> _methods;

    protected final Map<String, FakeComputerAccess> accesses = new HashMap<String, FakeComputerAccess>();

    public ManagedPeripheral(final IPeripheral peripheral) {
        this.peripheral = peripheral;
        _methods = Arrays.asList(peripheral.getMethodNames());
    }

    @Override
    public String[] methods() {
        return peripheral.getMethodNames();
    }

    @Override
    public Object[] invoke(final String method, final Context context, final Arguments args) throws Exception {
        final int index = _methods.indexOf(method);
        if (index < 0) {
            throw new NoSuchMethodException();
        }
        final Object[] argArray = Iterables.toArray(args, Object.class);
        for (int i = 0; i < argArray.length; ++i) {
            if (argArray[i] instanceof byte[]) {
                argArray[i] = new String((byte[]) argArray[i], "UTF-8");
            }
        }
        final FakeComputerAccess access;
        if (accesses.containsKey(context.address())) {
            access = accesses.get(context.address());
        } else {
            // The calling contexts is not visible to us, meaning we never got
            // an onConnect for it. Create a temporary access.
            access = new FakeComputerAccess(this, context);
        }
        return peripheral.callMethod(access, UnsupportedLuaContext.instance(), index, argArray);
    }

    @Override
    public void onConnect(final Node node) {
        super.onConnect(node);
        if (node.host() instanceof Context) {
            final FakeComputerAccess access = new FakeComputerAccess(this, (Context) node.host());
            accesses.put(node.address(), access);
            peripheral.attach(access);
        }
    }

    @Override
    public void onDisconnect(final Node node) {
        super.onDisconnect(node);
        if (node.host() instanceof Context) {
            final FakeComputerAccess access = accesses.remove(node.address());
            if (access != null) {
                peripheral.detach(access);
            }
        } else if (node == this.node) {
            for (FakeComputerAccess access : accesses.values()) {
                peripheral.detach(access);
            }
            accesses.clear();
        }
    }

    /**
     * Map interaction with the computer to our format as good as we can.
     */
    private static class FakeComputerAccess implements IComputerAccess {
        protected final ManagedPeripheral owner;
        protected final Context context;

        public FakeComputerAccess(final ManagedPeripheral owner, final Context context) {
            this.owner = owner;
            this.context = context;
        }

        @Override
        public String mount(final String desiredLocation, final IMount mount) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String mountWritable(final String desiredLocation, final IWritableMount mount) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unmount(final String location) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int getID() {
            return context.address().hashCode();
        }

        @Override
        public void queueEvent(final String event, final Object[] arguments) {
            context.signal(event, arguments);
        }

        @Override
        public String getAttachmentName() {
            return owner.node.address();
        }
    }

    /**
     * Since we abstract away anything language specific, we cannot support the
     * Lua context specific operations ComputerCraft provides.
     */
    private final static class UnsupportedLuaContext implements ILuaContext {
        protected static final UnsupportedLuaContext Instance = new UnsupportedLuaContext();

        private UnsupportedLuaContext() {
        }

        public static UnsupportedLuaContext instance() {
            return Instance;
        }

        @Override
        public Object[] pullEvent(final String filter) throws Exception {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] pullEventRaw(final String filter) throws InterruptedException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object[] yield(final Object[] arguments) throws InterruptedException {
            throw new UnsupportedOperationException();
        }
    }
}
