package li.cil.oc.common.capabilities;

import li.cil.oc.api.util.Location;
import li.cil.oc.api.network.NodeContainer;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;

public final class CapabilityEnvironment {
    @CapabilityInject(NodeContainer.class)
    public static Capability<NodeContainer> ENVIRONMENT_CAPABILITY;

    public static void register() {
        CapabilityManager.INSTANCE.register(NodeContainer.class, new Capability.IStorage<NodeContainer>() {
            @Nullable
            @Override
            public NBTBase writeNBT(final Capability<NodeContainer> capability, final NodeContainer instance, final EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(final Capability<NodeContainer> capability, final NodeContainer instance, final EnumFacing side, final NBTBase nbt) {
            }
        }, () -> new NodeContainer() {
            @Override
            public Location getLocation() {
                return null;
            }

            @Override
            public Node getNode() {
                return null;
            }

            @Override
            public void onConnect(final Node node) {
            }

            @Override
            public void onDisconnect(final Node node) {
            }

            @Override
            public void onMessage(final Message message) {
            }

            @Override
            public boolean canConnect() {
                return false;
            }

            @Override
            public NBTTagCompound serializeNBT() {
                return new NBTTagCompound();
            }

            @Override
            public void deserializeNBT(final NBTTagCompound nbt) {
            }
        });
    }

    private CapabilityEnvironment() {
    }
}
