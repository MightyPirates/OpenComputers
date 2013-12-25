package li.cil.oc.common.tileentity

import li.cil.oc.api.network._
import li.cil.oc.server.driver
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{Settings, api}
import net.minecraft.item.{ItemBlock, ItemStack}
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}
import net.minecraftforge.common.ForgeDirection
import scala.{Array, Some}

class Adapter extends Environment {
  val node = api.Network.newNode(this, Visibility.Network).create()

  private val blocks = Array.fill[Option[(ManagedEnvironment, api.driver.Block)]](6)(None)

  private val blocksData = Array.fill[Option[BlockData]](6)(None)

  private var filter: Option[ItemStack] = None

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    for (block <- blocks) block match {
      case Some((environment, _)) if environment.canUpdate => environment.update()
      case _ => // Empty.
    }
  }

  def neighborChanged() = if (node != null && node.network != null) {
    for (d <- ForgeDirection.VALID_DIRECTIONS) {
      val (x, y, z) = (this.x + d.offsetX, this.y + d.offsetY, this.z + d.offsetZ)
      driver.Registry.driverFor(world, x, y, z) match {
        case Some(newDriver) => blocks(d.ordinal()) match {
          case Some((oldEnvironment, driver)) =>
            if (newDriver != driver) {
              // This is... odd. Maybe moved by some other mod?
              node.disconnect(oldEnvironment.node)
              if (isBlockSupported(x, y, z)) {
                val environment = newDriver.createEnvironment(world, x, y, z)
                blocks(d.ordinal()) = Some((environment, newDriver))
                blocksData(d.ordinal()) = Some(new BlockData(environment.getClass.getName, new NBTTagCompound()))
                node.connect(environment.node)
              }
              else {
                blocks(d.ordinal()) = None
                blocksData(d.ordinal()) = None
              }
            } // else: the more things change, the more they stay the same.
          case _ if isBlockSupported(x, y, z) =>
            // A challenger appears.
            val environment = newDriver.createEnvironment(world, x, y, z)
            blocks(d.ordinal()) = Some((environment, newDriver))
            blocksData(d.ordinal()) match {
              case Some(data) if data.name == environment.getClass.getName =>
                environment.load(data.data)
              case _ =>
            }
            blocksData(d.ordinal()) = Some(new BlockData(environment.getClass.getName, new NBTTagCompound()))
            node.connect(environment.node)
          case _ => // Not supported by filter.
        }
        case _ => blocks(d.ordinal()) match {
          case Some((environment, driver)) =>
            // We had something there, but it's gone now...
            node.disconnect(environment.node)
            environment.save(blocksData(d.ordinal()).get.data)
            blocks(d.ordinal()) = None
          case _ => // Nothing before, nothing now.
        }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      neighborChanged()
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)

    val blocksNbt = nbt.getTagList(Settings.namespace + "adapter.blocks")
    (0 until (blocksNbt.tagCount min blocksData.length)).
      map(blocksNbt.tagAt).
      map(_.asInstanceOf[NBTTagCompound]).
      zipWithIndex.
      foreach {
      case (blockNbt, i) =>
        if (blockNbt.hasKey("name") && blockNbt.hasKey("data")) {
          blocksData(i) = Some(new BlockData(blockNbt.getString("name"), blockNbt.getCompoundTag("data")))
        }
    }

    if (nbt.hasKey(Settings.namespace + "adapter.filter")) {
      filter = Option(ItemStack.loadItemStackFromNBT(nbt.getCompoundTag(Settings.namespace + "adapter.filter")))
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)

    val blocksNbt = new NBTTagList()
    for (i <- 0 until blocks.length) {
      val blockNbt = new NBTTagCompound()
      blocksData(i) match {
        case Some(data) =>
          blocks(i) match {
            case Some((environment, _)) => environment.save(data.data)
            case _ =>
          }
          blockNbt.setString("name", data.name)
          blockNbt.setCompoundTag("data", data.data)
        case _ =>
      }
      blocksNbt.appendTag(blockNbt)
    }
    nbt.setTag(Settings.namespace + "adapter.blocks", blocksNbt)

    if (filter.isDefined) {
      nbt.setNewCompoundTag(Settings.namespace + "adapter.filter", filter.get.writeToNBT)
    }
  }

  // ----------------------------------------------------------------------- //

  private def isBlockSupported(x: Int, y: Int, z: Int) =
    filter.isDefined && (filter.get.getItem match {
      case block: ItemBlock =>
        block.getBlockID == world.getBlockId(x, y, z) && block.getMetadata(block.getDamage(filter.get)) == world.getBlockMetadata(x, y, z)
      case _ => false
    })

  private class BlockData(val name: String, val data: NBTTagCompound)

}
