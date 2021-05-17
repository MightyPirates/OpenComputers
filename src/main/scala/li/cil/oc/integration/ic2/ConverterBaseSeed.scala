package li.cil.oc.integration.ic2

import ic2.api.crops.BaseSeed
import ic2.api.crops.Crops
import li.cil.oc.api.driver.Converter
import net.minecraft.item.ItemStack
import java.util
import scala.collection.convert.WrapAsScala._

class ConverterBaseSeed extends Converter {
  override def convert(value: Any, output: util.Map[AnyRef, AnyRef]): Unit = if (value.isInstanceOf[ItemStack]) {
    val stack = value.asInstanceOf[ItemStack]
    val cc = Crops.instance.getCropCard(stack)
    if (cc != null && stack.getTagCompound().getByte("scan") == 4) {
      output += "crop" -> Map("name" -> cc.name,
        "tier" -> cc.tier,
        "growth" -> stack.getTagCompound().getByte("growth"),
        "gain" -> stack.getTagCompound().getByte("gain"),
        "resistance" -> stack.getTagCompound().getByte("resistance")
      )
    }
  }
}