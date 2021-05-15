package li.cil.oc.util

import net.minecraft.item.ItemStack

import scala.math.ScalaNumber

object ResultWrapper {
  def result(args: Any*): Array[AnyRef] = {
    def unwrap(arg: Any): AnyRef = arg match {
      case x: ScalaNumber => x.underlying
      case x: ItemStack if x.isEmpty => null
      case x => x.asInstanceOf[AnyRef]
    }
    Array(args map unwrap: _*)
  }
}
