package li.cil.oc.common.block

import li.cil.oc.common.tileentity
import net.minecraft.world.World

class AccessPoint extends Switch {
  override protected def customTextures = Array(
    None,
    Some("AccessPointTop"),
    Some("SwitchSide"),
    Some("SwitchSide"),
    Some("SwitchSide"),
    Some("SwitchSide")
  )

  // ----------------------------------------------------------------------- //

  override def createTileEntity(world: World, metadata: Int) = new tileentity.AccessPoint()
}
