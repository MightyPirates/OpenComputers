package li.cil.oc.common.block

import java.util
import java.util.Random

import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.common.block.property.PropertyTile
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLiving.SpawnPlacementType
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util._
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.property.ExtendedBlockState
import net.minecraftforge.common.property.IExtendedBlockState

import scala.collection.convert.WrapAsJava._
import scala.reflect.ClassTag

class Print(protected implicit val tileTag: ClassTag[tileentity.Print]) extends BlockRedstoneAware with traits.CustomDrops[tileentity.Print] {
  setLightOpacity(1)
  setHardness(1)
  setCreativeTab(null)
  ItemBlacklist.hide(this)

  // ----------------------------------------------------------------------- //

  override def createBlockState() = new ExtendedBlockState(this, Array.empty, Array(PropertyTile.Tile))

  override def getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState =
    (state, world.getTileEntity(pos)) match {
      case (extendedState: IExtendedBlockState, t: tileentity.Print) =>
        extendedState.withProperty(property.PropertyTile.Tile, t)
      case _ => state
    }

  // ----------------------------------------------------------------------- //

  override def canRenderInLayer(state: IBlockState, layer: BlockRenderLayer): Boolean = layer == BlockRenderLayer.CUTOUT_MIPPED

  override protected def tooltipBody(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean): Unit = {
    super.tooltipBody(metadata, stack, player, tooltip, advanced)
    val data = new PrintData(stack)
    data.tooltip.foreach(s => tooltip.addAll(s.lines.toIterable))
  }

  override protected def tooltipTail(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean): Unit = {
    super.tooltipTail(metadata, stack, player, tooltip, advanced)
    val data = new PrintData(stack)
    if (data.isBeaconBase) {
      tooltip.add(Localization.Tooltip.PrintBeaconBase)
    }
    if (data.emitRedstone) {
      tooltip.add(Localization.Tooltip.PrintRedstoneLevel(data.redstoneLevel))
    }
    if (data.emitLight) {
      tooltip.add(Localization.Tooltip.PrintLightValue(data.lightLevel))
    }
  }

  override def isOpaqueCube(state: IBlockState): Boolean = false

  override def getLightValue(state: IBlockState, world: IBlockAccess, pos: BlockPos): Int =
    world match {
      case world: World if world.isBlockLoaded(pos) => world.getTileEntity(pos) match {
        case print: tileentity.Print => print.data.lightLevel
        case _ => super.getLightValue(state, world, pos)
      }
      case _ => super.getLightValue(state, world, pos)
    }

  override def getLightOpacity(state: IBlockState, world: IBlockAccess, pos: BlockPos): Int =
    world match {
      case world: World if world.isBlockLoaded(pos) => world.getTileEntity(pos) match {
        case print: tileentity.Print if Settings.get.printsHaveOpacity => (print.data.opacity * 4).toInt
        case _ => super.getLightOpacity(state, world, pos)
      }
      case _ => super.getLightOpacity(state, world, pos)
    }

  override def isFullCube(state: IBlockState): Boolean = false

  override def shouldSideBeRendered(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing) = true

  override def isBlockSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) =
    world.getTileEntity(pos) match {
      case print: tileentity.Print => print.isSideSolid(side)
      case _ => false
    }

  override def isSideSolid(state: IBlockState, world: IBlockAccess, pos: BlockPos, side: EnumFacing): Boolean =
    isBlockSolid(world, pos, side)

  override def getPickBlock(state: IBlockState, target: RayTraceResult, world: World, pos: BlockPos, player: EntityPlayer): ItemStack = {
    world.getTileEntity(pos) match {
      case print: tileentity.Print => print.data.createItemStack()
      case _ => null
    }
  }

  override def getBoundingBox(state: IBlockState, world: IBlockAccess, pos: BlockPos): AxisAlignedBB = {
    world.getTileEntity(pos) match {
      case print: tileentity.Print => print.bounds
      case _ => super.getBoundingBox(state, world, pos)
    }
  }

  override def addCollisionBoxToList(state: IBlockState, world: World, pos: BlockPos, mask: AxisAlignedBB, list: util.List[AxisAlignedBB], entity: Entity, par7: Boolean): Unit = {
    world.getTileEntity(pos) match {
      case print: tileentity.Print => print.addCollisionBoxesToList(mask, list, pos)
      case _ => super.addCollisionBoxToList(state, world, pos, mask, list, entity, par7)
    }
  }

  override def collisionRayTrace(state: IBlockState, world: World, pos: BlockPos, start: Vec3d, end: Vec3d): RayTraceResult = {
    world.getTileEntity(pos) match {
      case print: tileentity.Print => print.rayTrace(start, end, pos)
      case _ => super.collisionRayTrace(state, world, pos, start, end)
    }
  }

  override def canCreatureSpawn(state: IBlockState, world: IBlockAccess, pos: BlockPos, `type`: SpawnPlacementType): Boolean = true

  override def tickRate(world: World) = 20

  override def updateTick(world: World, pos: BlockPos, state: IBlockState, rand: Random): Unit = {
    if (!world.isRemote) world.getTileEntity(pos) match {
      case print: tileentity.Print =>
        if (print.state) print.toggleState()
        if (print.state) world.scheduleUpdate(pos, state.getBlock, tickRate(world))
      case _ =>
    }
  }

  override def isBeaconBase(world: IBlockAccess, pos: BlockPos, beacon: BlockPos): Boolean = {
    world.getTileEntity(pos) match {
      case print: tileentity.Print => print.data.isBeaconBase
      case _ => false
    }
  }

  // ----------------------------------------------------------------------- //

  override def createNewTileEntity(worldIn: World, meta: Int) = new tileentity.Print()

  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, hand: EnumHand, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    world.getTileEntity(pos) match {
      case print: tileentity.Print => print.activate()
      case _ => super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ)
    }
  }

  override protected def doCustomInit(tileEntity: tileentity.Print, player: EntityLivingBase, stack: ItemStack): Unit = {
    super.doCustomInit(tileEntity, player, stack)
    tileEntity.data.load(stack)
    tileEntity.updateBounds()
    tileEntity.updateRedstone()
    tileEntity.getWorld.checkLight(tileEntity.getPos)
  }

  override protected def doCustomDrops(tileEntity: tileentity.Print, player: EntityPlayer, willHarvest: Boolean): Unit = {
    super.doCustomDrops(tileEntity, player, willHarvest)
    if (!player.capabilities.isCreativeMode) {
      InventoryUtils.spawnStackInWorld(tileEntity.position, tileEntity.data.createItemStack())
    }
  }

  override def breakBlock(world: World, pos: BlockPos, state: IBlockState): Unit = {
    world.getTileEntity(pos) match {
      case print: tileentity.Print if print.data.emitRedstone(print.state) =>
        world.notifyNeighborsOfStateChange(pos, this, false)
        for (side <- EnumFacing.values) {
          world.notifyNeighborsOfStateChange(pos.offset(side), this, false)
        }
      case _ =>
    }
    super.breakBlock(world, pos, state)
  }
}
