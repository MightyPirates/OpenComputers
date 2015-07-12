package li.cil.oc.client.renderer.block

import li.cil.oc.client.Textures
import li.cil.oc.util.RenderState
import net.minecraft.block.Block
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.client.renderer.RenderHelper
import org.lwjgl.opengl.GL11

object Assembler {
  def render(block: Block, metadata: Int, x: Int, y: Int, z: Int, renderer: RenderBlocks) {
    val previousRenderAllFaces = renderer.renderAllFaces
    renderer.renderAllFaces = true

    // Bottom.
    renderer.setRenderBounds(0, 0, 0, 1, 7 / 16f, 1)
    renderer.renderStandardBlock(block, x, y, z)
    // Middle.
    renderer.setRenderBounds(2 / 16f, 7 / 16f, 2 / 16f, 14 / 16f, 9 / 16f, 14 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    // Top.
    renderer.setRenderBounds(0, 9 / 16f, 0, 1, 1, 1)
    renderer.renderStandardBlock(block, x, y, z)

    renderer.renderAllFaces = previousRenderAllFaces
  }

  def render(block: Block, metadata: Int, renderer: RenderBlocks) {
    // Bottom.
    renderer.setRenderBounds(0, 0, 0, 1, 7 / 16f, 1)
    BlockRenderer.renderFaceYPos(block, metadata, renderer)
    BlockRenderer.renderFaceYNeg(block, metadata, renderer)
    BlockRenderer.renderFaceXPos(block, metadata, renderer)
    BlockRenderer.renderFaceXNeg(block, metadata, renderer)
    BlockRenderer.renderFaceZPos(block, metadata, renderer)
    BlockRenderer.renderFaceZNeg(block, metadata, renderer)

    // Middle.
    val previousRenderAllFaces = renderer.renderAllFaces
    renderer.renderAllFaces = true
    renderer.setRenderBounds(2 / 16f, 7 / 16f, 2 / 16f, 14 / 16f, 9 / 16f, 14 / 16f)
    BlockRenderer.renderFaceXPos(block, metadata, renderer)
    BlockRenderer.renderFaceXNeg(block, metadata, renderer)
    BlockRenderer.renderFaceZPos(block, metadata, renderer)
    BlockRenderer.renderFaceZNeg(block, metadata, renderer)
    renderer.renderAllFaces = previousRenderAllFaces

    // Top.
    renderer.setRenderBounds(0, 9 / 16f, 0, 1, 1, 1)
    BlockRenderer.renderFaceYPos(block, metadata, renderer)
    BlockRenderer.renderFaceYNeg(block, metadata, renderer)
    BlockRenderer.renderFaceXPos(block, metadata, renderer)
    BlockRenderer.renderFaceXNeg(block, metadata, renderer)
    BlockRenderer.renderFaceZPos(block, metadata, renderer)
    BlockRenderer.renderFaceZNeg(block, metadata, renderer)

    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
    RenderState.makeItBlend()
    RenderHelper.disableStandardItemLighting()

    renderer.setOverrideBlockTexture(Textures.Assembler.iconTopOn)
    renderer.setRenderBounds(0, 0, 0, 1, 1.05, 1)
    BlockRenderer.renderFaceYPos(block, metadata, renderer)

    renderer.setOverrideBlockTexture(Textures.Assembler.iconSideOn)
    renderer.setRenderBounds(-0.005, 0, 0, 1.005, 1, 1)
    BlockRenderer.renderFaceXPos(block, metadata, renderer)
    BlockRenderer.renderFaceXNeg(block, metadata, renderer)
    renderer.setRenderBounds(0, 0, -0.005, 1, 1, 1.005)
    BlockRenderer.renderFaceZPos(block, metadata, renderer)
    BlockRenderer.renderFaceZNeg(block, metadata, renderer)

    renderer.clearOverrideBlockTexture()
    RenderHelper.enableStandardItemLighting()
    GL11.glPopAttrib()
  }
}
