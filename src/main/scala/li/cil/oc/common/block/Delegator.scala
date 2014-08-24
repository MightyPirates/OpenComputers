package li.cil.oc.common.block

import java.util
import java.util.Random

import cpw.mods.fml.common.Optional
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.client.KeyBindings
import li.cil.oc.common.tileentity.traits.{BundledRedstoneAware, Colored, Rotatable}
import li.cil.oc.util.mods.Mods
import li.cil.oc.util.{Color, ItemCosts, SideTracker}
import li.cil.oc.{CreativeTab, Settings}
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{Entity, EntityLivingBase, EnumCreatureType}
import net.minecraft.item.{ItemBlock, ItemStack}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{AxisAlignedBB, MovingObjectPosition, StatCollector, Vec3}
import net.minecraft.world.{IBlockAccess, World}
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.input
import powercrystals.minefactoryreloaded.api.rednet.{IConnectableRedNet, IRedNetNetworkContainer, RedNetConnectionType}

import scala.collection.mutable

class Delegator[Child <: Delegate](id: Int) extends Block(id, Material.iron) {
  setHardness(2f)
  setCreativeTab(CreativeTab)

  // For Immibis Microblock support.
  val ImmibisMicroblocks_TransformableBlockMarker = null

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

  def subBlock(stack: ItemStack): Option[Child] =
    if (stack != null) stack.getItem match {
      case block: ItemBlock if Block.blocksList(block.getBlockID) == this =>
        subBlock(block.getMetadata(stack.getItemDamage))
      case _ => None
    }
    else None

  def subBlock(world: IBlockAccess, x: Int, y: Int, z: Int): Option[Child] =
    if (world.getBlockId(x, y, z) == blockID) subBlock(world.getBlockMetadata(x, y, z))
    else None

  def subBlock(metadata: Int): Option[Child] =
    metadata match {
      case blockId if blockId >= 0 && blockId < subBlocks.length => Some(subBlocks(blockId))
      case _ => None
    }

