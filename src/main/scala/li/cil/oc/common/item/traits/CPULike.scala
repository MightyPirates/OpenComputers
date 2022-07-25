package li.cil.oc.common.item.traits

import java.util

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.driver.item.MutableProcessor
import li.cil.oc.integration.opencomputers.DriverCPU
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.ActionResultType
import net.minecraft.util.Hand
import net.minecraft.util.Util
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.util.text.TranslationTextComponent
import net.minecraft.world.World

import scala.collection.convert.ImplicitConversionsToScala._
import scala.language.existentials

trait CPULike extends Delegate {
  def cpuTier: Int

  override protected def tooltipData: Seq[Any] = Seq(Settings.get.cpuComponentSupport(cpuTier))

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[ITextComponent]) {
    for (curr <- Tooltip.get("cpu.Architecture", api.Machine.getArchitectureName(DriverCPU.architecture(stack)))) {
      tooltip.add(new StringTextComponent(curr))
    }
  }

  override def use(stack: ItemStack, world: World, player: PlayerEntity): ActionResult[ItemStack] = {
    if (player.isCrouching) {
      if (!world.isClientSide) {
        api.Driver.driverFor(stack) match {
          case driver: MutableProcessor =>
            val architectures = driver.allArchitectures.toList
            if (architectures.nonEmpty) {
              val currentIndex = architectures.indexOf(driver.architecture(stack))
              val newIndex = (currentIndex + 1) % architectures.length
              val archClass = architectures(newIndex)
              val archName = api.Machine.getArchitectureName(archClass)
              driver.setArchitecture(stack, archClass)
              player.sendMessage(new TranslationTextComponent(Settings.namespace + "tooltip.cpu.Architecture", archName), Util.NIL_UUID)
            }
            player.swing(Hand.MAIN_HAND)
          case _ => // No known driver for this processor.
        }
      }
    }
    new ActionResult(ActionResultType.sidedSuccess(world.isClientSide), stack)
  }
}
