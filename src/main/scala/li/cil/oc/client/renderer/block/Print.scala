package li.cil.oc.client.renderer.block

import li.cil.oc.api.Items
import li.cil.oc.common.block
import li.cil.oc.common.tileentity
import li.cil.oc.util.ExtendedAABB._
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.util.IIcon

object Print {
  lazy val printBlock = Items.get("print").block().asInstanceOf[block.Print]

  def render(print: tileentity.Print, x: Int, y: Int, z: Int, block: Block, renderer: RenderBlocks): Unit = {
    for (shape <- if (print.state) print.data.stateOn else print.data.stateOff) {
      val bounds = shape.bounds.rotateTowards(print.facing)
      printBlock.colorMultiplierOverride = shape.tint
      printBlock.textureOverride = Option(resolveTexture(shape.texture))
      renderer.setRenderBounds(
        bounds.minX, bounds.minY, bounds.minZ,
        bounds.maxX, bounds.maxY, bounds.maxZ)
      renderer.renderStandardBlock(block, x, y, z)
    }
    printBlock.colorMultiplierOverride = None
    printBlock.textureOverride = None
  }

  def resolveTexture(name: String): IIcon = {
    val icon = Minecraft.getMinecraft.getTextureMapBlocks.getTextureExtry(name)
    if (icon == null) Minecraft.getMinecraft.getTextureManager.getTexture(TextureMap.locationBlocksTexture).asInstanceOf[TextureMap].getAtlasSprite("missingno")
    else icon
  }
}
