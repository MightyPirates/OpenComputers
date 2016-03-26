package li.cil.oc.common.block

import java.util
import java.util.Random

import com.google.common.base.Strings
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.common.block.property.PropertyTile
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.ItemBlacklist
import li.cil.oc.util.ExtendedAABB
import li.cil.oc.util.ExtendedAABB._
import li.cil.oc.util.InventoryUtils
import net.minecraft.block.state.BlockState
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLiving.SpawnPlacementType
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumWorldBlockLayer
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.property.ExtendedBlockState
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

import scala.collection.convert.WrapAsJava._
import scala.reflect.ClassTag

class Print(protected implicit val tileTag: ClassTag[tileentity.Print]) extends RedstoneAware with traits.CustomDrops[tileentity.Print] {
  setLightOpacity(1)
  setHardness(1)
  setCreativeTab(null)
  ItemBlacklist.hide(this)

  // ----------------------------------------------------------------------- //

  override def createBlockState(): BlockState = new ExtendedBlockState(this, Array.empty, Array(PropertyTile.Tile))

  override def getExtendedState(state: IBlockState, world: IBlockAccess, pos: BlockPos): IBlockState =
    (state, world.getTileEntity(pos)) match {
      case (extendedState: IExtendedBlockState, t: tileentity.Print) =>
        extendedState.withProperty(property.PropertyTile.Tile, t)
      case _ => state
    }

  // ----------------------------------------------------------------------- //

  override def canRenderInLayer(layer: EnumWorldBlockLayer): Boolean = layer == EnumWorldBlockLayer.CUTOUT_MIPPED

  @SideOnly(Side.CLIENT) override
  def colorMultiplier(world: IBlockAccess, pos: BlockPos, tint: Int): Int = tint

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

  override def isOpaqueCube = false

  override def getLightValue(blockAccess: IBlockAccess, pos: BlockPos): Int =
    blockAccess match {
      case world: World if world.isBlockLoaded(pos) => world.getTileEntity(pos) match {
        case print: tileentity.Print => print.data.lightLevel
        case _ => super.getLightValue(world, pos)
      }
      case _ => super.getLightOpacity(blockAccess, pos)
    }

  override def getLightOpacity(blockAccess: IBlockAccess, pos: BlockPos): Int =
    blockAccess match {
      case world: World if world.isBlockLoaded(pos) => world.getTileEntity(pos) match {
        case print: tileentity.Print if Settings.get.printsHaveOpacity => (print.data.opacity * 4).toInt
        case _ => super.getLightOpacity(world, pos)
      }
      case _ => super.getLightOpacity(blockAccess, pos)
    }

  override def isVisuallyOpaque = false

  override def isFullCube = false

  override def shouldSideBeRendered(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = true

  override def isBlockSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing) = isSideSolid(world, pos, side)

  override def isSideSolid(world: IBlockAccess, pos: BlockPos, side: EnumFacing): Boolean = {
    world.getTileEntity(pos) match {
      case print: tileentity.Print =>
        val shapes = if (print.state) print.data.stateOn else print.data.stateOff
        for (shape <- shapes if !Strings.isNullOrEmpty(shape.texture)) {
          val bounds = shape.bounds.rotateTowards(print.facing)
          val fullX = bounds.minX == 0 && bounds.maxX == 1
          val fullY = bounds.minY == 0 && bounds.maxY == 1
          val fullZ = bounds.minZ == 0 && bounds.maxZ == 1
          if (side match {
            case EnumFacing.DOWN => bounds.minY == 0 && fullX && fullZ
            case EnumFacing.UP => bounds.maxY == 1 && fullX && fullZ
            case EnumFacing.NORTH => bounds.minZ == 0 && fullX && fullY
            case EnumFacing.SOUTH => bounds.maxZ == 1 && fullX && fullY
            case EnumFacing.WEST => bounds.minX == 0 && fullY && fullZ
            case EnumFacing.EAST => bounds.maxX == 1 && fullY && fullZ
            case _ => false
          }) return true
        }
      case _ =>
    }
    false
  }

  override def getPickBlock(target: MovingObjectPosition, world: World, pos: BlockPos): ItemStack = {
    world.getTileEntity(pos) match {
      case print: tileentity.Print => print.data.createItemStack()
      case _ => null
    }
  }

  override def addCollisionBoxesToList(world: World, pos: BlockPos, state: IBlockState, mask: AxisAlignedBB, list: util.List[AxisAlignedBB], entity: Entity): Unit = {
    world.getTileEntity(pos) match {
      case print: tileentity.Print =>
        if (if (print.state) print.data.noclipOn else print.data.noclipOff) return

        def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])
        val shapes = if (print.state) print.data.stateOn else print.data.stateOff
        for (shape <- shapes) {
          val bounds = shape.bounds.rotateTowards(print.facing).offset(pos)
          if (bounds.intersectsWith(mask)) {
            add(list, bounds)
          }
        }
      case _ => super.addCollisionBoxesToList(world, pos, state, mask, list, entity)
    }
  }

  override def collisionRayTrace(world: World, pos: BlockPos, start: Vec3, end: Vec3): MovingObjectPosition = {
    world.getTileEntity(pos) match {
      case print: tileentity.Print =>
        var closestDistance = Double.PositiveInfinity
        var closest: Option[MovingObjectPosition] = None
        for (shape <- if (print.state) print.data.stateOn else print.data.stateOff) {
          val bounds = shape.bounds.rotateTowards(print.facing).offset(pos)
          val hit = bounds.calculateIntercept(start, end)
          if (hit != null) {
            val distance = hit.hitVec.distanceTo(start)
            if (distance < closestDistance) {
              closestDistance = distance
              closest = Option(hit)
            }
          }
        }
        closest.map(hit => new MovingObjectPosition(hit.hitVec, hit.sideHit, pos)).orNull
      case _ => super.collisionRayTrace(world, pos, start, end)
    }
  }

  override def setBlockBoundsBasedOnState(world: IBlockAccess, pos: BlockPos): Unit = {
    world.getTileEntity(pos) match {
      case print: tileentity.Print => setBlockBounds(if (print.state) print.boundsOn else print.boundsOff)
      case _ => super.setBlockBoundsBasedOnState(world, pos)
    }
  }

  override def setBlockBoundsForItemRender(metadata: Int): Unit = {
    setBlockBounds(ExtendedAABB.unitBounds)
  }

  override def canCreatureSpawn(world: IBlockAccess, pos: BlockPos, `type`: SpawnPlacementType): Boolean = true

  override def tickRate(world: World) = 20

  override def updateTick(world: World, pos: BlockPos, state: IBlockState, rand: Random): Unit = {
    if (!world.isRemote) world.getTileEntity(pos) match {
      case print: tileentity.Print => if (print.state) print.toggleState()
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

  override def onBlockActivated(world: World, pos: BlockPos, state: IBlockState, player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    world.getTileEntity(pos) match {
      case print: tileentity.Print => print.activate()
      case _ => super.onBlockActivated(world, pos, state, player, side, hitX, hitY, hitZ)
    }
  }

  override protected def doCustomInit(tileEntity: tileentity.Print, player: EntityLivingBase, stack: ItemStack): Unit = {
    super.doCustomInit(tileEntity, player, stack)
    tileEntity.data.load(stack)
    tileEntity.updateBounds()
    tileEntity.world.checkLight(tileEntity.getPos)
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
        world.notifyNeighborsOfStateChange(pos, this)
        for (side <- EnumFacing.values) {
          world.notifyNeighborsOfStateChange(pos.offset(side), this)
        }
      case _ =>
    }
    super.breakBlock(world, pos, state)
  }
}
