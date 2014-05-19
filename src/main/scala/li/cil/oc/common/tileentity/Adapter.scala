package li.cil.oc.common.tileentity

import li.cil.oc.api.network._
import li.cil.oc.{Settings, api}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.{NBTTagList, NBTTagCompound}
import net.minecraftforge.common.ForgeDirection
import scala.collection.mutable
import net.minecraft.world.World

class Adapter extends traits.Environment with Analyzable {
  val node = api.Network.newNode(this, Visibility.Network).create()

  private val blocks = Array.fill[Option[(ManagedEnvironment, api.driver.Block)]](6)(None)

  private val updatingBlocks = mutable.ArrayBuffer.empty[ManagedEnvironment]

  private val blocksData = Array.fill[Option[BlockData]](6)(None)

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float) = blocks collect {
    case Some(((environment, _))) => environment.node
  }

  // ----------------------------------------------------------------------- //

  override def canUpdate = isServer

  override def updateEntity() {
    super.updateEntity()
    if (updatingBlocks.nonEmpty) {
      for (block <- updatingBlocks) {
        block.update()
      }
    }
  }

  def neighborChanged() = if (node != null && node.network != null) {
    for (d <- ForgeDirection.VALID_DIRECTIONS) {
      val (x, y, z) = (this.x + d.offsetX, this.y + d.offsetY, this.z + d.offsetZ)
      world.getBlockTileEntity(x, y, z) match {
        case env: traits.Environment =>
        // Don't provide adaption for our stuffs. This is mostly to avoid
        // cables and other non-functional stuff popping up in the adapter
        // due to having a power interface. Might revisit this at some point,
        // but the only 'downside' is that it can't be used to manipulate
        // inventories, which I actually consider a plus :P
        case _ =>
          Option(api.Driver.driverFor(world, x, y, z)) match {
            case Some(newDriver) => blocks(d.ordinal()) match {
              case Some((oldEnvironment, driver)) =>
                if (newDriver != driver) {
                  // This is... odd. Maybe moved by some other mod?
                  node.disconnect(oldEnvironment.node)
                  val environment = newDriver.createEnvironment(world, x, y, z)
                  blocks(d.ordinal()) = Some((environment, newDriver))
                  updatingBlocks -= oldEnvironment
                  if (environment.canUpdate) {
                    updatingBlocks += environment
                  }
                  blocksData(d.ordinal()) = Some(new BlockData(environment.getClass.getName, new NBTTagCompound()))
                  node.connect(environment.node)
                } // else: the more things change, the more they stay the same.
              case _ =>
                // A challenger appears.
                val environment = newDriver.createEnvironment(world, x, y, z)
                blocks(d.ordinal()) = Some((environment, newDriver))
                if (environment.canUpdate) {
                  updatingBlocks += environment
                }
                blocksData(d.ordinal()) match {
                  case Some(data) if data.name == environment.getClass.getName =>
                    environment.load(data.data)
                  case _ =>
                }
                blocksData(d.ordinal()) = Some(new BlockData(environment.getClass.getName, new NBTTagCompound()))
                node.connect(environment.node)
            }
            case _ => blocks(d.ordinal()) match {
              case Some((environment, driver)) =>
                // We had something there, but it's gone now...
                node.disconnect(environment.node)
                environment.save(blocksData(d.ordinal()).get.data)
                blocks(d.ordinal()) = None
                updatingBlocks -= environment
              case _ => // Nothing before, nothing now.
            }
          }
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      neighborChanged()
      Adapter.promoteNeighbors(world, x, y, z)
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      updatingBlocks.clear()
      Adapter.demoteNeighbors(world, x, y, z)
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

  private class BlockData(val name: String, val data: NBTTagCompound)

}

object Adapter {
  val simpleComponents = mutable.WeakHashMap.empty[SimpleComponentWithVisibility, Int]

  def promoteNeighbors(world: World, x: Int, y: Int, z: Int) {
    simpleComponents.synchronized {
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        world.getBlockTileEntity(x + side.offsetX, y + side.offsetY, z + side.offsetZ) match {
          case component: Environment with SimpleComponentWithVisibility =>
            component.node match {
              case node: Component if node.visibility == Visibility.Neighbors || node.visibility == Visibility.Network =>
                simpleComponents.update(component, simpleComponents.getOrElseUpdate(component, 0) + 1)
                node.setVisibility(Visibility.Network)
              case _ =>
            }
          case _ =>
        }
      }
    }
  }

  def demoteNeighbors(world: World, x: Int, y: Int, z: Int) {
    simpleComponents.synchronized {
      for (side <- ForgeDirection.VALID_DIRECTIONS) {
        world.getBlockTileEntity(x + side.offsetX, y + side.offsetY, z + side.offsetZ) match {
          case component: Environment with SimpleComponentWithVisibility =>
            component.node match {
              case node: Component if node.visibility == Visibility.Network =>
                simpleComponents.update(component, simpleComponents.getOrElseUpdate(component, 1) - 1)
                if (simpleComponents(component) <= 0) {
                  simpleComponents.remove(component)
                  node.setVisibility(Visibility.Neighbors)
                }
              case _ =>
            }
          case _ =>
        }
      }
    }
  }
}