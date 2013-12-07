package li.cil.oc.common.item

import cpw.mods.fml.common.Loader
import li.cil.oc.Settings
import net.minecraft.client.renderer.texture.IconRegister

class IronNugget(val parent: Delegator) extends Delegate {
  val unlocalizedName = "IronNugget"

  override val showInItemList = !Loader.isModLoaded("gregtech_addon")

  override def registerIcons(iconRegister: IconRegister) = {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Settings.resourceDomain + ":iron_nugget")
  }
}
