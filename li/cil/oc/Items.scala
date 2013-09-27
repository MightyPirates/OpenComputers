package li.cil.oc

import li.cil.oc.common.items.{ItemMulti, ItemRedstoneCard, ItemGraphicsCard, ItemHdd}
import li.cil.oc.common.util.ItemComponentCache
import li.cil.oc.server.components.{Disk, RedstoneCard, GraphicsCard}

object Items {
  var multi: ItemMulti = null

  var gpu: ItemGraphicsCard = null
  var hdd: ItemHdd = null
  var rs: ItemRedstoneCard = null

  def init() {
    multi = new ItemMulti(Config.itemId)

    gpu = new ItemGraphicsCard(multi)
    hdd = new ItemHdd(multi)
    rs = new ItemRedstoneCard(multi)

    ItemComponentCache.register(gpu.itemId, nbt => new GraphicsCard(nbt))
    ItemComponentCache.register(hdd.itemId, nbt => new Disk(nbt))
    ItemComponentCache.register(rs.itemId, nbt => new RedstoneCard(nbt))
  }
}