package li.cil.oc.server.command

import li.cil.oc.common.PacketBuilder
import li.cil.oc.common.command.SimpleCommand
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

object NetworkProfilingCommand extends SimpleCommand("oc_profileNetwork") {
  aliases += "oc_pn"

  override def getCommandUsage(source: ICommandSender) = name + " <boolean>"

  override def processCommand(source: ICommandSender, command: Array[String]) {
    PacketBuilder.isProfilingEnabled =
      if (command != null && command.length > 0)
        CommandBase.parseBoolean(source, command(0))
      else
        !PacketBuilder.isProfilingEnabled
  }

  // OP levels for reference:
  // 1 - Ops can bypass spawn protection.
  // 2 - Ops can use /clear, /difficulty, /effect, /gamemode, /gamerule, /give, /summon, /setblock and /tp, and can edit command blocks.
  // 3 - Ops can use /ban, /deop, /kick, and /op.
  // 4 - Ops can use /stop.

  override def getRequiredPermissionLevel = 3
}
