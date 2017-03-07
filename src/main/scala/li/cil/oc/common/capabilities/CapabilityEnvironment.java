package li.cil.oc.common.capabilities;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
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
    @CapabilityInject(Environment.class)
    public static Capability<Environment> ENVIRONMENT_CAPABILITY;

    public static void register() {
        CapabilityManager.INSTANCE.register(Environment.class, new Capability.IStorage<Environment>() {
            @Nullable
            @Override
            public NBTBase writeNBT(final Capability<Environment> capability, final Environment instance, final EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(final Capability<Environment> capability, final Environment instance, final EnumFacing side, final NBTBase nbt) {
            }
        }, () -> new Environment() {
            @Override
            public EnvironmentHost getHost() {
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
