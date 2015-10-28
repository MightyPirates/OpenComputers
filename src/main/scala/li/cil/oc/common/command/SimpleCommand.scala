package li.cil.oc.common.command

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.server.MinecraftServer
import net.minecraft.util.BlockPos

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

abstract class SimpleCommand(val name: String) extends CommandBase {
  protected var aliases = mutable.ListBuffer.empty[String]

  override def getName = name

  override def getAliases = aliases

  override def canCommandSenderUse(source: ICommandSender) = super.canCommandSenderUse(source) || (MinecraftServer.getServer != null && MinecraftServer.getServer.isSinglePlayer)

  override def isUsernameIndex(command: Array[String], i: Int) = false

  override def addTabCompletionOptions(source: ICommandSender, command: Array[String], pos: BlockPos) = List.empty[AnyRef]
}
