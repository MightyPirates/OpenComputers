package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.tileentity
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection

class Cable(val parent: SpecialDelegator) extends SpecialDelegate {
  GameRegistry.registerTileEntity(classOf[tileentity.Cable], "oc.cable")

  val unlocalizedName = "Cable"

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Cable)

  // ----------------------------------------------------------------------- //

  override def getLightOpacity(world: World, x: Int, y: Int, z: Int) = 0

  override def isBlockNormalCube(world: World, x: Int, y: Int, z: Int) = false

  override def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = false

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, blockId: Int) {
    world.markBlockForRenderUpdate(x, y, z)
    super.onNeighborBlockChange(world, x, y, z, blockId)
  }

  // ----------------------------------------------------------------------- //

  override def setBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int) {
    world.getBlockTileEntity(x, y, z) match {
      case cable: tileentity.Cable =>
        val bounds = cable.bounds
        parent.setBlockBounds(
          bounds.minX.toFloat, bounds.minY.toFloat, bounds.minZ.toFloat,
          bounds.maxX.toFloat, bounds.maxY.toFloat, bounds.maxZ.toFloat)
      case _ =>
    }
  }
}
