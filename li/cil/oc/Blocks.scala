package li.cil.oc

import li.cil.oc.common.block._

object Blocks {
  var blockSimple: SimpleDelegator = null
  var blockSpecial: SpecialDelegator = null

  var computer: Computer = null
  var screen1, screen2, screen3: Screen = null
  var keyboard: Keyboard = null

  var powerSupply: PowerConverter = null
  var powerDistributor: PowerDistributor = null

  var adapter: Adapter = null
  var diskDrive: DiskDrive = null
  var cable: Cable = null

  def init() {
    // IMPORTANT: the multi block must come first, since the sub blocks will
    // try to register with it. Also, the order the sub blocks are created in
    // must not be changed since that order determines their actual IDs.
    blockSimple = new SimpleDelegator(Config.blockId)
    blockSpecial = new SpecialDelegator(Config.blockSpecialId)

    computer = new Computer(blockSimple)

    screen1 = new ScreenTier1(blockSimple)
    screen2 = new ScreenTier2(blockSimple)
    screen3 = new ScreenTier3(blockSimple)

    keyboard = new Keyboard(blockSpecial)
    cable = new Cable(blockSpecial)

    powerSupply = new PowerConverter(blockSimple)
    powerDistributor = new PowerDistributor(blockSimple)

    adapter = new Adapter(blockSimple)
    diskDrive = new DiskDrive(blockSimple)
  }
}