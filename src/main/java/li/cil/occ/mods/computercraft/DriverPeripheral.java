package li.cil.occ.mods.computercraft;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.occ.OpenComponents;
import li.cil.occ.util.Reflection;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import com.google.common.collect.Iterables;

import dan200.computer.api.IComputerAccess;
import dan200.computer.api.ILuaContext;
import dan200.computer.api.IMount;
import dan200.computer.api.IPeripheral;
import dan200.computer.api.IPeripheralHandler;
import dan200.computer.api.IWritableMount;

public final class DriverPeripheral implements li.cil.oc.api.driver.Block {
    private static final Set<Class<?>> blacklist = new HashSet<Class<?>>();
    private static Method cc_getPeripheralFromClass;

    static {
        for (String name : OpenComponents.peripheralBlacklist) {
            final Class<?> clazz = Reflection.getClass(name);
            if (clazz != null) {
                blacklist.add(clazz);
            }
        }

        try {
            cc_getPeripheralFromClass = Class.forName( "dan200.ComputerCraft" ).getMethod("getPeripheralFromClass", Class.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public IPeripheral findPeripheral(TileEntity te) {
        if (te instanceof IPeripheral) {
            return (IPeripheral)te;
        } else {
            try {
                if (cc_getPeripheralFromClass != null) {
                    IPeripheralHandler iph = ((IPeripheralHandler)cc_getPeripheralFromClass.invoke(null, te.getClass()));
                    if (iph != null)
                        return iph.getPeripheral(te);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    
    @Override
    public boolean worksWith(final World world, final int x, final int y, final int z) {
        final TileEntity tileEntity = world.getBlockTileEntity(x, y, z);
        return tileEntity != null
                // This ensures we don't get duplicate components, in case the
                // tile entity is natively compatible with OpenComputers.
                && !li.cil.oc.api.network.Environment.class.isAssignableFrom(tileEntity.getClass())
                // The black list is used to avoid peripherals that are known
                // to be incompatible with OpenComputers when used directly.
                && !blacklist.contains(tileEntity.getClass())
                // Actual check if it's a peripheral.
                && findPeripheral(tileEntity) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment(findPeripheral(world.getBlockTileEntity(x, y, z)));
    }

    public static class Environment extends li.cil.oc.api.prefab.ManagedEnvironment implements li.cil.oc.api.network.ManagedPeripheral {
        protected final IPeripheral peripheral;

        protected final List<String> _methods;

        protected final Map<String, FakeComputerAccess> accesses = new HashMap<String, FakeComputerAccess>();

        public Environment(final IPeripheral peripheral) {
            this.peripheral = peripheral;
            _methods = Arrays.asList(peripheral.getMethodNames());
            node = Network.newNode(this, Visibility.Network).create();
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
            if (accesses.containsKey(context.node().address())) {
                access = accesses.get(context.node().address());
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
                    access.close();
                }
                accesses.clear();
            }
        }

        /**
         * Map interaction with the computer to our format as good as we can.
         */
        private static class FakeComputerAccess implements IComputerAccess {
            protected final Environment owner;
            protected final Context context;
            protected final Map<String, li.cil.oc.api.network.ManagedEnvironment> fileSystems = new HashMap<String, li.cil.oc.api.network.ManagedEnvironment>();

            public FakeComputerAccess(final Environment owner, final Context context) {
                this.owner = owner;
                this.context = context;
            }

            public void close() {
                for (li.cil.oc.api.network.ManagedEnvironment fileSystem : fileSystems.values()) {
                    fileSystem.node().remove();
                }
                fileSystems.clear();
            }

            @Override
            public String mount(final String desiredLocation, final IMount mount) {
                if (fileSystems.containsKey(desiredLocation)) {
                    return null;
                }
                return mount(desiredLocation, FileSystem.asManagedEnvironment(FileSystem.fromComputerCraft(mount)));
            }

            @Override
            public String mountWritable(final String desiredLocation, final IWritableMount mount) {
                if (fileSystems.containsKey(desiredLocation)) {
                    return null;
                }
                return mount(desiredLocation, FileSystem.asManagedEnvironment(FileSystem.fromComputerCraft(mount)));
            }

            private String mount(final String path, final li.cil.oc.api.network.ManagedEnvironment fileSystem) {
                fileSystems.put(path, fileSystem); //TODO This is per peripheral/Environment. It would be far better with per computer
                context.node().connect(fileSystem.node());
                return path;
            }

            @Override
            public void unmount(final String location) {
                final li.cil.oc.api.network.ManagedEnvironment fileSystem = fileSystems.remove(location);
                if (fileSystem != null) {
                    fileSystem.node().remove();
                }
            }

            @Override
            public int getID() {
                return context.node().address().hashCode();
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
}
