package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.OpenComputers
import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.client.Textures
import net.minecraftforge.oredict.OreDictionary

import scala.collection.convert.WrapAsScala._

object OreDictImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val stacks = OreDictionary.getOres(data).filter(stack => stack != null && stack.getItem != null)
    if (stacks != null && stacks.nonEmpty) new ItemStackImageRenderer(stacks.toArray)
    else {
      OpenComputers.log.warn(s"Failed looking up OreDictionary entry '$data'.")
      new TextureImageRenderer(Textures.guiManualMissingItem)
    }
  }
}
