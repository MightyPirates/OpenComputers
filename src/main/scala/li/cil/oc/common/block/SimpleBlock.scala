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
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

abstract class SimpleBlock(material: Material = Material.iron) extends BlockContainer(material) {
  setHardness(2f)
  setResistance(5)
  setCreativeTab(CreativeTab)

  var showInItemList = true

  protected val validRotations_ = Array(EnumFacing.UP, EnumFacing.DOWN)

  protected val bounds = new ThreadLocal[AxisAlignedBB]() {
    override def initialValue() = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ)
  }

  def createItemStack(amount: Int = 1) = new ItemStack(this, amount)

  override def createNewTileEntity(world: World, meta: Int): TileEntity = null

  // ----------------------------------------------------------------------- //
  // Synchronized block size, because threading...
  //
  // These functions can mess things up badly in single player if not
  // synchronized because the bounds fields are in an instance stored in the
  // static block list... which is used by both server and client thread.
  //
  // Also, final getBlockBoundsMin/MaxX/Y/Z(), really?
  // ----------------------------------------------------------------------- //

  protected def setBlockBounds(bounds: AxisAlignedBB): Unit = {
    this.bounds.set(bounds)
    setBlockBounds(
      bounds.minX.toFloat,
      bounds.minY.toFloat,
      bounds.minZ.toFloat,
      bounds.maxX.toFloat,
      bounds.maxY.toFloat,
      bounds.maxZ.toFloat)
  }

  override def getCollisionBoundingBox(world: World, pos: BlockPos, state: IBlockState) = {
    setBlockBoundsBasedOnState(world, pos)
    new AxisAlignedBB(
      pos.getX + bounds.get.minX, pos.getY + bounds.get.minY, pos.getZ + bounds.get.minZ,
      pos.getX + bounds.get.maxX, pos.getY + bounds.get.maxY, pos.getZ + bounds.get.maxZ)
  }

  @SideOnly(Side.CLIENT)
  override def getSelectedBoundingBox(world: World, pos: BlockPos): AxisAlignedBB = {
    new AxisAlignedBB(
      pos.getX + bounds.get.minX, pos.getY + bounds.get.minY, pos.getZ + bounds.get.minZ,
      pos.getX + bounds.get.maxX, pos.getY + bounds.get.maxY, pos.getZ + bounds.get.maxZ)
  }

  @SideOnly(Side.CLIENT)
  override def shouldSideBeRendered(world: IBlockAccess, pos: BlockPos, side: EnumFacing) =
    (side == EnumFacing.DOWN && bounds.get.minY > 0) ||
      (side == EnumFacing.UP && bounds.get.maxY < 1) ||
      (side == EnumFacing.NORTH && bounds.get.minZ > 0) ||
      (side == EnumFacing.SOUTH && bounds.get.maxZ < 1) ||
      (side == EnumFacing.WEST && bounds.get.minX > 0) ||
      (side == EnumFacing.EAST && bounds.get.maxX < 1) ||
      !world.getBlockState(pos).getBlock.isOpaqueCube

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  override def getRenderType = 3

  @SideOnly(Side.CLIENT)
  override def colorMultiplier(world: IBlockAccess, pos: BlockPos, renderPass: Int) =
    world.getTileEntity(pos) match {
      case colored: Colored => colored.getColor
      case _ => getRenderColor(world.getBlockState(pos))
    }

  @SideOnly(Side.CLIENT)
  def preItemRender(metadata: Int) {}

  final override def setBlockBoundsForItemRender() = setBlockBoundsForItemRender(0)

  def setBlockBoundsForItemRender(metadata: Int) = super.setBlockBoundsForItemRender()

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

  override def isSideSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = true

  override def canHarvestBlock(world: IBlockAccess, pos: BlockPos, player: EntityPlayer) = true

  override def getHarvestTool(state: IBlockState): String = null

  override def canBeReplacedByLeaves(world: IBlockAccess, pos: BlockPos) = false

  override def canCreatureSpawn(world: IBlockAccess, pos: BlockPos, `type`: SpawnPlacementType) = false

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
        world.markBlockForUpdate(pos)
        true
      case _ => false
    }

  override def recolorBlock(world: World, pos: BlockPos, side: EnumFacing, color: EnumDyeColor) =
    world.getTileEntity(pos) match {
      case colored: Colored if colored.getColor != Color.rgbValues(color) =>
        colored.setColor(Color.rgbValues(color))
        world.markBlockForUpdate(pos)
        true // Blame Vexatos.
      case _ => super.recolorBlock(world, pos, side, color)
    }

  // ----------------------------------------------------------------------- //

  // NOTE: must not be final for immibis microblocks to work.
  override def onBlockActivated(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) =
    world.getTileEntity(pos) match {
      case colored: Colored if Color.isDye(player.getHeldItem) =>
        colored.setColor(Color.rgbValues(Color.dyeColor(player.getHeldItem)))
        world.markBlockForUpdate(pos)
        if (!player.capabilities.isCreativeMode && colored.consumesDye) {
          player.getHeldItem.splitStack(1)
        }
        true
      case _ => localOnBlockActivated(world, pos, player, side, hitX, hitY, hitZ)
    }

  def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = false
}
