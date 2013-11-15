package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.world.World

class Robot(val parent: SpecialDelegator) extends SpecialDelegate {
  val unlocalizedName = "Robot"

  // ----------------------------------------------------------------------- //

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Robot)
}
