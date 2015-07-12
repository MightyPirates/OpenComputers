package li.cil.oc.client

import li.cil.oc.common.command.SimpleCommand
import net.minecraft.client.gui.GuiScreen
import net.minecraft.command.ICommandSender
import net.minecraftforge.client.ClientCommandHandler

object CommandHandler {
  def register(): Unit = {
    ClientCommandHandler.instance.registerCommand(SetClipboardCommand)
  }

  object SetClipboardCommand extends SimpleCommand("oc_setclipboard") {
    override def getCommandUsage(source: ICommandSender): String = name + " <value>"

    override def processCommand(source: ICommandSender, command: Array[String]): Unit = {
      if (source.getEntityWorld.isRemote && command != null && command.length > 0) {
        GuiScreen.setClipboardString(command(0))
      }
    }

    // OP levels for reference:
    // 1 - Ops can bypass spawn protection.
    // 2 - Ops can use /clear, /difficulty, /effect, /gamemode, /gamerule, /give, /summon, /setblock and /tp, and can edit command blocks.
    // 3 - Ops can use /ban, /deop, /kick, and /op.
    // 4 - Ops can use /stop.

    override def getRequiredPermissionLevel = 0
  }

}
