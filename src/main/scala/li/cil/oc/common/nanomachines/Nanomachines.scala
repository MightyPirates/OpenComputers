package li.cil.oc.common.nanomachines

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.nanomachines.BehaviorProvider
import li.cil.oc.api.nanomachines.Controller
import li.cil.oc.server.PacketSender
import li.cil.oc.util.PlayerUtils
import net.minecraft.entity.player.EntityPlayer

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

object Nanomachines extends api.detail.NanomachinesAPI {
  val providers = mutable.Set.empty[BehaviorProvider]

  val serverControllers = mutable.WeakHashMap.empty[EntityPlayer, ControllerImpl]
  val clientControllers = mutable.WeakHashMap.empty[EntityPlayer, ControllerImpl]

  def controllers(player: EntityPlayer) = if (player.getEntityWorld.isRemote) clientControllers else serverControllers

  override def addProvider(provider: BehaviorProvider): Unit = providers += provider

  override def getProviders: java.lang.Iterable[BehaviorProvider] = providers

  def getController(player: EntityPlayer): Controller = {
    if (hasController(player)) controllers(player).getOrElseUpdate(player, new ControllerImpl(player))
    else null
  }

  def hasController(player: EntityPlayer) = {
    PlayerUtils.getPersistedData(player).getBoolean(Constants.namespace + "hasNanomachines")
  }

  def installController(player: EntityPlayer) = {
    if (!hasController(player)) {
      PlayerUtils.getPersistedData(player).setBoolean(Constants.namespace + "hasNanomachines", true)
    }
    getController(player) // Initialize controller instance.
  }

  override def uninstallController(player: EntityPlayer): Unit = {
    getController(player) match {
      case controller: ControllerImpl =>
        controller.dispose()
        controllers(player) -= player
        PlayerUtils.getPersistedData(player).removeTag(Constants.namespace + "hasNanomachines")
        if (!player.getEntityWorld.isRemote) {
          PacketSender.sendNanomachineConfiguration(player)
        }
      case _ => // Doesn't have one anyway.
    }
  }
}
