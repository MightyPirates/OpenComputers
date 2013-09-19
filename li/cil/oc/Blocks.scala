package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import li.cil.oc.common.block.BlockComputer
import li.cil.oc.common.block.BlockMulti
import li.cil.oc.common.block.BlockScreen
import li.cil.oc.common.tileentity.TileEntityComputer
import li.cil.oc.common.tileentity.TileEntityScreen

object Blocks {
  var multi: BlockMulti = null
  var computer: BlockComputer = null
  var screen: BlockScreen = null

  def init() {
    // IMPORTANT: the multi block must come first, since the sub blocks will
    // try to register with it. Also, the order the sub blocks are created in
    // must not be changed since that order determines their actual IDs.
    multi = new BlockMulti
    computer = new BlockComputer
    screen = new BlockScreen

    GameRegistry.registerTileEntity(classOf[TileEntityComputer], "oc.computer")
    GameRegistry.registerTileEntity(classOf[TileEntityScreen], "oc.screen")
  }
}