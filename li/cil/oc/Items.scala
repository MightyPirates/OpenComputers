package li.cil.oc

import li.cil.oc.common.items.{ItemRedstoneCard, ItemGraphicsCard, ItemHDD}
import li.cil.oc.common.util.ItemComponentCache
import li.cil.oc.server.components.{RedstoneCard, GraphicsCard}

object Items {
  var gpu: ItemGraphicsCard = null
  var hdd: ItemHDD = null
  var rs: ItemRedstoneCard = null

  def init() {
    gpu = new ItemGraphicsCard
    hdd = new ItemHDD
    rs = new ItemRedstoneCard

    ItemComponentCache.register(gpu.itemID, nbt => new GraphicsCard(nbt))
    ItemComponentCache.register(rs.itemID, nbt => new RedstoneCard(nbt))
  }
}