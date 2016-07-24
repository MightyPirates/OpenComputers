package li.cil.oc.common.nanomachines.provider

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.nanomachines.Behavior
import li.cil.oc.api.prefab.AbstractBehavior
import li.cil.oc.util.PlayerUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumParticleTypes

object ParticleProvider extends ScalaProvider("b48c4bbd-51bb-4915-9367-16cff3220e4b") {
  final val ParticleTypes = Array(
    EnumParticleTypes.FIREWORKS_SPARK,
    EnumParticleTypes.TOWN_AURA,
    EnumParticleTypes.SMOKE_NORMAL,
    EnumParticleTypes.SPELL_WITCH,
    EnumParticleTypes.NOTE,
    EnumParticleTypes.ENCHANTMENT_TABLE,
    EnumParticleTypes.FLAME,
    EnumParticleTypes.LAVA,
    EnumParticleTypes.WATER_SPLASH,
    EnumParticleTypes.REDSTONE,
    EnumParticleTypes.SLIME,
    EnumParticleTypes.HEART,
    EnumParticleTypes.VILLAGER_HAPPY
  )

  override def createScalaBehaviors(player: EntityPlayer): Iterable[Behavior] = ParticleTypes.map(new ParticleBehavior(_, player))

  override def writeBehaviorToNBT(behavior: Behavior, nbt: NBTTagCompound): Unit = {
    behavior match {
      case particles: ParticleBehavior =>
        nbt.setInteger("effectName", particles.effectType.getParticleID)
      case _ => // Wat.
    }
  }

  override def readBehaviorFromNBT(player: EntityPlayer, nbt: NBTTagCompound): Behavior = {
    val effectType = EnumParticleTypes.getParticleFromId(nbt.getInteger("effectName"))
    new ParticleBehavior(effectType, player)
  }

  class ParticleBehavior(var effectType: EnumParticleTypes, player: EntityPlayer) extends AbstractBehavior(player) {
    override def getNameHint = "particles." + effectType.getParticleName

    override def update(): Unit = {
      val world = player.getEntityWorld
      if (world.isRemote && Settings.get.enableNanomachinePfx) {
        PlayerUtils.spawnParticleAround(player, effectType, api.Nanomachines.getController(player).getInputCount(this) * 0.25)
      }
    }
  }

}
