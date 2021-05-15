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
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagString
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.fml.common.event.FMLInterModComms.IMCEvent

import scala.collection.convert.WrapAsScala._

object IMC {
  def handleEvent(e: IMCEvent): Unit = {
    for (message <- e.getMessages) {
      if (message.key == api.IMC.REGISTER_ASSEMBLER_TEMPLATE && message.isNBTMessage) {
        if (message.getNBTValue.hasKey("name", NBT.TAG_STRING))
          OpenComputers.log.debug(s"Registering new assembler template '${message.getNBTValue.getString("name")}' from mod ${message.getSender}.")
        else
          OpenComputers.log.debug(s"Registering new, unnamed assembler template from mod ${message.getSender}.")
        try AssemblerTemplates.add(message.getNBTValue) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering assembler template.", t)
        }
      }
      else if (message.key == api.IMC.REGISTER_DISASSEMBLER_TEMPLATE && message.isNBTMessage) {
        if (message.getNBTValue.hasKey("name", NBT.TAG_STRING))
          OpenComputers.log.debug(s"Registering new disassembler template '${message.getNBTValue.getString("name")}' from mod ${message.getSender}.")
        else
          OpenComputers.log.debug(s"Registering new, unnamed disassembler template from mod ${message.getSender}.")
        try DisassemblerTemplates.add(message.getNBTValue) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering disassembler template.", t)
        }
      }
      else if (message.key == api.IMC.REGISTER_TOOL_DURABILITY_PROVIDER && message.isStringMessage) {
        OpenComputers.log.debug(s"Registering new tool durability provider '${message.getStringValue}' from mod ${message.getSender}.")
        try ToolDurabilityProviders.add(getStaticMethod(message.getStringValue, classOf[ItemStack])) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering tool durability provider.", t)
        }
      }
      else if (message.key == api.IMC.REGISTER_WRENCH_TOOL && message.isStringMessage) {
        OpenComputers.log.debug(s"Registering new wrench usage '${message.getStringValue}' from mod ${message.getSender}.")
        try Wrench.addUsage(getStaticMethod(message.getStringValue, classOf[EntityPlayer], classOf[BlockPos], classOf[Boolean])) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering wrench usage.", t)
        }
      }
      else if (message.key == api.IMC.REGISTER_WRENCH_TOOL_CHECK && message.isStringMessage) {
        OpenComputers.log.debug(s"Registering new wrench tool check '${message.getStringValue}' from mod ${message.getSender}.")
        try Wrench.addCheck(getStaticMethod(message.getStringValue, classOf[ItemStack])) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering wrench check.", t)
        }
      }
      else if (message.key == api.IMC.REGISTER_ITEM_CHARGE && message.isNBTMessage) {
        OpenComputers.log.debug(s"Registering new item charge implementation '${message.getNBTValue.getString("name")}' from mod ${message.getSender}.")
        try ItemCharge.add(
          getStaticMethod(message.getNBTValue.getString("canCharge"), classOf[ItemStack]),
          getStaticMethod(message.getNBTValue.getString("charge"), classOf[ItemStack], classOf[Double], classOf[Boolean])
        ) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering item charge implementation.", t)
        }
      }
      else if (message.key == api.IMC.BLACKLIST_PERIPHERAL && message.isStringMessage) {
        OpenComputers.log.debug(s"Blacklisting CC peripheral '${message.getStringValue}' as requested by mod ${message.getSender}.")
        if (!Settings.get.peripheralBlacklist.contains(message.getStringValue)) {
          Settings.get.peripheralBlacklist.add(message.getStringValue)
        }
      }
      else if (message.key == api.IMC.BLACKLIST_HOST && message.isNBTMessage) {
        OpenComputers.log.debug(s"Blacklisting component '${message.getNBTValue.getString("name")}' for host '${message.getNBTValue.getString("host")}' as requested by mod ${message.getSender}.")
        try Registry.blacklistHost(new ItemStack(message.getNBTValue.getCompoundTag("item")), Class.forName(message.getNBTValue.getString("host"))) catch {
          case t: Throwable => OpenComputers.log.warn("Failed blacklisting component.", t)
        }
      }
      else if (message.key == api.IMC.REGISTER_ASSEMBLER_FILTER && message.isStringMessage) {
        OpenComputers.log.debug(s"Registering new assembler template filter '${message.getStringValue}' from mod ${message.getSender}.")
        try AssemblerTemplates.addFilter(message.getStringValue) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering assembler template filter.", t)
        }
      }
      else if (message.key == api.IMC.REGISTER_INK_PROVIDER && message.isStringMessage) {
        OpenComputers.log.debug(s"Registering new ink provider '${message.getStringValue}' from mod ${message.getSender}.")
        try PrintData.addInkProvider(getStaticMethod(message.getStringValue, classOf[ItemStack])) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering ink provider.", t)
        }
      }
      else if (message.key == api.IMC.REGISTER_PROGRAM_DISK_LABEL && message.isNBTMessage) {
        OpenComputers.log.debug(s"Registering new program location mapping for program '${message.getNBTValue.getString("program")}' being on disk '${message.getNBTValue.getString("label")}' from mod ${message.getSender}.")
        ProgramLocations.addMapping(message.getNBTValue.getString("program"), message.getNBTValue.getString("label"), message.getNBTValue.getTagList("architectures", NBT.TAG_STRING).map((tag: NBTTagString) => tag.getString()).toArray: _*)
      }
      else {
        OpenComputers.log.warn(s"Got an unrecognized or invalid IMC message '${message.key}' from mod ${message.getSender}.")
      }
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
