package li.cil.oc

import li.cil.oc.common.item

object Items {
  var multi: item.Delegator = null

  var rs: item.RedstoneCard = null
  var lan: item.NetworkCard = null
  var wlan: item.WirelessNetworkCard = null
  var psu: item.PowerSupply = null

  var gpu1: item.GraphicsCard = null
  var gpu2: item.GraphicsCard = null
  var gpu3: item.GraphicsCard = null

  var ram1: item.Memory = null
  var ram2: item.Memory = null
  var ram3: item.Memory = null

  var hdd1: item.HardDiskDrive = null
  var hdd2: item.HardDiskDrive = null
  var hdd3: item.HardDiskDrive = null
  var disk: item.Disk = null

  def init() {
    multi = new item.Delegator(Config.itemId)

    rs = new item.RedstoneCard(multi)
    lan = new item.NetworkCard(multi)

    gpu1 = new item.GraphicsCard(multi, 0)
    gpu2 = new item.GraphicsCard(multi, 1)
    gpu3 = new item.GraphicsCard(multi, 2)

    ram1 = new item.Memory(multi, 32)
    ram2 = new item.Memory(multi, 64)
    ram3 = new item.Memory(multi, 128)

    hdd1 = new item.HardDiskDrive(multi, 2)
    hdd2 = new item.HardDiskDrive(multi, 4)
    hdd3 = new item.HardDiskDrive(multi, 8)
    disk = new item.Disk(multi)

    psu = new item.PowerSupply(multi)
    wlan = new item.WirelessNetworkCard(multi)
  }
}