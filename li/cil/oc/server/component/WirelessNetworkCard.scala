package li.cil.oc.server.component

import li.cil.oc.api.network._
import li.cil.oc.util.RTree
import li.cil.oc.{Config, api}
import net.minecraft.block.Block
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.WorldEvent
import scala.Some
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.language.implicitConversions

class WirelessNetworkCard(val owner: TileEntity) extends NetworkCard {
  override val node = api.Network.newNode(this, Visibility.Network).
    withComponent("modem", Visibility.Neighbors).
    withConnector(Config.bufferWireless).
    create()

  var strength = 64.0

  // ----------------------------------------------------------------------- //

  @LuaCallback(value = "getStrength", direct = true)
  def getStrength(context: Context, args: Arguments): Array[AnyRef] = result(strength)

  @LuaCallback(value = "setStrength", direct = true)
  def setStrength(context: Context, args: Arguments): Array[AnyRef] = this.synchronized {
    strength = args.checkDouble(0) max 0
    result(strength)
  }

  override def isWireless(context: Context, args: Arguments): Array[AnyRef] = result(true)

  override def send(context: Context, args: Arguments) = this.synchronized {
    val address = args.checkString(0)
    val port = checkPort(args.checkInteger(1))
    if (strength > 0) {
      checkPower()
      for ((card, distance) <- WirelessNetwork.computeReachableFrom(this)
           if card.node.address == address && card.openPorts.contains(port)) {
        card.node.sendToReachable("computer.signal",
          Seq("modem_message", node.address, Int.box(port), Double.box(distance)) ++ args.drop(2): _*)
      }
    }
    super.send(context, args)
  }

  override def broadcast(context: Context, args: Arguments) = this.synchronized {
    val port = checkPort(args.checkInteger(0))
    if (strength > 0) {
      checkPower()
      for ((card, distance) <- WirelessNetwork.computeReachableFrom(this)
           if card.openPorts.contains(port)) {
        card.node.sendToReachable("computer.signal",
          Seq("modem_message", node.address, Int.box(port), Double.box(distance)) ++ args.drop(2): _*)
      }
    }
    super.broadcast(context, args)
  }

  private def checkPower() {
    val rate = Config.wirelessRangePerPower
    if (rate != 0 && !node.changeBuffer(-strength / rate))
      throw new Exception("not enough energy")
  }

  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      WirelessNetwork.add(this)
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      WirelessNetwork.remove(this)
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    if (nbt.hasKey("oc.net.strength")) {
      strength = nbt.getDouble("oc.net.strength")
    }
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setDouble("oc.net.strength", strength)
  }
}

object WirelessNetwork {
  MinecraftForge.EVENT_BUS.register(this)

  val dimensions = mutable.Map.empty[Int, RTree[WirelessNetworkCard]]

  @ForgeSubscribe
  def onWorldUnload(e: WorldEvent.Unload) {
    if (!e.world.isRemote) {
      dimensions.remove(e.world.provider.dimensionId)
    }
  }

  @ForgeSubscribe
  def onWorldLoad(e: WorldEvent.Load) {
    if (!e.world.isRemote) {
      dimensions.remove(e.world.provider.dimensionId)
    }
  }

  def add(card: WirelessNetworkCard) {
    dimensions.getOrElseUpdate(dimension(card), new RTree[WirelessNetworkCard](4)((card) => (card.owner.xCoord, card.owner.yCoord, card.owner.zCoord))).add(card)
  }

  def remove(card: WirelessNetworkCard) {
    dimensions.get(dimension(card)) match {
      case Some(set) => set.remove(card)
      case _ =>
    }
  }

  def computeReachableFrom(card: WirelessNetworkCard) = {
    dimensions.get(dimension(card)) match {
      case Some(tree) if card.strength > 0 =>
        val range = card.strength + 1
        tree.query(offset(card, -range), offset(card, range)).
          filter(_ != card).
          map(zipWithDistance(card)).
          filter(_._2 <= range * range).
          map { case (c, distance) => (c, Math.sqrt(distance)) }.
          filter(isUnobstructed(card))
      case _ => Iterable.empty[(WirelessNetworkCard, Double)] // Should not be possible.
    }
  }

  private def dimension(card: WirelessNetworkCard) = card.owner.worldObj.provider.dimensionId

  private def offset(card: WirelessNetworkCard, value: Double) =
    (card.owner.xCoord + 0.5 + value, card.owner.yCoord + 0.5 + value, card.owner.zCoord + 0.5 + value)

  private def zipWithDistance(reference: WirelessNetworkCard)(card: WirelessNetworkCard) =
    (card, card.owner.getDistanceFrom(
      reference.owner.xCoord + 0.5,
      reference.owner.yCoord + 0.5,
      reference.owner.zCoord + 0.5))

  private def isUnobstructed(reference: WirelessNetworkCard)(info: (WirelessNetworkCard, Double)): Boolean = {
    val (card, distance) = info
    val gap = distance - 1
    if (gap > 0) {
      // If there's some space between the two wireless network cards we try to
      // figure out if the signal might have been obstructed. We do this by
      // taking a few samples (more the further they are apart) and check if we
      // hit a block. For each block hit we subtract its hardness from the
      // surplus strength left after crossing the distance between the two. If
      // we reach a point where the surplus strength does not suffice we block
      // the message.

      // Unit vector from reference card (sender) to this one (receiver).
      val dx = (card.owner.xCoord - reference.owner.xCoord) / distance
      val dy = (card.owner.yCoord - reference.owner.yCoord) / distance
      val dz = (card.owner.zCoord - reference.owner.zCoord) / distance

      // Accumulated obstructions and number of samples.
      var hardness = 0.0
      val samples = Math.sqrt(gap).toInt

      val world = card.owner.worldObj
      val ox = reference.owner.xCoord - (if (reference.owner.xCoord < 0) 2 else 1)
      val oy = reference.owner.yCoord - (if (reference.owner.yCoord < 0) 2 else 1)
      val oz = reference.owner.zCoord - (if (reference.owner.zCoord < 0) 2 else 1)
      for (i <- 0 until samples) {
        val sample = 0.5 + world.rand.nextDouble() * gap
        // Adding some jitter to avoid only tracking the perfect line between
        // two modems when they are diagonal to each other.
        val x = (ox + world.rand.nextInt(3) + 0.5 + dx * sample).toInt
        val y = (oy + world.rand.nextInt(3) + 0.5 + dy * sample).toInt
        val z = (oz + world.rand.nextInt(3) + 0.5 + dz * sample).toInt
        //world.playAuxSFX(2005, x, y, z, 0)
        Option(Block.blocksList(world.getBlockId(x, y, z))) match {
          case Some(block) =>
            hardness += block.blockHardness
          case _ =>
        }
      }

      // Normalize and scale obstructions:
      hardness *= gap.toDouble / samples.toDouble

      // Remaining signal strength.
      val strength = reference.strength - gap

      // See if we have enough power to overcome the obstructions.
      strength > hardness
    }
    else true
  }
}