package li.cil.oc.integration.util

import java.lang.reflect.Method

import li.cil.oc.common.IMC
import li.cil.oc.util.BlockPosition
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

import scala.collection.mutable

object Wrench {
  private val usages = mutable.LinkedHashSet.empty[Method]
  private val checks = mutable.LinkedHashSet.empty[Method]

  def addUsage(wrench: Method): Unit = usages += wrench

  def addCheck(checker: Method): Unit = checks += checker

  def isWrench(stack: ItemStack): Boolean = stack != null && checks.exists(IMC.tryInvokeStatic(_, stack)(false))

  def holdsApplicableWrench(player: EntityPlayer, position: BlockPosition): Boolean =
    player.getHeldItem != null && usages.exists(IMC.tryInvokeStatic(_, player, Int.box(position.x), Int.box(position.y), Int.box(position.z), Boolean.box(false))(false))

  def wrenchUsed(player: EntityPlayer, position: BlockPosition): Unit =
    if (player.getHeldItem != null) usages.foreach(IMC.tryInvokeStaticVoid(_, player, Int.box(position.x), Int.box(position.y), Int.box(position.z), Boolean.box(true)))
}
