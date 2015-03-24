package li.cil.oc.client.renderer.block

import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.client.renderer.RenderBlocks
import net.minecraftforge.common.util.ForgeDirection

object Keyboard {
  def render(keyboard: tileentity.Keyboard, x: Int, y: Int, z: Int, block: Block, renderer: RenderBlocks): Boolean = {
    if (keyboard.facing == ForgeDirection.UP || keyboard.facing == ForgeDirection.DOWN) {
      keyboard.yaw match {
        case ForgeDirection.NORTH =>
          renderer.uvRotateTop = 0
          renderer.uvRotateBottom = 0
        case ForgeDirection.SOUTH =>
          renderer.uvRotateTop = 3
          renderer.uvRotateBottom = 3
        case ForgeDirection.WEST =>
          renderer.uvRotateTop = 2
          renderer.uvRotateBottom = 1
        case ForgeDirection.EAST =>
          renderer.uvRotateTop = 1
          renderer.uvRotateBottom = 2
        case _ => throw new AssertionError("Impossible yaw value on keyboard.")
      }
      if (keyboard.facing == ForgeDirection.DOWN) {
        renderer.flipTexture = true
      }
    }
    val result = renderer.renderStandardBlock(block, x, y, z)
    renderer.uvRotateTop = 0
    renderer.uvRotateBottom = 0
    renderer.flipTexture = false
    result
  }
}
