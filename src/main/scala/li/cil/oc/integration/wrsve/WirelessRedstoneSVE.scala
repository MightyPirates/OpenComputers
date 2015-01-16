package li.cil.oc.integration.wrsve

import li.cil.oc.integration.util.WirelessRedstone.WirelessRedstoneSystem
import li.cil.oc.server.component.RedstoneWireless
import li.cil.oc.util.BlockPosition
import net.minecraft.world.World

import scala.language.reflectiveCalls

object WirelessRedstoneSVE extends WirelessRedstoneSystem {
  private val ether = try {
    Option(Class.forName("net.slimevoid.wirelessredstone.ether.RedstoneEther").getMethod("getInstance").invoke(null).asInstanceOf[ {
      def addTransmitter(world: World, x: Int, y: Int, z: Int, frequency: AnyRef): Unit

      def remTransmitter(world: World, x: Int, y: Int, z: Int, frequency: AnyRef): Unit

      def addReceiver(world: World, x: Int, y: Int, z: Int, frequency: AnyRef): Unit

      def remReceiver(world: World, x: Int, y: Int, z: Int, frequency: AnyRef): Unit

      def setTransmitterState(world: World, x: Int, y: Int, z: Int, frequency: AnyRef, state: Boolean): Unit

      def getFreqState(world: World, frequency: AnyRef): Boolean
    }])
  }
  catch {
    case _: Throwable => None
  }

  def removeTransmitter(rs: RedstoneWireless) {
    val blockPos = BlockPosition(rs.redstone)
    ether.foreach(_.remTransmitter(rs.redstone.world, blockPos.x, blockPos.y, blockPos.z, rs.wirelessFrequency.toString))
  }

  def addReceiver(rs: RedstoneWireless) {
    val blockPos = BlockPosition(rs.redstone)
    ether.foreach(_.addReceiver(rs.redstone.world, blockPos.x, blockPos.y, blockPos.z, rs.wirelessFrequency.toString))
  }

  def removeReceiver(rs: RedstoneWireless) {
    val blockPos = BlockPosition(rs.redstone)
    ether.foreach(_.remReceiver(rs.redstone.world, blockPos.x, blockPos.y, blockPos.z, rs.wirelessFrequency.toString))
  }

  def updateOutput(rs: RedstoneWireless) {
    val blockPos = BlockPosition(rs.redstone)
    ether.foreach(_.addTransmitter(rs.redstone.world, blockPos.x, blockPos.y, blockPos.z, rs.wirelessFrequency.toString))
    ether.foreach(_.setTransmitterState(rs.redstone.world, blockPos.x, blockPos.y, blockPos.z, rs.wirelessFrequency.toString, rs.wirelessOutput))
  }

  def getInput(rs: RedstoneWireless) = ether.fold(false)(_.getFreqState(rs.redstone.world, rs.wirelessFrequency.toString))
}
