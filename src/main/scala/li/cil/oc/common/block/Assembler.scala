package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.block.BlockState
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockReader
import net.minecraft.world.World

class Assembler extends SimpleBlock with traits.PowerAcceptor with traits.StateAware with traits.GUI {
  override def energyThroughput = Settings.get.assemblerRate

  override def guiType = GuiType.Assembler

  override def newBlockEntity(world: IBlockReader) = new tileentity.Assembler(tileentity.TileEntityTypes.ASSEMBLER)
}
