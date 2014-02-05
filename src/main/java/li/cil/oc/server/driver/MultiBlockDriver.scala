package li.cil.oc.server.driver

import li.cil.oc.api
import li.cil.oc.api.driver
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World

class MultiBlockDriver(val blocks: driver.Block*) extends driver.Block {
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
      new MultiBlockEnvironment(name, list: _*)
  }

  override def worksWith(world: World, stack: ItemStack) = blocks.exists(_.worksWith(world, stack))

  override def worksWith(world: World, x: Int, y: Int, z: Int) = blocks.exists(_.worksWith(world, x, y, z))

  override def equals(obj: Any) = obj match {
    case multi: MultiBlockDriver if multi.blocks.length == blocks.length =>
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

class MultiBlockEnvironment(val name: String, val environments: (driver.Block, ManagedEnvironment)*) extends ManagedEnvironment {
  // Block drivers with visibility < network usually won't make much sense,
  // but let's play it safe and use the least possible visibility based on
  // the drivers we encapsulate.
  val node = api.Network.newNode(this, (environments.filter(_._2.node != null).map(_._2.node.reachability) ++ Seq(Visibility.None)).max).
    withComponent(name).
    create()

  // Force all wrapped components to be neighbor visible, since we as their
  // only neighbor will take care of all component-related interaction.
  for ((_, environment) <- environments) environment.node match {
    case component: Component => component.setVisibility(Visibility.Neighbors)
    case _ =>
  }

  override def canUpdate = environments.exists(_._2.canUpdate)

  override def update() {
    for ((_, environment) <- environments if environment.canUpdate) {
      environment.update()
    }
  }

  override def onMessage(message: Message) {}

  override def onConnect(node: Node) {
    if (node == this.node) {
      for ((_, environment) <- environments if environment.node != null) {
        node.connect(environment.node)
      }
    }
  }

  override def onDisconnect(node: Node) {
    if (node == this.node) {
      for ((_, environment) <- environments if environment.node != null) {
        environment.node.remove()
      }
    }
  }

  override def load(nbt: NBTTagCompound) {
    node.load(nbt)
    for ((driver, environment) <- environments) {
      val name = driver.getClass.getName
      if (nbt.hasKey(name)) {
        environment.load(nbt.getCompoundTag(name))
      }
    }
  }

  override def save(nbt: NBTTagCompound) {
    node.save(nbt)
    for ((driver, environment) <- environments) {
      val name = driver.getClass.getName
      nbt.setNewCompoundTag(name, environment.save)
    }
  }
}