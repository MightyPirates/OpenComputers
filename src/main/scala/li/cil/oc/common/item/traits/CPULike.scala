package li.cil.oc.common.item.traits

import java.util

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.item.MutableProcessor
import li.cil.oc.integration.opencomputers.DriverCPU
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.util.text.TextComponentTranslation
import net.minecraft.world.World

import scala.collection.convert.WrapAsScala._
import scala.language.existentials

trait CPULike extends Delegate {
  def cpuTier: Int

  override protected def tooltipData: Seq[Any] = Seq(Settings.get.cpuComponentSupport(cpuTier))

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[String]) {
    tooltip.addAll(Tooltip.get("CPU.Architecture", api.Machine.getArchitectureName(DriverCPU.architecture(stack))))
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ActionResult[ItemStack] = {
    if (player.isSneaking) {
      if (!world.isRemote) {
        api.Driver.driverFor(stack) match {
          case driver: MutableProcessor =>
            val architectures = driver.allArchitectures.toList
            if (architectures.nonEmpty) {
              val currentIndex = architectures.indexOf(driver.architecture(stack))
              val newIndex = (currentIndex + 1) % architectures.length
              val archClass = architectures(newIndex)
              val archName = api.Machine.getArchitectureName(archClass)
              driver.setArchitecture(stack, archClass)
              player.addChatMessage(new TextComponentTranslation(Settings.namespace + "tooltip.CPU.Architecture", archName))
            }
            player.swingArm(EnumHand.MAIN_HAND)
          case _ => // No known driver for this processor.
        }
      }
    }
    ActionResult.newResult(EnumActionResult.SUCCESS, stack)
  }
}
