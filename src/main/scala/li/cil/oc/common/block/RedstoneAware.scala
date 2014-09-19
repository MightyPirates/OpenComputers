package li.cil.oc.common.block

import cpw.mods.fml.common.Optional
import li.cil.oc.common.tileentity.traits
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware
import li.cil.oc.util.mods.Mods
import net.minecraft.block.Block
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection
import powercrystals.minefactoryreloaded.api.rednet.{IRedNetNetworkContainer, IRedNetOmniNode}
import powercrystals.minefactoryreloaded.api.rednet.connectivity.RedNetConnectionType

@Optional.Interface(iface = "powercrystals.minefactoryreloaded.api.rednet.IConnectableRedNet", modid = Mods.IDs.MineFactoryReloaded)
abstract class RedstoneAware extends SimpleBlock with IRedNetOmniNode {
  override def hasTileEntity(metadata: Int) = true

  // ----------------------------------------------------------------------- //

  override def canProvidePower = true

  override def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getTileEntity(x, y, z) match {
      case redstone: traits.RedstoneAware => redstone.isOutputEnabled
      case _ => false
    }

  override def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    isProvidingWeakPower(world, x, y, z, side)

  override def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getTileEntity(x, y, z) match {
      case redstone: traits.RedstoneAware => math.min(math.max(redstone.output(side), 0), 15)
      case _ => super.isProvidingWeakPower(world, x, y, z, side)
    }

  // ----------------------------------------------------------------------- //

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, block: Block) {
    if (Mods.MineFactoryReloaded.isAvailable) {
      world.getTileEntity(x, y, z) match {
        case t: BundledRedstoneAware => for (side <- ForgeDirection.VALID_DIRECTIONS) {
          world.getBlock(x + side.offsetX, y + side.offsetY, z + side.offsetZ) match {
            case block: IRedNetNetworkContainer =>
            case _ => for (color <- 0 until 16) {
              t.rednetInput(side, color, 0)
            }
          }
        }
        case _ =>
      }
    }
    world.getTileEntity(x, y, z) match {
      case redstone: traits.RedstoneAware => redstone.checkRedstoneInputChanged()
      case _ => // Ignore.
    }
  }

  // ----------------------------------------------------------------------- //

  override def getConnectionType(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) = RedNetConnectionType.CableAll

  override def getOutputValue(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, color: Int) =
    world.getTileEntity(x, y, z) match {
      case t: BundledRedstoneAware => t.bundledOutput(side, color)
      case _ => 0
    }

  override def getOutputValues(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getTileEntity(x, y, z) match {
      case t: BundledRedstoneAware => t.bundledOutput(side)
      case _ => Array.fill(16)(0)
    }

  override def onInputChanged(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, inputValue: Int) {}

  override def onInputsChanged(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, inputValues: Array[Int]) =
    world.getTileEntity(x, y, z) match {
      case t: BundledRedstoneAware => for (color <- 0 until 16) {
        t.rednetInput(side, color, inputValues(color))
      }
      case _ =>
    }
}
