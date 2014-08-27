package li.cil.oc.client.renderer.block

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler
import li.cil.oc.Settings
import li.cil.oc.client.Textures
import li.cil.oc.client.renderer.tileentity.RobotRenderer
import li.cil.oc.common.block._
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.block.Block
import net.minecraft.client.renderer.{RenderBlocks, Tessellator}
import net.minecraft.world.IBlockAccess
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.GL11

object BlockRenderer extends ISimpleBlockRenderingHandler {
  def getRenderId = Settings.blockRenderId

  override def shouldRender3DInInventory() = true

  override def renderInventoryBlock(block: Block, metadata: Int, modelID: Int, renderer: RenderBlocks) {
    RenderState.checkError(getClass.getName + ".renderInventoryBlock: entering (aka: wasntme)")

    GL11.glPushMatrix()
    Delegator.subBlock(block, metadata) match {
      case Some(cable: Cable) =>
        GL11.glScalef(1.6f, 1.6f, 1.6f)
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
        Tessellator.instance.startDrawingQuads()
        Cable.render(block, metadata, renderer)
        Tessellator.instance.draw()

        RenderState.checkError(getClass.getName + ".renderInventoryBlock: cable")
      case Some(proxy@(_: RobotProxy | _: RobotAfterimage)) =>
        GL11.glScalef(1.5f, 1.5f, 1.5f)
        GL11.glTranslatef(-0.5f, -0.45f, -0.5f)
        RobotRenderer.renderChassis()

        RenderState.checkError(getClass.getName + ".renderInventoryBlock: robot")
      case Some(assembler: RobotAssembler) =>
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
        Tessellator.instance.startDrawingQuads()
        Assembler.render(block, metadata, renderer)
        Tessellator.instance.draw()

        RenderState.checkError(getClass.getName + ".renderInventoryBlock: assembler")
      case Some(hologram: Hologram) =>
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
        Tessellator.instance.startDrawingQuads()
        Hologram.render(block, metadata, renderer)
        Tessellator.instance.draw()

        RenderState.checkError(getClass.getName + ".renderInventoryBlock: hologram")
      case _ =>
        block match {
          case delegator: Delegator[_] =>
            delegator.setBlockBoundsForItemRender(metadata)
            delegator.preItemRender(metadata)
          case _ => block.setBlockBoundsForItemRender()
        }
        renderer.setRenderBoundsFromBlock(block)
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
        Tessellator.instance.startDrawingQuads()
        renderFaceYNeg(block, metadata, renderer)
        renderFaceYPos(block, metadata, renderer)
        renderFaceZNeg(block, metadata, renderer)
        renderFaceZPos(block, metadata, renderer)
        renderFaceXNeg(block, metadata, renderer)
        renderFaceXPos(block, metadata, renderer)
        Tessellator.instance.draw()

        RenderState.checkError(getClass.getName + ".renderInventoryBlock: standard block")
    }
    GL11.glPopMatrix()

    RenderState.checkError(getClass.getName + ".renderInventoryBlock: leaving")
  }

  override def renderWorldBlock(world: IBlockAccess, x: Int, y: Int, z: Int, block: Block, modelId: Int, renderer: RenderBlocks) = {
    RenderState.checkError(getClass.getName + ".renderWorldBlock: entering (aka: wasntme)")

    world.getBlockTileEntity(x, y, z) match {
      case cable: tileentity.Cable =>
        Cable.render(world, x, y, z, block, renderer)

        RenderState.checkError(getClass.getName + ".renderWorldBlock: cable")

        true
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

        RenderState.checkError(getClass.getName + ".renderWorldBlock: keyboard")

        result
      case rack: tileentity.ServerRack =>
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
            for (i <- 0 until 4 if rack.isPresent(i).isDefined) {
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
              renderer.setOverrideBlockTexture(Textures.ServerRack.icons(ForgeDirection.NORTH.ordinal))
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

        RenderState.checkError(getClass.getName + ".renderWorldBlock: rack")

        true
      case assembler: tileentity.RobotAssembler =>
        Assembler.render(assembler.block, assembler.getBlockMetadata, x, y, z, renderer)

        RenderState.checkError(getClass.getName + ".renderWorldBlock: assembler")

        true
      case hologram: tileentity.Hologram =>
        Hologram.render(hologram.block, hologram.getBlockMetadata, x, y, z, renderer)

        RenderState.checkError(getClass.getName + ".renderWorldBlock: hologram")

        true
      case _ =>
        val result = renderer.renderStandardBlock(block, x, y, z)

        RenderState.checkError(getClass.getName + ".renderWorldBlock: standard block")

        result
    }
  }

  def renderFaceXPos(block: Block, metadata: Int, renderer: RenderBlocks) {
    Tessellator.instance.setNormal(1, 0, 0)
    renderer.renderFaceXPos(block, 0, 0, 0, renderer.getBlockIconFromSideAndMetadata(block, ForgeDirection.EAST.ordinal, metadata))
  }

  def renderFaceXNeg(block: Block, metadata: Int, renderer: RenderBlocks) {
    Tessellator.instance.setNormal(-1, 0, 0)
    renderer.renderFaceXNeg(block, 0, 0, 0, renderer.getBlockIconFromSideAndMetadata(block, ForgeDirection.WEST.ordinal, metadata))
  }

  def renderFaceYPos(block: Block, metadata: Int, renderer: RenderBlocks) {
    Tessellator.instance.setNormal(0, 1, 0)
    renderer.renderFaceYPos(block, 0, 0, 0, renderer.getBlockIconFromSideAndMetadata(block, ForgeDirection.UP.ordinal, metadata))
  }

  def renderFaceYNeg(block: Block, metadata: Int, renderer: RenderBlocks) {
    Tessellator.instance.setNormal(0, -1, 0)
    renderer.renderFaceYNeg(block, 0, 0, 0, renderer.getBlockIconFromSideAndMetadata(block, ForgeDirection.DOWN.ordinal, metadata))
  }

  def renderFaceZPos(block: Block, metadata: Int, renderer: RenderBlocks) {
    Tessellator.instance.setNormal(0, 0, 1)
    renderer.renderFaceZPos(block, 0, 0, 0, renderer.getBlockIconFromSideAndMetadata(block, ForgeDirection.SOUTH.ordinal, metadata))
  }

  def renderFaceZNeg(block: Block, metadata: Int, renderer: RenderBlocks) {
    Tessellator.instance.setNormal(0, 0, -1)
    renderer.renderFaceZNeg(block, 0, 0, 0, renderer.getBlockIconFromSideAndMetadata(block, ForgeDirection.NORTH.ordinal, metadata))
  }
}
