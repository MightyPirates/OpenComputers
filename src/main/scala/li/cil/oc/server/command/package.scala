package li.cil.oc.server

import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ChatComponentText
import net.minecraft.util.IChatComponent

import scala.language.implicitConversions

package object command {
  implicit def string2text(s: String): IChatComponent = new ChatComponentText(s)

  def getOpLevel(sender: ICommandSender): Int = {
    // Shitty minecraft server logic & shitty minecraft server code.
    val srv = MinecraftServer.getServer
    if (srv.isSinglePlayer && srv.worldServers.head.getWorldInfo.areCommandsAllowed &&
      srv.getServerOwner.equalsIgnoreCase(sender.getName) /* || srv.commandsAllowedForAll */ )
      return 4

    sender match {
      case _: MinecraftServer => 4
      case p: EntityPlayerMP =>

        val e = srv.getConfigurationManager.getOppedPlayers.getEntry(p.getGameProfile)
        if (e == null) 0 else e.getPermissionLevel
      case _ => 0
    }
  }
}
