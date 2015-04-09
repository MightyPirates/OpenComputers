package li.cil.oc.client.renderer.markdown.segment.render

import com.google.common.base.Strings
import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

object ItemImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val splitIndex = data.lastIndexOf('@')
    val (name, optMeta) = if (splitIndex > 0) data.splitAt(splitIndex) else (data, "")
    val meta = if (Strings.isNullOrEmpty(optMeta)) 0 else Integer.parseInt(optMeta.drop(1))
    Item.itemRegistry.getObject(name) match {
      case item: Item => new ItemStackImageRenderer(Array(new ItemStack(item, 1, meta)))
      case _ => null
    }
  }
}
