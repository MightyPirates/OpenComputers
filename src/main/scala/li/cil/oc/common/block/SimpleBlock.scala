package li.cil.oc.common.block

import li.cil.oc.CreativeTab
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.traits.Colored
import li.cil.oc.common.tileentity.traits.Inventory
import li.cil.oc.common.tileentity.traits.Rotatable
import li.cil.oc.util.Color
import li.cil.oc.util.Tooltip
import net.minecraft.block.BlockContainer
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLiving.SpawnPlacementType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.EnumDyeColor
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util._
import net.minecraft.util.math.BlockPos
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

abstract class SimpleBlock(material: Material = Material.IRON) extends BlockContainer(material) {
  setHardness(2f)
  setResistance(5)
  setCreativeTab(CreativeTab)

  var showInItemList = true

  protected val validRotations_ = Array(EnumFacing.UP, EnumFacing.DOWN)

  def createItemStack(amount: Int = 1) = new ItemStack(this, amount)

  override def createNewTileEntity(world: World, meta: Int): TileEntity = null

  @SideOnly(Side.CLIENT)
  override def shouldSideBeRendered(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing): Boolean = {
    val bounds = getBoundingBox(state, world, pos)
    (side == EnumFacing.DOWN && bounds.minY > 0) ||
      (side == EnumFacing.UP && bounds.maxY < 1) ||
      (side == EnumFacing.NORTH && bounds.minZ > 0) ||
      (side == EnumFacing.SOUTH && bounds.maxZ < 1) ||
      (side == EnumFacing.WEST && bounds.minX > 0) ||
      (side == EnumFacing.EAST && bounds.maxX < 1) ||
      isOpaqueCube(state)
  }

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  override def getRenderType(state: IBlockState): EnumBlockRenderType = EnumBlockRenderType.MODEL

//  @SideOnly(Side.CLIENT)
//  override def colorMultiplier(world: IBlockAccess, pos: BlockPos, renderPass: Int) =
//    world.getTileEntity(pos) match {
//      case colored: Colored => colored.getColor
//      case _ => super.colorMultiplier(world, pos, renderPass)
//    }

  @SideOnly(Side.CLIENT)
  def preItemRender(metadata: Int) {}

  // ----------------------------------------------------------------------- //
  // ItemBlock
  // ----------------------------------------------------------------------- //

  def rarity(stack: ItemStack) = EnumRarity.COMMON

  @SideOnly(Side.CLIENT)
  def addInformation(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    tooltipHead(metadata, stack, player, tooltip, advanced)
    tooltipBody(metadata, stack, player, tooltip, advanced)
    tooltipTail(metadata, stack, player, tooltip, advanced)
  }

  protected def tooltipHead(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
  }

  protected def tooltipBody(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
    tooltip.addAll(Tooltip.get(getClass.getSimpleName))
  }

  protected def tooltipTail(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: java.util.List[String], advanced: Boolean) {
  }

  // ----------------------------------------------------------------------- //
  // Rotation
  // ----------------------------------------------------------------------- //

  def getFacing(world: IBlockAccess, pos: BlockPos) =
    world.getTileEntity(pos) match {
      case tileEntity: Rotatable => tileEntity.facing
      case _ => EnumFacing.SOUTH
    }

  def setFacing(world: World, pos: BlockPos, value: EnumFacing) =
    world.getTileEntity(pos) match {
      case rotatable: Rotatable => rotatable.setFromFacing(value); true
      case _ => false
    }

  def setRotationFromEntityPitchAndYaw(world: World, pos: BlockPos, value: Entity) =
    world.getTileEntity(pos) match {
      case rotatable: Rotatable => rotatable.setFromEntityPitchAndYaw(value); true
      case _ => false
    }

  def toLocal(world: IBlockAccess, pos: BlockPos, value: EnumFacing) =
    world.getTileEntity(pos) match {
      case rotatable: Rotatable => rotatable.toLocal(value)
      case _ => value
    }

  // ----------------------------------------------------------------------- //
  // Block
  // ----------------------------------------------------------------------- //

  override def isSideSolid(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing): Boolean = true

  override def canHarvestBlock(world: IBlockAccess, pos: BlockPos, player: EntityPlayer) = true

  override def getHarvestTool(state: IBlockState): String = null

  override def canBeReplacedByLeaves(state: IBlockState, world: IBlockAccess, pos: BlockPos): Boolean = false

  override def canCreatureSpawn(state: IBlockState, world: IBlockAccess, pos: BlockPos, `type`: SpawnPlacementType): Boolean = false

  override def getValidRotations(world: World, pos: BlockPos) = validRotations_

  override def breakBlock(world: World, pos: BlockPos, state: IBlockState): Unit = {
    if (!world.isRemote) world.getTileEntity(pos) match {
      case inventory: Inventory => inventory.dropAllSlots()
      case _ => // Ignore.
    }
    super.breakBlock(world, pos, state)
  }

  // ----------------------------------------------------------------------- //

  override def rotateBlock(world: World, pos: BlockPos, axis: EnumFacing) =
    world.getTileEntity(pos) match {
      case rotatable: tileentity.traits.Rotatable if rotatable.rotate(axis) =>
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3)
        true
      case _ => false
    }

  override def recolorBlock(world: World, pos: BlockPos, side: EnumFacing, color: EnumDyeColor) =
    world.getTileEntity(pos) match {
      case colored: Colored if colored.getColor != Color.rgbValues(color) =>
        colored.setColor(Color.rgbValues(color))
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3)
        true // Blame Vexatos.
      case _ => super.recolorBlock(world, pos, side, color)
    }

  // ----------------------------------------------------------------------- //

  // NOTE: must not be final for immibis microblocks to work.
  override def onBlockActivated(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, hand: EnumHand, heldItem: ItemStack, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean =
    world.getTileEntity(pos) match {
      case colored: Colored if Color.isDye(heldItem) =>
        colored.setColor(Color.rgbValues(Color.dyeColor(heldItem)))
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3)
        if (!player.capabilities.isCreativeMode && colored.consumesDye) {
          heldItem.splitStack(1)
        }
        true
      case _ => localOnBlockActivated(world, pos, player, hand, heldItem, side, hitX, hitY, hitZ)
    }

  def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, hand: EnumHand, heldItem: ItemStack, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = false
}
