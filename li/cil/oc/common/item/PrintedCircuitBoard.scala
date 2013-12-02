package li.cil.oc.common.item

import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IconRegister

class PrintedCircuitBoard(val parent: Delegator) extends Delegate {
  val unlocalizedName = "PrintedCircuitBoard"

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":circuit_board_printed")
  }
}
