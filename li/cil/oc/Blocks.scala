package li.cil.oc

import li.cil.oc.common.block.BlockComputer
import li.cil.oc.common.block.BlockScreen

object Blocks {
  var computer: BlockComputer = null
  var screen: BlockScreen = null

  def init() {
    computer = new BlockComputer
    screen = new BlockScreen
  }
}