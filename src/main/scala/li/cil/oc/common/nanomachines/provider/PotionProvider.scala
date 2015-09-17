package li.cil.oc.common.nanomachines.provider

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.nanomachines.Behavior
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect

import scala.collection.convert.WrapAsScala._

object PotionProvider extends SimpleProvider {
  final val Id = "c29e4eec-5a46-479a-9b3d-ad0f06da784a"

  // Lazy to give other mods a chance to register their potions.
  lazy val PotionBlacklist = Settings.get.nanomachinePotionBlacklist.map {
    case name: String => Potion.potionTypes.find(p => p != null && p.getName == name)
    case id: java.lang.Number if id.intValue() >= 0 && id.intValue() < Potion.potionTypes.length => Option(Potion.potionTypes(id.intValue()))
    case _ => None
  }.collect {
    case Some(potion) => potion
  }.toSet

  override def doCreateBehaviors(player: EntityPlayer) = {
    Potion.potionTypes.filter(_ != null).filterNot(PotionBlacklist.contains).map(new PotionBehavior(_, player))
  }

  override def doWriteToNBT(behavior: Behavior, nbt: NBTTagCompound): Unit = {
    behavior match {
      case potionBehavior: PotionBehavior =>
        nbt.setInteger("potionId", potionBehavior.potion.id)
      case _ => // Shouldn't happen, ever.
    }
  }

  override def doReadFromNBT(player: EntityPlayer, nbt: NBTTagCompound) = {
    val potionId = nbt.getInteger("potionId")
    new PotionBehavior(Potion.potionTypes(potionId), player)
  }

  class PotionBehavior(val potion: Potion, player: EntityPlayer) extends SimpleBehavior(player) {
    final val RefreshInterval = 40

    def amplifier(player: EntityPlayer) = api.Nanomachines.getController(player).getInputCount(this) - 1

    override def getNameHint: String = potion.getName.stripPrefix("potion.")

    override def onEnable(): Unit = {}

    override def onDisable(): Unit = {}

    override def update(): Unit = {
      player.getActivePotionEffect(potion) match {
        case effect: PotionEffect if effect.getDuration > RefreshInterval / 2 => // Effect still active.
        case _ => player.addPotionEffect(new PotionEffect(potion.id, RefreshInterval, amplifier(player)))
      }
    }
  }

}
