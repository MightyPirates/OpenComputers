package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.Settings
import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer
import net.minecraft.util.ResourceLocation

object TextureImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val path = if (data.startsWith("/")) data else "doc/" + data
    val location = new ResourceLocation(Settings.resourceDomain, path)
    new TextureImageRenderer(location)
  }
}
