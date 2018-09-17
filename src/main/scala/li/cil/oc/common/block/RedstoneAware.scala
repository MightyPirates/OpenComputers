package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.fml.common.Optional
/* TODO MFR
import powercrystals.minefactoryreloaded.api.rednet.IRedNetNetworkContainer
import powercrystals.minefactoryreloaded.api.rednet.IRedNetOmniNode
import powercrystals.minefactoryreloaded.api.rednet.connectivity.RedNetConnectionType
*/

@Optional.Interface(iface = "powercrystals.minefactoryreloaded.api.rednet.IRedNetOmniNode", modid = Mods.IDs.MineFactoryReloaded)
abstract class RedstoneAware extends SimpleBlock /* with IRedNetOmniNode TODO MFR */ {
  override def canProvidePower(state: IBlockState): Boolean = true

  override def canConnectRedstone(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing): Boolean =
    world.getTileEntity(pos) match {
      case redstone: tileentity.traits.RedstoneAware => redstone.isOutputEnabled
      case _ => false
    }

  override def getStrongPower(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) =
    getWeakPower(state, world, pos, side)

  override def getWeakPower(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) =
    world.getTileEntity(pos) match {
      case redstone: tileentity.traits.RedstoneAware if side != null => redstone.output(side.getOpposite) max 0
      case _ => super.getWeakPower(state, world, pos, side)
    }

  // ----------------------------------------------------------------------- //

  override def neighborChanged(state: IBlockState, world: World, pos: BlockPos, neighborBlock: Block): Unit = {
    /* TODO MFR
    if (Mods.MineFactoryReloaded.isAvailable) {
      val position = BlockPosition(x, y, z)
      world.getTileEntity(position) match {
        case t: tileentity.traits.BundledRedstoneAware => for (side <- EnumFacing.values) {
          world.getBlock(position.offset(side)) match {
            case block: IRedNetNetworkContainer =>
            case _ => for (color <- 0 until 16) {
              t.rednetInput(side, color, 0)
            }
          }
        }
        case _ =>
      }
    }
    */
    world.getTileEntity(pos) match {
      case redstone: tileentity.traits.RedstoneAware => redstone.checkRedstoneInputChanged()
      case _ => // Ignore.
    }
  }

  // ----------------------------------------------------------------------- //

  /* TODO MFR
  override def getConnectionType(world: World, x: Int, y: Int, z: Int, side: EnumFacing) = RedNetConnectionType.CableAll

  override def getOutputValue(world: World, x: Int, y: Int, z: Int, side: EnumFacing, color: Int) =
    world.getTileEntity(x, y, z) match {
      case t: tileentity.traits.BundledRedstoneAware => t.bundledOutput(side, color)
      case _ => 0
    }

  override def getOutputValues(world: World, x: Int, y: Int, z: Int, side: EnumFacing) =
    world.getTileEntity(x, y, z) match {
      case t: tileentity.traits.BundledRedstoneAware => t.bundledOutput(side)
      case _ => Array.fill(16)(0)
    }

  override def onInputChanged(world: World, x: Int, y: Int, z: Int, side: EnumFacing, inputValue: Int) {}

  override def onInputsChanged(world: World, x: Int, y: Int, z: Int, side: EnumFacing, inputValues: Array[Int]) =
    world.getTileEntity(x, y, z) match {
      case t: tileentity.traits.BundledRedstoneAware => for (color <- 0 until 16) {
        t.rednetInput(side, color, inputValues(color))
      }
      case _ =>
    }
  */
}
