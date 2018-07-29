package li.cil.oc.server.command

import li.cil.oc.common.command.SimpleCommand
import li.cil.oc.OpenComputers
import li.cil.oc.common.event.ChunkloaderUpgradeHandler
import net.minecraft.command.ICommandSender
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ChatComponentText

object ChunkloaderListCommand extends SimpleCommand("oc_chunkloaders") {
  aliases += "oc_cl"

  override def getCommandUsage(source: ICommandSender): String = name

  override def processCommand(source: ICommandSender, command: Array[String]) {
    source match {
      case player: EntityPlayer =>
        list((s: String) => { player.addChatMessage(new ChatComponentText(s)) })
      case _ =>
        list((s: String) => { OpenComputers.log.info(s"[chunkloader] $s") })
    }
  }

  private def list(out: String => Unit) {
    val cnt = ChunkloaderUpgradeHandler.chunkloaders.size
    if (cnt > 0) {
      out(s"Currently there are ${ChunkloaderUpgradeHandler.chunkloaders.size} registered chunkloaders:")
      ChunkloaderUpgradeHandler.chunkloaders.foreach(loader => {
        out(s"$loader")
      })
    } else {
      out(s"There is no currently registered chunkloaders.")
    }
    if (ChunkloaderUpgradeHandler.restoredTickets.size > 0) {
      out(s"Currently there are ${ChunkloaderUpgradeHandler.restoredTickets.size} tickets in unloaded chunks:")
      ChunkloaderUpgradeHandler.restoredTickets.values.foreach(ticket => {
        out(s"$ticket")
      })
    }
    val unloadedTickets = ChunkloaderUpgradeHandler.PersonalHandler.unloadedTickets
    if (unloadedTickets.size > 0) {
      out(s"Currently there are ${unloadedTickets.size} tickets in unloaded dimensions:")
      unloadedTickets.values.foreach(ticket => {
        out(s"$ticket")
      })
    }
  }

  // OP levels for reference:
  // 1 - Ops can bypass spawn protection.
  // 2 - Ops can use /clear, /difficulty, /effect, /gamemode, /gamerule, /give, /summon, /setblock and /tp, and can edit command blocks.
  // 3 - Ops can use /ban, /deop, /kick, and /op.
  // 4 - Ops can use /stop.

  override def getRequiredPermissionLevel = 1
}
