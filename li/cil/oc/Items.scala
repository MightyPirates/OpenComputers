package li.cil.oc

import li.cil.oc.common.item

object Items {
  var multi: item.Delegator = null

  var gpu: item.GraphicsCard = null
  var rs: item.RedstoneCard = null

  var ram32k: item.Memory = null
  var ram64k: item.Memory = null
  var ram128k: item.Memory = null

  var hdd: item.Hdd = null

  def init() {
    multi = new item.Delegator(Config.itemId)

    gpu = new item.GraphicsCard(multi)
    rs = new item.RedstoneCard(multi)

    ram32k = new item.Memory(multi, 32)
    ram64k = new item.Memory(multi, 64)
    ram128k = new item.Memory(multi, 128)

    hdd = new item.Hdd(multi, 2 * 1024 * 1024)
  }
}