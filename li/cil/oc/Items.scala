package li.cil.oc

import li.cil.oc.common.item

object Items {
  var multi: item.Delegator = null

  var analyzer: item.Analyzer = null
  var disk: item.Disk = null
  var gpu1, gpu2, gpu3: item.GraphicsCard = null
  var hdd1, hdd2, hdd3: item.HardDiskDrive = null
  var lan: item.NetworkCard = null
  var psu: item.PowerSupply = null
  var ram1, ram2, ram3: item.Memory = null
  var rs: item.RedstoneCard = null
  var wlan: item.WirelessNetworkCard = null

  def init() {
    multi = new item.Delegator(Config.itemId)

    analyzer = new item.Analyzer(multi)
    disk = new item.Disk(multi)
    gpu1 = new item.GraphicsCard(multi, 0)
    gpu2 = new item.GraphicsCard(multi, 1)
    gpu3 = new item.GraphicsCard(multi, 2)
    hdd1 = new item.HardDiskDrive(multi, 2)
    hdd2 = new item.HardDiskDrive(multi, 4)
    hdd3 = new item.HardDiskDrive(multi, 8)
    lan = new item.NetworkCard(multi)
    psu = new item.PowerSupply(multi)
    ram1 = new item.Memory(multi, 32)
    ram2 = new item.Memory(multi, 64)
    ram3 = new item.Memory(multi, 128)
    rs = new item.RedstoneCard(multi)
    wlan = new item.WirelessNetworkCard(multi)
  }
}