package li.cil.oc.common.block

import java.util
import java.util.Random

import com.google.common.base.Strings
import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.NEI
import li.cil.oc.util.ExtendedAABB
import li.cil.oc.util.ExtendedAABB._
import net.minecraft.block.Block
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.EnumCreatureType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.IIcon
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsJava._
import scala.reflect.ClassTag

class Print(protected implicit val tileTag: ClassTag[tileentity.Print]) extends RedstoneAware with traits.SpecialBlock with traits.CustomDrops[tileentity.Print] {
  setLightOpacity(0)
  setHardness(1)
  setCreativeTab(null)
  NEI.hide(this)
  setBlockTextureName(Settings.resourceDomain + "GenericTop")

  // This is used when rendering to allow tinting individual shapes of models.
  var colorMultiplierOverride: Option[Int] = None
  // Also used in model rendering, can't use renderer's override logic because that'll disable tinting.
  var textureOverride: Option[IIcon] = None
  // Again, used in model rendering, used to know whether we can potentially skip rendering sides.
  var isSingleShape = false

  @SideOnly(Side.CLIENT)
  override def getIcon(world: IBlockAccess, x: Int, y: Int, z: Int, globalSide: ForgeDirection, localSide: ForgeDirection): IIcon =
    textureOverride.getOrElse(super.getIcon(world, x, y, z, globalSide, localSide))

  override def colorMultiplier(world: IBlockAccess, x: Int, y: Int, z: Int) =
    colorMultiplierOverride.getOrElse(super.colorMultiplier(world, x, y, z))

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

  override def getLightValue(world: IBlockAccess, x: Int, y: Int, z: Int): Int =
    world.getTileEntity(x, y, z) match {
      case print: tileentity.Print => print.data.lightLevel
      case _ => super.getLightValue(world, x, y, z)
    }

  override def getLightOpacity(world: IBlockAccess, x: Int, y: Int, z: Int): Int =
    world.getTileEntity(x, y, z) match {
      case print: tileentity.Print if Settings.get.printsHaveOpacity => (print.data.opacity * 4).toInt
      case _ => super.getLightOpacity(world, x, y, z)
    }

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = !isSingleShape || (world.getTileEntity(x, y, z) match {
    case print: tileentity.Print if isSideSolid(world, x, y, z, side.getOpposite) =>
      side match {
        case ForgeDirection.DOWN => minY > 0
        case ForgeDirection.UP => maxY < 1
        case ForgeDirection.NORTH => minZ > 0
        case ForgeDirection.SOUTH => maxZ < 1
        case ForgeDirection.WEST => minX > 0
        case ForgeDirection.EAST => maxX < 1
        case _ => true
      }
    case _ => super.shouldSideBeRendered(world, x, y, z, side)
  })

  override def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = isSideSolid(world, x, y, z, side)

