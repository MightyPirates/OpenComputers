package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.Config
import li.cil.oc.CreativeTab
import li.cil.oc.common.tileentity.TileEntityComputer
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.MathHelper
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection
import li.cil.oc.common.CommonProxy
import li.cil.oc.OpenComputers

class BlockComputer extends Block(Config.blockComputerId, Material.iron) {
  // ----------------------------------------------------------------------- //
  // Construction
  // ----------------------------------------------------------------------- //

  setHardness(2f)
  GameRegistry.registerBlock(this, "oc.computer")
  GameRegistry.registerTileEntity(classOf[TileEntityComputer], "oc.computer")
  setUnlocalizedName("oc.computer")
  setCreativeTab(CreativeTab)

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
    if (player.isSneaking())
      if (canWrench(player, x, y, z))
        setRotation(world, x, y, z, rotation(world, x, y, z) + 1)
      else
        false
    else
    {   // TODO Open GUI if we're a client.
      val computer = world.getBlockTileEntity(x, y, z).asInstanceOf[TileEntityComputer]
      computer.turnOn()
      player.openGui(OpenComputers,0, world, x,y, z)
      true
    }
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

  override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, entity: EntityLivingBase, itemStack: ItemStack) {
    if (!world.isRemote) {
      val facing = MathHelper.floor_double(entity.rotationYaw * 4 / 360 + 0.5) & 3
      setRotation(world, x, y, z, facing)
    }
  }

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) =
    Array(ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.NORTH, ForgeDirection.EAST)

  def rotation(world: IBlockAccess, x: Int, y: Int, z: Int) =
    // Renderer(down, up, north, south, west, east) -> Facing(south, west, north, east) inverted.
    Array(0, 0, 0, 2, 3, 1)(world.getBlockMetadata(x, y, z))

  private def setRotation(world: World, x: Int, y: Int, z: Int, value: Int) =
    // Facing(south, west, north, east) -> Renderer(down, up, north, south, west, east) inverted.
    world.setBlockMetadataWithNotify(x, y, z, Array(2, 5, 3, 4)((value + 4) % 4), 3)

  private def canWrench(player: EntityPlayer, x: Int, y: Int, z: Int) = {
    if (player.getCurrentEquippedItem() != null)
      try {
        player.getCurrentEquippedItem().getItem().asInstanceOf[{
          def canWrench(player: EntityPlayer, x: Int, y: Int, z: Int): Boolean
        }].canWrench(player, x, y, z)
      }
      catch {
        case e: Throwable => false
      }
    else
      false
  }
}