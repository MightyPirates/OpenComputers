package li.cil.oc.common.item

import java.util
import li.cil.oc.Config
import li.cil.oc.util.PackedColor
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class GraphicsCard(val parent: Delegator, val tier: Int) extends Delegate {
  val unlocalizedName = "GraphicsCard" + Array("Basic", "Advanced", "Professional").apply(tier)

  override def addInformation(item: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.add("Used change what's displayed on screens.")
    val (w, h) = Config.screenResolutionsByTier(tier)
    tooltip.add("Maximum resolution: %dx%d".format(w, h))
    tooltip.add("Maximum color depth: %d bit".format(PackedColor.Depth.bits(Config.screenDepthsByTier(tier))))
    tooltip.add("Operations/tick: " + (tier match {
      case 0 => "1/1/4/2/2"
      case 1 => "2/4/8/4/4"
      case 2 => "4/8/16/8/8"
    }))
  }

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourceDomain + ":gpu" + tier)
  }
}