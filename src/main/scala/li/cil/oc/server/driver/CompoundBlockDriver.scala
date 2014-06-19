package li.cil.oc.server.driver

import com.google.common.base.Strings
import cpw.mods.fml.relauncher.ReflectionHelper
import li.cil.oc.api.driver
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.network.ManagedEnvironment
import net.minecraft.inventory.IInventory
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World

class CompoundBlockDriver(val blocks: driver.Block*) extends driver.Block {
  override def createEnvironment(world: World, x: Int, y: Int, z: Int) = {
    val list = blocks.map {
      driver => Option(driver.createEnvironment(world, x, y, z)) match {
        case Some(environment) => (driver, environment)
        case _ => null
      }
    } filter (_ != null)
    if (list.isEmpty) null
    else new CompoundBlockEnvironment(cleanName(tryGetName(world, x, y, z, list.map(_._2))), list: _*)
  }

  override def worksWith(world: World, x: Int, y: Int, z: Int) = blocks.forall(_.worksWith(world, x, y, z))

  override def equals(obj: Any) = obj match {
    case multi: CompoundBlockDriver if multi.blocks.length == blocks.length => blocks.intersect(multi.blocks).length == blocks.length
    case _ => false
  }

  private def tryGetName(world: World, x: Int, y: Int, z: Int, environments: Seq[ManagedEnvironment]): String = {
    for (environment <- environments) environment match {
      case named: NamedBlock => return named.preferredName
      case _ =>
    }
    try world.getTileEntity(x, y, z) match {
      case inventory: IInventory if !Strings.isNullOrEmpty(inventory.getInventoryName) => return inventory.getInventoryName.stripPrefix("container.")
    } catch {
      case _: Throwable =>
    }
    try {
      val block = world.getBlock(x, y, z)
      val stack = try Option(block.getPickBlock(null, world, x, y, z)) catch {
        case _: Throwable =>
          if (Item.getItemFromBlock(block) != null) {
            Some(new ItemStack(block, 1, block.getDamageValue(world, x, y, z)))
          }
          else None
      }
      if (stack.isDefined) {
        return stack.get.getUnlocalizedName.stripPrefix("tile.")
      }
    } catch {
      case _: Throwable =>
    }
    try world.getTileEntity(x, y, z) match {
      case tileEntity: TileEntity =>
        val map = ReflectionHelper.getPrivateValue[java.util.Map[Class[_], String], TileEntity](classOf[TileEntity], tileEntity, "classToNameMap", "field_145853_j")
        return map.get(tileEntity.getClass)
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