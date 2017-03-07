package li.cil.oc.common.nanomachines.provider

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.nanomachines.Behavior
import li.cil.oc.api.nanomachines.DisableReason
import li.cil.oc.api.prefab.nanomachines.AbstractBehavior
import li.cil.oc.integration.util.DamageSourceWithRandomCause
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

object HungryProvider extends ScalaProvider("d697c24a-014c-4773-a288-23084a59e9e8") {
  final val FillCount = 10 // Create a bunch of these to have a higher chance of one being picked / available.

  final val HungryDamage = new DamageSourceWithRandomCause("oc.nanomachinesHungry", 3).
    setDamageBypassesArmor().
    setDamageIsAbsolute()

  override def createScalaBehaviors(player: EntityPlayer): Iterable[Behavior] = Iterable.fill(FillCount)(new HungryBehavior(player))

  override protected def readBehaviorFromNBT(player: EntityPlayer, nbt: NBTTagCompound): Behavior = new HungryBehavior(player)

  class HungryBehavior(player: EntityPlayer) extends AbstractBehavior(player) {
    override def onDisable(reason: DisableReason): Unit = {
      if (reason == DisableReason.OutOfEnergy) {
        player.attackEntityFrom(HungryDamage, Settings.get.nanomachinesHungryDamage)
        api.Nanomachines.getController(player).changeBuffer(Settings.get.nanomachinesHungryEnergyRestored)
      }
    }
  }

}
