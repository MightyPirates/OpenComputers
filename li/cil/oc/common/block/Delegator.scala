package li.cil.oc.common.block

import cpw.mods.fml.common.registry.GameRegistry
import java.util
import li.cil.oc.Config
import li.cil.oc.CreativeTab
import li.cil.oc.api.Network
import li.cil.oc.api.network.Node
import li.cil.oc.api.network.environment.Environment
import li.cil.oc.common.tileentity.Rotatable
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection
import scala.collection.mutable

/**
 * Block proxy for all real block implementations.
 *
 * All of our blocks are implemented as "sub-blocks" of this block, i.e. they
 * are instances of this block with differing metadata. This way we only need a
 * single block ID to represent 15 blocks.
 *
 * This means this block contains barely any logic, it only forwards calls to
 * the underlying sub block, based on the metadata. The only actual logic done
 * in here is:
 * - block rotation, if the block has a rotatable tile entity. In that case all
 * sides are also translated into local coordinate space for the sub block
 * (i.e. "up" will always be up relative to the block itself. So if it's
 * rotated up may actually be west).
 * - Component network logic for adding / removing blocks from the component
 * network when they are placed / removed.
 */
class Delegator[Child <: Delegate](id: Int) extends Block(id, Material.iron) {
  setHardness(2f)
  setCreativeTab(CreativeTab)
  GameRegistry.registerBlock(this, classOf[Item], "oc.block." + id)

  // ----------------------------------------------------------------------- //
  // SubBlock
  // ----------------------------------------------------------------------- //

  val subBlocks = mutable.ArrayBuffer.empty[Child]
  subBlocks.sizeHint(16)

  def add(subBlock: Child) = {
    val blockId = subBlocks.length
    subBlocks += subBlock
    blockId
  }

  def subBlock(world: IBlockAccess, x: Int, y: Int, z: Int): Option[Child] =
    subBlock(world.getBlockMetadata(x, y, z))

  def subBlock(metadata: Int) =
    metadata match {
      case blockId if blockId >= 0 && blockId < subBlocks.length => Some(subBlocks(blockId))
      case _ => None
    }

  // ----------------------------------------------------------------------- //
  // Rotation
  // ----------------------------------------------------------------------- //

  def getFacing(world: IBlockAccess, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case tileEntity: Rotatable => tileEntity.facing
      case _ => ForgeDirection.UNKNOWN
    }

  def setFacing(world: World, x: Int, y: Int, z: Int, value: ForgeDirection) =
    world.getBlockTileEntity(x, y, z) match {
      case rotatable: Rotatable =>
        rotatable.setFromFacing(value); true
      case _ => false
    }

  def setRotationFromEntityPitchAndYaw(world: World, x: Int, y: Int, z: Int, value: Entity) =
    world.getBlockTileEntity(x, y, z) match {
      case rotatable: Rotatable =>
        rotatable.setFromEntityPitchAndYaw(value); true
      case _ => false
    }

  private def toLocal(world: IBlockAccess, x: Int, y: Int, z: Int, value: ForgeDirection) =
    world.getBlockTileEntity(x, y, z) match {
      case rotatable: Rotatable => rotatable.toLocal(value)
      case _ => value
    }

  // ----------------------------------------------------------------------- //
  // Block
  // ----------------------------------------------------------------------- //

  override def breakBlock(world: World, x: Int, y: Int, z: Int, blockId: Int, metadata: Int) = {
    subBlock(metadata) match {
      case Some(subBlock) => {
        world.getBlockTileEntity(x, y, z) match {
          case environment: Environment =>
            environment.node.network.remove(environment.node)
          case _ => // Nothing special to do.
        }
        subBlock.breakBlock(world, x, y, z, blockId)
      }
      case _ => // Invalid but avoid match error.
    }
    super.breakBlock(world, x, y, z, blockId, metadata)
  }

