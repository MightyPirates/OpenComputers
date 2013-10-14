package li.cil.oc

import li.cil.oc.common.item

object Items {
  var multi: item.Delegator = null

  var gpu: item.GraphicsCard = null
  var rs: item.RedstoneCard = null
  var lan: item.NetworkCard = null

  var ram1: item.Memory = null
  var ram2: item.Memory = null
  var ram3: item.Memory = null

  var hdd1: item.HardDiskDrive = null
  var hdd2: item.HardDiskDrive = null
  var hdd3: item.HardDiskDrive = null
  var disk: item.Disk = null

  def init() {
    multi = new item.Delegator(Config.itemId)

    gpu = new item.GraphicsCard(multi)
    rs = new item.RedstoneCard(multi)
    lan = new item.NetworkCard(multi)

    ram1 = new item.Memory(multi, 32)
    ram2 = new item.Memory(multi, 64)
    ram3 = new item.Memory(multi, 128)

    hdd1 = new item.HardDiskDrive(multi, 2)
    hdd2 = new item.HardDiskDrive(multi, 4)
    hdd3 = new item.HardDiskDrive(multi, 8)
    disk = new item.Disk(multi)
  }
}