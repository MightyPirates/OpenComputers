package li.cil.oc.util

import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.CompoundNBT
import net.minecraft.particles.IParticleData

object PlayerUtils {
  def persistedData(player: PlayerEntity): CompoundNBT = {
    val nbt = player.getPersistentData
    if (!nbt.contains(PlayerEntity.PERSISTED_NBT_TAG)) {
      nbt.put(PlayerEntity.PERSISTED_NBT_TAG, new CompoundNBT())
    }
    nbt.getCompound(PlayerEntity.PERSISTED_NBT_TAG)
  }

  def spawnParticleAround(player: PlayerEntity, effectType: IParticleData, chance: Double = 1.0): Unit = {
    val rng = player.level.random
    if (chance >= 1 || rng.nextDouble() < chance) {
      val bounds = player.getBoundingBox
      val x = bounds.minX + (bounds.maxX - bounds.minX) * rng.nextDouble() * 1.5
      val y = bounds.minY + (bounds.maxY - bounds.minY) * rng.nextDouble() * 0.5
      val z = bounds.minZ + (bounds.maxZ - bounds.minZ) * rng.nextDouble() * 1.5
      player.level.addParticle(effectType, x, y, z, 0, 0, 0)
    }
  }
}
