package li.cil.oc

import li.cil.oc.common.item
import li.cil.oc.server.component
import li.cil.oc.util.ItemComponentCache

object Items {
  var multi: item.Delegator = null

  var gpu: item.GraphicsCard = null
  var hdd: item.Hdd = null
  var rs: item.RedstoneCard = null

  var ram16k: item.Memory = null
  var ram32k: item.Memory = null
  var ram64k: item.Memory = null

  def init() {
    multi = new item.Delegator(Config.itemId)

    gpu = new item.GraphicsCard(multi)
    hdd = new item.Hdd(multi)
    rs = new item.RedstoneCard(multi)

    ram16k = new item.Memory(multi, 16)
    ram32k = new item.Memory(multi, 32)
    ram64k = new item.Memory(multi, 64)

    ItemComponentCache.register(gpu.itemId, nbt => new component.GraphicsCard(nbt))
    ItemComponentCache.register(hdd.itemId, nbt => new component.Disk(nbt))
    ItemComponentCache.register(rs.itemId, nbt => new component.RedstoneCard(nbt))
  }
}