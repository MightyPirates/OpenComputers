package li.cil.oc.client.renderer.markdown.segment.render

import li.cil.oc.api.manual.ImageProvider
import li.cil.oc.api.manual.ImageRenderer
import li.cil.oc.api.manual.InteractiveImageRenderer
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation
import net.minecraftforge.registries.ForgeRegistries

object BlockImageProvider extends ImageProvider {
  override def getImage(data: String): ImageRenderer = {
    ForgeRegistries.BLOCKS.getValue(new ResourceLocation(data.toLowerCase)) match {
      case block: Block if block.asItem() != null => new ItemStackImageRenderer(Array(new ItemStack(block)))
      case _ => new TextureImageRenderer(TextureImageProvider.ManualMissingItem) with InteractiveImageRenderer {
        override def getTooltip(tooltip: String): String = "oc:gui.Manual.Warning.BlockMissing"

        override def onMouseClick(mouseX: Int, mouseY: Int): Boolean = false
      }
    }
  }
}
