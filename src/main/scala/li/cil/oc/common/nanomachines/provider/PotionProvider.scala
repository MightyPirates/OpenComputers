package li.cil.oc.common.nanomachines.provider

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.nanomachines.Behavior
import li.cil.oc.api.nanomachines.DisableReason
import li.cil.oc.api.prefab.AbstractBehavior
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect

import scala.collection.convert.WrapAsScala._

object PotionProvider extends ScalaProvider("c29e4eec-5a46-479a-9b3d-ad0f06da784a") {
  // Lazy to give other mods a chance to register their potions.
  lazy val PotionWhitelist = filterPotions(Settings.get.nanomachinePotionWhitelist)

  def filterPotions(list: Iterable[String]) = list.map(Potion.getPotionFromResourceLocation).filter(_ != null).toSet

  def isPotionEligible(potion: Potion) = potion != null && PotionWhitelist.contains(potion)

  override def createScalaBehaviors(player: EntityPlayer) = {
    Potion.REGISTRY.filter(isPotionEligible).map(new PotionBehavior(_, player))
  }

  override def writeBehaviorToNBT(behavior: Behavior, nbt: NBTTagCompound): Unit = {
    behavior match {
      case potionBehavior: PotionBehavior =>
        nbt.setInteger("potionId", Potion.getIdFromPotion(potionBehavior.potion))
      case _ => // Shouldn't happen, ever.
    }
  }

  override def readBehaviorFromNBT(player: EntityPlayer, nbt: NBTTagCompound) = {
    val potionId = nbt.getInteger("potionId")
    new PotionBehavior(Potion.getPotionById(potionId), player)
  }

  class PotionBehavior(val potion: Potion, player: EntityPlayer) extends AbstractBehavior(player) {
    final val Duration = 600

    def amplifier(player: EntityPlayer) = api.Nanomachines.getController(player).getInputCount(this) - 1

    override def getNameHint: String = potion.getName.stripPrefix("potion.")

    override def onDisable(reason: DisableReason): Unit = {
      player.removePotionEffect(potion)
    }

    override def update(): Unit = {
      player.addPotionEffect(new PotionEffect(potion, Duration, amplifier(player)))
    }
  }

}
