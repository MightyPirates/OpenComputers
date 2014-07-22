package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network._
import li.cil.oc.common.tileentity.traits
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.MovingAverage
import li.cil.oc.{Settings, api}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection

import scala.collection.mutable

trait Hub extends traits.Environment with SidedEnvironment {
  override def node: Node = null

  override protected def isConnected = plugs.exists(plug => plug.node.address != null && plug.node.network != null)

  protected val plugs = ForgeDirection.VALID_DIRECTIONS.map(side => new Plug(side))

  val queue = mutable.Queue.empty[(ForgeDirection, Packet)]

  var maxQueueSize = queueBaseSize

  var relayDelay = relayBaseDelay

  var relayAmount = relayBaseAmount

  var relayCooldown = -1

  // 20 cycles
  val packetsPerCycleAvg = new MovingAverage(20)

  // ----------------------------------------------------------------------- //

  protected def queueBaseSize = Settings.get.switchDefaultMaxQueueSize

  protected def queueSizePerUpgrade = Settings.get.switchQueueSizeUpgrade

  protected def relayBaseDelay = Settings.get.switchDefaultRelayDelay

  protected def relayDelayPerUpgrade = Settings.get.switchRelayDelayUpgrade

  protected def relayBaseAmount = Settings.get.switchDefaultRelayAmount

  protected def relayAmountPerUpgrade = Settings.get.switchRelayAmountUpgrade

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
    else {
      relayCooldown = -1
      if (queue.nonEmpty) queue.synchronized {
        packetsPerCycleAvg += queue.size
        for (i <- 0 until math.min(queue.size, relayAmount)) {
          val (sourceSide, packet) = queue.dequeue()
          relayPacket(sourceSide, packet)
        }
        if (queue.nonEmpty) {
          relayCooldown = relayDelay
        }
      }
      else if (world.getTotalWorldTime % relayDelay == 0) {
        packetsPerCycleAvg += 0
      }
    }
  }

  protected def tryEnqueuePacket(sourceSide: ForgeDirection, packet: Packet) = queue.synchronized {
    if (packet.ttl > 0 && queue.size < maxQueueSize) {
      queue += sourceSide -> packet.hop()
      if (relayCooldown < 0) {
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
    if (message.name == "network.message" && !plugs.exists(_.node == message.source)) message.data match {
      case Array(packet: Packet) => tryEnqueuePacket(plug.side, packet)
      case _ =>
    }
  }

  protected def createNode(plug: Plug): Node = api.Network.newNode(plug, Visibility.Network).create()
}
