package li.cil.oc.common.block

import cpw.mods.fml.common.Optional
import li.cil.oc.common.tileentity
import li.cil.oc.integration.Mods
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraft.block.Block
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection
import powercrystals.minefactoryreloaded.api.rednet.IRedNetNetworkContainer
import powercrystals.minefactoryreloaded.api.rednet.IRedNetOmniNode
import powercrystals.minefactoryreloaded.api.rednet.connectivity.RedNetConnectionType

@Optional.Interface(iface = "powercrystals.minefactoryreloaded.api.rednet.IRedNetOmniNode", modid = Mods.IDs.MineFactoryReloaded)
abstract class RedstoneAware extends SimpleBlock with IRedNetOmniNode {
  override def hasTileEntity(metadata: Int) = true

  // ----------------------------------------------------------------------- //

  override def canProvidePower = true

  override def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getTileEntity(x, y, z) match {
      case redstone: tileentity.traits.RedstoneAware => redstone.isOutputEnabled
      case _ => false
    }

  override def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getTileEntity(x, y, z) match {
      case redstone: tileentity.traits.RedstoneAware => redstone.output(side) max 0
      case _ => super.isProvidingWeakPower(world, x, y, z, side)
    }

  // ----------------------------------------------------------------------- //

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, block: Block) {
    if (Mods.MineFactoryReloaded.isAvailable) {
      val position = BlockPosition(x, y, z)
      world.getTileEntity(position) match {
        case t: tileentity.traits.BundledRedstoneAware => for (side <- ForgeDirection.VALID_DIRECTIONS) {
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
    world.getTileEntity(x, y, z) match {
      case redstone: tileentity.traits.RedstoneAware =>
        if (redstone.canUpdate)
          redstone.checkRedstoneInputChanged()
        else
          ForgeDirection.VALID_DIRECTIONS.foreach(redstone.updateRedstoneInput)
      case _ => // Ignore.
    }
  }

  // ----------------------------------------------------------------------- //

  override def getConnectionType(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) = RedNetConnectionType.CableAll

  override def getOutputValue(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, color: Int) =
    world.getTileEntity(x, y, z) match {
      case t: tileentity.traits.BundledRedstoneAware => t.bundledOutput(side, color)
      case _ => 0
    }

  override def getOutputValues(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getTileEntity(x, y, z) match {
      case t: tileentity.traits.BundledRedstoneAware => t.bundledOutput(side)
      case _ => Array.fill(16)(0)
    }

  override def onInputChanged(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, inputValue: Int) {}

  override def onInputsChanged(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, inputValues: Array[Int]) =
    world.getTileEntity(x, y, z) match {
      case t: tileentity.traits.BundledRedstoneAware => for (color <- 0 until 16) {
        t.rednetInput(side, color, inputValues(color))
      }
      case _ =>
    }
}
