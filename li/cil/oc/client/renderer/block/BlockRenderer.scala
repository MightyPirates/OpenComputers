package li.cil.oc.client.renderer.block

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler
import li.cil.oc.client.renderer.tileentity.{CableRenderer, RobotRenderer}
import li.cil.oc.common.block.{RobotProxy, Keyboard, Cable, Delegator}
import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.client.renderer.{Tessellator, RenderBlocks}
import net.minecraft.util.Icon
import net.minecraft.world.IBlockAccess
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.GL11

object BlockRenderer extends ISimpleBlockRenderingHandler {
  var getRenderId = -1

  def shouldRender3DInInventory() = true

  def renderInventoryBlock(block: Block, metadata: Int, modelID: Int, renderer: RenderBlocks) {
    Delegator.subBlock(block, metadata) match {
      case Some(cable: Cable) =>
        GL11.glTranslatef(0, 0.3f, 0)
        GL11.glScalef(1.6f, 1.6f, 1.6f)
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
        CableRenderer.renderCable(ForgeDirection.DOWN.flag)
        GL11.glTranslatef(0.5f, 0.5f, 0.5f)

      case Some(proxy: RobotProxy) =>
        GL11.glTranslatef(0, -0.1f, 0)
        GL11.glScalef(1.5f, 1.5f, 1.5f)
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
        block.setBlockBoundsForItemRender()
        renderer.setRenderBoundsFromBlock(block)
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
        val t = Tessellator.instance
        for (side <- ForgeDirection.VALID_DIRECTIONS) {
          t.startDrawingQuads()
          t.setNormal(side.offsetX, side.offsetY, side.offsetZ)
          renderFace(side.ordinal)(renderer.getBlockIconFromSideAndMetadata(block, side.ordinal, metadata))
          t.draw()
        }
        GL11.glTranslatef(0.5f, 0.5f, 0.5f)
    }
  }

  def renderWorldBlock(world: IBlockAccess, x: Int, y: Int, z: Int, block: Block, modelId: Int, renderer: RenderBlocks) = {
    Delegator.subBlock(block, world.getBlockMetadata(x, y, z)) match {
      case Some(keyboard: Keyboard) =>
        world.getBlockTileEntity(x, y, z) match {
          case teK: tileentity.Keyboard =>
            if (teK.facing == ForgeDirection.UP || teK.facing == ForgeDirection.DOWN) {
              teK.yaw match {
                case ForgeDirection.NORTH =>
                  renderer.uvRotateTop = 0
                  renderer.uvRotateBottom = 0
                case ForgeDirection.EAST =>
                  renderer.uvRotateTop = 1
                  renderer.uvRotateBottom = 2

                case ForgeDirection.SOUTH =>
                  renderer.uvRotateTop = 3
                  renderer.uvRotateBottom = 3
                case ForgeDirection.WEST =>
                  renderer.uvRotateTop = 2
                  renderer.uvRotateBottom = 1
                case _ =>

              }
              if (teK.facing == ForgeDirection.DOWN) {
                renderer.flipTexture = true
              }
            }
            val ret = renderer.renderStandardBlock(block, x, y, z)
            renderer.uvRotateTop = 0
            renderer.uvRotateBottom = 0
            renderer.flipTexture = false
            ret
          case _ =>
            true
        }
      case _ => renderer.renderStandardBlock(block, x, y, z)
    }
  }
}
