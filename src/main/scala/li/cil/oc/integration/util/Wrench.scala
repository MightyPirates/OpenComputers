package li.cil.oc.integration.util

import java.lang.reflect.Method

import li.cil.oc.common.IMC
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos

import scala.collection.mutable

object Wrench {
  private val usages = mutable.LinkedHashSet.empty[Method]
  private val checks = mutable.LinkedHashSet.empty[Method]

  def addUsage(wrench: Method): Unit = usages += wrench

  def addCheck(checker: Method): Unit = checks += checker

  def isWrench(stack: ItemStack): Boolean = !stack.isEmpty && checks.exists(IMC.tryInvokeStatic(_, stack)(false))

  def holdsApplicableWrench(player: PlayerEntity, position: BlockPos): Boolean =
    !player.getItemInHand(Hand.MAIN_HAND).isEmpty && usages.exists(IMC.tryInvokeStatic(_, player, position, java.lang.Boolean.FALSE)(false))

  def wrenchUsed(player: PlayerEntity, position: BlockPos): Unit =
    if (!player.getItemInHand(Hand.MAIN_HAND).isEmpty) usages.foreach(IMC.tryInvokeStaticVoid(_, player, position, java.lang.Boolean.TRUE))
}
