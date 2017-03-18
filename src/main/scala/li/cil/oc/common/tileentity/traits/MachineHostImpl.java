package li.cil.oc.common.tileentity.traits;

import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.NodeContainerHost;
import li.cil.oc.api.util.Location;
import li.cil.oc.common.inventory.ItemHandlerIterableWrapper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;

public final class MachineHostImpl implements MachineHost, LocationProxy, INBTSerializable<NBTTagCompound> {
    public interface Computer extends Location {
        void onRunningChanged();
    }

    // ----------------------------------------------------------------------- //
    // Persisted state.

    private final Machine machine = li.cil.oc.api.Machine.create(this);

    // ----------------------------------------------------------------------- //
    // Computed state.

    private final Computer computer;
    private final NodeContainerHost nodeContainerHost;
    private final Iterable<ItemStack> internalItems;

    private boolean isRunning;

    // For client side rendering of error LED indicator.
    private boolean didCrash;

    // ----------------------------------------------------------------------- //

    public MachineHostImpl(final Computer computer, final NodeContainerHost nodeContainerHost, final IItemHandler itemHandler) {
        this.computer = computer;
        this.nodeContainerHost = nodeContainerHost;
        this.internalItems = new ItemHandlerIterableWrapper(itemHandler);
    }

    public void setRunning(final boolean value) {
        if (value == isRunning) {
            return;
        }
        isRunning = value;
        if (isRunning) {
            didCrash = false;
        }

//    if (getWorld != null) {
//      getWorld.notifyBlockUpdate(getPos, getWorld.getBlockState(getPos), getWorld.getBlockState(getPos), 3)
//      if (getWorld.isRemote) {
//        runSound.foreach(sound =>
//          if (_isRunning) Sound.startLoop(this, sound, 0.5f, 50 + getWorld.rand.nextInt(50))
//          else Sound.stopLoop(this)
//        )
//      }
//    }
    }

    public void update() {
        machine.update();

        final boolean newIsRunning = machine.isRunning();
        final boolean newDidCrash = machine.lastError() != null;
        if (isRunning != newIsRunning || didCrash != newDidCrash) {
            isRunning = newIsRunning;
            didCrash = newDidCrash;
            computer.onRunningChanged();
        }
    }

    public void dispose() {
        machine.stop();
    }

    // ----------------------------------------------------------------------- //
    // MachineHost

    @Override
    public Machine getMachine() {
        return machine;
    }

    @Override
    public Iterable<ItemStack> internalComponents() {
        return internalItems;
    }

    // ----------------------------------------------------------------------- //
    // NodeContainerHost

    @Override
    public void markHostChanged() {
        nodeContainerHost.markHostChanged();
    }

    // ----------------------------------------------------------------------- //
    // LocationProxy

    @Override
    public Location getLocation() {
        return computer;
    }

    // ----------------------------------------------------------------------- //
    // INBTSerializable

    @Override
    public NBTTagCompound serializeNBT() {
        return machine.serializeNBT();
    }

    @Override
    public void deserializeNBT(final NBTTagCompound nbt) {
        machine.deserializeNBT(nbt);
//    // God, this is so ugly... will need to rework the robot architecture.
//    // This is required for loading auxiliary data (kernel state), because the
//    // coordinates in the actual robot won't be set properly, otherwise.
//    this match {
//      case proxy: TileEntityRobot => proxy.robot.setPos(getPos)
//      case _ =>
//    }
//    machine.load(nbt.getCompoundTag(ComputerTag))
//
    // Kickstart initialization to avoid values getting overwritten by
    // readFromNBTForClient if that packet is handled after a manual
    // initialization / state change packet.
    setRunning(machine.isRunning());
//    _isOutputEnabled = hasRedstoneCard
    }

    // extends Environment with ComponentManager with RotatableImpl with BundledRedstoneAware with api.network.Analyzable with StateAware with Tickable
//  override def getNode = if (isServer) machine.node else null
//
//  protected def runSound = Option("computer_running")
//
//  // ----------------------------------------------------------------------- //
//
//  override def getCurrentState = {
//    if (isRunning) util.EnumSet.of(api.util.StateAware.State.IsWorking)
//    else util.EnumSet.noneOf(classOf[api.util.StateAware.State])
//  }
//
//  // ----------------------------------------------------------------------- //
//
//  def hasRedstoneCard = items.exists {
//    case Some(item) => machine.isRunning && DriverRedstoneCard.worksWith(item, getClass)
//    case _ => false
//  }
//
//  // ----------------------------------------------------------------------- //
//
//  override def markDirty() {
//    super.markDirty()
//    if (isServer) {
//      machine.onHostChanged()
//      isOutputEnabled = hasRedstoneCard
//    }
//  }
//
//  override protected def onRotationChanged() {
//    super.onRotationChanged()
//    checkRedstoneInputChanged()
//  }
//
//  override protected def onRedstoneInputChanged(side: EnumFacing, oldMaxValue: Int, newMaxValue: Int) {
//    super.onRedstoneInputChanged(side, oldMaxValue, newMaxValue)
//    machine.node.sendToNeighbors("redstone.changed", toLocal(side), Int.box(oldMaxValue), Int.box(newMaxValue))
//  }
//
//  // ----------------------------------------------------------------------- //
}
