package li.cil.oc.integration.gregtech

import java.util

import li.cil.oc.api.driver.Converter
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTTagCompound, NBTTagString}
import li.cil.oc.util.ExtendedNBT._
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.fluids.FluidStack

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable.ArrayBuffer

class ConverterDataStick extends Converter {
  override def convert(value: Any, output: util.Map[AnyRef, AnyRef]): Unit = if (value.isInstanceOf[ItemStack]) {
    val stack = value.asInstanceOf[ItemStack]
    val nbt = stack.stackTagCompound
    if (nbt != null) {
      if (nbt.hasKey("prospection_tier"))
        nbt.getString("title") match {
          case "Raw Prospection Data" => getRawProspectionData(output, nbt)
          case "Analyzed Prospection Data" =>
            getRawProspectionData(output, nbt)
            output += "Analyzed Prospection Data" ->
              nbt.getTagList("pages", NBT.TAG_STRING)
                .toArray[NBTTagString].map((tag: NBTTagString) => tag.func_150285_a_().split('\n'))
          case _ =>
        }
      else if (nbt.hasKey("author") && nbt.getString("author").contains("Recipe Generator") && nbt.hasKey("output")) {
        val outputItem = ItemStack.loadItemStackFromNBT(nbt.getCompoundTag("output"))
        output += "output" -> outputItem.getDisplayName
        output += "time" -> nbt.getInteger("time").toString
        output += "eu" -> nbt.getInteger("eu").toString
        val inputs = new ArrayBuffer[ItemStack]()
        var index = 0
        while (nbt.hasKey(index.toString)) {
          inputs += ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(index.toString))
          index += 1
        }
        output += "inputItems" -> inputs.map((s: ItemStack) => s.getDisplayName -> s.stackSize)
        index = 0
        val inputFluids = new ArrayBuffer[FluidStack]()
        while (nbt.hasKey("f" + index)) {
          inputFluids += FluidStack.loadFluidStackFromNBT(nbt.getCompoundTag("f" + index))
          index += 1
        }
        output += "inputFluids" -> inputFluids.map((s: FluidStack) => s.getLocalizedName -> s.amount)
      }
    }
  }
  private def getRawProspectionData(output: util.Map[AnyRef, AnyRef], nbt: NBTTagCompound) =
    output += "Raw Prospection Data" -> Map(
      "prospection_tier" -> nbt.getByte("prospection_tier"),
      "prospection_pos" -> nbt.getString("prospection_pos"),
      "prospection_ores" -> nbt.getString("prospection_ores").split('|'),
      "prospection_oils" -> nbt.getString("prospection_oils").split('|'),
      "prospection_oils_pos" -> nbt.getString("prospection_oils_pos"),
      "prospection_radius" -> nbt.getString("prospection_radius")
    )
}
