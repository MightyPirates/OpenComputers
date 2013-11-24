package li.cil.oc.common.item

import java.util
import li.cil.oc.Config
import li.cil.oc.util.{Tooltip, PackedColor}
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack

class GraphicsCard(val parent: Delegator, val tier: Int) extends Delegate {
  val unlocalizedName = "GraphicsCard" + Array("Basic", "Advanced", "Professional").apply(tier)

  override def addInformation(item: ItemStack, player: EntityPlayer, tooltip: util.List[String], advanced: Boolean) {
    tooltip.add("Used to change what's displayed on screens.")
    val (w, h) = Config.screenResolutionsByTier(tier)
    tooltip.add("Maximum resolution: " + Tooltip.format("%dx%d".format(w, h), Tooltip.Color.White))
    tooltip.add("Maximum color depth: " + Tooltip.format("%d bit".format(PackedColor.Depth.bits(Config.screenDepthsByTier(tier))), Tooltip.Color.White))
    tooltip.add("Operations/tick: " + Tooltip.format(tier match {
      case 0 => "1/1/4/2/2"
      case 1 => "2/4/8/4/4"
      case 2 => "4/8/16/8/8"
    }, Tooltip.Color.White))
  }

  override def registerIcons(iconRegister: IconRegister) {
    super.registerIcons(iconRegister)

    icon = iconRegister.registerIcon(Config.resourceDomain + ":gpu" + tier)
  }
}