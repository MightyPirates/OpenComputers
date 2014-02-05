package li.cil.oc.server.driver

import li.cil.oc.api.driver
import net.minecraft.block.Block
import net.minecraft.item.{Item, ItemStack}
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
    else new CompoundBlockEnvironment(tryGetName(world, x, y, z), list: _*)
  }

  override def worksWith(world: World, stack: ItemStack) = blocks.forall(_.worksWith(world, stack))

  override def worksWith(world: World, x: Int, y: Int, z: Int) = blocks.forall(_.worksWith(world, x, y, z))

  override def equals(obj: Any) = obj match {
    case multi: CompoundBlockDriver if multi.blocks.length == blocks.length =>
      (multi.blocks, blocks).zipped.forall((a, b) => a.getClass.getName == b.getClass.getName)
      true
    case _ => false
  }

  private def tryGetName(world: World, x: Int, y: Int, z: Int) = {
    val blockId = world.getBlockId(x, y, z)
    val isValidBlock = blockId >= 0 && blockId < Block.blocksList.length && Block.blocksList(blockId) != null
    if (isValidBlock) {
      val block = Block.blocksList(blockId)
      val itemStack = try Option(block.getPickBlock(null, world, x, y, z)) catch {
        case _: Throwable =>
          if (Item.itemsList(blockId) != null) {
            Some(new ItemStack(blockId, 1, block.getDamageValue(world, x, y, z)))
          }
          else None
      }
      itemStack match {
        case Some(stack) => cleanName(stack.getUnlocalizedName)
        case _ => "multi"
      }
    }
    else "multi"
  }

  private def cleanName(name: String) = {
    val withoutNameSpace = if (name.contains(":")) name.substring(name.indexOf(":") + 1) else name
    val withoutPrefixes = if (withoutNameSpace.contains(".")) withoutNameSpace.substring(withoutNameSpace.lastIndexOf(".") + 1) else withoutNameSpace
    val safeStart = if (withoutPrefixes.matches("""^[^a-zA-Z_]""")) "_" + withoutPrefixes else withoutPrefixes
    val identifier = safeStart.replaceAll("""[^\w_]""", "_")
    identifier
  }
}