package li.cil.oc.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import java.util.Random;

public final class PlayerUtils {
    public static NBTTagCompound getPersistedData(final EntityPlayer player) {
        final NBTTagCompound nbt = player.getEntityData();
        if (!nbt.hasKey(EntityPlayer.PERSISTED_NBT_TAG, Constants.NBT.TAG_COMPOUND)) {
            nbt.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return nbt.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    public static void spawnParticleAround(final EntityPlayer player, final EnumParticleTypes effectType, final double chance) {
        final World world = player.getEntityWorld();
        final Random rng = world.rand;
        if (chance >= 1 || rng.nextDouble() < chance) {
            final AxisAlignedBB bounds = player.getEntityBoundingBox();
            final double x = bounds.minX + (bounds.maxX - bounds.minX) * rng.nextDouble();
            final double y = bounds.minY + (bounds.maxY - bounds.minY) * rng.nextDouble();
            final double z = bounds.minZ + (bounds.maxZ - bounds.minZ) * rng.nextDouble();
            world.spawnParticle(effectType, x, y, z, 0, 0, 0);
        }
    }

    // ----------------------------------------------------------------------- //

    private PlayerUtils() {
    }
}
