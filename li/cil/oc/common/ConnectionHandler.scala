package li.cil.oc.common

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.network.{Player, IConnectionHandler}
import li.cil.oc.Settings
import li.cil.oc.util.LuaStateFactory
import li.cil.oc.util.mods.ProjectRed
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.packet.{Packet1Login, NetHandler}
import net.minecraft.network.{NetLoginHandler, INetworkManager}
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ChatMessageComponent

object ConnectionHandler extends IConnectionHandler {
  def playerLoggedIn(player: Player, netHandler: NetHandler, manager: INetworkManager) {
    if (netHandler.isServerHandler) player match {
      case p: EntityPlayerMP =>
        if (!LuaStateFactory.isAvailable) {
          p.sendChatToPlayer(ChatMessageComponent.createFromText("§aOpenComputers§f: ").addKey(Settings.namespace + "gui.Chat.WarningLuaFallback"))
        }
        if (ProjectRed.isAvailable && !ProjectRed.isAPIAvailable) {
          p.sendChatToPlayer(ChatMessageComponent.createFromText("§aOpenComputers§f: ").addKey(Settings.namespace + "gui.Chat.WarningProjectRed"))
        }
        if (!Settings.get.pureIgnorePower && !Loader.isModLoaded("UniversalElectricity")) {
          p.sendChatToPlayer(ChatMessageComponent.createFromText("§aOpenComputers§f: ").addKey(Settings.namespace + "gui.Chat.WarningPower"))
        }
      case _ =>
    }
  }

  def connectionReceived(netHandler: NetLoginHandler, manager: INetworkManager) = null

  def connectionOpened(netClientHandler: NetHandler, server: String, port: Int, manager: INetworkManager) {}

  def connectionOpened(netClientHandler: NetHandler, server: MinecraftServer, manager: INetworkManager) {}

  def connectionClosed(manager: INetworkManager) {}

  def clientLoggedIn(clientHandler: NetHandler, manager: INetworkManager, login: Packet1Login) {}
}
