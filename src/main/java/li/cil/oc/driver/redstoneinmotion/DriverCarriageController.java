package li.cil.oc.driver.redstoneinmotion;

import li.cil.oc.api.network.Arguments;
import li.cil.oc.api.network.Callback;
import li.cil.oc.api.network.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.driver.ManagedTileEntityEnvironment;
import li.cil.oc.driver.TileEntityDriver;
import li.cil.oc.util.Reflection;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;

public final class DriverCarriageController extends TileEntityDriver {
    private static final Class<?> CarriageControllerEntity = Reflection.getClass("JAKJ.RedstoneInMotion.CarriageControllerEntity");
    private static final Class<?> CarriageObstructionException = Reflection.getClass("JAKJ.RedstoneInMotion.CarriageObstructionException");
    private static final Class<?> Directions = Reflection.getClass("JAKJ.RedstoneInMotion.Directions");

    @Override
    public Class<?> getFilterClass() {
        return CarriageControllerEntity;
    }

    @Override
    public ManagedEnvironment createEnvironment(final World world, final int x, final int y, final int z) {
        return new Environment(world.getBlockTileEntity(x, y, z));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<TileEntity> {
        private boolean isAnchored;

        // Arguments for an actual move, stored here until we hit the next
        // call to update(). See below for more information as to why.
        private boolean shouldMove;
        private boolean isSimulating;
        private int direction;

        // Used to check whether we should send a success signal after loading.
        private boolean isMoving;

        // Used to delay success signals after a move to make sure the computer
        // that triggered the move is reachable again.
        private int signalDelay = 10;

        public Environment(final TileEntity tileEntity) {
            super(tileEntity, "carriage");
        }

        @Callback(direct = true)
        public Object[] getAnchored(final Context context, final Arguments args) {
            return new Object[]{isAnchored};
        }

        @Callback
        public Object[] setAnchored(final Context context, final Arguments args) {
            isAnchored = args.checkBoolean(0);
            return new Object[]{isAnchored};
        }

        @Callback
        public Object[] move(final Context context, final Arguments args) {
            // We execute moves in the update() call to the environment instead
            // of in here, because the move may cause the calling computer to
            // be persisted - which is not possible while it has an active call
            // (namely to this function).
            direction = checkDirection(args);
            isSimulating = args.count() > 1 && args.checkBoolean(1);
            shouldMove = true;
            context.pause(0.1);
            return new Object[]{true};
        }

        @Callback
        public Object[] simulate(final Context context, final Arguments args) {
            // IMPORTANT: we have to do the simulation asynchronously, too,
            // because that may also try to persist the computer that called us,
            // and it must not be running when we do that.
            direction = checkDirection(args);
            isSimulating = true;
            shouldMove = true;
            context.pause(0.1);
            return new Object[]{true};
        }

        @Override
        public boolean canUpdate() {
            return true;
        }

        @Override
        public void update() {
            if (node != null && node.network() != null && isMoving) {
                --signalDelay;
                if (signalDelay <= 0) {
                    isMoving = false;
                    node.sendToReachable("computer.signal", "carriage_moved", true);
                }
            }
            if (shouldMove) {
                shouldMove = false;
                isMoving = true;
                try {
                    Reflection.invoke(tileEntity, "SetupMotion", Directions.getEnumConstants()[direction], isSimulating, isAnchored);
                    Reflection.invoke(tileEntity, "Move");
                    if (isSimulating) {
                        node.sendToReachable("computer.signal", "carriage_moved", true);
                    }
                } catch (final Throwable e) {
                    if (CarriageObstructionException != null && CarriageObstructionException.isAssignableFrom(e.getClass())) {
                        try {
                            final int x = (Integer) Reflection.get(e, "X");
                            final int y = (Integer) Reflection.get(e, "Y");
                            final int z = (Integer) Reflection.get(e, "Z");
                            node.sendToReachable("computer.signal", "carriage_moved", false, e.getMessage() != null ? e.getMessage() : e.toString(), x, y, z);
                        } catch (Throwable e2) {
                            node.sendToReachable("computer.signal", "carriage_moved", false, e2.getMessage() != null ? e2.getMessage() : e2.toString());
                        }
                    } else {
                        node.sendToReachable("computer.signal", "carriage_moved", false, e.getMessage() != null ? e.getMessage() : e.toString());
                    }
                } finally {
                    // At this point we have already been saved if the move was
                    // successful, so we can safely always revert this to false.
                    isMoving = false;
                }
            }
        }

        @Override
        public void load(final NBTTagCompound nbt) {
            super.load(nbt);
            isMoving = nbt.getBoolean("moving");
            isAnchored = nbt.getBoolean("anchored");
        }

        @Override
        public void save(final NBTTagCompound nbt) {
            super.save(nbt);
            nbt.setBoolean("moving", isMoving);
            nbt.setBoolean("anchored", isAnchored);
        }

        private int checkDirection(final Arguments args) {
            if (shouldMove || isMoving) {
                throw new RuntimeException("already moving");
            }
            if (args.isString(0)) {
                final String name = args.checkString(0).toLowerCase();
                if (!sideNames.containsKey(name)) {
                    throw new IllegalArgumentException("invalid direction");
                }
                return sideNames.get(name);
            } else {
                final int index = args.checkInteger(0);
                if (index < 0 || index > 5) {
                    throw new IllegalArgumentException("invalid direction");
                }
                return index;
            }
        }

        private static final Map<String, Integer> sideNames;

        static {
            sideNames = new HashMap<String, Integer>();
            sideNames.put("negy", 0);
            sideNames.put("posy", 1);
            sideNames.put("negz", 2);
            sideNames.put("posz", 3);
            sideNames.put("negx", 4);
            sideNames.put("posx", 5);

            sideNames.put("down", 0);
            sideNames.put("up", 1);
            sideNames.put("north", 2);
            sideNames.put("south", 3);
            sideNames.put("west", 4);
            sideNames.put("east", 5);
        }
    }
}
