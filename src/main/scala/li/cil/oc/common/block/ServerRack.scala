package li.cil.oc.common.block

import li.cil.oc.Settings
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.common.property.IUnlistedProperty
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.mutable.ArrayBuffer

class ServerRack extends RedstoneAware with traits.PowerAcceptor with traits.Rotatable with traits.StateAware with traits.GUI {
  @SideOnly(Side.CLIENT)
  override def getMixedBrightnessForBlock(world: IBlockAccess, pos: BlockPos) = {
    if (pos.getY >= 0 && pos.getY < 256) world.getTileEntity(pos) match {
      case rack: tileentity.ServerRack =>
        def brightness(pos: BlockPos) = world.getCombinedLight(pos, world.getBlockState(pos).getBlock.getLightValue(world, pos))
        val value = brightness(pos.offset(rack.facing))
        val skyBrightness = (value >> 20) & 15
        val blockBrightness = (value >> 4) & 15
        ((skyBrightness * 3 / 4) << 20) | ((blockBrightness * 3 / 4) << 4)
      case _ => super.getMixedBrightnessForBlock(world, pos)
    }
    else super.getMixedBrightnessForBlock(world, pos)
  }

  override def isOpaqueCube = false

  override def isFullCube = false

  override def isBlockSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = side == EnumFacing.SOUTH

  override def isSideSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = toLocal(world, pos, side) != EnumFacing.SOUTH

  override protected def setDefaultExtendedState(state: IBlockState) = setDefaultState(state)

  override protected def addExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos) =
    (state, world.getTileEntity(pos)) match {
      case (extendedState: IExtendedBlockState, tile: tileentity.traits.TileEntity) =>
        super.addExtendedState(extendedState.withProperty(property.PropertyTile.Tile, tile), world, pos)
      case _ => None
    }

  override protected def createProperties(listed: ArrayBuffer[IProperty[_ <: Comparable[AnyRef]]], unlisted: ArrayBuffer[IUnlistedProperty[_ <: Comparable[AnyRef]]]) {
    super.createProperties(listed, unlisted)
    unlisted += property.PropertyTile.Tile.asInstanceOf[IUnlistedProperty[_ <: Comparable[AnyRef]]]
  }

  // ----------------------------------------------------------------------- //

  override def energyThroughput = Settings.get.serverRackRate

  override def guiType = GuiType.Rack

  override def hasTileEntity(state: IBlockState) = true

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.ServerRack()
}
