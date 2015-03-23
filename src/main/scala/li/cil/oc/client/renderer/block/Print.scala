package li.cil.oc.client.renderer.block

import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.util.IIcon

object Print {
  def render(print: tileentity.Print, x: Int, y: Int, z: Int, block: Block, renderer: RenderBlocks): Unit = {
    for (shape <- if (print.state) print.data.stateOn else print.data.stateOff) {
      renderer.setOverrideBlockTexture(resolveTexture(shape.texture))
      renderer.setRenderBounds(
        shape.bounds.minX, shape.bounds.minY, shape.bounds.minZ,
        shape.bounds.maxX, shape.bounds.maxY, shape.bounds.maxZ)
      renderer.renderStandardBlock(block, x, y, z)
    }
    renderer.clearOverrideBlockTexture()
  }

  private def resolveTexture(name: String): IIcon =
    Option(Minecraft.getMinecraft.getTextureMapBlocks.getTextureExtry(name)).
      getOrElse(Minecraft.getMinecraft.getTextureMapBlocks.getTextureExtry("wool_colored_magenta"))
}
