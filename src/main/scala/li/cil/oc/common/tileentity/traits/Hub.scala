package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network._
import li.cil.oc.common.tileentity.traits
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{api, Settings}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection
import scala.collection.mutable

trait Hub extends traits.Environment with SidedEnvironment {
  override def node: Node = null

  protected val plugs = ForgeDirection.VALID_DIRECTIONS.map(side => new Plug(side))

  protected val queue = mutable.Queue.empty[(ForgeDirection, Packet)]

  protected val maxQueueSize = 20

  protected var relayCooldown = 0

  protected val relayDelay = 5

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = side != ForgeDirection.UNKNOWN

  override def sidedNode(side: ForgeDirection) = if (side != ForgeDirection.UNKNOWN) plugs(side.ordinal()).node else null

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (relayCooldown > 0) {
      relayCooldown -= 1
    }
    else if (queue.nonEmpty) queue.synchronized {
      val (sourceSide, packet) = queue.dequeue()
      relayPacket(sourceSide, packet)
      relayCooldown = relayDelay
    }
  }

  protected def tryEnqueuePacket(sourceSide: ForgeDirection, packet: Packet) = queue.synchronized {
    if (packet.ttl > 0 && queue.size < maxQueueSize) {
      queue += sourceSide -> packet.hop()
      if (relayCooldown <= 0) {
        relayCooldown = relayDelay
      }
      true
    }
    else false
  }

  protected def relayPacket(sourceSide: ForgeDirection, packet: Packet) {
    for (side <- ForgeDirection.VALID_DIRECTIONS if side != sourceSide) {
      sidedNode(side).sendToReachable("network.message", packet)
    }
  }

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    nbt.getTagList(Settings.namespace + "plugs").iterator[NBTTagCompound].zip(plugs).foreach {
      case (plugNbt, plug) => plug.node.load(plugNbt)
    }
    nbt.getTagList(Settings.namespace + "queue").foreach[NBTTagCompound](tag => {
      val side = ForgeDirection.getOrientation(tag.getInteger("side"))
      val packet = api.Network.newPacket(tag)
      queue += side -> packet
    })
    if (nbt.hasKey(Settings.namespace + "relayCooldown")) {
      relayCooldown = nbt.getInteger(Settings.namespace + "relayCooldown")
    }
  }

  override def writeToNBT(nbt: NBTTagCompound) = queue.synchronized {
    super.writeToNBT(nbt)
    // Side check for Waila (and other mods that may call this client side).
    if (isServer) {
      nbt.setNewTagList(Settings.namespace + "plugs", plugs.map(plug => {
        val plugNbt = new NBTTagCompound()
        plug.node.save(plugNbt)
        plugNbt
      }))
      nbt.setNewTagList(Settings.namespace + "queue", queue.map {
        case (sourceSide, packet) =>
          val tag = new NBTTagCompound()
          tag.setInteger("side", sourceSide.ordinal())
          packet.save(tag)
          tag
      })
      if (relayCooldown > 0) {
        nbt.setInteger(Settings.namespace + "relayCooldown", relayCooldown)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  protected class Plug(val side: ForgeDirection) extends api.network.Environment {
    val node = createNode(this)

    override def onMessage(message: Message) {
      if (isPrimary) {
        onPlugMessage(this, message)
      }
    }

    override def onConnect(node: Node) = onPlugConnect(this, node)

    override def onDisconnect(node: Node) = onPlugDisconnect(this, node)

    def isPrimary = plugs(plugs.indexWhere(_.node.network == node.network)) == this

    def plugsInOtherNetworks = plugs.filter(_.node.network != node.network)
  }

  protected def onPlugConnect(plug: Plug, node: Node) {}

  protected def onPlugDisconnect(plug: Plug, node: Node) {}

  protected def onPlugMessage(plug: Plug, message: Message) {
    if (message.name == "network.message") message.data match {
      case Array(packet: Packet) => tryEnqueuePacket(plug.side, packet)
      case _ =>
    }
  }

  protected def createNode(plug: Plug): Node = api.Network.newNode(plug, Visibility.Network).create()
}
