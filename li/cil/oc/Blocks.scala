package li.cil.oc

import li.cil.oc.common.block._

object Blocks {
  var blockSimple: Delegator = null
  var blockSpecial: Delegator = null
  var computer: Computer = null
  var screen: Screen = null
  var keyboard: Keyboard = null
  var powersupply: PowerSupply = null
  var powerdistributer: PowerDistributor = null
  def init() {
    // IMPORTANT: the multi block must come first, since the sub blocks will
    // try to register with it. Also, the order the sub blocks are created in
    // must not be changed since that order determines their actual IDs.
    blockSimple = new Delegator(Config.blockId)
    blockSpecial = new SpecialDelegator(Config.blockSpecialId)

    computer = new Computer(blockSimple)
    screen = new Screen(blockSimple)
    keyboard = new Keyboard(blockSpecial)
    powersupply = new PowerSupply(blockSimple)
    powerdistributer = new PowerDistributor(blockSimple)
  }
}