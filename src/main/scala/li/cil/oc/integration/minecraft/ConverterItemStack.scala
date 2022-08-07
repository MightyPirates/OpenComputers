package li.cil.oc.integration.minecraft

import java.util

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.integration.Mods
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.item
import net.minecraft.item.Item
import net.minecraft.nbt.{CompoundNBT, ListNBT, StringNBT}
import net.minecraft.tags.ItemTags
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable

object ConverterItemStack extends api.driver.Converter {
  def getTagValue(tag: CompoundNBT, key: String): AnyRef = tag.getTagType(key) match {
    case NBT.TAG_INT => Int.box(tag.getInt(key))
    case NBT.TAG_STRING => tag.getString(key)
    case NBT.TAG_BYTE => Byte.box(tag.getByte(key))
    case NBT.TAG_COMPOUND => tag.getCompound(key)
    case NBT.TAG_LIST => tag.getList(key, NBT.TAG_STRING)
    case _ => null
  }

  def withTag(tag: CompoundNBT, key: String, tagId: Int, f: AnyRef => AnyRef): AnyRef = {
    if (tag.contains(key, tagId)) {
      Option(getTagValue(tag, key)) match {
        case Some(value) => f(value)
        case _ => null
      }
    } else null
  }

  def withCompound(tag: CompoundNBT, key: String, f: CompoundNBT => AnyRef): AnyRef = {
    withTag(tag, key, NBT.TAG_COMPOUND, { case value: CompoundNBT => f(value)})
  }

  def withList(tag: CompoundNBT, key: String, f: ListNBT => AnyRef): AnyRef = {
    withTag(tag, key, NBT.TAG_STRING, { case value: ListNBT => f(value)})
  }

  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: item.ItemStack =>
        if (Settings.get.insertIdsInConverters) {
          output += "id" -> Int.box(Item.getId(stack.getItem))
          output += "oreNames" -> stack.getItem.getTags.map(_.toString).toArray
        }
        output += "damage" -> Int.box(stack.getDamageValue)
        output += "maxDamage" -> Int.box(stack.getMaxDamage)
        output += "size" -> Int.box(stack.getCount)
        output += "maxSize" -> Int.box(stack.getMaxStackSize)
        output += "hasTag" -> Boolean.box(stack.hasTag)
        output += "name" -> stack.getItem.getRegistryName
        output += "label" -> stack.getDisplayName.getString

        // custom mod tags
        if (stack.hasTag) {
          val tags = stack.getTag

          //Lore tags
          withCompound(tags, "display", withList(_, "Lore", {
              output += "lore" -> _.map((tag: StringNBT) => tag.getAsString).mkString("\n")
            })
          )

          withTag(tags, "Energy", NBT.TAG_INT, value => output += "Energy" -> value)

          if (Settings.get.allowItemStackNBTTags) {
            output += "tag" -> ItemUtils.saveTag(stack.getTag)
          }
        }

        val enchantments = mutable.ArrayBuffer.empty[mutable.Map[String, Any]]
        EnchantmentHelper.getEnchantments(stack).collect {
          case (enchantment, level) =>
            val map = mutable.Map[String, Any](
              "name" -> enchantment.getRegistryName,
              "label" -> enchantment.getFullname(level),
              "level" -> level
            )
            enchantments += map
        }
        if (enchantments.nonEmpty) {
          output += "enchantments" -> enchantments
        }
      case _ =>
    }
}
