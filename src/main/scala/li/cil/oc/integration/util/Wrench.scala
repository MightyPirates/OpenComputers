package li.cil.oc.integration.util

import java.lang.reflect.Method

import li.cil.oc.common.IMC
import net.minecraft.entity.player.EntityPlayer

import scala.collection.mutable

object Wrench {
  private val wrenches = mutable.LinkedHashSet.empty[Method]

  def add(wrench: Method): Unit = wrenches += wrench

  def holdsApplicableWrench(player: EntityPlayer, x: Int, y: Int, z: Int): Boolean =
    player.getCurrentEquippedItem != null && wrenches.exists(IMC.tryInvokeStatic(_, player, int2Integer(x), int2Integer(y), int2Integer(z), boolean2Boolean(false))(false))

  def wrenchUsed(player: EntityPlayer, x: Int, y: Int, z: Int): Unit =
    if (player.getCurrentEquippedItem != null) wrenches.foreach(IMC.tryInvokeStaticVoid(_, player, int2Integer(x), int2Integer(y), int2Integer(z), boolean2Boolean(true)))
}
