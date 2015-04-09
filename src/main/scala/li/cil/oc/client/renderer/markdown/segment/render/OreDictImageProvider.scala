package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary

import scala.collection.convert.WrapAsScala._

object OreDictImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val stacks = OreDictionary.getOres(data)
    if (stacks != null && stacks.nonEmpty) new ItemStackImageRenderer(stacks.toArray(new Array[ItemStack](stacks.size())))
    else null
  }
}
