package li.cil.oc.server.command

import li.cil.oc.Settings
import li.cil.oc.common.command.SimpleCommand
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

object WirelessRenderingCommand extends SimpleCommand("oc_renderWirelessNetwork") {
  aliases += "oc_wlan"

  override def getCommandUsage(source: ICommandSender) = name + " <boolean>"

  override def processCommand(source: ICommandSender, command: Array[String]) {
    Settings.rTreeDebugRenderer =
      if (command != null && command.length > 0)
        CommandBase.parseBoolean(command(0))
      else
        !Settings.rTreeDebugRenderer
  }

  // OP levels for reference:
  // 1 - Ops can bypass spawn protection.
  // 2 - Ops can use /clear, /difficulty, /effect, /gamemode, /gamerule, /give, /summon, /setblock and /tp, and can edit command blocks.
  // 3 - Ops can use /ban, /deop, /kick, and /op.
  // 4 - Ops can use /stop.

  override def getRequiredPermissionLevel = 2
}
