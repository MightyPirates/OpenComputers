package li.cil.oc.client.renderer.block

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler
import li.cil.oc.Settings
import li.cil.oc.client.renderer.tileentity.RobotRenderer
import li.cil.oc.common.block._
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.block.Block
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.client.renderer.Tessellator
import net.minecraft.util.IIcon
import net.minecraft.world.IBlockAccess
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.opengl.GL11

object BlockRenderer extends ISimpleBlockRenderingHandler {
  def getRenderId = Settings.blockRenderId

  override def shouldRender3DInInventory(modelID: Int) = true

  override def renderInventoryBlock(block: Block, metadata: Int, modelID: Int, realRenderer: RenderBlocks) {
    RenderState.checkError(getClass.getName + ".renderInventoryBlock: entering (aka: wasntme)")

    val renderer = patchedRenderer(realRenderer, block)
    GL11.glPushMatrix()
    block match {
      case _: Assembler =>
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
        Tessellator.instance.startDrawingQuads()
        Assembler.render(block, metadata, renderer)
        Tessellator.instance.draw()

        RenderState.checkError(getClass.getName + ".renderInventoryBlock: assembler")
      case _: Cable =>
        GL11.glScalef(1.6f, 1.6f, 1.6f)
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
        Tessellator.instance.startDrawingQuads()
        Cable.render(block, metadata, renderer)
        Tessellator.instance.draw()

        RenderState.checkError(getClass.getName + ".renderInventoryBlock: cable")
      case _: Hologram =>
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
        Tessellator.instance.startDrawingQuads()
        Hologram.render(block, metadata, renderer)
        Tessellator.instance.draw()

        RenderState.checkError(getClass.getName + ".renderInventoryBlock: hologram")
      case _: Printer =>
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
        Tessellator.instance.startDrawingQuads()
        Printer.render(block, metadata, renderer)
        Tessellator.instance.draw()

        RenderState.checkError(getClass.getName + ".renderInventoryBlock: printer")
      case _@(_: RobotProxy | _: RobotAfterimage) =>
        GL11.glScalef(1.5f, 1.5f, 1.5f)
        GL11.glTranslatef(-0.5f, -0.4f, -0.5f)
        RobotRenderer.renderChassis()

        RenderState.checkError(getClass.getName + ".renderInventoryBlock: robot")
      case _: NetSplitter =>
        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
        Tessellator.instance.startDrawingQuads()
        NetSplitter.render(block, metadata, renderer)
        Tessellator.instance.draw()

        RenderState.checkError(getClass.getName + ".renderInventoryBlock: splitter")
      case _ =>
        block match {
          case simple: SimpleBlock =>
            simple.setBlockBoundsForItemRender(metadata)
            simple.preItemRender(metadata)
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

  override def renderWorldBlock(world: IBlockAccess, x: Int, y: Int, z: Int, block: Block, modelId: Int, realRenderer: RenderBlocks) = {
    RenderState.checkError(getClass.getName + ".renderWorldBlock: entering (aka: wasntme)")

    val renderer = patchedRenderer(realRenderer, block)
    world.getTileEntity(x, y, z) match {
      case assembler: tileentity.Assembler =>
        Assembler.render(assembler.block, assembler.getBlockMetadata, x, y, z, renderer)

        RenderState.checkError(getClass.getName + ".renderWorldBlock: assembler")

        true
      case cable: tileentity.Cable =>
        Cable.render(world, x, y, z, block, renderer)

        RenderState.checkError(getClass.getName + ".renderWorldBlock: cable")

        true
      case hologram: tileentity.Hologram =>
        Hologram.render(hologram.block, hologram.getBlockMetadata, x, y, z, renderer)

        RenderState.checkError(getClass.getName + ".renderWorldBlock: hologram")

        true
      case keyboard: tileentity.Keyboard =>
        val result = Keyboard.render(keyboard, x, y, z, block, renderer)

        RenderState.checkError(getClass.getName + ".renderWorldBlock: keyboard")

        result
      case print: tileentity.Print =>
        Print.render(print.data, print.state, print.facing, x, y, z, block, renderer)

        RenderState.checkError(getClass.getName + ".renderWorldBlock: print")

        true
      case printer: tileentity.Printer =>
        Printer.render(block, x, y, z, renderer)

        RenderState.checkError(getClass.getName + ".renderWorldBlock: printer")

        true
      case rack: tileentity.ServerRack =>
        ServerRack.render(rack, x, y, z, block, renderer)

        RenderState.checkError(getClass.getName + ".renderWorldBlock: rack")

        true
      case splitter: tileentity.NetSplitter =>
        NetSplitter.render(ForgeDirection.VALID_DIRECTIONS.map(splitter.isSideOpen), block, x, y, z, renderer)

        RenderState.checkError(getClass.getName + ".renderWorldBlock: splitter")

        true
      case _ =>
        val result = renderer.renderStandardBlock(block, x, y, z)

        RenderState.checkError(getClass.getName + ".renderWorldBlock: standard block")

        result
    }
  }

  private def needsFlipping(block: Block) =
    block.isInstanceOf[Hologram] ||
      block.isInstanceOf[Printer] ||
      block.isInstanceOf[Print] ||
      block.isInstanceOf[NetSplitter]

  // The texture flip this works around only seems to occur for blocks with custom block renderers?
  def patchedRenderer(renderer: RenderBlocks, block: Block) =
    if (needsFlipping(block)) {
      PatchedRenderBlocks.blockAccess = renderer.blockAccess
      PatchedRenderBlocks.overrideBlockTexture = renderer.overrideBlockTexture
      PatchedRenderBlocks.flipTexture = renderer.flipTexture
      PatchedRenderBlocks.renderAllFaces = renderer.renderAllFaces
      PatchedRenderBlocks.useInventoryTint = renderer.useInventoryTint
      PatchedRenderBlocks.renderFromInside = renderer.renderFromInside
      PatchedRenderBlocks.renderMinX = renderer.renderMinX
      PatchedRenderBlocks.renderMaxX = renderer.renderMaxX
      PatchedRenderBlocks.renderMinY = renderer.renderMinY
      PatchedRenderBlocks.renderMaxY = renderer.renderMaxY
      PatchedRenderBlocks.renderMinZ = renderer.renderMinZ
      PatchedRenderBlocks.renderMaxZ = renderer.renderMaxZ
      PatchedRenderBlocks.lockBlockBounds = renderer.lockBlockBounds
      PatchedRenderBlocks.partialRenderBounds = renderer.partialRenderBounds
      PatchedRenderBlocks.uvRotateEast = renderer.uvRotateEast
      PatchedRenderBlocks.uvRotateWest = renderer.uvRotateWest
      PatchedRenderBlocks.uvRotateSouth = renderer.uvRotateSouth
      PatchedRenderBlocks.uvRotateNorth = renderer.uvRotateNorth
      PatchedRenderBlocks.uvRotateTop = renderer.uvRotateTop
      PatchedRenderBlocks.uvRotateBottom = renderer.uvRotateBottom
      PatchedRenderBlocks
    }
    else renderer

  object PatchedRenderBlocks extends RenderBlocks {
    override def renderFaceXPos(block: Block, x: Double, y: Double, z: Double, texture: IIcon) {
      flipTexture = !flipTexture
      super.renderFaceXPos(block, x, y, z, texture)
      flipTexture = !flipTexture
    }

    override def renderFaceZNeg(block: Block, x: Double, y: Double, z: Double, texture: IIcon) {
      flipTexture = !flipTexture
      super.renderFaceZNeg(block, x, y, z, texture)
      flipTexture = !flipTexture
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
