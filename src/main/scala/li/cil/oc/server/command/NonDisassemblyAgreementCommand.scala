package li.cil.oc.server.command

import li.cil.oc.Settings
import li.cil.oc.common.command.SimpleCommand
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.command.WrongUsageException
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound

object NonDisassemblyAgreementCommand extends SimpleCommand("oc_preventDisassembling") {
  aliases += "oc_nodis"
  aliases += "oc_prevdis"

  override def getCommandUsage(source: ICommandSender) = name + " <boolean>"

  override def processCommand(source: ICommandSender, command: Array[String]) {
    source match {
      case player: EntityPlayer =>
        val stack = player.getHeldItem
        if (stack != null) {
          if (!stack.hasTagCompound) {
            stack.setTagCompound(new NBTTagCompound())
          }
          val nbt = stack.getTagCompound
          val preventDisassembly =
            if (command != null && command.length > 0)
              CommandBase.parseBoolean(command(0))
            else
              !nbt.getBoolean(Settings.namespace + "undisassemblable")
          if (preventDisassembly)
            nbt.setBoolean(Settings.namespace + "undisassemblable", true)
          else
            nbt.removeTag(Settings.namespace + "undisassemblable")
          if (nbt.hasNoTags) stack.setTagCompound(null)
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
