package li.cil.oc

import li.cil.oc.common.items.ItemGraphicsCard
import li.cil.oc.common.items.ItemHDD
import li.cil.oc.common.util.ItemComponentCache
import li.cil.oc.server.components.GraphicsCard

object Items {
  var gpu: ItemGraphicsCard = null
  var hdd: ItemHDD = null

  def init() {
    gpu = new ItemGraphicsCard
    hdd = new ItemHDD

    ItemComponentCache.register(gpu.itemID, nbt => new GraphicsCard(nbt))
  }
}