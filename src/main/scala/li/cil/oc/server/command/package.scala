package li.cil.oc.server

import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.server.MinecraftServer
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentString
import net.minecraftforge.fml.common.FMLCommonHandler

import scala.language.implicitConversions

package object command {
  implicit def string2text(s: String): ITextComponent = new TextComponentString(s)

  def getOpLevel(sender: ICommandSender): Int = {
    // Shitty minecraft server logic & shitty minecraft server code.
    val srv = FMLCommonHandler.instance().getMinecraftServerInstance
    if (srv.isSinglePlayer && srv.worldServers.head.getWorldInfo.areCommandsAllowed &&
      srv.getServerOwner.equalsIgnoreCase(sender.getName) /* || srv.commandsAllowedForAll */ )
      return 4

    sender match {
      case _: MinecraftServer => 4
      case p: EntityPlayerMP =>

        val e = srv.getPlayerList.getOppedPlayers.getEntry(p.getGameProfile)
        if (e == null) 0 else e.getPermissionLevel
      case _ => 0
    }
  }
}
