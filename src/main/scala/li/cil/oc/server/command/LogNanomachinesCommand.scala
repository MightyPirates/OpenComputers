package li.cil.oc.server.command

import li.cil.oc.api
import li.cil.oc.common.command.SimpleCommand
import li.cil.oc.common.nanomachines.ControllerImpl
import net.minecraft.command.ICommandSender
import net.minecraft.command.WrongUsageException
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.MinecraftServer
import net.minecraftforge.fml.common.FMLCommonHandler

object LogNanomachinesCommand extends SimpleCommand("oc_nanomachines") {
  aliases += "oc_nm"

  override def getUsage(source: ICommandSender): String = name + " [player]"

  override def execute(server: MinecraftServer, source: ICommandSender, command: Array[String]): Unit = {
    (if (command.length > 0) {
      val player = command(0)
      val config = FMLCommonHandler.instance.getMinecraftServerInstance.getPlayerList
      config.getPlayerByUsername(player)
    } else source) match {
      case player: EntityPlayer =>
        api.Nanomachines.installController(player) match {
          case controller: ControllerImpl => controller.print()
          case _ => // Someone did something.
        }
      case _ => throw new WrongUsageException("Player entity not found.")
    }
  }

  // OP levels for reference:
  // 1 - Ops can bypass spawn protection.
  // 2 - Ops can use /clear, /difficulty, /effect, /gamemode, /gamerule, /give, /summon, /setblock and /tp, and can edit command blocks.
  // 3 - Ops can use /ban, /deop, /kick, and /op.
  // 4 - Ops can use /stop.

  override def getRequiredPermissionLevel = 2
}
