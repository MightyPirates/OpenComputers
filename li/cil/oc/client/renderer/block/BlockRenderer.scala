package li.cil.oc.client.renderer.block

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler
import li.cil.oc.client.renderer.tileentity.{CableRenderer, RobotRenderer}
import li.cil.oc.common.block.{RobotAfterimage, RobotProxy, Cable, Delegator}
import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.client.renderer.{Tessellator, RenderBlocks}
import net.minecraft.util.Icon
import net.minecraft.world.IBlockAccess
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.GL11
import li.cil.oc.Blocks

object BlockRenderer extends ISimpleBlockRenderingHandler {
  var getRenderId = -1

  def shouldRender3DInInventory() = true

  def renderInventoryBlock(block: Block, metadata: Int, modelID: Int, renderer: RenderBlocks) {
    GL11.glPushMatrix()
    Delegator.subBlock(block, metadata) match {
      case Some(cable: Cable) =>
        GL11.glScalef(1.6f, 1.6f, 1.6f)
        GL11.glTranslatef(-0.5f, -0.3f, -0.5f)
        CableRenderer.renderCable(ForgeDirection.DOWN.flag)
      case Some(proxy@(_: RobotProxy | _: RobotAfterimage)) =>
        GL11.glScalef(1.5f, 1.5f, 1.5f)
        GL11.glTranslatef(-0.5f, -0.45f, -0.5f)
        RobotRenderer.renderChassis()
      case _ =>
        val renderFace = Array(
          (icon: Icon) => renderer.renderFaceYNeg(block, 0, 0, 0, icon),
          (icon: Icon) => renderer.renderFaceYPos(block, 0, 0, 0, icon),
          (icon: Icon) => renderer.renderFaceZNeg(block, 0, 0, 0, icon),
          (icon: Icon) => renderer.renderFaceZPos(block, 0, 0, 0, icon),
          (icon: Icon) => renderer.renderFaceXNeg(block, 0, 0, 0, icon),
          (icon: Icon) => renderer.renderFaceXPos(block, 0, 0, 0, icon)
        )
        block match {
          case delegator: Delegator[_] =>
            delegator.setBlockBoundsForItemRender(metadata)
            delegator.preItemRender(metadata)
          case _ => block.setBlockBoundsForItemRender()
        }
        renderer.setRenderBoundsFromBlock(block)
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
        val t = Tessellator.instance
        for (side <- ForgeDirection.VALID_DIRECTIONS) {
          t.startDrawingQuads()
          t.setNormal(side.offsetX, side.offsetY, side.offsetZ)
          renderFace(side.ordinal)(renderer.getBlockIconFromSideAndMetadata(block, side.ordinal, metadata))
          t.draw()
        }
    }
    GL11.glPopMatrix()
  }

  def renderWorldBlock(world: IBlockAccess, x: Int, y: Int, z: Int, block: Block, modelId: Int, renderer: RenderBlocks) =
    world.getBlockTileEntity(x, y, z) match {
      case keyboard: tileentity.Keyboard =>
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
      case rack: tileentity.Rack =>
        val previousRenderAllFaces = renderer.renderAllFaces
        val u1 = 1 / 16f
        val u2 = 15 / 16f
        val v1 = 2 / 16f
        val v2 = 14 / 16f
        val fs = 3 / 16f

        // Top and bottom.
        renderer.renderAllFaces = true
        renderer.setRenderBounds(0, 0, 0, 1, v1, 1)
        renderer.renderStandardBlock(block, x, y, z)
        renderer.setRenderBounds(0, v2, 0, 1, 1, 1)
        renderer.renderStandardBlock(block, x, y, z)

        // Sides.
        val front = rack.facing
        def renderSide(side: ForgeDirection, lx: Double, lz: Double, hx: Double, hz: Double) {
          if (side == front) {
            for (i <- 0 until 4 if rack.getStackInSlot(i) != null) {
              side match {
                case ForgeDirection.WEST =>
                  renderer.setRenderBounds(lx, v2 - (i + 1) * fs, lz + u1, u2, v2 - i * fs, hz - u1)
                case ForgeDirection.EAST =>
                  renderer.setRenderBounds(u1, v2 - (i + 1) * fs, lz + u1, hx, v2 - i * fs, hz - u1)
                case ForgeDirection.NORTH =>
                  renderer.setRenderBounds(lx + u1, v2 - (i + 1) * fs, lz, hx - u1, v2 - i * fs, u2)
                case ForgeDirection.SOUTH =>
                  renderer.setRenderBounds(lx + u1, v2 - (i + 1) * fs, u1, hx - u1, v2 - i * fs, hz)
                case _ =>
              }
              renderer.renderStandardBlock(block, x, y, z)
            }
          }
          else {
            val isBack = front == side.getOpposite
            if (isBack) {
              renderer.setOverrideBlockTexture(Blocks.serverRack.icons(ForgeDirection.NORTH.ordinal))
            }
            renderer.setRenderBounds(lx, v1, lz, hx, v2, hz)
            renderer.renderStandardBlock(block, x, y, z)
            renderer.clearOverrideBlockTexture()
          }
        }

        renderSide(ForgeDirection.WEST, 0, 0, u1, 1)
        renderSide(ForgeDirection.EAST, u2, 0, 1, 1)
        renderSide(ForgeDirection.NORTH, 0, 0, 1, u1)
        renderSide(ForgeDirection.SOUTH, 0, u2, 1, 1)

        renderer.renderAllFaces = previousRenderAllFaces
        true
      case _ => renderer.renderStandardBlock(block, x, y, z)
    }
}
