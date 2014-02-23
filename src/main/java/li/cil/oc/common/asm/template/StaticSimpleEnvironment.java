package li.cil.oc.common.asm.template;

import cpw.mods.fml.common.FMLCommonHandler;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.asm.SimpleComponentTickHandler;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import java.util.HashMap;
import java.util.Map;

// This class contains actual implementations of methods injected into tile
// entities marked as simple components using the SimpleComponent interface.
// They are called from the template methods, to keep the injected methods
// minimal, instruction wise, and avoid weird dependencies making the injection
// unnecessarily complicated.
public final class StaticSimpleEnvironment {
    private StaticSimpleEnvironment() {
    }

    private static final Map<Environment, Node> nodes = new HashMap<Environment, Node>();

    public static Node node(final SimpleComponentImpl self) {
        // Save ourselves the lookup time in the hash map and avoid mixing in
        // client side tile entities into the map when in single player.
        if (FMLCommonHandler.instance().getEffectiveSide().isClient()) {
            return null;
        }
        if (!nodes.containsKey(self)) {
            final String name = self.getComponentName();
            nodes.put(self, Network.
                    newNode(self, Visibility.Network).
                    withComponent(name).
                    create());
        }
        return nodes.get(self);
    }

    public static void validate(final SimpleComponentImpl self) {
        self.validate0();
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
            synchronized (SimpleComponentTickHandler.pendingOperations) {
                SimpleComponentTickHandler.pendingOperations.add(new Runnable() {
                    @Override
                    public void run() {
                        Network.joinOrCreateNetwork((TileEntity) self);
                    }
                });
            }
        }
    }

    public static void invalidate(final SimpleComponentImpl self) {
        self.invalidate0();
        final Node node = node(self);
        if (node != null) {
            node.remove();
            nodes.remove(self);
        }
    }

    public static void onChunkUnload(final SimpleComponentImpl self) {
        self.onChunkUnload0();
        final Node node = node(self);
        if (node != null) {
            node.remove();
            nodes.remove(self);
        }
    }

    public static void readFromNBT(final SimpleComponentImpl self, NBTTagCompound nbt) {
        self.readFromNBT0(nbt);
        final Node node = node(self);
        if (node != null) {
            node.load(nbt.getCompoundTag("oc:node"));
        }
    }

    public static void writeToNBT(final SimpleComponentImpl self, NBTTagCompound nbt) {
        self.writeToNBT0(nbt);
        final Node node = node(self);
        if (node != null) {
            final NBTTagCompound nodeNbt = new NBTTagCompound();
            node.save(nodeNbt);
            nbt.setTag("oc:node", nodeNbt);
        }
    }
}
