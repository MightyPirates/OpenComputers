package li.cil.oc.client.renderer.markdown.segment.render

import com.google.common.base.Strings
import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.api.manual.InteractiveImageRenderer
import li.cil.oc.client.Textures
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.registries.ForgeRegistries

object ItemImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val splitIndex = data.lastIndexOf('@')
    val (name, optMeta) = if (splitIndex > 0) data.splitAt(splitIndex) else (data, "")
    ForgeRegistries.ITEMS.getValue(new ResourceLocation(name)) match {
      case item: Item => {
        val stack = new ItemStack(item, 1)
        if (!Strings.isNullOrEmpty(optMeta)) stack.setDamageValue(Integer.parseInt(optMeta.drop(1)))
        new ItemStackImageRenderer(Array(stack))
      }
      case _ => new TextureImageRenderer(Textures.GUI.ManualMissingItem) with InteractiveImageRenderer {
        override def getTooltip(tooltip: String): String = "oc:gui.Manual.Warning.ItemMissing"

        override def onMouseClick(mouseX: Int, mouseY: Int): Boolean = false
      }
    }
  }
}
