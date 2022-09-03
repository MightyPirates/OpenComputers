package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.api.manual.InteractiveImageRenderer
import li.cil.oc.client.Textures
import net.minecraft.item.ItemStack
import net.minecraft.tags._
import net.minecraft.util.ResourceLocation

import scala.collection.mutable
import scala.collection.convert.ImplicitConversionsToScala._

object OreDictImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val desired = new ResourceLocation(data.toLowerCase)
    val stacks = mutable.ArrayBuffer.empty[ItemStack]
    ItemTags.getWrappers.find(t => desired.equals(t.getName)).foreach {
      stacks ++= _.getValues.map(new ItemStack(_))
    }
    ItemTags.getWrappers.find(t => desired.equals(t.getName)).foreach {
      stacks ++= _.getValues.map(new ItemStack(_))
    }
    if (stacks.nonEmpty) new ItemStackImageRenderer(stacks.toArray)
    else new TextureImageRenderer(TextureImageProvider.ManualMissingItem) with InteractiveImageRenderer {
      override def getTooltip(tooltip: String): String = "oc:gui.Manual.Warning.OreDictMissing"

      override def onMouseClick(mouseX: Int, mouseY: Int): Boolean = false
    }
  }
}