  override def getSubBlocks(itemId: Int, creativeTab: CreativeTabs, list: util.List[_]) = {
    // Workaround for MC's untyped lists...
    def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])
    subBlockItemStacks().sortBy(_.getUnlocalizedName).foreach(add(list, _))
  }

  def subBlockItemStacks() = (0 until subBlocks.length).
    filter(id => subBlocks(id).showInItemList && subBlocks(id).parent == this).
    map(id => new ItemStack(this, 1, id))

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

  override def canHarvestBlock(player: EntityPlayer, meta: Int) = true

  override def canBeReplacedByLeaves(world: World, x: Int, y: Int, z: Int) = false

  override def canCreatureSpawn(creature: EnumCreatureType, world: World, x: Int, y: Int, z: Int) = false

  def getUnlocalizedName(metadata: Int) =
    subBlock(metadata) match {
      case Some(subBlock) => subBlock.unlocalizedName
      case _ => Settings.namespace + "tile"
    }

  override def damageDropped(metadata: Int) =
    subBlock(metadata) match {
      case Some(subBlock) => subBlock.itemDamage
      case _ => metadata
    }

  override def getPickBlock(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.pick(target, world, x, y, z)
      case _ => super.getPickBlock(target, world, x, y, z)
    }

  override def getBlockDropped(world: World, x: Int, y: Int, z: Int, metadata: Int, fortune: Int) =
    subBlock(metadata) match {
      case Some(subBlock) => subBlock.drops(world, x, y, z, fortune).getOrElse(super.getBlockDropped(world, x, y, z, metadata, fortune))
      case _ => super.getBlockDropped(world, x, y, z, metadata, fortune)
    }

  def dropBlockAsItem(world: World, x: Int, y: Int, z: Int, stack: ItemStack) {
    dropBlockAsItem_do(world, x, y, z, stack)
  }

  override def getExplosionResistance(entity: Entity, world: World, x: Int, y: Int, z: Int, explosionX: Double, explosionY: Double, explosionZ: Double) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.explosionResistance(entity)
      case _ => super.getExplosionResistance(entity, world, x, y, z, explosionX, explosionY, explosionZ)
    }

  override def isAirBlock(world: World, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.isAir(world, x, y, z)
      case _ => super.isAirBlock(world, x, y, z)
    }

  override def isBlockNormalCube(world: World, x: Int, y: Int, z: Int) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case Some(subBlock) => subBlock.isNormalCube(world, x, y, z)
      case _ => false
    }

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.validRotations(world, x, y, z)
      case _ => super.getValidRotations(world, x, y, z)
    }

  override def canPlaceBlockOnSide(world: World, x: Int, y: Int, z: Int, side: Int, stack: ItemStack) =
    subBlock(stack) match {
      case Some(subBlock) => subBlock.canPlaceBlockOnSide(world, x, y, z, ForgeDirection.getOrientation(side).getOpposite)
      case _ => super.canPlaceBlockOnSide(world, x, y, z, side, stack)
    }

  override def getCollisionBoundingBoxFromPool(world: World, x: Int, y: Int, z: Int) = {
    setBlockBoundsBasedOnState(world, x, y, z)
    super.getCollisionBoundingBoxFromPool(world, x, y, z)
  }

  override def setBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      // This function can mess things up badly in single player if not
      // synchronized because it sets fields in an instance stored in the
      // static block list... which is used by both server and client thread.
      // The other place where this is locked is in collisionRayTrace below,
      // which seems to be the only built-in function that *logically* depends
      // on the state bounds (rest is rendering which is unimportant).
      case Some(subBlock) => subBlock.synchronized(subBlock.updateBounds(world, x, y, z))
      case _ =>
    }

  def setBlockBounds(bounds: AxisAlignedBB) {
    setBlockBounds(
      bounds.minX.toFloat, bounds.minY.toFloat, bounds.minZ.toFloat,
      bounds.maxX.toFloat, bounds.maxY.toFloat, bounds.maxZ.toFloat)
  }

  override def collisionRayTrace(world: World, x: Int, y: Int, z: Int, origin: Vec3, direction: Vec3) =
    subBlock(world, x, y, z) match {
      // See setBlockBoundsBasedOnState for info on the lock.
      case Some(subBlock) => subBlock.synchronized(subBlock.intersect(world, x, y, z, origin, direction))
      case _ => super.collisionRayTrace(world, x, y, z, origin, direction)
    }

  // Allow delegates to fall back to the default implementation.
  def superCollisionRayTrace(world: World, x: Int, y: Int, z: Int, origin: Vec3, direction: Vec3) =
    super.collisionRayTrace(world, x, y, z, origin, direction)

  override def rotateBlock(world: World, x: Int, y: Int, z: Int, axis: ForgeDirection) =
    world.getBlockTileEntity(x, y, z) match {
      case rotatable: Rotatable if rotatable.rotate(axis) =>
        world.markBlockForRenderUpdate(x, y, z)
        true
      case _ => false
    }

  // ----------------------------------------------------------------------- //

  override def canConnectRedstone(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.canConnectToRedstone(
        world, x, y, z, side match {
          case -1 => ForgeDirection.UP
          case 0 => ForgeDirection.NORTH
          case 1 => ForgeDirection.EAST
          case 2 => ForgeDirection.SOUTH
          case 3 => ForgeDirection.WEST
        })
      case _ => false
    }

  override def isProvidingStrongPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.isProvidingStrongPower(world, x, y, z, ForgeDirection.getOrientation(side).getOpposite)
      case _ => 0
    }

  override def isProvidingWeakPower(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.isProvidingWeakPower(world, x, y, z, ForgeDirection.getOrientation(side).getOpposite)
      case _ => 0
    }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = subBlock(metadata) match {
    case Some(subBlock) => subBlock.hasTileEntity
    case _ => false
  }

  override def createTileEntity(world: World, metadata: Int): TileEntity = {
    if (!world.isRemote) {
      SideTracker.addServerThread()
    }
    subBlock(metadata) match {
      case Some(subBlock) => subBlock.createTileEntity(world).orNull
      case _ => null
    }
  }

  // ----------------------------------------------------------------------- //

  override def updateTick(world: World, x: Int, y: Int, z: Int, rng: Random) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.update(world, x, y, z)
      case _ =>
    }

  override def onBlockAdded(world: World, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.addedToWorld(world, x, y, z)
      case _ => // Invalid but avoid match error.
    }

  override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, player: EntityLivingBase, stack: ItemStack) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.addedByEntity(world, x, y, z, player, stack)
      case _ => // Invalid but avoid match error.
    }

  override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int, metadata: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.aboutToBeRemoved(world, x, y, z)
      case _ => // Invalid but avoid match error.
    }

  override def breakBlock(world: World, x: Int, y: Int, z: Int, blockId: Int, metadata: Int) = {
    subBlock(metadata) match {
      case Some(subBlock) => subBlock.removedFromWorld(world, x, y, z, blockId)
      case _ => // Invalid but avoid match error.
    }
    super.breakBlock(world, x, y, z, blockId, metadata)
  }

  override def removeBlockByPlayer(world: World, player: EntityPlayer, x: Int, y: Int, z: Int) =
    (subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.removedByEntity(world, x, y, z, player)
      case _ => true
    }) && super.removeBlockByPlayer(world, player, x, y, z)

  override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, blockId: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.neighborBlockChanged(world, x, y, z, blockId)
      case _ => // Invalid but avoid match error.
    }

  override def onNeighborTileChange(world: World, x: Int, y: Int, z: Int, tileX: Int, tileY: Int, tileZ: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.neighborTileChanged(world, x, y, z, tileX, tileY, tileZ)
      case _ => // Invalid but avoid match error.
    }

  override def onBlockClicked(world: World, x: Int, y: Int, z: Int, player: EntityPlayer) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.leftClick(world, x, y, z, player)
      case _ => // Invalid but avoid match error.
    }

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float): Boolean =
    world.getBlockTileEntity(x, y, z) match {
      case colored: Colored if Color.isDye(player.getHeldItem) =>
        colored.color = Color.dyeColor(player.getHeldItem)
        world.markBlockForUpdate(x, y, z)
        true
      case _ => subBlock(world, x, y, z) match {
        case Some(subBlock) => subBlock.rightClick(
          world, x, y, z, player, ForgeDirection.getOrientation(side), hitX, hitY, hitZ)
        case _ => false
      }
    }

  override def onEntityWalking(world: World, x: Int, y: Int, z: Int, entity: Entity) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.walk(world, x, y, z, entity)
      case _ => super.onEntityWalking(world, x, y, z, entity)
    }

  override def onEntityCollidedWithBlock(world: World, x: Int, y: Int, z: Int, entity: Entity) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.collide(world, x, y, z, entity)
      case _ => super.onEntityCollidedWithBlock(world, x, y, z, entity)
    }

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  def addInformation(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    subBlock(metadata) match {
      case Some(subBlock) => subBlock.tooltipLines(stack, player, tooltip, advanced)
      case _ =>
    }
    if (ItemCosts.hasCosts(stack)) {
      if (KeyBindings.showMaterialCosts) {
        ItemCosts.addTooltip(stack, tooltip.asInstanceOf[util.List[String]])
      }
      else {
        tooltip.add(StatCollector.translateToLocalFormatted(
          Settings.namespace + "tooltip.MaterialCosts",
          input.Keyboard.getKeyName(KeyBindings.materialCosts.keyCode)))
      }
    }
  }

  override def getRenderType = Settings.blockRenderId

  override def getLightOpacity(world: World, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.opacity(world, x, y, z)
      case _ => 255
    }

  @SideOnly(Side.CLIENT)
  override def getMixedBrightnessForBlock(world: IBlockAccess, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) =>
        val result = subBlock.mixedBrightness(world, x, y, z)
        if (result < 0) super.getMixedBrightnessForBlock(world, x, y, z) else result
      case _ => super.getMixedBrightnessForBlock(world, x, y, z)
    }

  override def getLightValue(world: IBlockAccess, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.luminance(world, x, y, z)
      case _ => super.getLightValue(world, x, y, z)
    }

  @SideOnly(Side.CLIENT)
  override def getRenderColor(metadata: Int) =
    subBlock(metadata) match {
      case Some(subBlock) => subBlock.color
      case _ => super.getRenderColor(metadata)
    }

  @SideOnly(Side.CLIENT)
  override def colorMultiplier(world: IBlockAccess, x: Int, y: Int, z: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) => subBlock.color(world, x, y, z)
      case _ => super.colorMultiplier(world, x, y, z)
    }

  @SideOnly(Side.CLIENT)
  override def getIcon(side: Int, metadata: Int) =
    subBlock(metadata) match {
      case Some(subBlock) => subBlock.icon(ForgeDirection.getOrientation(side)) match {
        case Some(icon) => icon
        case _ => super.getIcon(side, metadata)
      }
      case _ => super.getIcon(side, metadata)
    }

  @SideOnly(Side.CLIENT)
  override def getBlockTexture(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    subBlock(world, x, y, z) match {
      case Some(subBlock) =>
        val orientation = ForgeDirection.getOrientation(side)
        subBlock.icon(world, x, y, z, orientation, toLocal(world, x, y, z, orientation)) match {
          case Some(icon) => icon
          case _ => super.getBlockTexture(world, x, y, z, side)
        }
      case _ => super.getBlockTexture(world, x, y, z, side)
    }

  override def setBlockBoundsForItemRender() {
    setBlockBounds(0, 0, 0, 1, 1, 1)
  }

  def setBlockBoundsForItemRender(metadata: Int): Unit =
    subBlock(metadata) match {
      case Some(subBlock) => subBlock.itemBounds()
      case _ => setBlockBoundsForItemRender()
    }

  @SideOnly(Side.CLIENT)
  def preItemRender(metadata: Int) =
    subBlock(metadata) match {
      case Some(subBlock) => subBlock.preItemRender()
      case _ =>
    }

  @SideOnly(Side.CLIENT)
  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)
    subBlocks.foreach(_.registerIcons(iconRegister))
  }
}

