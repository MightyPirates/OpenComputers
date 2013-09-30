package li.cil.oc.common.item

import li.cil.oc.Config
import li.cil.oc.api
import li.cil.oc.server.component
import net.minecraft.client.renderer.texture.IconRegister

class RedstoneCard(val parent: Delegator) extends Delegate {
  def unlocalizedName = "RedstoneCard"

  api.Persistable.add("oc.item." + unlocalizedName, () => new component.RedstoneCard())

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourceDomain + ":rscard")
  }
}
