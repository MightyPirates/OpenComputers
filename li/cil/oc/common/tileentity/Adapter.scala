package li.cil.oc.common.tileentity

import li.cil.oc.api.network._
import li.cil.oc.server.driver
import li.cil.oc.{Settings, api}
import net.minecraft.item.{ItemBlock, ItemStack}
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}
import net.minecraftforge.common.ForgeDirection
import scala.{Array, Some}
import net.minecraft.entity.player.EntityPlayer

class Adapter extends Environment with Inventory with Analyzable {
  val node = api.Network.newNode(this, Visibility.Network).create()

  private val blocks = Array.fill[Option[(ManagedEnvironment, api.driver.Block)]](6)(None)

  private val blocksData = Array.fill[Option[BlockData]](6)(None)

  // ----------------------------------------------------------------------- //

  def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = blocks collect {
    case Some(((environment, _))) => environment.node
  }

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
      driver.Registry.blockDriverFor(world, x, y, z) match {
        case Some(newDriver) => blocks(d.ordinal()) match {
          case Some((oldEnvironment, driver)) =>
            if (newDriver != driver || !isBlockSupported(x, y, z)) {
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

    items(0) match {
      case Some(stack) if driver.Registry.blockDriverFor(stack).isEmpty => setInventorySlotContents(0, null)
      case _ =>
    }

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
  }

  // ----------------------------------------------------------------------- //

  def getSizeInventory = 1

  def getInvName = Settings.namespace + "container.Adapter"

  def getInventoryStackLimit = 0

  override def getInventoryStackRequired = 0

  def isItemValidForSlot(i: Int, stack: ItemStack) =
    stack != null && stack.stackSize > 0 && driver.Registry.blockDriverFor(stack).isDefined

  override def onInventoryChanged() {
    super.onInventoryChanged()
    neighborChanged()
  }

  // ----------------------------------------------------------------------- //

  private def isBlockSupported(x: Int, y: Int, z: Int) =
    items(0).isDefined && (items(0).get.getItem match {
      case block: ItemBlock =>
        block.getBlockID == world.getBlockId(x, y, z) && block.getMetadata(block.getDamage(items(0).get)) == world.getBlockMetadata(x, y, z)
      case _ => false
    })

  private class BlockData(val name: String, val data: NBTTagCompound)

}