object Delegator {
  def subBlock(block: Block, metadata: Int): Option[Delegate] =
    block match {
      case delegator: Delegator[_] => delegator.subBlock(metadata)
      case _ => None
    }

  def subBlock(world: IBlockAccess, x: Int, y: Int, z: Int): Option[Delegate] = {
    val blockId = world.getBlockId(x, y, z)
    if (blockId > 0) subBlock(Block.blocksList(blockId), world.getBlockMetadata(x, y, z))
    else None
  }

  def subBlock(stack: ItemStack): Option[Delegate] =
    if (stack != null) stack.getItem match {
      case block: ItemBlock =>
        subBlock(Block.blocksList(block.getBlockID), block.getMetadata(stack.getItemDamage))
      case _ => None
    }
    else None
}

class SimpleDelegator(id: Int) extends Delegator[SimpleDelegate](id)

class SpecialDelegator(id: Int) extends Delegator[SpecialDelegate](id) {
  override def isOpaqueCube = false

  override def renderAsNormalBlock = false

  override def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) =
    subBlock(world.getBlockMetadata(x, y, z)) match {
      case Some(subBlock) => subBlock.isSolid(world, x, y, z, ForgeDirection.getOrientation(side))
      case _ => true
    }

  @SideOnly(Side.CLIENT)
  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: Int) = {
    val direction = ForgeDirection.getOrientation(side)
    subBlock(world.getBlockMetadata(x - direction.offsetX, y - direction.offsetY, z - direction.offsetZ)) match {
      case Some(subBlock) => subBlock.shouldSideBeRendered(world, x, y, z, direction)
      case _ => super.shouldSideBeRendered(world, x, y, z, side)
    }
  }
}

