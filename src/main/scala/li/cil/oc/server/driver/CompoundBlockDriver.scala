package li.cil.oc.server.driver

import com.google.common.base.Strings
import li.cil.oc.api.driver
import li.cil.oc.api.driver.DriverBlock
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.network.ManagedEnvironment
import net.minecraft.inventory.IInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

class CompoundBlockDriver(val sidedBlocks: Array[DriverBlock]) extends DriverBlock {
  override def createEnvironment(world: World, pos: BlockPos, side: EnumFacing): CompoundBlockEnvironment = {
    val list = sidedBlocks.map {
      driver => Option(driver.createEnvironment(world, pos, side)) match {
        case Some(environment) => (driver.getClass.getName, environment)
        case _ => null
      }
    } filter (_ != null)
    if (list.isEmpty) null
    else new CompoundBlockEnvironment(cleanName(tryGetName(world, pos, list.map(_._2))), list: _*)
  }

  override def worksWith(world: World, pos: BlockPos, side: EnumFacing): Boolean = sidedBlocks.forall(_.worksWith(world, pos, side))

  override def equals(obj: Any): Boolean = obj match {
    case multi: CompoundBlockDriver if multi.sidedBlocks.length == sidedBlocks.length => sidedBlocks.intersect(multi.sidedBlocks).length == sidedBlocks.length
    case _ => false
  }

  // TODO rework this method
  private def tryGetName(world: World, pos: BlockPos, environments: Seq[ManagedEnvironment]): String = {
    environments.collect {
      case named: NamedBlock => named
    }.sortBy(_.priority).lastOption match {
      case Some(named) => return named.preferredName
      case _ => // No preferred name.
    }
    try world.getTileEntity(pos) match {
      case inventory: IInventory if !Strings.isNullOrEmpty(inventory.getName) => return inventory.getName.stripPrefix("container.")
    } catch {
      case _: Throwable =>
    }
    try {
      val block = world.getBlockState(pos).getBlock
      val stack = if (Item.getItemFromBlock(block) != null) {
        Some(new ItemStack(block, 1, block.damageDropped(world.getBlockState(pos))))
      }
      else None
      if (stack.isDefined) {
        return stack.get.getUnlocalizedName.stripPrefix("tile.")
      }
    } catch {
      case _: Throwable =>
    }
    try world.getTileEntity(pos) match {
      case tileEntity: TileEntity =>
        return TileEntity.getKey(tileEntity.getClass).getResourcePath
    } catch {
      case _: Throwable =>
    }
    "component"
  }

  private def cleanName(name: String) = {
    val safeStart = if (name.matches( """^[^a-zA-Z_]""")) "_" + name else name
    val identifier = safeStart.replaceAll( """[^\w_]""", "_").trim
    if (Strings.isNullOrEmpty(identifier)) "component"
    else identifier.toLowerCase
  }
}
