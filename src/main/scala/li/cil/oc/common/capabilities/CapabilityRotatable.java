package li.cil.oc.common.capabilities;

import li.cil.oc.api.tileentity.Rotatable;
import li.cil.oc.api.util.Pitch;
import li.cil.oc.api.util.Yaw;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;

public final class CapabilityRotatable {
    @CapabilityInject(Rotatable.class)
    public static Capability<Rotatable> ROTATABLE_CAPABILITY;

    public static void register() {
        CapabilityManager.INSTANCE.register(Rotatable.class, new Capability.IStorage<Rotatable>() {
            @Nullable
            @Override
            public NBTBase writeNBT(final Capability<Rotatable> capability, final Rotatable instance, final EnumFacing side) {
                return instance.serializeNBT();
            }

            @Override
            public void readNBT(final Capability<Rotatable> capability, final Rotatable instance, final EnumFacing side, final NBTBase nbt) {
                if (nbt instanceof NBTTagByte) {
                    instance.deserializeNBT((NBTTagByte) nbt);
                }
            }
        }, () -> new Rotatable() {
            @Override
            public Pitch getPitch() {
                return Pitch.FORWARD;
            }

            @Override
            public boolean setPitch(final Pitch value) {
                return false;
            }

            @Override
            public Yaw getYaw() {
                return Yaw.SOUTH;
            }

            @Override
            public boolean setYaw(final Yaw value) {
                return false;
            }

            @Override
            public boolean rotate(final EnumFacing.Axis around) {
                return false;
            }

            @Override
            public EnumFacing getFacing() {
                return EnumFacing.SOUTH;
            }

            @Override
            public EnumFacing[] getValidRotations() {
                return new EnumFacing[0];
            }

            @Override
            public EnumFacing toGlobal(final EnumFacing value) {
                return value;
            }

            @Override
            public EnumFacing toLocal(final EnumFacing value) {
                return value;
            }

            @Override
            public NBTTagByte serializeNBT() {
                return new NBTTagByte((byte) 0);
            }

            @Override
            public void deserializeNBT(final NBTTagByte nbt) {
            }
        });
    }

    private CapabilityRotatable() {
    }
}
