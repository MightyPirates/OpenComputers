package li.cil.oc.common.block

import li.cil.oc.common.GuiType
import li.cil.oc.common.tileentity
import li.cil.oc.integration.util.ItemBlacklist
import net.minecraft.block.state.IBlockState
import net.minecraft.world.World

// TODO Remove in 1.7
class Switch extends SimpleBlock with traits.GUI {
  ItemBlacklist.hide(this)

  override def guiType = GuiType.Switch

  override def hasTileEntity(state: IBlockState) = true

  override def createNewTileEntity(world: World, metadata: Int) = new tileentity.Switch()
}
