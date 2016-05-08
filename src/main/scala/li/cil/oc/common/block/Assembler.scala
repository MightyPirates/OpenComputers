package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.integration.coloredlights.ModColoredLights
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World

class Assembler extends SimpleBlock with traits.PowerAcceptor with traits.StateAware with traits.GUI {
  ModColoredLights.setLightLevel(this, 0, 3, 5)

  override def isOpaqueCube(state: IBlockState): Boolean = false

  override def isFullCube(state: IBlockState): Boolean = false

  override def isBlockSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = side == EnumFacing.DOWN || side == EnumFacing.UP

  override def isSideSolid(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) = side == EnumFacing.DOWN || side == EnumFacing.UP

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.assemblerRate

  override def guiType = GuiType.Assembler

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Assembler()
}
