package li.cil.oc.common.asm.template;

import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;

// This is a template implementation of methods injected into classes that are
// marked for component functionality. These methods will be copied into tile
// entities marked as simple components as necessary by the class transformer.
@SuppressWarnings("unused")
public abstract class SimpleEnvironment extends TileEntity implements SimpleComponentImpl {
    public SimpleEnvironment(TileEntityType<?> type) {
        super(type);
    }
    
    @Override
    public Node node() {
        return StaticSimpleEnvironment.node(this);
    }

    @Override
    public void onConnect(Node node) {
    }

    @Override
    public void onDisconnect(Node node) {
    }

    @Override
    public void onMessage(Message message) {
    }

    // These are always injected, after possibly existing versions have been
    // renamed to the below variants from the SimpleComponentImpl interface.
    // This allows transparent wrapping of already present implementations,
    // instead of plain overwriting them.

    @Override
    public void clearRemoved() {
        StaticSimpleEnvironment.validate(this);
    }

    @Override
    public void setRemoved() {
        StaticSimpleEnvironment.invalidate(this);
    }

    @Override
    public void onChunkUnloaded() {
        StaticSimpleEnvironment.onChunkUnloaded(this);
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        StaticSimpleEnvironment.load(this, state, nbt);
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        return StaticSimpleEnvironment.save(this, nbt);
    }

    // The following methods are only injected if their real versions do not
    // exist in the class we're injecting into. Otherwise their real versions
    // are renamed to these variations, which simply delegate to the parent.
    // This way they are always guaranteed to be present, so we can simply call
    // them through an interface, and need no runtime reflection.

    public void validate_OpenComputers() {
        super.clearRemoved();
    }

    public void invalidate_OpenComputers() {
        super.setRemoved();
    }

    public void onChunkUnloaded_OpenComputers() {
        super.onChunkUnloaded();
    }

    public void readFromNBT_OpenComputers(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
    }

    public CompoundNBT writeToNBT_OpenComputers(CompoundNBT nbt) {
        return super.save(nbt);
    }
}
