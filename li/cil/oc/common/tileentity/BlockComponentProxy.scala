package li.cil.oc.common.tileentity

import li.cil.oc.common.computer.IComputer
import li.cil.oc.server.computer.Drivers
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagIntArray
import net.minecraft.nbt.NBTTagList
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

/** Mixin for block component add/remove logic. */
trait BlockComponentProxy {
  protected val blockComponents = Array.fill(6)(0)

  protected val computer: IComputer

  def world: World

  def coordinates: (Int, Int, Int)

  def blockDriver(id: Int) = blockComponents.indexOf(id) match {
    case -1 => None
    case side => {
      val d = ForgeDirection.getOrientation(side)
      val (x, y, z) = coordinates
      val (cx, cy, cz) = (x + d.offsetX, y + d.offsetY, z + d.offsetZ)
      Drivers.driverFor(world, cx, cy, cz)
    }
  }

  def blockComponent(id: Int) = blockComponents.indexOf(id) match {
    case -1 => None
    case side => {
      val d = ForgeDirection.getOrientation(side)
      val (x, y, z) = coordinates
      val (cx, cy, cz) = (x + d.offsetX, y + d.offsetY, z + d.offsetZ)
      Drivers.driverFor(world, cx, cy, cz) match {
        case None => None
        case Some(driver) => Some(driver.instance.component(world, cx, cy, cz))
      }
    }
  }

  def readBlocksFromNBT(nbt: NBTTagCompound) = {
//    val components = nbt.getTagList("components")
//    for (i <- 0 until components.tagCount) {
//      val component = components.tagAt(i).asInstanceOf[NBTTagCompound]
//      val side = component.getByte("side")
//      val id = component.getInteger("id")
//      val componentName = component.getString("componentName")
//      blockComponents(side) = (id, componentName)
//    }
    nbt.getIntArray("components").copyToArray(blockComponents)
  }

  def writeBlocksToNBT(nbt: NBTTagCompound) = {
//    val components = new NBTTagList
//    for (side <- 0 until blockComponents.length) {
//      val (id, componentName) = blockComponents(side)
//      if (id != 0) {
//        val component = new NBTTagCompound
//        component.setByte("side", side.toByte)
//        component.setInteger("id", id)
//        component.setString("componentName", componentName)
//        components.appendTag(component)
//      }
//    }
//    nbt.setTag("components", components)
    nbt.setIntArray("components", blockComponents)
  }

  protected def checkBlockChanged(side: Int): Unit = {
    val d = ForgeDirection.getOrientation(side)
    val (x, y, z) = coordinates
    val (cx, cy, cz) = (x + d.offsetX, y + d.offsetY, z + d.offsetZ)
    Drivers.driverFor(world, cx, cy, cz) match {
      case None if blockComponents(side) != 0 => {
        // There was a block component and now there isn't, remove it.
//        val (id, _)  = blockComponents(side)
//        computer.remove(id)
//        blockComponents(side) = (0, "")
        computer.remove(blockComponents(side))
        blockComponents(side) = 0
      }
      case None => // Nothing to do, but avoid match errors.
      case Some(driver) => {
//        val (oldId, oldComponentName) = blockComponents(side)
        val component = driver.instance.component(world, cx, cy, cz)
        val id = driver.instance.id(component)
//        val componentName = driver.instance.componentName
        // TODO we can still miss changes this way
//        if (oldId != id || oldComponentName != componentName) {
        if (blockComponents(side) != id) {
//          computer.remove(oldId)
          computer.remove(blockComponents(side))
          blockComponents(side) =
//            if (computer.add(component, driver)) (id, componentName)
//            else (0, "")
            if (computer.add(component, driver)) id
            else 0
        }
      }
    }
  }
}