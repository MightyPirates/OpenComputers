package li.cil.oc.client.renderer.markdown.segment.render

import com.google.common.base.Strings
import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.api.manual.InteractiveImageRenderer
import li.cil.oc.client.Textures
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.registries.ForgeRegistries

object BlockImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    val splitIndex = data.lastIndexOf('@')
    val (name, optMeta) = if (splitIndex > 0) data.toLowerCase.splitAt(splitIndex) else (data.toLowerCase, "")
    val meta = if (Strings.isNullOrEmpty(optMeta)) 0 else Integer.parseInt(optMeta.drop(1))
    ForgeRegistries.BLOCKS.getValue(new ResourceLocation(name)) match {
      case block: Block if block.asItem() != null => {
        val stack = new ItemStack(block, 1)
        if (!Strings.isNullOrEmpty(optMeta)) stack.setDamageValue(Integer.parseInt(optMeta.drop(1)))
        new ItemStackImageRenderer(Array(stack))
      }
      case _ => new TextureImageRenderer(TextureImageProvider.ManualMissingItem) with InteractiveImageRenderer {
        override def getTooltip(tooltip: String): String = "oc:gui.Manual.Warning.BlockMissing"

        override def onMouseClick(mouseX: Int, mouseY: Int): Boolean = false
      }
    }
  }
}
