package li.cil.oc.client.renderer.block

import net.minecraft.block.Block
import net.minecraft.client.renderer.RenderBlocks

object Hologram {
  def render(block: Block, metadata: Int, x: Int, y: Int, z: Int, renderer: RenderBlocks) {
    // Center.
    renderer.setRenderBounds(4 / 16f, 0, 4 / 16f, 12 / 16f, 3 / 16f, 12 / 16f)
    renderer.renderStandardBlock(block, x, y, z)

    val previousRenderAllFaces = renderer.renderAllFaces
    renderer.renderAllFaces = true

    // Walls.
    renderer.setRenderBounds(0, 0, 0, 2 / 16f, 7 / 16f, 1)
    renderer.renderStandardBlock(block, x, y, z)

    renderer.setRenderBounds(14 / 16f, 0, 0, 1, 7 / 16f, 1)
    renderer.renderStandardBlock(block, x, y, z)

    renderer.setRenderBounds(2 / 16f, 0, 0, 14 / 16f, 7 / 16f, 2 / 16f)
    renderer.renderStandardBlock(block, x, y, z)

    renderer.setRenderBounds(2 / 16f, 0, 14 / 16f, 14 / 16f, 7 / 16f, 1)
    renderer.renderStandardBlock(block, x, y, z)

    // Inner.
    renderer.setRenderBounds(2 / 16f, 2 / 16f, 2 / 16f, 4 / 16f, 5 / 16f, 14 / 16f)
    renderer.renderStandardBlock(block, x, y, z)

    renderer.setRenderBounds(12 / 16f, 2 / 16f, 2 / 16f, 14 / 16f, 5 / 16f, 14 / 16f)
    renderer.renderStandardBlock(block, x, y, z)

    renderer.setRenderBounds(4 / 16f, 2 / 16f, 2 / 16f, 12 / 16f, 5 / 16f, 4 / 16f)
    renderer.renderStandardBlock(block, x, y, z)

    renderer.setRenderBounds(4 / 16f, 2 / 16f, 12 / 16f, 12 / 16f, 5 / 16f, 14 / 16f)
    renderer.renderStandardBlock(block, x, y, z)

    renderer.renderAllFaces = previousRenderAllFaces
  }

  def render(block: Block, metadata: Int, renderer: RenderBlocks) {
    val previousRenderAllFaces = renderer.renderAllFaces
    renderer.renderAllFaces = true

    // Base and walls.
    renderer.setRenderBounds(4 / 16f, 0, 4 / 16f, 12 / 16f, 3 / 16f, 12 / 16f)
    BlockRenderer.renderFaceYPos(block, metadata, renderer)

    renderer.setRenderBounds(0, 0, 0, 1, 7 / 16f, 1)
    BlockRenderer.renderFaceYNeg(block, metadata, renderer)
    BlockRenderer.renderFaceXPos(block, metadata, renderer)
    BlockRenderer.renderFaceXNeg(block, metadata, renderer)
    BlockRenderer.renderFaceZPos(block, metadata, renderer)
    BlockRenderer.renderFaceZNeg(block, metadata, renderer)

    // Layer 1.
    renderer.setRenderBounds(2 / 16f, 3 / 16f, 2 / 16f, 4 / 16f, 5 / 16f, 14 / 16f)
    BlockRenderer.renderFaceYPos(block, metadata, renderer)
    BlockRenderer.renderFaceXPos(block, metadata, renderer)

    renderer.setRenderBounds(12 / 16f, 3 / 16f, 2 / 16f, 14 / 16f, 5 / 16f, 14 / 16f)
    BlockRenderer.renderFaceYPos(block, metadata, renderer)
    BlockRenderer.renderFaceXNeg(block, metadata, renderer)

    renderer.setRenderBounds(4 / 16f, 3 / 16f, 2 / 16f, 12 / 16f, 5 / 16f, 4 / 16f)
    BlockRenderer.renderFaceYPos(block, metadata, renderer)
    BlockRenderer.renderFaceZPos(block, metadata, renderer)

    renderer.setRenderBounds(4 / 16f, 3 / 16f, 12 / 16f, 12 / 16f, 5 / 16f, 14 / 16f)
    BlockRenderer.renderFaceYPos(block, metadata, renderer)
    BlockRenderer.renderFaceZNeg(block, metadata, renderer)

    // Layer 2.
    renderer.setRenderBounds(0, 3 / 16f, 0, 2 / 16f, 7 / 16f, 1)
    BlockRenderer.renderFaceYPos(block, metadata, renderer)
    BlockRenderer.renderFaceXPos(block, metadata, renderer)

    renderer.setRenderBounds(14 / 16f, 3 / 16f, 0, 1, 7 / 16f, 1)
    BlockRenderer.renderFaceYPos(block, metadata, renderer)
    BlockRenderer.renderFaceXNeg(block, metadata, renderer)

    renderer.setRenderBounds(2 / 16f, 3 / 16f, 0, 14 / 16f, 7 / 16f, 2 / 16f)
    BlockRenderer.renderFaceYPos(block, metadata, renderer)
    BlockRenderer.renderFaceZPos(block, metadata, renderer)

    renderer.setRenderBounds(2 / 16f, 3 / 16f, 14 / 16f, 14 / 16f, 7 / 16f, 1)
    BlockRenderer.renderFaceYPos(block, metadata, renderer)
    BlockRenderer.renderFaceZNeg(block, metadata, renderer)

    renderer.renderAllFaces = previousRenderAllFaces
  }
}
