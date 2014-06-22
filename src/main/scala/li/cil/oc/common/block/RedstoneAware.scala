package li.cil.oc.common.block

import li.cil.oc.common.tileentity.traits
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

abstract class RedstoneAware extends Delegate {
  override def hasTileEntity = true

  override def canConnectToRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getBlockTileEntity(x, y, z) match {
      case redstone: traits.RedstoneAware => redstone.isOutputEnabled
      case _ => false
    }

  override def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    isProvidingWeakPower(world, x, y, z, side)

  override def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getBlockTileEntity(x, y, z) match {
      case redstone: traits.RedstoneAware => math.min(math.max(redstone.output(side), 0), 15)
      case _ => super.isProvidingWeakPower(world, x, y, z, side)
    }

  override def neighborBlockChanged(world: World, x: Int, y: Int, z: Int, blockId: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case redstone: traits.RedstoneAware => redstone.checkRedstoneInputChanged()
      case _ => // Ignore.
    }
}
