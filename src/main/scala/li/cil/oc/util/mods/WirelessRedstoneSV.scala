package li.cil.oc.util.mods

import li.cil.oc.server.component.RedstoneWireless
import net.minecraft.world.World

import scala.language.reflectiveCalls

object WirelessRedstoneSV {
  private val ether = try {
    Option(Class.forName("wirelessredstone.ether.RedstoneEther").getMethod("getInstance").invoke(null).asInstanceOf[ {
      def addTransmitter(world: World, x: Int, y: Int, z: Int, frequency: AnyRef)

      def remTransmitter(world: World, x: Int, y: Int, z: Int, frequency: AnyRef)

      def addReceiver(world: World, x: Int, y: Int, z: Int, frequency: AnyRef)

      def remReceiver(world: World, x: Int, y: Int, z: Int, frequency: AnyRef)

      def setTransmitterState(world: World, x: Int, y: Int, z: Int, frequency: AnyRef, state: Boolean)

      def getFreqState(world: World, frequency: AnyRef): Boolean
    }])
  }
  catch {
    case _: Throwable => None
  }

  def removeTransmitter(rs: RedstoneWireless) {
    val te = rs.owner
    ether.foreach(_.remTransmitter(te.world, te.x, te.y, te.z, rs.wirelessFrequency.toString))
  }

  def addReceiver(rs: RedstoneWireless) {
    val te = rs.owner
    ether.foreach(_.addReceiver(te.world, te.x, te.y, te.z, rs.wirelessFrequency.toString))
  }

  def removeReceiver(rs: RedstoneWireless) {
    val te = rs.owner
    ether.foreach(_.remReceiver(te.world, te.x, te.y, te.z, rs.wirelessFrequency.toString))
  }

  def updateOutput(rs: RedstoneWireless) {
    val te = rs.owner
    ether.foreach(_.addTransmitter(te.world, te.x, te.y, te.z, rs.wirelessFrequency.toString))
    ether.foreach(_.setTransmitterState(te.world, te.x, te.y, te.z, rs.wirelessFrequency.toString, rs.wirelessOutput))
  }

  def getInput(rs: RedstoneWireless) = ether.fold(false)(_.getFreqState(rs.owner.world, rs.wirelessFrequency.toString))
}
