package li.cil.oc.common.block

import li.cil.oc.api
import li.cil.oc.common.tileentity
import net.minecraft.world.World

class Keyboard(val parent: SpecialDelegator) extends SpecialDelegate {
  val unlocalizedName = "Keyboard"

  override def hasTileEntity = true

  override def createTileEntity(world: World) = Some(new tileentity.Keyboard(world.isRemote))

  override def update(world: World, x: Int, y: Int, z: Int) =
    world.getBlockTileEntity(x, y, z) match {
      case keyboard: tileentity.Keyboard => api.Network.joinOrCreateNetwork(keyboard)
      case _ =>
    }
}