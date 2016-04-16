package li.cil.oc.common.nanomachines.provider

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.nanomachines.Behavior
import li.cil.oc.api.prefab.AbstractBehavior
import li.cil.oc.util.PlayerUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

object ParticleProvider extends ScalaProvider("b48c4bbd-51bb-4915-9367-16cff3220e4b") {
  final val ParticleNames = Array(
    "fireworksSpark",
    "townaura",
    "smoke",
    "witchMagic",
    "note",
    "enchantmenttable",
    "flame",
    "lava",
    "splash",
    "reddust",
    "slime",
    "heart",
    "happyVillager"
  )

  override def createScalaBehaviors(player: EntityPlayer): Iterable[Behavior] = ParticleNames.map(new ParticleBehavior(_, player))

  override def writeBehaviorToNBT(behavior: Behavior, nbt: NBTTagCompound): Unit = {
    behavior match {
      case particles: ParticleBehavior =>
        nbt.setString("effectName", particles.effectName)
      case _ => // Wat.
    }
  }

  override def readBehaviorFromNBT(player: EntityPlayer, nbt: NBTTagCompound): Behavior = {
    val effectName = nbt.getString("effectName")
    new ParticleBehavior(effectName, player)
  }

  class ParticleBehavior(var effectName: String, player: EntityPlayer) extends AbstractBehavior(player) {
    override def getNameHint = "particles." + effectName

    override def update(): Unit = {
      val world = player.getEntityWorld
      if (world.isRemote && Settings.get.enableNanomachinePfx) {
        PlayerUtils.spawnParticleAround(player, effectName, api.Nanomachines.getController(player).getInputCount(this) * 0.25)
      }
    }
  }

}
