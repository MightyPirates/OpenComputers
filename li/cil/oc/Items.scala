package li.cil.oc

import li.cil.oc.common.items.ItemHDD

object Items {
  var hdd: ItemHDD = null

  def init() {
    hdd = new ItemHDD()
  }
}