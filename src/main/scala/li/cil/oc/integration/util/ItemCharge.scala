package li.cil.oc.integration.util

import java.lang.reflect.Method

import li.cil.oc.common.IMC
import net.minecraft.item.ItemStack

import scala.collection.mutable

object ItemCharge {
  private val chargers = mutable.LinkedHashSet.empty[(Method, Method)]

  def add(canCharge: Method, charge: Method): Unit = chargers += ((canCharge, charge))

  def canCharge(stack: ItemStack): Boolean = stack != null && chargers.exists(charger => IMC.tryInvokeStatic(charger._1, stack)(false))

  def charge(stack: ItemStack, amount: Double): Double = {
    if (stack != null) chargers.find(charger => IMC.tryInvokeStatic(charger._1, stack)(false)) match {
      case Some(charger) => IMC.tryInvokeStatic(charger._2, stack, Double.box(amount), java.lang.Boolean.FALSE)(0.0)
      case _ => 0.0
    }
    else 0.0
  }
}
