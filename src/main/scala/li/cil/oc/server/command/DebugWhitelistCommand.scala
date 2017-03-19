package li.cil.oc.server.command

import li.cil.oc.Settings
import li.cil.oc.common.command.SimpleCommand
import li.cil.oc.util.DebugCardAccess
import net.minecraft.command.{ICommandSender, WrongUsageException}
import net.minecraft.server.MinecraftServer
import net.minecraft.util.text.TextComponentString

object DebugWhitelistCommand extends SimpleCommand("oc_debugWhitelist") {
  // Required OP levels:
  //  to revoke your cards - 0
  //  to do other whitelist manipulation - 2

  override def getRequiredPermissionLevel = 0
  private def isOp(sender: ICommandSender) = getOpLevel(sender) >= 2

  override def getUsage(sender: ICommandSender): String =
    if (isOp(sender)) name + " [revoke|add|remove] <player> OR " + name + " [revoke|list]"
    else name + " revoke"

  override def execute(server: MinecraftServer, sender: ICommandSender, args: Array[String]): Unit = {
    val wl = Settings.Debug.debugCardAccess match {
      case w: DebugCardAccess.Whitelist => w
      case _ => throw new WrongUsageException("§cDebug card whitelisting is not enabled.")
    }

    def revokeUser(player: String): Unit = {
      if (wl.isWhitelisted(player)) {
        wl.invalidate(player)
        sender.sendMessage(new TextComponentString("§aAll your debug cards were invalidated."))
      } else sender.sendMessage(new TextComponentString("§cYou are not whitelisted to use debug card."))
    }

    args match {
      case Array("revoke") => revokeUser(sender.getName)
      case Array("revoke", player) if isOp(sender) => revokeUser(player)
      case Array("list") if isOp(sender) =>
        val players = wl.getWhitelist
        if (players.nonEmpty)
          sender.sendMessage(new TextComponentString("§aCurrently whitelisted players: §e" + players.mkString(", ")))
        else
          sender.sendMessage(new TextComponentString("§cThere is no currently whitelisted players."))
      case Array("add", player) if isOp(sender) =>
        wl.add(player)
        sender.sendMessage(new TextComponentString("§aPlayer was added to whitelist."))
      case Array("remove", player) if isOp(sender) =>
        wl.remove(player)
        sender.sendMessage(new TextComponentString("§aPlayer was removed from whitelist"))
      case _ =>
        sender.sendMessage(new TextComponentString("§e" + getUsage(sender)))
    }
  }
}
