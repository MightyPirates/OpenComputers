package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.api.manual.InteractiveImageRenderer
import li.cil.oc.client.Textures
import net.minecraftforge.oredict.OreDictionary

import scala.collection.convert.WrapAsScala._

object OreDictImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val stacks = OreDictionary.getOres(data).filter(stack => stack != null && stack.getItem != null)
    if (stacks != null && stacks.nonEmpty) new ItemStackImageRenderer(stacks.toArray)
    else new TextureImageRenderer(Textures.GUI.ManualMissingItem) with InteractiveImageRenderer {
      override def getTooltip(tooltip: String): String = "oc:gui.Manual.Warning.OreDictMissing"

      override def onMouseClick(mouseX: Int, mouseY: Int): Boolean = false
    }
  }
}
