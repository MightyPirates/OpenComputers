package li.cil.oc.common.block
import scala.collection.mutable.MutableList
import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.Config
import li.cil.oc.CreativeTab
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.MathHelper
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

/**
 * Block proxy for all real block implementations.
 *
 * All of our blocks are implemented as "sub-blocks" of this block, i.e. they
 * are instances of this block with differing metadata. This way we only need a
 * single block ID to represent all our blocks.
 *
 * This means this block contains barely any logic, it only forwards calls to
 * the underlying sub block, based on the metadata. The only actual logic done
 * in here is for the block rotation (and, well, the forwarding).
 *
 * We also provide rotation logic for all these sub blocks. Thus the metadata
 * looks like this: [rotation: 3][sub block type: 5], where the number after
 * the colon is the number of bits used. So there can be 31 sub types, with an
 * arbitrary rotation.
 */
class BlockMulti extends Block(Config.blockId, Material.iron) {
  setHardness(2f)
  setCreativeTab(CreativeTab)
  GameRegistry.registerBlock(this, classOf[ItemBlockMulti], "oc.block")

  // ----------------------------------------------------------------------- //
  // SubBlock
  // ----------------------------------------------------------------------- //

  val subBlocks = MutableList.empty[SubBlock]

  def subBlock(metadata: Int) =
    subBlockIdFromMetadata(metadata) match {
      case id if id >= 0 && id < subBlocks.length => Some(subBlocks(id))
      case _ => None
    }

  def add(subBlock: SubBlock) = {
    val blockId = subBlocks.length
    subBlocks += subBlock
    blockId
  }

  def subBlockId(world: IBlockAccess, x: Int, y: Int, z: Int) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case Some(subBlock) if world.getBlockId(x, y, z) == this.blockID => subBlock.blockId
      case _ => -1
    }

  // Kept private to avoid unnecessary dependencies in case we ever want to
  // change where in the metadata this is stored.

  private def subBlockIdFromMetadata(metadata: Int) = metadata & 0x1F

  private def subBlockIdToMetadata(blockId: Int) = blockId & 0x1F

  private def rotationFromMetadata(metadata: Int) = (metadata >> 5) & 0x07

  private def rotationToMetadata(rotation: Int) = (rotation & 0x07) << 5

  // ----------------------------------------------------------------------- //
  // Rotation
  // ----------------------------------------------------------------------- //

  // Renderer(down, up, north, south, west, east)
  def rotation(world: IBlockAccess, x: Int, y: Int, z: Int) =
    ForgeDirection.getOrientation(
      rotationFromMetadata(world.getBlockMetadata(x, y, z)))

  def rotation(world: World, x: Int, y: Int, z: Int, direction: ForgeDirection) =
    world.setBlockMetadataWithNotify(x, y, z, rotationToMetadata(direction.ordinal), 3)

  // ----------------------------------------------------------------------- //
  // Block
  // ----------------------------------------------------------------------- //

  override def breakBlock(world: World, x: Int, y: Int, z: Int, blockId: Int, metadata: Int) = {
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case None => // Invalid but avoid match error.
      case Some(subBlock) => subBlock.breakBlock(world, x, y, z, blockId, metadata)
    }
    super.breakBlock(world, x, y, z, blockId, metadata)
  }

  override def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case None => false
      case Some(subBlock) => subBlock.canConnectRedstone(world, x, y, z, side)
    }

  override def createTileEntity(world: World, metadata: Int): TileEntity =
    subBlock(metadata) match {
      case None => null
      case Some(subBlock) => subBlock.createTileEntity(world, metadata)
    }

  override def getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case None => super.getCollisionBoundingBoxFromPool(world, x, y, z)
      case Some(subBlock) => subBlock.getCollisionBoundingBoxFromPool(world, x, y, z)
    }

  override def getIcon(side: Int, metadata: Int) =
    subBlock(metadata) match {
      case None => super.getIcon(side, metadata)
      case Some(subBlock) => subBlock.getBlockTextureFromSide(side) match {
        case null => super.getIcon(side, metadata)
        case icon => icon
      }
    }

  override def getLightOpacity(world: World, x: Int, y: Int, z: Int) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case None => 255
      case Some(subBlock) => subBlock.getLightOpacity(world, x, y, z)
    }

  override def getSubBlocks(itemId: Int, creativeTab: CreativeTabs, list: java.util.List[_]) = {
    // Workaround for MC's untyped lists... I'm too tired to rage anymore.
    def add[T](list: java.util.List[T], value: Any) = list.add(value.asInstanceOf[T])
    (0 until subBlocks.length).
      foreach(id => add(list, new ItemStack(this, 1, subBlockIdToMetadata(id))))
  }

  def getUnlocalizedName(metadata: Int) =
    subBlock(metadata) match {
      case None => "oc.block._"
      case Some(subBlock) => subBlock.unlocalizedName
    }

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case None => super.getValidRotations(world, x, y, z)
      case Some(subBlock) => subBlock.getValidRotations(world, x, y, z)
    }

  override def hasTileEntity(metadata: Int) = subBlock(metadata) match {
    case None => false
    case Some(subBlock) => subBlock.hasTileEntity(metadata)
  }

  override def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case None => 0
      case Some(subBlock) => subBlock.isProvidingStrongPower(world, x, y, z, side)
    }

  override def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case None => 0
      case Some(subBlock) => subBlock.isProvidingWeakPower(world, x, y, z, side)
    }

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    def canWrench = {
      if (player.getCurrentEquippedItem != null)
        try {
          player.getCurrentEquippedItem.getItem.asInstanceOf[{
            def canWrench(player: EntityPlayer, x: Int, y: Int, z: Int): Boolean
          }].canWrench(player, x, y, z)
        }
        catch {
          case _: Throwable => false
        }
      else
        false
    }

    if (!player.isSneaking() && canWrench) {
      // TODO set the next valid rotation (based on getValidRotations).
      //setRotation(world, x, y, z, rotation(world, x, y, z) + 1)
      true
    }
    else
      subBlock(world.getBlockMetadata(x, y, z)) match {
        case None => false
        case Some(subBlock) => subBlock.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)
      }
  }

  override def onBlockAdded(world: World, x: Int, y: Int, z: Int) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case None => // Invalid but avoid match error.
      case Some(subBlock) => subBlock.onBlockAdded(world, x, y, z)
    }

  override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, player: EntityLivingBase, item: ItemStack) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case None => // Invalid but avoid match error.
      case Some(subBlock) => subBlock.onBlockPlacedBy(world, x, y, z, player, item)
    }

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, blockId: Int) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case None => // Invalid but avoid match error.
      case Some(subBlock) => subBlock.onNeighborBlockChange(world, x, y, z, blockId)
    }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)
    subBlocks.foreach(_.registerIcons(iconRegister))
  }

  override def removeBlockByPlayer(world: World, player: EntityPlayer, x: Int, y: Int, z: Int) =
    (subBlock(world.getBlockMetadata(x, y, z)) match {
      case None => true
      case Some(subBlock) => subBlock.onBlockRemovedBy(world, x, y, z, player)
    }) && super.removeBlockByPlayer(world, player, x, y, z)
}