  override def getRenderColor(metadata: Int) =
    subBlock(metadata) match {
      case Some(subBlock) => subBlock.getRenderColor
      case _ => super.getRenderColor(metadata)
    }

  override def colorMultiplier(world: IBlockAccess, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.colorMultiplier(world, x, y, z)
      case _ => super.colorMultiplier(world, x, y, z)
    }

  override def getDamageValue(world: World, x: Int, y: Int, z: Int) = world.getBlockMetadata(x, y, z)

  override def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.canConnectRedstone(
        world, x, y, z, toLocal(world, x, y, z, side match {
          case -1 => ForgeDirection.UP
          case 0 => ForgeDirection.NORTH
          case 1 => ForgeDirection.EAST
          case 2 => ForgeDirection.SOUTH
          case 3 => ForgeDirection.WEST
        }))
      case _ => false
    }

  override def canProvidePower = true

  override def createTileEntity(world: World, metadata: Int): TileEntity =
    subBlock(metadata) match {
      case Some(subBlock) => subBlock.createTileEntity(world).orNull
      case _ => null
    }

  override def getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      // TODO Rotate to world space if we have a Rotatable?
      case Some(subBlock) => subBlock.getCollisionBoundingBoxFromPool(world, x, y, z)
      case _ => super.getCollisionBoundingBoxFromPool(world, x, y, z)
    }

  override def getBlockTexture(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) =>
        val orientation = ForgeDirection.getOrientation(side)
        subBlock.getBlockTextureFromSide(world, x, y, z, orientation, toLocal(world, x, y, z, orientation)) match {
          case Some(icon) => icon
          case _ => super.getBlockTexture(world, x, y, z, side)
        }
      case _ => super.getBlockTexture(world, x, y, z, side)
    }

  override def getLightValue(world: IBlockAccess, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.getLightValue(world, x, y, z)
      case _ => 0
    }

  override def getIcon(side: Int, metadata: Int) =
    subBlock(metadata) match {
      case Some(subBlock) => subBlock.icon(ForgeDirection.getOrientation(side)) match {
        case Some(icon) => icon
        case _ => super.getIcon(side, metadata)
      }
      case _ => super.getIcon(side, metadata)
    }

  override def getLightOpacity(world: World, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.getLightOpacity(world, x, y, z)
      case _ => 255
    }

  override def getSubBlocks(itemId: Int, creativeTab: CreativeTabs, list: util.List[_]) = {
    // Workaround for MC's untyped lists...
    def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])
    (0 until subBlocks.length).
      foreach(id => add(list, new ItemStack(this, 1, id)))
  }

  override def getRenderType = Config.blockRenderId

  def getUnlocalizedName(metadata: Int) =
    subBlock(metadata) match {
      case Some(subBlock) => subBlock.unlocalizedName
      case _ => "oc.block"
    }

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.getValidRotations(world, x, y, z)
      case _ => super.getValidRotations(world, x, y, z)
    }

  override def hasTileEntity(metadata: Int) = subBlock(metadata) match {
    case Some(subBlock) => subBlock.hasTileEntity
    case _ => false
  }

  override def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.isProvidingStrongPower(
        world, x, y, z, toLocal(world, x, y, z, ForgeDirection.getOrientation(side).getOpposite))
      case _ => 0
    }

  override def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.isProvidingWeakPower(
        world, x, y, z, toLocal(world, x, y, z, ForgeDirection.getOrientation(side).getOpposite))
      case _ => 0
    }

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    // Helper method to detect items that can be used to rotate blocks, such as
    // wrenches. This structural type is compatible with the BuildCraft wrench
    // interface.
    def canWrench = {
      if (player.getCurrentEquippedItem != null)
        try {
          player.getCurrentEquippedItem.getItem.asInstanceOf[ {
            def canWrench(player: EntityPlayer, x: Int, y: Int, z: Int): Boolean
          }].canWrench(player, x, y, z)
        }
        catch {
          case _: Throwable => false
        }
      else
        false
    }

    // Get valid rotations to skip rotating if there's only one.
    val valid = getValidRotations(world, x, y, z)
    if (valid.length > 1 && canWrench)
      world.getBlockTileEntity(x, y, z) match {
        case rotatable: Rotatable => {
          if (player.isSneaking) {
            // Rotate pitch. Get the valid pitch rotations.
            val validPitch = valid.collect {
              case direction if direction == ForgeDirection.DOWN || direction == ForgeDirection.UP => direction
              case direction if direction != ForgeDirection.UNKNOWN => ForgeDirection.NORTH
            }
            // Check if there's more than one, and if so set to the next one.
            if (validPitch.length > 1) {
              if (!world.isRemote)
                rotatable.pitch = validPitch((validPitch.indexOf(rotatable.pitch) + 1) % validPitch.length)
              return true
            }
          }
          else {
            // Rotate yaw. Get the valid yaw rotations.
            val validYaw = valid.collect {
              case direction if direction != ForgeDirection.DOWN && direction != ForgeDirection.UP && direction != ForgeDirection.UNKNOWN => direction
            }
            // Check if there's more than one, and if so set to the next one.
            if (validYaw.length > 1) {
              if (!world.isRemote)
                rotatable.yaw = validYaw((validYaw.indexOf(rotatable.yaw) + 1) % validYaw.length)
              return true
            }
          }
        }
        case _ => // Cannot rotate this block.
      }

    // No rotating tool in hand or can't rotate: activate the block.
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.onBlockActivated(
        world, x, y, z, player, ForgeDirection.getOrientation(side), hitX, hitY, hitZ)
      case _ => false
    }
  }

  override def onBlockAdded(world: World, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => {
        world.getBlockTileEntity(x, y, z) match {
          case _: Node => Network.joinOrCreateNetwork(world, x, y, z)
          case _ => // Nothing special to do.
        }
        subBlock.onBlockAdded(world, x, y, z)
      }
      case _ => // Invalid but avoid match error.
    }

  override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, player: EntityLivingBase, item: ItemStack) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.onBlockPlacedBy(world, x, y, z, player, item)
      case _ => // Invalid but avoid match error.
    }

  override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int, metadata: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.onBlockPreDestroy(world, x, y, z)
      case _ => // Invalid but avoid match error.
    }

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, blockId: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.onNeighborBlockChange(world, x, y, z, blockId)
      case _ => // Invalid but avoid match error.
    }

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)
    subBlocks.foreach(_.registerIcons(iconRegister))
  }

  override def removeBlockByPlayer(world: World, player: EntityPlayer, x: Int, y: Int, z: Int) =
    (subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.onBlockRemovedBy(world, x, y, z, player)
      case _ => true
    }) && super.removeBlockByPlayer(world, player, x, y, z)

  override def rotateBlock(world: World, x: Int, y: Int, z: Int, axis: ForgeDirection) = {
    val newFacing = getFacing(world, x, y, z).getRotation(axis)
    if (getValidRotations(world, x, y, z).contains(newFacing))
      setFacing(world, x, y, z, newFacing)
    else false
  }
}

class SimpleDelegator(id: Int) extends Delegator[SimpleDelegate](id)

class SpecialDelegator(id: Int) extends Delegator[SpecialDelegate](id) {
  override def isBlockNormalCube(world: World, x: Int, y: Int, z: Int) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case Some(subBlock) => subBlock.isBlockNormalCube(world, x, y, z)
      case _ => false
    }

  override def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case Some(subBlock) => subBlock.isBlockSolid(world, x, y, z, ForgeDirection.getOrientation(side))
      case _ => true
    }

  override def isOpaqueCube = false

  override def renderAsNormalBlock = false

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case Some(subBlock) => subBlock.shouldSideBeRendered(world, x, y, z, ForgeDirection.getOrientation(side))
      case _ => super.shouldSideBeRendered(world, x, y, z, side)
    }
}