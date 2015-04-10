package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer

object TextureImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = new TextureImageRenderer(data)
}
