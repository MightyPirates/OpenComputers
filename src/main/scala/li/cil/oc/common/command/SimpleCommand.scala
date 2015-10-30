package li.cil.oc.common.command

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.server.MinecraftServer

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

abstract class SimpleCommand(val name: String) extends CommandBase {
  protected var aliases = mutable.ListBuffer.empty[String]

  override def getCommandName = name

  override def getCommandAliases = aliases

  override def canCommandSenderUseCommand(source: ICommandSender) = super.canCommandSenderUseCommand(source) || (MinecraftServer.getServer != null && MinecraftServer.getServer.isSinglePlayer)

  override def isUsernameIndex(command: Array[String], i: Int) = false

  override def addTabCompletionOptions(source: ICommandSender, command: Array[String]) = List.empty[AnyRef]
}
