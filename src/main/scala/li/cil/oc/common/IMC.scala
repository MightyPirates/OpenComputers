package li.cil.oc.common

import java.lang.reflect.Method
import java.lang.reflect.Modifier

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.template.AssemblerTemplates
import li.cil.oc.common.template.DisassemblerTemplates
import li.cil.oc.integration.util.ItemCharge
import li.cil.oc.integration.util.Wrench
import li.cil.oc.server.driver.Registry
import li.cil.oc.server.machine.ProgramLocations
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT
import net.minecraft.nbt.StringNBT
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.fml.InterModComms.IMCMessage

import scala.collection.convert.WrapAsScala._

object IMC {
  def handleMessage(message: IMCMessage): Unit = {
    message.getMessageSupplier.get.asInstanceOf[AnyRef] match {
      case template: CompoundNBT if message.getMethod == api.IMC.REGISTER_ASSEMBLER_TEMPLATE => {
        if (template.contains("name", NBT.TAG_STRING))
          OpenComputers.log.debug(s"Registering new assembler template '${template.getString("name")}' from mod ${message.getSenderModId}.")
        else
          OpenComputers.log.debug(s"Registering new, unnamed assembler template from mod ${message.getSenderModId}.")
        try AssemblerTemplates.add(template) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering assembler template.", t)
        }
      }
      case template: CompoundNBT if message.getMethod == api.IMC.REGISTER_DISASSEMBLER_TEMPLATE => {
        if (template.contains("name", NBT.TAG_STRING))
          OpenComputers.log.debug(s"Registering new disassembler template '${template.getString("name")}' from mod ${message.getSenderModId}.")
        else
          OpenComputers.log.debug(s"Registering new, unnamed disassembler template from mod ${message.getSenderModId}.")
        try DisassemblerTemplates.add(template) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering disassembler template.", t)
        }
      }
      case name: String if message.getMethod == api.IMC.REGISTER_TOOL_DURABILITY_PROVIDER => {
        OpenComputers.log.debug(s"Registering new tool durability provider '${name}' from mod ${message.getSenderModId}.")
        try ToolDurabilityProviders.add(getStaticMethod(name, classOf[ItemStack])) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering tool durability provider.", t)
        }
      }
      case name: String if message.getMethod == api.IMC.REGISTER_WRENCH_TOOL => {
        OpenComputers.log.debug(s"Registering new wrench usage '${name}' from mod ${message.getSenderModId}.")
        try Wrench.addUsage(getStaticMethod(name, classOf[PlayerEntity], classOf[BlockPos], classOf[Boolean])) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering wrench usage.", t)
        }
      }
      case name: String if message.getMethod == api.IMC.REGISTER_WRENCH_TOOL_CHECK => {
        OpenComputers.log.debug(s"Registering new wrench tool check '${name}' from mod ${message.getSenderModId}.")
        try Wrench.addCheck(getStaticMethod(name, classOf[ItemStack])) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering wrench check.", t)
        }
      }
      case implInfo: CompoundNBT if message.getMethod == api.IMC.REGISTER_ITEM_CHARGE => {
        OpenComputers.log.debug(s"Registering new item charge implementation '${implInfo.getString("name")}' from mod ${message.getSenderModId}.")
        try ItemCharge.add(
          getStaticMethod(implInfo.getString("canCharge"), classOf[ItemStack]),
          getStaticMethod(implInfo.getString("charge"), classOf[ItemStack], classOf[Double], classOf[Boolean])
        ) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering item charge implementation.", t)
        }
      }
      case name: String if message.getMethod == api.IMC.BLACKLIST_PERIPHERAL => {
        OpenComputers.log.debug(s"Blacklisting CC peripheral '${name}' as requested by mod ${message.getSenderModId}.")
        if (!Settings.get.peripheralBlacklist.contains(name)) {
          Settings.get.peripheralBlacklist.add(name)
        }
      }
      case compInfo: CompoundNBT if message.getMethod == api.IMC.BLACKLIST_HOST => {
        OpenComputers.log.debug(s"Blacklisting component '${compInfo.getString("name")}' for host '${compInfo.getString("host")}' as requested by mod ${message.getSenderModId}.")
        try Registry.blacklistHost(ItemStack.of(compInfo.getCompound("item")), Class.forName(compInfo.getString("host"))) catch {
          case t: Throwable => OpenComputers.log.warn("Failed blacklisting component.", t)
        }
      }
      case name: String if message.getMethod == api.IMC.REGISTER_ASSEMBLER_FILTER => {
        OpenComputers.log.debug(s"Registering new assembler template filter '${name}' from mod ${message.getSenderModId}.")
        try AssemblerTemplates.addFilter(name) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering assembler template filter.", t)
        }
      }
      case name: String if message.getMethod == api.IMC.REGISTER_INK_PROVIDER => {
        OpenComputers.log.debug(s"Registering new ink provider '${name}' from mod ${message.getSenderModId}.")
        try PrintData.addInkProvider(getStaticMethod(name, classOf[ItemStack])) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering ink provider.", t)
        }
      }
      case diskInfo: CompoundNBT if message.getMethod == api.IMC.REGISTER_PROGRAM_DISK_LABEL => {
        OpenComputers.log.debug(s"Registering new program location mapping for program '${diskInfo.getString("program")}' being on disk '${diskInfo.getString("label")}' from mod ${message.getSenderModId}.")
        ProgramLocations.addMapping(diskInfo.getString("program"), diskInfo.getString("label"), diskInfo.getList("architectures", NBT.TAG_STRING).map((tag: StringNBT) => tag.getAsString()).toArray: _*)
      }
      case _ => OpenComputers.log.warn(s"Got an unrecognized or invalid IMC message '${message.getMethod}' from mod ${message.getSenderModId}.")
    }
  }

  def getStaticMethod(name: String, signature: Class[_]*): Method = {
    val nameSplit = name.lastIndexOf('.')
    val className = name.substring(0, nameSplit)
    val methodName = name.substring(nameSplit + 1)
    val clazz = Class.forName(className)
    val method = clazz.getDeclaredMethod(methodName, signature: _*)
    if (!Modifier.isStatic(method.getModifiers)) throw new IllegalArgumentException(s"Method $name is not static.")
    method
  }

  def tryInvokeStatic[T](method: Method, args: AnyRef*)(default: T): T = try method.invoke(null, args: _*).asInstanceOf[T] catch {
    case t: Throwable =>
      OpenComputers.log.warn(s"Error invoking callback ${method.getDeclaringClass.getCanonicalName + "." + method.getName}.", t)
      default
  }

  def tryInvokeStaticVoid(method: Method, args: AnyRef*): Unit = try method.invoke(null, args: _*) catch {
    case t: Throwable =>
      OpenComputers.log.warn(s"Error invoking callback ${method.getDeclaringClass.getCanonicalName + "." + method.getName}.", t)
  }
}
