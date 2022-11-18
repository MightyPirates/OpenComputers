package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.api.manual.InteractiveImageRenderer
import li.cil.oc.client.Textures
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.tags._
import net.minecraft.util.ResourceLocation

import scala.collection.mutable
import scala.collection.convert.ImplicitConversionsToScala._

object OreDictImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val desired = new ResourceLocation(data.toLowerCase)
    val stacks = mutable.ArrayBuffer.empty[ItemStack]
    ItemTags.getAllTags.getTag(desired) match {
      case tag: ITag[Item] => stacks ++= tag.getValues.map(new ItemStack(_))
      case _ =>
    }
    if (stacks.isEmpty) {
      BlockTags.getAllTags.getTag(desired) match {
        case tag: ITag[Block] => stacks ++= tag.getValues.map(new ItemStack(_))
        case _ =>
      }
    }
    if (stacks.nonEmpty) new ItemStackImageRenderer(stacks.toArray)
    else new TextureImageRenderer(TextureImageProvider.ManualMissingItem) with InteractiveImageRenderer {
      override def getTooltip(tooltip: String): String = "oc:gui.Manual.Warning.OreDictMissing"

      override def onMouseClick(mouseX: Int, mouseY: Int): Boolean = false
    }
  }
}
