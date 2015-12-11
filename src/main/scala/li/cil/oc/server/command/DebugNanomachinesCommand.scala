package li.cil.oc.server.command

import li.cil.oc.api
import li.cil.oc.common.command.SimpleCommand
import li.cil.oc.common.nanomachines.ControllerImpl
import net.minecraft.command.ICommandSender
import net.minecraft.command.WrongUsageException
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ChatComponentText

object DebugNanomachinesCommand extends SimpleCommand("oc_debugNanomachines") {
  aliases += "oc_dn"

  override def getCommandUsage(source: ICommandSender): String = name

  override def processCommand(source: ICommandSender, args: Array[String]): Unit = {
    source match {
      case player: EntityPlayer =>
        api.Nanomachines.installController(player) match {
          case controller: ControllerImpl =>
            controller.debug()
            player.addChatMessage(new ChatComponentText("Debug configuration created, see log for mappings."))
          case _ => // Someone did something.
        }
      case _ => throw new WrongUsageException("Can only be used by players.")
    }
  }

  // OP levels for reference:
  // 1 - Ops can bypass spawn protection.
  // 2 - Ops can use /clear, /difficulty, /effect, /gamemode, /gamerule, /give, /summon, /setblock and /tp, and can edit command blocks.
  // 3 - Ops can use /ban, /deop, /kick, and /op.
  // 4 - Ops can use /stop.

  override def getRequiredPermissionLevel = 2
}
