package li.cil.oc.client.renderer.block

import net.minecraft.block.Block
import net.minecraft.client.renderer.RenderBlocks

object Printer {
  def render(block: Block, x: Int, y: Int, z: Int, renderer: RenderBlocks) {
    val previousRenderAllFaces = renderer.renderAllFaces
    renderer.renderAllFaces = true

    // Bottom.
    renderer.setRenderBounds(0, 0, 0, 1, 8 / 16f, 1)
    renderer.renderStandardBlock(block, x, y, z)
    // Corners.
    renderer.setRenderBounds(0 / 16f, 8 / 16f, 0 / 16f, 3 / 16f, 16 / 16f, 3 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(13 / 16f, 8 / 16f, 0 / 16f, 16 / 16f, 16 / 16f, 3 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(0 / 16f, 8 / 16f, 13 / 16f, 3 / 16f, 16 / 16f, 16 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(13 / 16f, 8 / 16f, 13 / 16f, 16 / 16f, 16 / 16f, 16 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    // Top.
    renderer.setRenderBounds(3 / 16f, 13 / 16f, 0 / 16f, 13 / 16f, 16 / 16f, 3 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(3 / 16f, 13 / 16f, 13 / 16f, 13 / 16f, 16 / 16f, 16 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(0 / 16f, 13 / 16f, 3 / 16f, 3 / 16f, 16 / 16f, 13 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(13 / 16f, 13 / 16f, 3 / 16f, 16 / 16f, 16 / 16f, 13 / 16f)
    renderer.renderStandardBlock(block, x, y, z)

    renderer.renderAllFaces = previousRenderAllFaces
  }

  def render(block: Block, metadata: Int, renderer: RenderBlocks) {
    // Bottom.
    renderer.setRenderBounds(0, 0, 0, 1, 8 / 16f, 1)
    renderAllFaces(block, metadata, renderer)
    // Corners.
    renderer.setRenderBounds(0 / 16f, 8 / 16f, 0 / 16f, 3 / 16f, 16 / 16f, 3 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(13 / 16f, 8 / 16f, 0 / 16f, 16 / 16f, 16 / 16f, 3 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(0 / 16f, 8 / 16f, 13 / 16f, 3 / 16f, 16 / 16f, 16 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(13 / 16f, 8 / 16f, 13 / 16f, 16 / 16f, 16 / 16f, 16 / 16f)
    renderAllFaces(block, metadata, renderer)
    // Top.
    renderer.setRenderBounds(3 / 16f, 13 / 16f, 0 / 16f, 13 / 16f, 16 / 16f, 3 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(3 / 16f, 13 / 16f, 13 / 16f, 13 / 16f, 16 / 16f, 16 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(0 / 16f, 13 / 16f, 3 / 16f, 3 / 16f, 16 / 16f, 13 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(13 / 16f, 13 / 16f, 3 / 16f, 16 / 16f, 16 / 16f, 13 / 16f)
    renderAllFaces(block, metadata, renderer)
  }

  private def renderAllFaces(block: Block, metadata: Int, renderer: RenderBlocks): Unit = {
    BlockRenderer.renderFaceYPos(block, metadata, renderer)
    BlockRenderer.renderFaceYNeg(block, metadata, renderer)
    BlockRenderer.renderFaceXPos(block, metadata, renderer)
    BlockRenderer.renderFaceXNeg(block, metadata, renderer)
    BlockRenderer.renderFaceZPos(block, metadata, renderer)
    BlockRenderer.renderFaceZNeg(block, metadata, renderer)
  }
}
