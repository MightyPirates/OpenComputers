package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.api.manual.InteractiveImageRenderer
import li.cil.oc.client.Textures
import net.minecraft.util.ResourceLocation

object TextureImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    try new TextureImageRenderer(new ResourceLocation(data)) catch {
      case t: Throwable => new TextureImageRenderer(Textures.GUI.ManualMissingItem) with InteractiveImageRenderer {
        override def getTooltip(tooltip: String): String = "oc:gui.Manual.Warning.ImageMissing"

        override def onMouseClick(mouseX: Int, mouseY: Int): Boolean = false
      }
    }
  }
}
