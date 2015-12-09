package li.cil.oc.client.renderer.markdown.segment.render

import com.google.common.base.Strings
import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.api.manual.InteractiveImageRenderer
import li.cil.oc.client.Textures
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation

object ItemImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val splitIndex = data.lastIndexOf('@')
    val (name, optMeta) = if (splitIndex > 0) data.splitAt(splitIndex) else (data, "")
    val meta = if (Strings.isNullOrEmpty(optMeta)) 0 else Integer.parseInt(optMeta.drop(1))
    Item.itemRegistry.getObject(new ResourceLocation(name)) match {
      case item: Item => new ItemStackImageRenderer(Array(new ItemStack(item, 1, meta)))
      case _ => new TextureImageRenderer(Textures.GUI.ManualMissingItem) with InteractiveImageRenderer {
        override def getTooltip(tooltip: String): String = "oc:gui.Manual.Warning.ItemMissing"

        override def onMouseClick(mouseX: Int, mouseY: Int): Boolean = false
      }
    }
  }
}
