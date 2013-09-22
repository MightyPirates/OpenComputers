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
        case Some(driver) => driver.instance.component(world, cx, cy, cz)
      }
    }
  }

  def readBlocksFromNBT(nbt: NBTTagCompound) = {
    nbt.getIntArray("components").copyToArray(blockComponents)
  }

  def writeBlocksToNBT(nbt: NBTTagCompound) = {
    nbt.setIntArray("components", blockComponents)
  }

  protected def checkBlockChanged(side: Int): Unit = {
    val d = ForgeDirection.getOrientation(side)
    val (x, y, z) = coordinates
    val (cx, cy, cz) = (x + d.offsetX, y + d.offsetY, z + d.offsetZ)
    Drivers.driverFor(world, cx, cy, cz) match {
      case None if blockComponents(side) != 0 => {
        // There was a block component and now there isn't, remove it.
        computer.remove(blockComponents(side))
        blockComponents(side) = 0
      }
      case None => // Nothing to do, but avoid match errors.
      case Some(driver) => {
        driver.instance.component(world, cx, cy, cz) match {
          case None => // Ignore.
          case Some(component) => {
            val id = driver.instance.id(component)
            // TODO we can miss changes this way, if a component was swapped for
            // another one with the same ID (frames?)
            if (blockComponents(side) != id) {
              computer.remove(blockComponents(side))
              blockComponents(side) =
                if (computer.add(component, driver)) id
                else 0
            }
          }
        }
      }
    }
  }
}