package li.cil.oc

import li.cil.oc.common.block.BlockComputer

object Blocks {
  var computer: BlockComputer = null

  def init() {
    computer = new BlockComputer()
  }
}