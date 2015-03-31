package li.cil.oc.integration.vanilla

import java.util

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.item
import net.minecraft.item.Item
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.convert.WrapAsScala._

object ConverterItemStack extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: item.ItemStack =>
        if (Settings.get.insertIdsInConverters) {
          output += "id" -> Int.box(Item.getIdFromItem(stack.getItem))
        }
        output += "damage" -> Int.box(stack.getItemDamage)
        output += "maxDamage" -> Int.box(stack.getMaxDamage)
        output += "size" -> Int.box(stack.stackSize)
        output += "maxSize" -> Int.box(stack.getMaxStackSize)
        output += "hasTag" -> Boolean.box(stack.hasTagCompound)
        output += "name" -> Item.itemRegistry.getNameForObject(stack.getItem)
        output += "label" -> stack.getDisplayName
        if (stack.hasTagCompound &&
          stack.getTagCompound.hasKey("display", NBT.TAG_COMPOUND) &&
          stack.getTagCompound.getCompoundTag("display").hasKey("Lore", NBT.TAG_LIST)) {
          output += "lore" -> stack.getTagCompound.
            getCompoundTag("display").
            getTagList("Lore", NBT.TAG_STRING).map((tag: NBTTagString) => tag.getString()).
            mkString("\n")
        }

        if (stack.hasTagCompound && Settings.get.allowItemStackNBTTags) {
          output += "tag" -> ItemUtils.saveTag(stack.getTagCompound)
        }
      case _ =>
    }
}
