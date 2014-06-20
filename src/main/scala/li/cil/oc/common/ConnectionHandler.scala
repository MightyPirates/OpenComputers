package li.cil.oc.common

import cpw.mods.fml.common.network.{IConnectionHandler, Player}
import li.cil.oc.util.LuaStateFactory
import li.cil.oc.util.mods.{Mods, ProjectRed}
import li.cil.oc.{Localization, OpenComputers, Settings, UpdateCheck}
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.packet.{NetHandler, Packet1Login}
import net.minecraft.network.{INetworkManager, NetLoginHandler}
import net.minecraft.server.MinecraftServer

object ConnectionHandler extends IConnectionHandler {
  def playerLoggedIn(player: Player, netHandler: NetHandler, manager: INetworkManager) {
    if (netHandler.isServerHandler) player match {
      case p: EntityPlayerMP =>
        if (!LuaStateFactory.isAvailable) {
          p.sendChatToPlayer(Localization.Chat.WarningLuaFallback)
        }
        if (Mods.ProjectRed.isAvailable && !ProjectRed.isAPIAvailable) {
          p.sendChatToPlayer(Localization.Chat.WarningProjectRed)
        }
        if (!Settings.get.pureIgnorePower && Settings.get.ignorePower) {
          p.sendChatToPlayer(Localization.Chat.WarningPower)
        }
        OpenComputers.tampered match {
          case Some(event) => p.sendChatToPlayer(Localization.Chat.WarningFingerprint(event))
          case _ =>
        }
        // Do update check in local games and for OPs.
        if (!MinecraftServer.getServer.isDedicatedServer || MinecraftServer.getServer.getConfigurationManager.isPlayerOpped(p.getCommandSenderName)) {
          UpdateCheck.checkForPlayer(p)
        }
      case _ =>
    }
  }

  def connectionReceived(netHandler: NetLoginHandler, manager: INetworkManager) = null

  def connectionOpened(netClientHandler: NetHandler, server: String, port: Int, manager: INetworkManager) {
  }

  def connectionOpened(netClientHandler: NetHandler, server: MinecraftServer, manager: INetworkManager) {
  }

  def connectionClosed(manager: INetworkManager) {
  }

  def clientLoggedIn(clientHandler: NetHandler, manager: INetworkManager, login: Packet1Login) {
  }
}
