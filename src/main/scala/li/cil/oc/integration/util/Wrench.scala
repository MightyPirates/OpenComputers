package li.cil.oc.integration.util

import java.lang.reflect.Method

import li.cil.oc.common.IMC
import li.cil.oc.util.BlockPosition
import net.minecraft.entity.player.EntityPlayer

import scala.collection.mutable

object Wrench {
  private val wrenches = mutable.LinkedHashSet.empty[Method]

  def add(wrench: Method): Unit = wrenches += wrench

  def holdsApplicableWrench(player: EntityPlayer, position: BlockPosition): Boolean =
    player.getCurrentEquippedItem != null && wrenches.exists(IMC.tryInvokeStatic(_, player, int2Integer(position.x), int2Integer(position.y), int2Integer(position.z), boolean2Boolean(false))(false))

  def wrenchUsed(player: EntityPlayer, position: BlockPosition): Unit =
    if (player.getCurrentEquippedItem != null) wrenches.foreach(IMC.tryInvokeStaticVoid(_, player, int2Integer(position.x), int2Integer(position.y), int2Integer(position.z), boolean2Boolean(true)))
}
