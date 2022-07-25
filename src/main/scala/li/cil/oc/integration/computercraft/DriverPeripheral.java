package li.cil.oc.integration.computercraft;

import dan200.computercraft.api.filesystem.IMount;
import dan200.computercraft.api.filesystem.IWritableMount;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.ILuaTask;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.lua.ObjectArguments;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IWorkMonitor;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.apis.PeripheralAPI;
import dan200.computercraft.core.asm.PeripheralMethod;
import li.cil.oc.OpenComputers;
import li.cil.oc.Settings;
import li.cil.oc.api.FileSystem;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.BlacklistedPeripheral;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.util.Reflection;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class DriverPeripheral implements li.cil.oc.api.driver.DriverBlock {
    private static Set<Class<?>> blacklist;

    private boolean isBlacklisted(final Object o) {
        // Check for our interface first, as that has priority.
        if (o instanceof BlacklistedPeripheral) {
            return ((BlacklistedPeripheral) o).isPeripheralBlacklisted();
        }

        // Delayed initialization of the resolved classes to allow registering
        // additional entries via IMC.
        if (blacklist == null) {
            blacklist = new HashSet<Class<?>>();
            for (String name : Settings.get().peripheralBlacklist()) {
                final Class<?> clazz = Reflection.getClass(name);
                if (clazz != null) {
                    blacklist.add(clazz);
                }
            }
        }
        for (Class<?> clazz : blacklist) {
            if (clazz.isInstance(o))
                return true;
        }
        return false;
    }

    private IPeripheral findPeripheral(final World world, final BlockPos pos, final Direction side) {
        try {
            final IPeripheral p = dan200.computercraft.shared.Peripherals.getPeripheral(world, pos, side, cap -> {});
            if (!isBlacklisted(p)) {
                return p;
            }
        } catch (Exception e) {
            OpenComputers.log().warn(String.format("Error accessing ComputerCraft peripheral @ (%d, %d, %d).", pos.getX(), pos.getY(), pos.getZ()), e);
        }
        return null;
    }

    @Override
    public boolean worksWith(final World world, final BlockPos pos, final Direction side) {
        final TileEntity tileEntity = world.getBlockEntity(pos);
        return tileEntity != null
                // This ensures we don't get duplicate components, in case the
                // tile entity is natively compatible with OpenComputers.
                && !li.cil.oc.api.network.Environment.class.isAssignableFrom(tileEntity.getClass())
                // The black list is used to avoid peripherals that are known
                // to be incompatible with OpenComputers when used directly.
                && !isBlacklisted(tileEntity)
                // Actual check if it's a peripheral.
                && findPeripheral(world, pos, side) != null;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final BlockPos pos, final Direction side) {
        return new Environment(findPeripheral(world, pos, side));
    }

    public static class Environment extends li.cil.oc.api.prefab.AbstractManagedEnvironment implements li.cil.oc.api.network.ManagedPeripheral, NamedBlock {
        protected final IPeripheral peripheral;

        protected final Map<String, PeripheralMethod> methods;
        protected final String[] methodNames;

        protected final Map<String, FakeComputerAccess> accesses = new HashMap<String, FakeComputerAccess>();

        public Environment(final IPeripheral peripheral) {
            this.peripheral = peripheral;
            methods = PeripheralAPI.getMethods(peripheral);
            methodNames = methods.keySet().toArray(new String[methods.size()]);
            setNode(Network.newNode(this, Visibility.Network).create());
        }

        @Override
        public String[] methods() {
            return methodNames;
        }

        @Override
        public Object[] invoke(final String name, final Context context, final Arguments args) throws Exception {
            final Object[] argArray = CallableHelper.convertArguments(args);
            final PeripheralMethod method = methods.get(name);
            if (method == null) throw new NoSuchMethodException();
            final FakeComputerAccess access;
            if (accesses.containsKey(context.node().address())) {
                access = accesses.get(context.node().address());
            } else {
                // The calling contexts is not visible to us, meaning we never got
                // an onConnect for it. Create a temporary access.
                access = new FakeComputerAccess(this, context);
            }
            return method.apply(peripheral, UnsupportedLuaContext.instance(), access, new ObjectArguments(argArray)).getResult();
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
            } else if (node == this.node()) {
                for (FakeComputerAccess access : accesses.values()) {
                    peripheral.detach(access);
                    access.close();
                }
                accesses.clear();
            }
        }

        @Override
        public String preferredName() {
            return peripheral.getType();
        }

        @Override
        public int priority() {
            return -1; // Lower than 'real' OC components
        }

        /**
         * Map interaction with the computer to our format as good as we can.
         */
        public static class FakeComputerAccess implements IComputerAccess {
            protected final Environment owner;
            protected final Context context;
            protected final Map<String, ManagedEnvironment> fileSystems = new HashMap<String, ManagedEnvironment>();

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
                return mount(desiredLocation, FileSystem.asManagedEnvironment(DriverComputerCraftMedia.fromComputerCraft(mount)));
            }

            @Override
            public String mount(String desiredLocation, IMount mount, String driveName) {
                if (fileSystems.containsKey(desiredLocation)) {
                    return null;
                }
                return mount(desiredLocation, FileSystem.asManagedEnvironment(DriverComputerCraftMedia.fromComputerCraft(mount), driveName));
            }

            @Override
            public String mountWritable(final String desiredLocation, final IWritableMount mount) {
                if (fileSystems.containsKey(desiredLocation)) {
                    return null;
                }
                return mount(desiredLocation, FileSystem.asManagedEnvironment(DriverComputerCraftMedia.fromComputerCraft(mount)));
            }

            @Override
            public String mountWritable(String desiredLocation, IWritableMount mount, String driveName) {
                if (fileSystems.containsKey(desiredLocation)) {
                    return null;
                }
                return mount(desiredLocation, FileSystem.asManagedEnvironment(DriverComputerCraftMedia.fromComputerCraft(mount), driveName));
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
                return owner.node().address();
            }

            @Override
            public Map<String, IPeripheral> getAvailablePeripherals() {
                return Collections.emptyMap();
            }

            @Override
            public IPeripheral getAvailablePeripheral(String name) {
                return null;
            }

            @Override
            public IWorkMonitor getMainThreadMonitor() {
                throw new UnsupportedOperationException();
            }
        }

        /**
         * Since we abstract away anything language specific, we cannot support the
         * Lua context specific operations ComputerCraft provides.
         */
        public final static class UnsupportedLuaContext implements ILuaContext {
            protected static final UnsupportedLuaContext Instance = new UnsupportedLuaContext();

            private UnsupportedLuaContext() {
            }

            public static UnsupportedLuaContext instance() {
                return Instance;
            }

            @Override
            public long issueMainThreadTask(ILuaTask task) throws LuaException {
                throw new UnsupportedOperationException();
            }

            @Override
            public MethodResult executeMainThreadTask(ILuaTask task) throws LuaException {
                throw new UnsupportedOperationException();
            }
        }
    }
}
