package li.cil.oc.server.driver

import com.google.common.base.Strings
import li.cil.oc.api.driver
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.network.ManagedEnvironment
import net.minecraft.inventory.IInventory
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

// TODO Remove blocks in OC 1.7.
class CompoundBlockDriver(val sidedBlocks: Array[driver.SidedBlock], val blocks: Array[driver.Block]) extends driver.SidedBlock {
  override def createEnvironment(world: World, pos: BlockPos, side: EnumFacing) = {
    val list = sidedBlocks.map {
      driver => Option(driver.createEnvironment(world, pos, side)) match {
        case Some(environment) => (driver.getClass.getName, environment)
        case _ => null
      }
    } ++ blocks.map {
      driver => Option(driver.createEnvironment(world, pos)) match {
        case Some(environment) => (driver.getClass.getName, environment)
        case _ => null
      }
    } filter (_ != null)
    if (list.isEmpty) null
    else new CompoundBlockEnvironment(cleanName(tryGetName(world, pos, list.map(_._2))), list: _*)
  }

  override def worksWith(world: World, pos: BlockPos, side: EnumFacing) = sidedBlocks.forall(_.worksWith(world, pos, side)) && blocks.forall(_.worksWith(world, pos))

  override def equals(obj: Any) = obj match {
    case multi: CompoundBlockDriver if multi.sidedBlocks.length == sidedBlocks.length && multi.blocks.length == blocks.length => sidedBlocks.intersect(multi.sidedBlocks).length == sidedBlocks.length && blocks.intersect(multi.blocks).length == blocks.length
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
        return TileEntity.classToNameMap.get(tileEntity.getClass)
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