  override def isSideSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection): Boolean = {
    world.getTileEntity(x, y, z) match {
      case print: tileentity.Print =>
        val shapes = if (print.state) print.data.stateOn else print.data.stateOff
        for (shape <- shapes if !Strings.isNullOrEmpty(shape.texture)) {
          val bounds = shape.bounds.rotateTowards(print.facing)
          val fullX = bounds.minX == 0 && bounds.maxX == 1
          val fullY = bounds.minY == 0 && bounds.maxY == 1
          val fullZ = bounds.minZ == 0 && bounds.maxZ == 1
          if (side match {
            case ForgeDirection.DOWN => bounds.minY == 0 && fullX && fullZ
            case ForgeDirection.UP => bounds.maxY == 1 && fullX && fullZ
            case ForgeDirection.NORTH => bounds.minZ == 0 && fullX && fullY
            case ForgeDirection.SOUTH => bounds.maxZ == 1 && fullX && fullY
            case ForgeDirection.WEST => bounds.minX == 0 && fullY && fullZ
            case ForgeDirection.EAST => bounds.maxX == 1 && fullY && fullZ
            case _ => false
          }) return true
        }
      case _ =>
    }
    false
  }

  override def getPickBlock(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int): ItemStack = {
    world.getTileEntity(x, y, z) match {
      case print: tileentity.Print => print.data.createItemStack()
      case _ => null
    }
  }

  override def addCollisionBoxesToList(world: World, x: Int, y: Int, z: Int, mask: AxisAlignedBB, list: util.List[_], entity: Entity): Unit = {
    world.getTileEntity(x, y, z) match {
      case print: tileentity.Print =>
        if (if (print.state) print.data.noclipOn else print.data.noclipOff) return

        def add[T](list: util.List[T], value: Any) = list.add(value.asInstanceOf[T])
        val shapes = if (print.state) print.data.stateOn else print.data.stateOff
        for (shape <- shapes) {
          val bounds = shape.bounds.rotateTowards(print.facing).offset(x, y, z)
          if (bounds.intersectsWith(mask)) {
            add(list, bounds)
          }
        }
      case _ => super.addCollisionBoxesToList(world, x, y, z, mask, list, entity)
    }
  }

  override protected def intersect(world: World, x: Int, y: Int, z: Int, start: Vec3, end: Vec3): MovingObjectPosition = {
    world.getTileEntity(x, y, z) match {
      case print: tileentity.Print =>
        var closestDistance = Double.PositiveInfinity
        var closest: Option[MovingObjectPosition] = None
        for (shape <- if (print.state) print.data.stateOn else print.data.stateOff) {
          val bounds = shape.bounds.rotateTowards(print.facing).offset(x, y, z)
          val hit = bounds.calculateIntercept(start, end)
          if (hit != null) {
            val distance = hit.hitVec.distanceTo(start)
            if (distance < closestDistance) {
              closestDistance = distance
              closest = Option(hit)
            }
          }
        }
        closest.map(hit => new MovingObjectPosition(x, y, z, hit.sideHit, hit.hitVec)).orNull
      case _ => super.intersect(world, x, y, z, start, end)
    }
  }

  override protected def doSetBlockBoundsBasedOnState(world: IBlockAccess, x: Int, y: Int, z: Int): Unit = {
    world.getTileEntity(x, y, z) match {
      case print: tileentity.Print => setBlockBounds(if (print.state) print.boundsOn else print.boundsOff)
      case _ => super.doSetBlockBoundsBasedOnState(world, x, y, z)
    }
  }

  override def setBlockBoundsForItemRender(metadata: Int): Unit = {
    setBlockBounds(ExtendedAABB.unitBounds)
  }

  override def canCreatureSpawn(creature: EnumCreatureType, world: IBlockAccess, x: Int, y: Int, z: Int): Boolean = true

  override def tickRate(world: World) = 20

  override def updateTick(world: World, x: Int, y: Int, z: Int, rng: Random): Unit = {
    if (!world.isRemote) world.getTileEntity(x, y, z) match {
      case print: tileentity.Print => if (print.state) print.toggleState()
      case _ =>
    }
  }

  override def isBeaconBase(world: IBlockAccess, x: Int, y: Int, z: Int, beaconX: Int, beaconY: Int, beaconZ: Int): Boolean = {
    world.getTileEntity(x, y, z) match {
      case print: tileentity.Print => print.data.isBeaconBase
      case _ => false
    }
  }

  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new tileentity.Print()

  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer, side: ForgeDirection, hitX: Float, hitY: Float, hitZ: Float): Boolean = {
    world.getTileEntity(x, y, z) match {
      case print: tileentity.Print => print.activate()
      case _ => super.onBlockActivated(world, x, y, z, player, side, hitX, hitY, hitZ)
    }
  }

  override protected def doCustomInit(tileEntity: tileentity.Print, player: EntityLivingBase, stack: ItemStack): Unit = {
    super.doCustomInit(tileEntity, player, stack)
    tileEntity.data.load(stack)
    tileEntity.updateBounds()
    tileEntity.world.func_147451_t(tileEntity.x, tileEntity.y, tileEntity.z)
  }

  override protected def doCustomDrops(tileEntity: tileentity.Print, player: EntityPlayer, willHarvest: Boolean): Unit = {
    super.doCustomDrops(tileEntity, player, willHarvest)
    if (!player.capabilities.isCreativeMode) {
      dropBlockAsItem(tileEntity.world, tileEntity.x, tileEntity.y, tileEntity.z, tileEntity.data.createItemStack())
    }
  }

  override def breakBlock(world: World, x: Int, y: Int, z: Int, block: Block, metadata: Int): Unit = {
    world.getTileEntity(x, y, z) match {
      case print: tileentity.Print if print.data.emitRedstone(print.state) =>
        world.notifyBlocksOfNeighborChange(x, y, z, this)
        for (side <- ForgeDirection.VALID_DIRECTIONS) {
          world.notifyBlocksOfNeighborChange(x + side.offsetX, y + side.offsetY, z + side.offsetZ, this)
        }
      case _ =>
    }
    super.breakBlock(world, x, y, z, block, metadata)
  }
}
