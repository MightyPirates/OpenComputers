package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
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
  override def canProvidePower = true

  override def canConnectRedstone(world: IBlockAccess, pos: BlockPos, side: EnumFacing) =
    world.getTileEntity(pos) match {
      case redstone: tileentity.traits.RedstoneAware => redstone.isOutputEnabled
      case _ => false
    }

  override def getStrongPower(world: IBlockAccess, pos: BlockPos, state: IBlockState, side: EnumFacing) =
    getWeakPower(world, pos, state, side)

  override def getWeakPower(world: IBlockAccess, pos: BlockPos, state: IBlockState, side: EnumFacing) =
    world.getTileEntity(pos) match {
      case redstone: tileentity.traits.RedstoneAware if side != null => math.min(math.max(redstone.output(side.getOpposite), 0), 15)
      case _ => super.getWeakPower(world, pos, state, side)
    }

  // ----------------------------------------------------------------------- //

  override def onNeighborBlockChange(world: World, pos: BlockPos, state: IBlockState, neighborBlock: Block) {
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
      case redstone: tileentity.traits.RedstoneAware =>
        if (redstone.canUpdate)
          redstone.checkRedstoneInputChanged()
        else
          EnumFacing.values().foreach(redstone.updateRedstoneInput)
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
