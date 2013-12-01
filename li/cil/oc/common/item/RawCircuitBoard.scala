package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IconRegister

class RawCircuitBoard(val parent: Delegator) extends Delegate {
  val unlocalizedName = "RawCircuitBoard"

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":raw_circuit_board")
  }
}
