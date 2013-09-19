package li.cil.oc.common.block

import li.cil.oc.Blocks
import li.cil.oc.OpenComputers
import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity.TileEntityComputer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.MathHelper
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

class BlockComputer extends SubBlock {
  val unlocalizedName = "Computer"

  // ----------------------------------------------------------------------- //
  // Rendering stuff
  // ----------------------------------------------------------------------- //

  // TODO Icon loading and rendering.
  /*
  override def getBlockTexture(block: IBlockAccess, x: Int, y: Int, z: Int, side: Int) = side match {
    case 0 | 1 => Icons.top
    case side if (side == block.getBlockMetadata(x, y, z)) => Icons.front
    case _ => Icons.side
  }

  override def getIcon(side: Int, metadata: Int) = side match {
    case 0 | 1 => Icons.top
    case 4 => Icons.front
    case _ => Icons.side
  }

  override def registerIcons(register: IconRegister) = {
    Icons.front = register.registerIcon("oc:computer")
    Icons.side = register.registerIcon("oc:computerSide")
    Icons.top = register.registerIcon("oc:computerTop")
  }
  */

  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new TileEntityComputer(world.isRemote)

  // ----------------------------------------------------------------------- //
  // Destruction / Interaction
  // ----------------------------------------------------------------------- //

  override def breakBlock(world: World, x: Int, y: Int, z: Int, `side?`: Int, metadata: Int) = {
    world.getBlockTileEntity(x, y, z).asInstanceOf[TileEntityComputer].turnOff()
    super.breakBlock(world, x, y, z, `side?`, metadata)
  }

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
    side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    if (!player.isSneaking()) {
      // Start the computer if it isn't already running and open the GUI.
      world.getBlockTileEntity(x, y, z).asInstanceOf[TileEntityComputer].turnOn()
      player.openGui(OpenComputers, GuiType.Computer.id, world, x, y, z)
      true
    }
    else false
  }

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, blockId: Int) = {
    if (!world.isRemote) {
      world.getBlockTileEntity(x, y, z).asInstanceOf[TileEntityComputer].
        onNeighborBlockChange(blockId)
    }
  }

  // ----------------------------------------------------------------------- //
  // Block rotation
  // ----------------------------------------------------------------------- //

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) =
    Array(ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.NORTH, ForgeDirection.EAST)
}