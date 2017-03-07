package li.cil.oc.common.capabilities;

import li.cil.oc.api.tileentity.Colored;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;

public final class CapabilityColored {
    @CapabilityInject(Colored.class)
    public static Capability<Colored> COLORED_CAPABILITY;

    public static void register() {
        CapabilityManager.INSTANCE.register(Colored.class, new Capability.IStorage<Colored>() {
            @Nullable
            @Override
            public NBTBase writeNBT(final Capability<Colored> capability, final Colored instance, final EnumFacing side) {
                return instance.serializeNBT();
            }

            @Override
            public void readNBT(final Capability<Colored> capability, final Colored instance, final EnumFacing side, final NBTBase nbt) {
                if (nbt instanceof NBTTagInt) {
                    instance.deserializeNBT((NBTTagInt) nbt);
                }
            }
        }, () -> new Colored() {
            @Override
            public int getColor() {
                return 0;
            }

            @Override
            public boolean setColor(final int value) {
                return false;
            }

            @Override
            public boolean consumesDye() {
                return false;
            }

            @Override
            public boolean controlsConnectivity() {
                return false;
            }

            @Override
            public NBTTagInt serializeNBT() {
                return new NBTTagInt(0);
            }

            @Override
            public void deserializeNBT(final NBTTagInt nbt) {
            }
        });
    }

    private CapabilityColored() {
    }
}
