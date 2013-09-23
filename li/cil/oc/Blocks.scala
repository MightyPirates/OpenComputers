package li.cil.oc

import li.cil.oc.common.block._

object Blocks {
  var blockSimple: BlockMulti = null
  var blockSpecial: BlockMulti = null
  var computer: BlockComputer = null
  var screen: BlockScreen = null
  var keyboard: BlockKeyboard = null

  def init() {
    // IMPORTANT: the multi block must come first, since the sub blocks will
    // try to register with it. Also, the order the sub blocks are created in
    // must not be changed since that order determines their actual IDs.
    blockSimple = new BlockMulti(Config.blockId)
    blockSpecial = new BlockSpecialMulti(Config.blockSpecialId)

    computer = new BlockComputer(blockSimple)
    screen = new BlockScreen(blockSimple)
    keyboard = new BlockKeyboard(blockSpecial)
  }
}