package li.cil.oc.common.nanomachines.provider

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.nanomachines.Behavior
import li.cil.oc.api.nanomachines.DisableReason
import li.cil.oc.api.prefab.AbstractBehavior
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.nbt.CompoundNBT
import net.minecraft.potion.Effect
import net.minecraft.potion.EffectInstance
import net.minecraft.util.ResourceLocation
import net.minecraftforge.registries.ForgeRegistries

import scala.collection.convert.WrapAsScala._

object PotionProvider extends ScalaProvider("c29e4eec-5a46-479a-9b3d-ad0f06da784a") {
  // Lazy to give other mods a chance to register their potions.
  lazy val PotionWhitelist = filterPotions(Settings.get.nanomachinePotionWhitelist)

  def filterPotions[T](list: Iterable[T]) = {
    list.map {
      case name: String => Option(ForgeRegistries.POTIONS.getValue(new ResourceLocation(name)))
      case loc: ResourceLocation => Option(ForgeRegistries.POTIONS.getValue(loc))
      case id: java.lang.Number => Option(Effect.byId(id.intValue()))
      case _ => None
    }.collect {
      case Some(potion) => potion
    }.toSet
  }

  def isPotionEligible(potion: Effect) = potion != null && PotionWhitelist.contains(potion)

  override def createScalaBehaviors(player: PlayerEntity) = {
    ForgeRegistries.POTIONS.getValues.filter(isPotionEligible).map(new PotionBehavior(_, player))
  }

  override def writeBehaviorToNBT(behavior: Behavior, nbt: CompoundNBT): Unit = {
    behavior match {
      case potionBehavior: PotionBehavior =>
        nbt.putString("potionId", potionBehavior.potion.getRegistryName.toString)
      case _ => // Shouldn't happen, ever.
    }
  }

  override def readBehaviorFromNBT(player: PlayerEntity, nbt: CompoundNBT) = {
    val potionId = nbt.getString("potionId")
    new PotionBehavior(ForgeRegistries.POTIONS.getValue(new ResourceLocation(potionId)), player)
  }

  class PotionBehavior(val potion: Effect, player: PlayerEntity) extends AbstractBehavior(player) {
    final val Duration = 600

    def amplifier(player: PlayerEntity) = api.Nanomachines.getController(player).getInputCount(this) - 1

    override def getNameHint: String = potion.getDescriptionId.stripPrefix("effect.")

    override def onDisable(reason: DisableReason): Unit = {
      player.removeEffect(potion)
    }

    override def update(): Unit = {
      player.addEffect(new EffectInstance(potion, Duration, amplifier(player), true, Settings.get.enableNanomachinePfx))
    }
  }

}
