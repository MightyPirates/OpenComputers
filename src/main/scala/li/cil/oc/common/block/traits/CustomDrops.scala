package li.cil.oc.common.block.traits

import java.util

import li.cil.oc.common.block.SimpleBlock
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

import scala.reflect.ClassTag

trait CustomDrops[Tile <: TileEntity] extends SimpleBlock {
  protected def tileTag: ClassTag[Tile]

  override def getDrops(world: World, x: Int, y: Int, z: Int, metadata: Int, fortune: Int): util.ArrayList[ItemStack] = new java.util.ArrayList[ItemStack]()

  override def onBlockPreDestroy(world: World, x: Int, y: Int, z: Int, metadata: Int): Unit = {}

  override def removedByPlayer(world: World, player: EntityPlayer, x: Int, y: Int, z: Int, willHarvest: Boolean): Boolean = {
    if (!world.isRemote) {
      val matcher = tileTag
      world.getTileEntity(x, y, z) match {
        case matcher(tileEntity) => doCustomDrops(tileEntity, player, willHarvest)
        case _ =>
      }
    }
    super.removedByPlayer(world, player, x, y, z, willHarvest)
  }

  override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, player: EntityLivingBase, stack: ItemStack): Unit = {
    super.onBlockPlacedBy(world, x, y, z, player, stack)
    val matcher = tileTag
    world.getTileEntity(x, y, z) match {
      case matcher(tileEntity) => doCustomInit(tileEntity, player, stack)
      case _ =>
    }
  }

  protected def doCustomInit(tileEntity: Tile, player: EntityLivingBase, stack: ItemStack): Unit = {}

  protected def doCustomDrops(tileEntity: Tile, player: EntityPlayer, willHarvest: Boolean): Unit = {}
}
