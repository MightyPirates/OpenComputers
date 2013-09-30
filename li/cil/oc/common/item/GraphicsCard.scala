package li.cil.oc.common.item

import li.cil.oc.Config
import li.cil.oc.api
import li.cil.oc.server.component
import net.minecraft.client.renderer.texture.IconRegister

class GraphicsCard(val parent: Delegator) extends Delegate {
  def unlocalizedName = "GraphicsCard"

  api.Persistable.add("oc.item." + unlocalizedName, () => new component.GraphicsCard())

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourceDomain + ":gpu")
  }
}