package li.cil.oc

import li.cil.oc.common.item
import li.cil.oc.common.util.ItemComponentCache
import li.cil.oc.server.component

object Items {
  var multi: item.Multi = null

  var gpu: item.GraphicsCard = null
  var hdd: item.Hdd = null
  var rs: item.RedstoneCard = null

  def init() {
    multi = new item.Multi(Config.itemId)

    gpu = new item.GraphicsCard(multi)
    hdd = new item.Hdd(multi)
    rs = new item.RedstoneCard(multi)

    ItemComponentCache.register(gpu.itemId, nbt => new component.GraphicsCard(nbt))
    ItemComponentCache.register(hdd.itemId, nbt => new component.Disk(nbt))
    ItemComponentCache.register(rs.itemId, nbt => new component.RedstoneCard(nbt))
  }
}