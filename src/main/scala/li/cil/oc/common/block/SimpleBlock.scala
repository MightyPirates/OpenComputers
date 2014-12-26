package li.cil.oc.common.block

import li.cil.oc.CreativeTab
import li.cil.oc.common.tileentity
import li.cil.oc.common.tileentity.traits.Colored
import li.cil.oc.common.tileentity.traits.Inventory
import li.cil.oc.common.tileentity.traits.Rotatable
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.Color
import li.cil.oc.util.Tooltip
import net.minecraft.block.Block
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

class SimpleBlock(material: Material = Material.iron) extends Block(material) {
  setHardness(2f)
  setCreativeTab(CreativeTab)

  var showInItemList = true

  protected val validRotations_ = Array(EnumFacing.UP, EnumFacing.DOWN)

  def createItemStack(amount: Int = 1) = new ItemStack(this, amount)

  // ----------------------------------------------------------------------- //
  // Rendering
  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def colorMultiplier(world: IBlockAccess, pos: BlockPos, renderPass: Int) =
    world.getTileEntity(pos) match {
      case colored: Colored => Color.rgbValues(colored.color)
      case _ => getRenderColor(world.getBlockState(pos))
    }

  @SideOnly(Side.CLIENT)
  def preItemRender(metadata: Int) {}

  final override def setBlockBoundsForItemRender() = setBlockBoundsForItemRender(0)

  def setBlockBoundsForItemRender(metadata: Int) = super.setBlockBoundsForItemRender()

  // ----------------------------------------------------------------------- //
  // ItemBlock
  // ----------------------------------------------------------------------- //

  def rarity = EnumRarity.COMMON

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

  override def isNormalCube(world: IBlockAccess, pos: BlockPos) = true

  override def canHarvestBlock(world: IBlockAccess, pos: BlockPos, player: EntityPlayer) = true

  override def canBeReplacedByLeaves(world: IBlockAccess, pos: BlockPos) = false

  override def canCreatureSpawn(world: IBlockAccess, pos: BlockPos, `type`: SpawnPlacementType) = false

  override def getValidRotations(world: World, pos: BlockPos) = validRotations_

  override def harvestBlock(world: World, player: EntityPlayer, pos: BlockPos, state: IBlockState, te: TileEntity) = {
    if (!world.isRemote) te match {
      case inventory: Inventory => inventory.dropAllSlots()
      case _ => // Ignore.
    }
    super.harvestBlock(world, player, pos, state, te)
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
      case colored: Colored if colored.color != color =>
        colored.color = color
        world.markBlockForUpdate(pos)
        false // Don't consume items.
      case _ => super.recolorBlock(world, pos, side, color)
    }

  // This function can mess things up badly in single player if not
  // synchronized because it sets fields in an instance stored in the
  // static block list... which is used by both server and client thread.
  // The other place where this is locked is in collisionRayTrace below,
  // which seems to be the only built-in function that *logically* depends
  // on the state bounds (rest is rendering which is unimportant).
  final override def setBlockBoundsBasedOnState(world: IBlockAccess, pos: BlockPos) =
    this.synchronized(doSetBlockBoundsBasedOnState(world, pos))

  protected def doSetBlockBoundsBasedOnState(world: IBlockAccess, pos: BlockPos): Unit =
    super.setBlockBoundsBasedOnState(world, pos)

  protected def setBlockBounds(bounds: AxisAlignedBB) {
    setBlockBounds(
      bounds.minX.toFloat,
      bounds.minY.toFloat,
      bounds.minZ.toFloat,
      bounds.maxX.toFloat,
      bounds.maxY.toFloat,
      bounds.maxZ.toFloat)
  }

  // NOTE: must not be final for immibis microblocks to work.
  override def collisionRayTrace(world: World, pos: BlockPos, origin: Vec3, end: Vec3) =
    this.synchronized(intersect(world, pos, origin, end))

  override def getCollisionBoundingBox(world: World, pos: BlockPos, state: IBlockState) = this.synchronized {
    doSetBlockBoundsBasedOnState(world, pos)
    super.getCollisionBoundingBox(world, pos, state)
  }

  protected def intersect(world: World, pos: BlockPos, origin: Vec3, end: Vec3) =
    super.collisionRayTrace(world, pos, origin, end)

  final override def canPlaceBlockOnSide(world: World, pos: BlockPos, side: EnumFacing) =
    localCanPlaceBlockOnSide(world, pos, toLocal(world, pos, side.getOpposite))

  def localCanPlaceBlockOnSide(world: World, pos: BlockPos, side: EnumFacing) =
    super.canPlaceBlockOnSide(world, BlockPosition(pos).toBlockPos, side.getOpposite)

  // ----------------------------------------------------------------------- //

  override def canConnectRedstone(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = false

  override def isProvidingStrongPower(worldIn: IBlockAccess, pos: BlockPos, state: IBlockState, side: EnumFacing) = super.isProvidingWeakPower(worldIn, pos, state, side)

  override def isProvidingWeakPower(worldIn: IBlockAccess, pos: BlockPos, state: IBlockState, side: EnumFacing) = 0

  // ----------------------------------------------------------------------- //

  // NOTE: must not be final for immibis microblocks to work.
  override def onBlockActivated(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) =
    world.getTileEntity(pos) match {
      case colored: Colored if Color.isDye(player.getHeldItem) =>
        colored.color = Color.dyeColor(player.getHeldItem)
        world.markBlockForUpdate(pos)
        true
      case _ => localOnBlockActivated(world, pos, player, side, hitX, hitY, hitZ)
    }

  def localOnBlockActivated(world: World, pos: BlockPos, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float) = false
}
