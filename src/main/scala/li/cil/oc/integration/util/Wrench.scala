package li.cil.oc.integration.util

import java.lang.reflect.Method

import li.cil.oc.common.IMC
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.BlockPos

import scala.collection.mutable

object Wrench {
  private val wrenches = mutable.LinkedHashSet.empty[Method]

  def add(wrench: Method): Unit = wrenches += wrench

  def holdsApplicableWrench(player: EntityPlayer, position: BlockPos): Boolean =
    player.getCurrentEquippedItem != null && wrenches.exists(IMC.tryInvokeStatic(_, player, int2Integer(position.getX), int2Integer(position.getY), int2Integer(position.getZ), boolean2Boolean(false))(false))

  def wrenchUsed(player: EntityPlayer, position: BlockPos): Unit =
    if (player.getCurrentEquippedItem != null) wrenches.foreach(IMC.tryInvokeStaticVoid(_, player, int2Integer(position.getX), int2Integer(position.getY), int2Integer(position.getZ), boolean2Boolean(true)))
}
