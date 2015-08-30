package li.cil.oc.client.renderer.block

import net.minecraft.block.Block
import net.minecraft.client.renderer.RenderBlocks

object Transposer {
  def render(block: Block, x: Int, y: Int, z: Int, renderer: RenderBlocks) {
    val previousRenderAllFaces = renderer.renderAllFaces
    renderer.renderAllFaces = true

    // Corners.
    renderer.setRenderBounds(0 / 16f, 0 / 16f, 0 / 16f, 7 / 16f, 7 / 16f, 7 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(0 / 16f, 0 / 16f, 9 / 16f, 7 / 16f, 7 / 16f, 16 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(0 / 16f, 9 / 16f, 0 / 16f, 7 / 16f, 16 / 16f, 7 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(0 / 16f, 9 / 16f, 9 / 16f, 7 / 16f, 16 / 16f, 16 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(9 / 16f, 0 / 16f, 0 / 16f, 16 / 16f, 7 / 16f, 7 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(9 / 16f, 0 / 16f, 9 / 16f, 16 / 16f, 7 / 16f, 16 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(9 / 16f, 9 / 16f, 0 / 16f, 16 / 16f, 16 / 16f, 7 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(9 / 16f, 9 / 16f, 9 / 16f, 16 / 16f, 16 / 16f, 16 / 16f)
    renderer.renderStandardBlock(block, x, y, z)

    // Gaps.
    renderer.setRenderBounds(0 / 16f, 0 / 16f, 7 / 16f, 5 / 16f, 5 / 16f, 9 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(0 / 16f, 11 / 16f, 7 / 16f, 5 / 16f, 16 / 16f, 9 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(11 / 16f, 0 / 16f, 7 / 16f, 16 / 16f, 5 / 16f, 9 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(11 / 16f, 11 / 16f, 7 / 16f, 16 / 16f, 16 / 16f, 9 / 16f)
    renderer.renderStandardBlock(block, x, y, z)

    renderer.setRenderBounds(0 / 16f, 7 / 16f, 0 / 16f, 5 / 16f, 9 / 16f, 5 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(0 / 16f, 7 / 16f, 11 / 16f, 5 / 16f, 9 / 16f, 16 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(11 / 16f, 7 / 16f, 0 / 16f, 16 / 16f, 9 / 16f, 5 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(11 / 16f, 7 / 16f, 11 / 16f, 16 / 16f, 9 / 16f, 16 / 16f)
    renderer.renderStandardBlock(block, x, y, z)

    renderer.setRenderBounds(7 / 16f, 0 / 16f, 0 / 16f, 9 / 16f, 5 / 16f, 5 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(7 / 16f, 0 / 16f, 11 / 16f, 9 / 16f, 5 / 16f, 16 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(7 / 16f, 11 / 16f, 0 / 16f, 9 / 16f, 16 / 16f, 5 / 16f)
    renderer.renderStandardBlock(block, x, y, z)
    renderer.setRenderBounds(7 / 16f, 11 / 16f, 11 / 16f, 9 / 16f, 16 / 16f, 16 / 16f)
    renderer.renderStandardBlock(block, x, y, z)

    // Core.
    renderer.setRenderBounds(1 / 16f, 1 / 16f, 1 / 16f, 15 / 16f, 15 / 16f, 15 / 16f)
    renderer.renderStandardBlock(block, x, y, z)

    renderer.renderAllFaces = previousRenderAllFaces
  }

  def render(block: Block, metadata: Int, renderer: RenderBlocks) {
    // Corners.
    renderer.setRenderBounds(0 / 16f, 0 / 16f, 0 / 16f, 7 / 16f, 7 / 16f, 7 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(0 / 16f, 0 / 16f, 9 / 16f, 7 / 16f, 7 / 16f, 16 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(0 / 16f, 9 / 16f, 0 / 16f, 7 / 16f, 16 / 16f, 7 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(0 / 16f, 9 / 16f, 9 / 16f, 7 / 16f, 16 / 16f, 16 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(9 / 16f, 0 / 16f, 0 / 16f, 16 / 16f, 7 / 16f, 7 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(9 / 16f, 0 / 16f, 9 / 16f, 16 / 16f, 7 / 16f, 16 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(9 / 16f, 9 / 16f, 0 / 16f, 16 / 16f, 16 / 16f, 7 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(9 / 16f, 9 / 16f, 9 / 16f, 16 / 16f, 16 / 16f, 16 / 16f)
    renderAllFaces(block, metadata, renderer)

    // Gaps.
    renderer.setRenderBounds(0 / 16f, 0 / 16f, 7 / 16f, 5 / 16f, 5 / 16f, 9 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(0 / 16f, 11 / 16f, 7 / 16f, 5 / 16f, 16 / 16f, 9 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(11 / 16f, 0 / 16f, 7 / 16f, 16 / 16f, 5 / 16f, 9 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(11 / 16f, 11 / 16f, 7 / 16f, 16 / 16f, 16 / 16f, 9 / 16f)
    renderAllFaces(block, metadata, renderer)

    renderer.setRenderBounds(0 / 16f, 7 / 16f, 0 / 16f, 5 / 16f, 9 / 16f, 5 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(0 / 16f, 7 / 16f, 11 / 16f, 5 / 16f, 9 / 16f, 16 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(11 / 16f, 7 / 16f, 0 / 16f, 16 / 16f, 9 / 16f, 5 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(11 / 16f, 7 / 16f, 11 / 16f, 16 / 16f, 9 / 16f, 16 / 16f)
    renderAllFaces(block, metadata, renderer)

    renderer.setRenderBounds(7 / 16f, 0 / 16f, 0 / 16f, 9 / 16f, 5 / 16f, 5 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(7 / 16f, 0 / 16f, 11 / 16f, 9 / 16f, 5 / 16f, 16 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(7 / 16f, 11 / 16f, 0 / 16f, 9 / 16f, 16 / 16f, 5 / 16f)
    renderAllFaces(block, metadata, renderer)
    renderer.setRenderBounds(7 / 16f, 11 / 16f, 11 / 16f, 9 / 16f, 16 / 16f, 16 / 16f)
    renderAllFaces(block, metadata, renderer)

    // Core.
    renderer.setRenderBounds(1 / 16f, 1 / 16f, 1 / 16f, 15 / 16f, 15 / 16f, 15 / 16f)
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
