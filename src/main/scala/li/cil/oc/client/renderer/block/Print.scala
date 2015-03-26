package li.cil.oc.client.renderer.block

import li.cil.oc.api.Items
import li.cil.oc.common.block
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.util.ExtendedAABB._
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.util.IIcon
import net.minecraftforge.common.util.ForgeDirection

object Print {
  lazy val printBlock = Items.get("print").block().asInstanceOf[block.Print]

  def render(data: PrintData, state: Boolean, facing: ForgeDirection, x: Int, y: Int, z: Int, block: Block, renderer: RenderBlocks): Unit = {
    val shapes = if (state) data.stateOn else data.stateOff
    if (shapes.size == 0) {
      printBlock.textureOverride = Option(resolveTexture("missingno"))
      renderer.setRenderBounds(0, 0, 0, 1, 1, 1)
      renderer.renderStandardBlock(block, x, y, z)
    }
    else for (shape <- shapes) {
      val bounds = shape.bounds.rotateTowards(facing)
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
