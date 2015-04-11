package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer
import net.minecraft.util.ResourceLocation

object TextureImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val location = new ResourceLocation(data)
    new TextureImageRenderer(location)
  }
}
