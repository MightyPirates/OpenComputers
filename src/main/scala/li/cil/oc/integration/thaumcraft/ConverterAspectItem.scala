package li.cil.oc.integration.thaumcraft

import java.util

import li.cil.oc.api.driver.Converter
import net.minecraft.item.ItemStack
import thaumcraft.api.aspects.{AspectList, IAspectContainer}
import thaumcraft.common.items.wands.ItemWandCasting

import scala.collection.convert.WrapAsScala._

object ConverterAspectItem extends Converter {
  override def convert(value: scala.Any, output: util.Map[AnyRef, AnyRef]): Unit = value match {
    case stack: ItemStack if stack.hasTagCompound =>
      var aspects = new AspectList()
      aspects.readFromNBT(stack.getTagCompound)
      if (aspects.size() > 0)
        output += "aspects" -> aspects
      stack.getItem match {
        case wand : ItemWandCasting =>
          aspects = wand.getAllVis(stack)
          if (aspects.size() > 0) {
            output += "aspects" -> aspects
          }
        case _ =>
      }

    case container : IAspectContainer =>
      output += "aspects" -> container.getAspects

    case aspects : AspectList =>
      var i = 1
      for (aspect <- aspects.getAspects) {
        if (aspect != null) {
          val aspectMap = new util.HashMap[AnyRef, AnyRef]()
          aspectMap += "name" -> aspect.getName
          aspectMap += "amount" -> aspects.getAmount(aspect).asInstanceOf[AnyRef]
          output += i.asInstanceOf[AnyRef] -> aspectMap
          i += 1;
        }
      }
    case _ =>
  }
}
