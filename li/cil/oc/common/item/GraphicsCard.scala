package li.cil.oc.common.item

import li.cil.oc.Config
import net.minecraft.client.renderer.texture.IconRegister

class GraphicsCard(val parent: Delegator, val tier: Int) extends Delegate {
  val unlocalizedName = "GraphicsCard" + Array("Basic", "Advanced", "Professional").apply(tier)

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourceDomain + ":gpu" + tier)
  }
}