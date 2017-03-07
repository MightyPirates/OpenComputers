package li.cil.oc.common.capabilities;

import li.cil.oc.api.tileentity.RedstoneAware;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;

public final class CapabilityRedstoneAware {
    @CapabilityInject(RedstoneAware.class)
    public static Capability<RedstoneAware> REDSTONE_AWARE_CAPABILITY;

    public static void register() {
        CapabilityManager.INSTANCE.register(RedstoneAware.class, new Capability.IStorage<RedstoneAware>() {
            @Nullable
            @Override
            public NBTBase writeNBT(final Capability<RedstoneAware> capability, final RedstoneAware instance, final EnumFacing side) {
                return instance.serializeNBT();
            }

            @Override
            public void readNBT(final Capability<RedstoneAware> capability, final RedstoneAware instance, final EnumFacing side, final NBTBase nbt) {
                if (nbt instanceof NBTTagCompound) {
                    instance.deserializeNBT((NBTTagCompound) nbt);
                }
            }
        }, () -> new RedstoneAware() {
            @Override
            public int getInput(final EnumFacing side) {
                return 0;
            }

            @Override
            public int getOutput(final EnumFacing side) {
                return 0;
            }

            @Override
            public void setOutput(final EnumFacing side, final int value) {
            }

            @Override
            public boolean isOutputEnabled() {
                return false;
            }

            @Override
            public void setOutputEnabled(final boolean value) {
            }

            @Override
            public boolean isOutputStrong() {
                return false;
            }

            @Override
            public int getMaxInput() {
                return 0;
            }

            @Override
            public void scheduleInputUpdate() {
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

    private CapabilityRedstoneAware() {
    }
}
