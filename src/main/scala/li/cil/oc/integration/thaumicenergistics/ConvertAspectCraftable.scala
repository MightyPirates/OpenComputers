package li.cil.oc.integration.thaumicenergistics
import java.util
import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.api.driver.Converter
import net.minecraft.item.ItemStack
import scala.collection.convert.WrapAsScala._

object ConvertAspectCraftable extends Converter {
  private val DistillationPattern = GameRegistry.findItem("thaumicenergistics", "crafting.aspect")
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]): Unit = value match {
    case stack: ItemStack if stack.getItem == DistillationPattern && stack.hasTagCompound =>
      output += "aspect" -> stack.getTagCompound.getString("Aspect")
    case _ =>
  }
}
