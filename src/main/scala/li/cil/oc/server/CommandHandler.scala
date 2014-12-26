package li.cil.oc.server

import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.event.FMLServerStartingEvent
import li.cil.oc.Settings
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

import scala.collection.convert.wrapAsJava._
import scala.collection.mutable

object CommandHandler {
  def register(e: FMLServerStartingEvent) {
    e.registerServerCommand(WirelessRenderingCommand)
  }

  // OP levels for reference:
  // 1 - Ops can bypass spawn protection.
  // 2 - Ops can use /clear, /difficulty, /effect, /gamemode, /gamerule, /give, /summon, /setblock and /tp, and can edit command blocks.
  // 3 - Ops can use /ban, /deop, /kick, and /op.
  // 4 - Ops can use /stop.

  object WirelessRenderingCommand extends SimpleCommand("oc_renderWirelessNetwork") {
    aliases += "oc_wlan"

    override def getCommandUsage(source: ICommandSender) = name + " <boolean>"

    override def execute(sender: ICommandSender, command: Array[String]) {
      Settings.rTreeDebugRenderer =
        if (command != null && command.length > 0)
          CommandBase.parseBoolean(command(0))
        else
          !Settings.rTreeDebugRenderer
    }

    override def getRequiredPermissionLevel = 2
  }

  abstract class SimpleCommand(val name: String) extends CommandBase {
    protected var aliases = mutable.ListBuffer.empty[String]

    override def getName = name

    override def getAliases = aliases

    override def canCommandSenderUse(sender: ICommandSender) = true

    override def addTabCompletionOptions(sender: ICommandSender, args: Array[String], pos: BlockPos) = List.empty[AnyRef]

    override def isUsernameIndex(command: Array[String], i: Int) = false
  }

}
