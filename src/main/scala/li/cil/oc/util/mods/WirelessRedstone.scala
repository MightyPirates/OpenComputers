package li.cil.oc.util.mods

import li.cil.oc.server.component.RedstoneWireless

object WirelessRedstone {
  def isAvailable = Mods.WirelessRedstoneCBE.isAvailable ||
    Mods.WirelessRedstoneSV.isAvailable

  def addReceiver(rs: RedstoneWireless) {
    WirelessRedstoneCBE.addReceiver(rs)
    WirelessRedstoneSV.addReceiver(rs)
  }

  def removeReceiver(rs: RedstoneWireless) {
    WirelessRedstoneCBE.removeReceiver(rs)
    WirelessRedstoneSV.removeReceiver(rs)
  }

  def removeTransmitter(rs: RedstoneWireless) {
    WirelessRedstoneCBE.removeTransmitter(rs)
    WirelessRedstoneSV.removeTransmitter(rs)
  }

  def updateOutput(rs: RedstoneWireless) {
    WirelessRedstoneCBE.updateOutput(rs)
    WirelessRedstoneSV.updateOutput(rs)
  }

  def getInput(rs: RedstoneWireless) = WirelessRedstoneCBE.getInput(rs) || WirelessRedstoneSV.getInput(rs)
}
