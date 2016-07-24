package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.util.Tooltip
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class Disassembler extends SimpleBlock with traits.PowerAcceptor with traits.StateAware with traits.GUI {
  override protected def tooltipBody(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(getClass.getSimpleName, (Settings.get.disassemblerBreakChance * 100).toInt.toString))
  }

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.disassemblerRate

  override def guiType = GuiType.Disassembler

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Disassembler()
}
