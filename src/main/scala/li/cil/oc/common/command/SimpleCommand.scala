package li.cil.oc.common.command

import java.util

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.server.MinecraftServer
import net.minecraftforge.fml.common.FMLCommonHandler

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

abstract class SimpleCommand(val name: String) extends CommandBase {
  protected var aliases = mutable.ListBuffer.empty[String]

  override def getName = name

  override def getAliases: util.List[String] = aliases

  override def checkPermission(server: MinecraftServer, sender: ICommandSender): Boolean = super.checkPermission(server, sender)|| (FMLCommonHandler.instance().getMinecraftServerInstance != null && FMLCommonHandler.instance().getMinecraftServerInstance.isSinglePlayer)

  override def isUsernameIndex(command: Array[String], i: Int) = false
}
