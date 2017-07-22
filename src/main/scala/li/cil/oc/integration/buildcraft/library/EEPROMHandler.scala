package li.cil.oc.integration.buildcraft.library

import buildcraft.api.library.LibraryTypeHandler.HandlerType
import buildcraft.api.library.LibraryTypeHandlerNBT
import li.cil.oc.Settings
import li.cil.oc.api
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumChatFormatting

import scala.collection.convert.WrapAsScala._

object EEPROMHandler extends LibraryTypeHandlerNBT("ocrom") {
  override def isHandler(stack: ItemStack, handlerType: HandlerType) = api.Items.get(stack) == api.Items.get("eeprom")

  override def getTextColor = 0xCCFFCC

  override def getName(stack: ItemStack) = {
    val driver = api.Driver.driverFor(stack)
    if (driver != null) {
      driver.dataTag(stack).getString(Settings.namespace + "label")
    }
    else EnumChatFormatting.OBFUSCATED.toString + "?????"
  }

  override def load(stack: ItemStack, nbt: NBTTagCompound): ItemStack = {
    val driver = api.Driver.driverFor(stack)
    if (driver != null) {
      val stackData = driver.dataTag(stack)
      nbt.func_150296_c().foreach {
        case key: String => stackData.setTag(key, nbt.getTag(key))
      }
    }
    stack
  }

  override def store(stack: ItemStack, nbt: NBTTagCompound): Boolean = {
    val driver = api.Driver.driverFor(stack)
    if (driver != null) {
      val stackData = driver.dataTag(stack)
      stackData.func_150296_c().foreach {
        case key: String => nbt.setTag(key, stackData.getTag(key))
      }
      true
    }
    else false
  }
}
