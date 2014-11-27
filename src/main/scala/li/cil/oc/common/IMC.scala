package li.cil.oc.common

import java.lang.reflect.Method
import java.lang.reflect.Modifier

import com.typesafe.config.Config
import cpw.mods.fml.common.event.FMLInterModComms.IMCEvent
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.template.AssemblerTemplates
import li.cil.oc.integration.util.Wrench
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.convert.WrapAsScala._

object IMC {
  def handleEvent(e: IMCEvent): Unit = {
    for (message <- e.getMessages) {
      if (message.key == "registerAssemblerTemplate" && message.isNBTMessage) {
        if (message.getNBTValue.hasKey("name", NBT.TAG_STRING))
          OpenComputers.log.info(s"Registering new assembler template '${message.getNBTValue.getString("name")}' from mod ${message.getSender}.")
        else
          OpenComputers.log.info(s"Registering new, unnamed assembler template from mod ${message.getSender}.")
        try AssemblerTemplates.add(message.getNBTValue) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering assembler template.", t)
        }
      }
      else if (message.key == "registerToolDurabilityProvider" && message.isStringMessage) {
        OpenComputers.log.info(s"Registering new tool durability provider '${message.getStringValue}' from mod ${message.getSender}.")
        try ToolDurabilityProviders.add(getStaticMethod(message.getStringValue, classOf[ItemStack])) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering tool durability provider.", t)
        }
      }
      else if (message.key == "requestSettings" && message.isStringMessage) {
        OpenComputers.log.info(s"Got a request for our configuration from mod ${message.getSender}.")
        try tryInvokeStaticVoid(getStaticMethod(message.getStringValue, classOf[Config]), Settings.get.config) catch {
          case t: Throwable => OpenComputers.log.warn("Failed sending config.", t)
        }
      }
      else if (message.key == "registerWrenchTool" && message.isStringMessage) {
        OpenComputers.log.info(s"Registering new wrench tool '${message.getStringValue}' from mod ${message.getSender}.")
        try Wrench.add(getStaticMethod(message.getStringValue, classOf[EntityPlayer], classOf[Int], classOf[Int], classOf[Int], classOf[Boolean])) catch {
          case t: Throwable => OpenComputers.log.warn("Failed registering wrench tool.", t)
        }
      }
      else if (message.key == "blacklistPeripheral" && message.isStringMessage) {
        OpenComputers.log.info(s"Blacklisting CC peripheral '${message.getStringValue}' as requested by mod ${message.getSender}.")
        if (!Settings.get.peripheralBlacklist.contains(message.getStringValue)) {
          Settings.get.peripheralBlacklist.add(message.getStringValue)
        }
      }
    }
  }

  def getStaticMethod(name: String, signature: Class[_]*) = {
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
