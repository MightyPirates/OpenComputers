package li.cil.oc.common.nanomachines.provider

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.nanomachines.Behavior
import li.cil.oc.api.prefab.AbstractBehavior
import li.cil.oc.util.PlayerUtils
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.CompoundNBT
import net.minecraft.particles.BasicParticleType
import net.minecraft.particles.ParticleType
import net.minecraft.particles.ParticleTypes
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.ForgeRegistry

object ParticleProvider extends ScalaProvider("b48c4bbd-51bb-4915-9367-16cff3220e4b") {
  final val ParticleTypeList: Array[BasicParticleType] = Array(
    ParticleTypes.FIREWORK,
    ParticleTypes.SMOKE,
    ParticleTypes.WITCH,
    ParticleTypes.NOTE,
    ParticleTypes.ENCHANT,
    ParticleTypes.FLAME,
    ParticleTypes.LAVA,
    ParticleTypes.SPLASH,
    ParticleTypes.ITEM_SLIME,
    ParticleTypes.HEART,
    ParticleTypes.HAPPY_VILLAGER
  )

  override def createScalaBehaviors(player: PlayerEntity): Iterable[Behavior] = ParticleTypeList.map(new ParticleBehavior(_, player))

  override def writeBehaviorToNBT(behavior: Behavior, nbt: CompoundNBT): Unit = {
    behavior match {
      case particles: ParticleBehavior =>
        nbt.putInt("effectName", ForgeRegistries.PARTICLE_TYPES.asInstanceOf[ForgeRegistry[ParticleType[_]]].getID(particles.effectType))
      case _ => // Wat.
    }
  }

  override def readBehaviorFromNBT(player: PlayerEntity, nbt: CompoundNBT): Behavior = {
    val effectType = ForgeRegistries.PARTICLE_TYPES.asInstanceOf[ForgeRegistry[ParticleType[_]]].getValue(nbt.getInt("effectName"))
    new ParticleBehavior(effectType.asInstanceOf[BasicParticleType], player)
  }

  class ParticleBehavior(var effectType: BasicParticleType, player: PlayerEntity) extends AbstractBehavior(player) {
    override def getNameHint = "particles." + effectType.getRegistryName.getPath

    override def update(): Unit = {
      val world = player.level
      if (world.isClientSide && Settings.get.enableNanomachinePfx) {
        PlayerUtils.spawnParticleAround(player, effectType, api.Nanomachines.getController(player).getInputCount(this) * 0.25)
      }
    }
  }

}
