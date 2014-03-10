package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network._
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.{api, Settings}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection
import scala.collection.mutable

trait Hub extends Environment with SidedEnvironment with Analyzable {
  val queueSize = 20

  protected val plugs = ForgeDirection.VALID_DIRECTIONS.map(side => new Plug(side))

  protected val queue = mutable.Queue.empty[(ForgeDirection, Packet)]

  // ----------------------------------------------------------------------- //

  override def node = null

  @SideOnly(Side.CLIENT)
  override def canConnect(side: ForgeDirection) = true

  override def sidedNode(side: ForgeDirection) = if (side != ForgeDirection.UNKNOWN) plugs(side.ordinal()).node else null

  // ----------------------------------------------------------------------- //

  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = null

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    super.updateEntity()
    if (world.getWorldTime % 5 == 0 && queue.nonEmpty) {
      val (sourceSide, packet) = queue.dequeue()
      relayPacket(sourceSide, packet)
    }
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
  }

  override def writeToNBT(nbt: NBTTagCompound) {
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
      case Array(packet: Packet) if packet.ttl > 0 && queue.size < queueSize => queue += plug.side -> packet.hop()
      case _ =>
    }
  }

  protected def createNode(plug: Plug): Node = api.Network.newNode(plug, Visibility.Network).create()
}
