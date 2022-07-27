package li.cil.oc.common.block

import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.block.BlockState
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

class Printer extends SimpleBlock with traits.StateAware with traits.GUI {
  override def guiType = GuiType.Printer

  override def newBlockEntity(world: IBlockReader) = new tileentity.Printer(tileentity.TileEntityTypes.PRINTER)
}
