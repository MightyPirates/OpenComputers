package li.cil.oc.integration.vanilla

import java.util

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import li.cil.oc.util.ItemUtils
import net.minecraft.item
import net.minecraft.item.Item
import net.minecraft.nbt.NBTTagString
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.oredict.OreDictionary

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object ConverterItemStack extends api.driver.Converter {
  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: item.ItemStack =>
        if (Settings.get.insertIdsInConverters) {
          output += "id" -> Int.box(Item.getIdFromItem(stack.getItem))
          output += "oreNames" -> OreDictionary.getOreIDs(stack).map(OreDictionary.getOreName)
        }
        output += "damage" -> Int.box(stack.getItemDamage)
        output += "maxDamage" -> Int.box(stack.getMaxDamage)
        output += "size" -> Int.box(stack.stackSize)
        output += "maxSize" -> Int.box(stack.getMaxStackSize)
        output += "hasTag" -> Boolean.box(stack.hasTagCompound)
        output += "name" -> Item.REGISTRY.getNameForObject(stack.getItem)
        output += "label" -> stack.getDisplayName
        if (stack.hasTagCompound &&
          stack.getTagCompound.hasKey("display", NBT.TAG_COMPOUND) &&
          stack.getTagCompound.getCompoundTag("display").hasKey("Lore", NBT.TAG_LIST)) {
          output += "lore" -> stack.getTagCompound.
            getCompoundTag("display").
            getTagList("Lore", NBT.TAG_STRING).map((tag: NBTTagString) => tag.getString).
            mkString("\n")
        }

        // IC2 reactor items custom damage
        if (stack.hasTagCompound && stack.getTagCompound.hasKey("advDmg", NBT.TAG_INT)) {
          output += "customDamage" -> Int.box(stack.getTagCompound.getInteger("advDmg"))
        }

        val enchantments = mutable.ArrayBuffer.empty[mutable.Map[String, Any]]
        EnchantmentHelper.getEnchantments(stack).collect {
          case (enchantment, level) =>
            val map = mutable.Map[String, Any](
              "name" -> enchantment.getName,
              "label" -> enchantment.getTranslatedName(level),
              "level" -> level
            )
            enchantments += map
        }
        if (enchantments.nonEmpty) {
          output += "enchantments" -> enchantments
        }

        if (stack.hasTagCompound && Settings.get.allowItemStackNBTTags) {
          output += "tag" -> ItemUtils.saveTag(stack.getTagCompound)
        }
      case _ =>
    }
}
