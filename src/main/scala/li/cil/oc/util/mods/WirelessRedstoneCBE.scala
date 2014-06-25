package li.cil.oc.util.mods

import codechicken.wirelessredstone.core.{WirelessReceivingDevice, WirelessTransmittingDevice}
import li.cil.oc.server.component.RedstoneWireless

import scala.language.reflectiveCalls

object WirelessRedstoneCBE {
  private def ether = try Option(Class.forName("codechicken.wirelessredstone.core.RedstoneEther").getMethod("server").invoke(null).asInstanceOf[ {
    def addReceivingDevice(device: WirelessReceivingDevice)

    def removeReceivingDevice(device: WirelessReceivingDevice)

    def addTransmittingDevice(device: WirelessTransmittingDevice)

    def removeTransmittingDevice(device: WirelessTransmittingDevice)

    def isFreqOn(freq: Int): Boolean
  }])
  catch {
    case _: Throwable => None
  }

  def addTransmitter(rs: RedstoneWireless) {
    if (rs.wirelessOutput && rs.wirelessFrequency > 0) {
      ether.foreach(_.addTransmittingDevice(rs))
    }
  }

  def removeTransmitter(rs: RedstoneWireless) {
    if (rs.wirelessFrequency > 0) {
      ether.foreach(_.removeTransmittingDevice(rs))
    }
  }

  def addReceiver(rs: RedstoneWireless) {
    ether.foreach(ether => {
      ether.addReceivingDevice(rs)
      if (rs.wirelessFrequency > 0) {
        rs.wirelessInput = ether.isFreqOn(rs.wirelessFrequency)
      }
    })
  }

  def removeReceiver(rs: RedstoneWireless) {
    ether.foreach(_.removeReceivingDevice(rs))
  }

  def updateOutput(rs: RedstoneWireless) {
    if (rs.wirelessOutput) {
      addTransmitter(rs)
    }
    else {
      removeTransmitter(rs)
    }
  }

  def getInput(rs: RedstoneWireless) = rs.wirelessInput
}
