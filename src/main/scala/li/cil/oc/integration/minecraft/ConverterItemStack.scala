package li.cil.oc.integration.minecraft

import java.util

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.integration.Mods
import net.minecraftforge.fml.common.Loader
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.enchantment.EnchantmentHelper
import li.cil.oc.util.ItemUtils
import net.minecraft.item
import net.minecraft.item.Item
import net.minecraft.nbt.{NBTTagCompound, NBTTagList, NBTTagString}
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.oredict.OreDictionary

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object ConverterItemStack extends api.driver.Converter {
  def getTagValue(tag: NBTTagCompound, key: String): AnyRef = tag.getTagId(key) match {
    case NBT.TAG_INT => Int.box(tag.getInteger(key))
    case NBT.TAG_STRING => tag.getString(key)
    case NBT.TAG_BYTE => Byte.box(tag.getByte(key))
    case NBT.TAG_COMPOUND => tag.getCompoundTag(key)
    case NBT.TAG_LIST => tag.getTagList(key, NBT.TAG_STRING)
    case _ => null
  }

  def withTag(tag: NBTTagCompound, key: String, tagId: Int, f: AnyRef => AnyRef): AnyRef = {
    if (tag.hasKey(key, tagId)) {
      Option(getTagValue(tag, key)) match {
        case Some(value) => f(value)
        case _ => null
      }
    } else null
  }

  def withCompound(tag: NBTTagCompound, key: String, f: NBTTagCompound => AnyRef): AnyRef = {
    withTag(tag, key, NBT.TAG_COMPOUND, { case value: NBTTagCompound => f(value)})
  }

  def withList(tag: NBTTagCompound, key: String, f: NBTTagList => AnyRef): AnyRef = {
    withTag(tag, key, NBT.TAG_STRING, { case value: NBTTagList => f(value)})
  }

  override def convert(value: AnyRef, output: util.Map[AnyRef, AnyRef]) =
    value match {
      case stack: item.ItemStack =>
        if (Settings.get.insertIdsInConverters) {
          output += "id" -> Int.box(Item.getIdFromItem(stack.getItem))
          output += "oreNames" -> OreDictionary.getOreIDs(stack).map(OreDictionary.getOreName)
        }
        output += "damage" -> Int.box(stack.getItemDamage)
        output += "maxDamage" -> Int.box(stack.getMaxDamage)
        output += "size" -> Int.box(stack.getCount)
        output += "maxSize" -> Int.box(stack.getMaxStackSize)
        output += "hasTag" -> Boolean.box(stack.hasTagCompound)
        output += "name" -> Item.REGISTRY.getNameForObject(stack.getItem)
        output += "label" -> stack.getDisplayName

        // custom mod tags
        if (stack.hasTagCompound) {
          val tags = stack.getTagCompound

          //Lore tags
          withCompound(tags, "display", withList(_, "Lore", {
              output += "lore" -> _.map((tag: NBTTagString) => tag.getString).mkString("\n")
            })
          )

          // IC2 reactor items custom damage
          withTag(tags, "advDmg", NBT.TAG_INT, dmg => output += "customDamage" -> dmg)

          // draconic upgrades
          if (Mods.DraconicEvolution.isModAvailable) {
            withCompound(tags, "DEUpgrades", de => {
              output += "DEUpgrades" -> de
            })
            (0 until 15).foreach(n => {
              val profileName: String = s"Profile_$n"
              Option(getTagValue(tags, profileName)) match {
                case Some(profile: NBTTagCompound) => output += profileName -> profile
                case _ =>
              }
            })
          }

          withTag(tags, "Energy", NBT.TAG_INT, value => output += "Energy" -> value)

          if (Settings.get.allowItemStackNBTTags) {
            output += "tag" -> ItemUtils.saveTag(stack.getTagCompound)
          }
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
      case _ =>
    }
}
