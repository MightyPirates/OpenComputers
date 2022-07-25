package li.cil.oc.common.block

import java.util

import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import net.minecraft.block.BlockState
import net.minecraft.client.util.ITooltipFlag
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.StringTextComponent
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

import scala.collection.convert.WrapAsScala._

class Disassembler extends SimpleBlock with traits.PowerAcceptor with traits.StateAware with traits.GUI {
  override protected def tooltipBody(stack: ItemStack, world: IBlockReader, tooltip: util.List[ITextComponent], advanced: ITooltipFlag) {
    for (curr <- Tooltip.get(getClass.getSimpleName.toLowerCase, (Settings.get.disassemblerBreakChance * 100).toInt.toString)) {
      tooltip.add(new StringTextComponent(curr))
    }
  }

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.disassemblerRate

  override def guiType = GuiType.Disassembler

  override def newBlockEntity(world: IBlockReader) = new tileentity.Disassembler()
}
