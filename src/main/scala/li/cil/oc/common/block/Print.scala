package li.cil.oc.common.block

import java.util
import java.util.Random

import li.cil.oc.Settings
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.NEI
import li.cil.oc.util.ExtendedAABB
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.EnumCreatureType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.MovingObjectPosition
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

  override protected def tooltipBody(metadata: Int, stack: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean): Unit = {
    super.tooltipBody(metadata, stack, player, tooltip, advanced)
    val data = new PrintData(stack)
    data.tooltip.foreach(s => tooltip.addAll(s.lines.toIterable))
  }

  override def shouldSideBeRendered(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = true

  override def isBlockSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = isSideSolid(world, x, y, z, side)

  override def isSideSolid(world: IBlockAccess, x: Int, y: Int, z: Int, side: ForgeDirection) = {
    world.getTileEntity(x, y, z) match {
      case print: tileentity.Print =>
        val bounds = if (print.state) print.boundsOn else print.boundsOff
        val fullX = bounds.minX == 0 && bounds.maxX == 1
        val fullY = bounds.minY == 0 && bounds.maxY == 1
        val fullZ = bounds.minZ == 0 && bounds.maxZ == 1
        side match {
          case ForgeDirection.DOWN => bounds.minY == 0 && fullX && fullZ
          case ForgeDirection.UP => bounds.maxY == 1 && fullX && fullZ
          case ForgeDirection.NORTH => bounds.minZ == 0 && fullX && fullY
          case ForgeDirection.SOUTH => bounds.maxZ == 1 && fullX && fullY
          case ForgeDirection.WEST => bounds.minX == 0 && fullY && fullZ
          case ForgeDirection.EAST => bounds.minX == 1 && fullY && fullZ
          case _ => false
        }
      case _ => super.isSideSolid(world, x, y, z, side)
    }
  }

  override def getPickBlock(target: MovingObjectPosition, world: World, x: Int, y: Int, z: Int, player: EntityPlayer): ItemStack = {
    world.getTileEntity(x, y, z) match {
      case print: tileentity.Print => print.data.createItemStack()
      case _ => null
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
  }

  override protected def doCustomDrops(tileEntity: tileentity.Print, player: EntityPlayer, willHarvest: Boolean): Unit = {
    super.doCustomDrops(tileEntity, player, willHarvest)
    if (!player.capabilities.isCreativeMode) {
      dropBlockAsItem(tileEntity.world, tileEntity.x, tileEntity.y, tileEntity.z, tileEntity.data.createItemStack())
    }
  }
}
