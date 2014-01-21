package li.cil.oc.common

import cpw.mods.fml.common.network.{Player, IConnectionHandler}
import net.minecraft.network.packet.{Packet1Login, NetHandler}
import net.minecraft.network.{NetLoginHandler, INetworkManager}
import net.minecraft.server.MinecraftServer
import net.minecraft.entity.player.EntityPlayerMP
import li.cil.oc.util.LuaStateFactory
import li.cil.oc.util.mods.ProjectRed

object ConnectionHandler extends IConnectionHandler {
  def playerLoggedIn(player: Player, netHandler: NetHandler, manager: INetworkManager) {
    if (netHandler.isServerHandler) player match {
      case p: EntityPlayerMP =>
        if (!LuaStateFactory.isAvailable) {
          p.addChatMessage(
            "§aOpenComputers§f: native Lua libraries are not available, computers will not be runnable.")
        }
        if (ProjectRed.isAvailable && !ProjectRed.isAPIAvailable) {
          p.addChatMessage(
            "§aOpenComputers§f: you are using a version of Project: Red that is incompatible with OpenComputers. Try updating your version of Project: Red.")
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
