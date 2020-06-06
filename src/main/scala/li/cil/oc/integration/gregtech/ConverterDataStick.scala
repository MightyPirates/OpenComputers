package li.cil.oc.integration.gregtech

import java.util

import li.cil.oc.api.driver.Converter
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTTagCompound, NBTTagList, NBTTagString}
import li.cil.oc.util.ExtendedNBT._
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.convert.WrapAsScala._

class ConverterDataStick extends Converter {
  override def convert(value: Any, output: util.Map[AnyRef, AnyRef]): Unit = if (value.isInstanceOf[ItemStack]) {
    val stack = value.asInstanceOf[ItemStack]
    val nbt = stack.stackTagCompound
    if (nbt.hasKey("prospection_tier"))
      nbt.getString("title") match {
        case "Raw Prospection Data" => getRawProspectionData(output, nbt)
        case "Analyzed Prospection Data" => {
          getRawProspectionData(output, nbt)
          output += "Analyzed Prospection Data" ->
            nbt.getTagList("pages", NBT.TAG_STRING)
              .toArray[NBTTagString].map( (tag: NBTTagString) => tag.func_150285_a_().split('\n'))
        }
        case _ =>
      }
  }
  def getRawProspectionData(output: util.Map[AnyRef, AnyRef], nbt: NBTTagCompound) =
    output += "Raw Prospection Data" -> Map(
      "prospection_tier" -> nbt.getByte("prospection_tier"),
      "prospection_pos" -> nbt.getString("prospection_pos"),
      "prospection_ores" -> nbt.getString("prospection_ores").split('|'),
      "prospection_oils" -> nbt.getString("prospection_oils").split('|'),
      "prospection_oils_pos" -> nbt.getString("prospection_oils_pos"),
      "prospection_radius" -> Integer.parseInt(nbt.getString("prospection_radius"))
    )
}
