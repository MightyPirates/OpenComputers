package li.cil.oc.common.asm.template;

import com.google.common.base.Strings;
import li.cil.oc.api.Network;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.common.asm.SimpleComponentTickHandler;
import li.cil.oc.util.SideTracker;
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
        if (SideTracker.isClient()) {
            return null;
        }
        final String name = self.getComponentName();
        // If the name is null (or empty) this indicates we don't have a valid
        // component right now, so if we have a node we kill it.
        if (Strings.isNullOrEmpty(name)) {
            final Node node = nodes.remove(self);
            if (node != null) {
                node.remove();
            }
        } else if (!nodes.containsKey(self)) {
            nodes.put(self, Network.
                    newNode(self, Visibility.Network).
                    withComponent(name).
                    create());
        }
        return nodes.get(self);
    }

    public static void validate(final SimpleComponentImpl self) {
        self.validate_OpenComputers();
        SimpleComponentTickHandler.schedule((TileEntity) self);
    }

    public static void invalidate(final SimpleComponentImpl self) {
        self.invalidate_OpenComputers();
        final Node node = node(self);
        if (node != null) {
            node.remove();
            nodes.remove(self);
        }
    }

    public static void onChunkUnload(final SimpleComponentImpl self) {
        self.onChunkUnload_OpenComputers();
        final Node node = node(self);
        if (node != null) {
            node.remove();
            nodes.remove(self);
        }
    }

    public static void readFromNBT(final SimpleComponentImpl self, NBTTagCompound nbt) {
        self.readFromNBT_OpenComputers(nbt);
        final Node node = node(self);
        if (node != null) {
            node.load(nbt.getCompoundTag("oc:node"));
        }
    }

    public static void writeToNBT(final SimpleComponentImpl self, NBTTagCompound nbt) {
        self.writeToNBT_OpenComputers(nbt);
        final Node node = node(self);
        if (node != null) {
            final NBTTagCompound nodeNbt = new NBTTagCompound();
            node.save(nodeNbt);
            nbt.setTag("oc:node", nodeNbt);
        }
    }
}