@Optional.Interface(iface = "powercrystals.minefactoryreloaded.api.rednet.IConnectableRedNet", modid = Mods.IDs.MineFactoryReloaded)
trait RedstoneDelegator[Child <: Delegate] extends Delegator[Child] with IConnectableRedNet {
  override def getConnectionType(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) = RedNetConnectionType.CableAll

  override def getOutputValue(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, color: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case t: BundledRedstoneAware => t.bundledOutput(side, color)
      case _ => 0
    }

  override def getOutputValues(world: World, x: Int, y: Int, z: Int, side: ForgeDirection) =
    world.getBlockTileEntity(x, y, z) match {
      case t: BundledRedstoneAware => t.bundledOutput(side)
      case _ => Array.fill(16)(0)
    }

  override def onInputChanged(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, inputValue: Int) {}

  override def onInputsChanged(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, inputValues: Array[Int]) =
    world.getBlockTileEntity(x, y, z) match {
      case t: BundledRedstoneAware => for (color <- 0 until 16) {
        t.rednetInput(side, color, inputValues(color))
      }
      case _ =>
    }

  abstract override def onNeighborBlockChange(world: World, x: Int, y: Int, z: Int, blockId: Int) {
    if (Mods.MineFactoryReloaded.isAvailable) {
      world.getBlockTileEntity(x, y, z) match {
        case t: BundledRedstoneAware => for (side <- ForgeDirection.VALID_DIRECTIONS) {
          Block.blocksList(world.getBlockId(x + side.offsetX, y + side.offsetY, z + side.offsetZ)) match {
            case block: IRedNetNetworkContainer =>
            case _ => for (color <- 0 until 16) {
              t.rednetInput(side, color, 0)
            }
          }
        }
        case _ =>
      }
    }
    super.onNeighborBlockChange(world, x, y, z, blockId)
  }
}

class SimpleRedstoneDelegator(id: Int) extends SimpleDelegator(id) with RedstoneDelegator[SimpleDelegate] {
  override def canProvidePower = true
}

class SpecialRedstoneDelegator(id: Int) extends SpecialDelegator(id) with RedstoneDelegator[SpecialDelegate] {
  override def canProvidePower = true
}
