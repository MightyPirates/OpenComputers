package li.cil.oc.server.driver

import li.cil.oc.api.driver
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.world.World

class CompoundBlockDriver(val blocks: driver.Block*) extends driver.Block {
  override def createEnvironment(world: World, x: Int, y: Int, z: Int) = blocks.map {
    driver => Option(driver.createEnvironment(world, x, y, z)) match {
      case Some(environment) => (driver, environment)
      case _ => null
    }
  } filter (_ != null) match {
    case Seq() => null
    case list =>
      val blockId = world.getBlockId(x, y, z)
      val isValidBlock = blockId >= 0 && blockId < Block.blocksList.length && Block.blocksList(blockId) != null
      val name =
        if (isValidBlock) {
          val metadata = world.getBlockMetadata(x, y, z)
          cleanName(new ItemStack(blockId, 1, metadata).getUnlocalizedName)
        }
        else "multi"
      new CompoundBlockEnvironment(name, list: _*)
  }

  override def worksWith(world: World, stack: ItemStack) = blocks.forall(_.worksWith(world, stack))

  override def worksWith(world: World, x: Int, y: Int, z: Int) = blocks.forall(_.worksWith(world, x, y, z))

  override def equals(obj: Any) = obj match {
    case multi: CompoundBlockDriver if multi.blocks.length == blocks.length =>
      (multi.blocks, blocks).zipped.forall((a, b) => a.getClass.getName == b.getClass.getName)
      true
    case _ => false
  }

  private def cleanName(name: String) = {
    val withoutNameSpace = if (name.contains(":")) name.substring(name.indexOf(":") + 1) else name
    val withoutPrefixes = if (withoutNameSpace.contains(".")) withoutNameSpace.substring(withoutNameSpace.lastIndexOf(".") + 1) else withoutNameSpace
    withoutPrefixes
  }
}