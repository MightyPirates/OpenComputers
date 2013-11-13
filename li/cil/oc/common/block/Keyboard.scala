package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.world.World

class Keyboard(val parent: SpecialDelegator) extends SpecialDelegate {
  val unlocalizedName = "Keyboard"

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Keyboard)
}