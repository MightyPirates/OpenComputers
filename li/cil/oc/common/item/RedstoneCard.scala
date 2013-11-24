package li.cil.oc.common.item

import cpw.mods.fml.common.Loader
import java.util
import li.cil.oc.Config
import li.cil.oc.util.Tooltip
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class RedstoneCard(val parent: Delegator) extends Delegate {
  val unlocalizedName = "RedstoneCard"

  override def addInformation(item: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.add("Allows reading and emitting redstone signals")
    tooltip.add("around the computer or robot.")
    if (Loader.isModLoaded("RedLogic")) {
      tooltip.add("RedLogic is " + Tooltip.format("supported", Tooltip.Color.Green) + ".")
    }
    if (Loader.isModLoaded("MineFactoryReloaded")) {
      tooltip.add("RedNet is " + Tooltip.format("supported", Tooltip.Color.Green) + ".")
    }
  }

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourceDomain + ":rscard")
  }
}
