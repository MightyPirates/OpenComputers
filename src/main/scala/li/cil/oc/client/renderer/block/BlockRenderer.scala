//package li.cil.oc.client.renderer.block
//
//import net.minecraftforge.fml.client.registry.ISimpleBlockRenderingHandler
//import net.minecraftforge.fml.common.Loader
//import li.cil.oc.Settings
//import li.cil.oc.client.Textures
//import li.cil.oc.client.renderer.tileentity.RobotRenderer
//import li.cil.oc.common.block._
//import li.cil.oc.common.tileentity
//import li.cil.oc.util.RenderState
//import net.minecraft.block.Block
//import net.minecraft.client.renderer.RenderBlocks
//import net.minecraft.client.renderer.Tessellator
//import net.minecraft.util.IIcon
//import net.minecraft.world.IBlockAccess
//import net.minecraft.util.EnumFacing
//import org.lwjgl.opengl.GL11
//
//object BlockRenderer extends ISimpleBlockRenderingHandler {
//  def getRenderId = Settings.blockRenderId
//
//  override def shouldRender3DInInventory(modelID: Int) = true
//
//  override def renderInventoryBlock(block: Block, metadata: Int, modelID: Int, renderer: RenderBlocks) {
//    RenderState.checkError(getClass.getName + ".renderInventoryBlock: entering (aka: wasntme)")
//
//    GL11.glPushMatrix()
//    block match {
//      case cable: Cable =>
//        GL11.glScalef(1.6f, 1.6f, 1.6f)
//        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
//        Tessellator.getInstance.startDrawingQuads()
//        Cable.render(block, metadata, renderer)
//        Tessellator.getInstance.draw()
//
//        RenderState.checkError(getClass.getName + ".renderInventoryBlock: cable")
//      case proxy@(_: RobotProxy | _: RobotAfterimage) =>
//        GL11.glScalef(1.5f, 1.5f, 1.5f)
//        GL11.glTranslatef(-0.5f, -0.45f, -0.5f)
//        RobotRenderer.renderChassis()
//
//        RenderState.checkError(getClass.getName + ".renderInventoryBlock: robot")
//      case assembler: Assembler =>
//        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
//        Tessellator.getInstance.startDrawingQuads()
//        Assembler.render(block, metadata, renderer)
//        Tessellator.getInstance.draw()
//
//        RenderState.checkError(getClass.getName + ".renderInventoryBlock: assembler")
//      case hologram: Hologram =>
//        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
//        Tessellator.getInstance.startDrawingQuads()
//        Hologram.render(block, metadata, renderer)
//        Tessellator.getInstance.draw()
//
//        RenderState.checkError(getClass.getName + ".renderInventoryBlock: hologram")
//      case _ =>
//        block match {
//          case simple: SimpleBlock =>
//            simple.setBlockBoundsForItemRender(metadata)
//            simple.preItemRender(metadata)
//          case _ => block.setBlockBoundsForItemRender()
//        }
//        renderer.setRenderBoundsFromBlock(block)
//        GL11.glTranslatef(-0.5f, -0.5f, -0.5f)
//        Tessellator.getInstance.startDrawingQuads()
//        renderFaceYNeg(block, metadata, renderer)
//        renderFaceYPos(block, metadata, renderer)
//        renderFaceZNeg(block, metadata, renderer)
//        renderFaceZPos(block, metadata, renderer)
//        renderFaceXNeg(block, metadata, renderer)
//        renderFaceXPos(block, metadata, renderer)
//        Tessellator.getInstance.draw()
//
//        RenderState.checkError(getClass.getName + ".renderInventoryBlock: standard block")
//    }
//    GL11.glPopMatrix()
//
//    RenderState.checkError(getClass.getName + ".renderInventoryBlock: leaving")
//  }
//
//  override def renderWorldBlock(world: IBlockAccess, x: Int, y: Int, z: Int, block: Block, modelId: Int, realRenderer: RenderBlocks) = {
//    RenderState.checkError(getClass.getName + ".renderWorldBlock: entering (aka: wasntme)")
//
//    val renderer = patchedRenderer(realRenderer)
//    world.getTileEntity(x, y, z) match {
//      case cable: tileentity.Cable =>
//        Cable.render(world, x, y, z, block, renderer)
//
//        RenderState.checkError(getClass.getName + ".renderWorldBlock: cable")
//
//        true
//      case keyboard: tileentity.Keyboard =>
//        if (keyboard.facing == EnumFacing.UP || keyboard.facing == EnumFacing.DOWN) {
//          keyboard.yaw match {
//            case EnumFacing.NORTH =>
//              renderer.uvRotateTop = 0
//              renderer.uvRotateBottom = 0
//            case EnumFacing.SOUTH =>
//              renderer.uvRotateTop = 3
//              renderer.uvRotateBottom = 3
//            case EnumFacing.WEST =>
//              renderer.uvRotateTop = 2
//              renderer.uvRotateBottom = 1
//            case EnumFacing.EAST =>
//              renderer.uvRotateTop = 1
//              renderer.uvRotateBottom = 2
//            case _ => throw new AssertionError("Impossible yaw value on keyboard.")
//          }
//          if (keyboard.facing == EnumFacing.DOWN) {
//            renderer.flipTexture = true
//          }
//        }
//        val result = renderer.renderStandardBlock(block, x, y, z)
//        renderer.uvRotateTop = 0
//        renderer.uvRotateBottom = 0
//        renderer.flipTexture = false
//
//        RenderState.checkError(getClass.getName + ".renderWorldBlock: keyboard")
//
//        result
//      case rack: tileentity.ServerRack =>
//        val previousRenderAllFaces = renderer.renderAllFaces
//        val u1 = 1 / 16f
//        val u2 = 15 / 16f
//        val v1 = 2 / 16f
//        val v2 = 14 / 16f
//        val fs = 3 / 16f
//
//        // Top and bottom.
//        renderer.renderAllFaces = true
//        renderer.setRenderBounds(0, 0, 0, 1, v1, 1)
//        renderer.renderStandardBlock(block, x, y, z)
//        renderer.setRenderBounds(0, v2, 0, 1, 1, 1)
//        renderer.renderStandardBlock(block, x, y, z)
//
//        // Sides.
//        val front = rack.facing
//        def renderSide(side: EnumFacing, lx: Double, lz: Double, hx: Double, hz: Double) {
//          if (side == front) {
//            for (i <- 0 until 4 if rack.isPresent(i).isDefined) {
//              side match {
//                case EnumFacing.WEST =>
//                  renderer.setRenderBounds(lx, v2 - (i + 1) * fs, lz + u1, u2, v2 - i * fs, hz - u1)
//                case EnumFacing.EAST =>
//                  renderer.setRenderBounds(u1, v2 - (i + 1) * fs, lz + u1, hx, v2 - i * fs, hz - u1)
//                case EnumFacing.NORTH =>
//                  renderer.setRenderBounds(lx + u1, v2 - (i + 1) * fs, lz, hx - u1, v2 - i * fs, u2)
//                case EnumFacing.SOUTH =>
//                  renderer.setRenderBounds(lx + u1, v2 - (i + 1) * fs, u1, hx - u1, v2 - i * fs, hz)
//                case _ =>
//              }
//              renderer.renderStandardBlock(block, x, y, z)
//            }
//          }
//          else {
//            val isBack = front == side.getOpposite
//            if (isBack) {
//              renderer.setOverrideBlockTexture(Textures.ServerRack.icons(EnumFacing.NORTH.ordinal))
//            }
//            renderer.setRenderBounds(lx, v1, lz, hx, v2, hz)
//            renderer.renderStandardBlock(block, x, y, z)
//            renderer.clearOverrideBlockTexture()
//          }
//        }
//
//        renderSide(EnumFacing.WEST, 0, 0, u1, 1)
//        renderSide(EnumFacing.EAST, u2, 0, 1, 1)
//        renderSide(EnumFacing.NORTH, 0, 0, 1, u1)
//        renderSide(EnumFacing.SOUTH, 0, u2, 1, 1)
//
//        renderer.renderAllFaces = previousRenderAllFaces
//
//        RenderState.checkError(getClass.getName + ".renderWorldBlock: rack")
//
//        true
//      case assembler: tileentity.Assembler =>
//        Assembler.render(assembler.block, assembler.getBlockMetadata, x, y, z, renderer)
//
//        RenderState.checkError(getClass.getName + ".renderWorldBlock: assembler")
//
//        true
//      case hologram: tileentity.Hologram =>
//        Hologram.render(hologram.block, hologram.getBlockMetadata, x, y, z, renderer)
//
//        RenderState.checkError(getClass.getName + ".renderWorldBlock: hologram")
//
//        true
//      case _ =>
//        val result = renderer.renderStandardBlock(block, x, y, z)
//
//        RenderState.checkError(getClass.getName + ".renderWorldBlock: standard block")
//
//        result
//    }
//  }
//
//  val isOneSevenTwo = Loader.instance.getMinecraftModContainer.getVersion == "1.7.2"
//
//  def patchedRenderer(renderer: RenderBlocks) = if (isOneSevenTwo) {
//    PatchedRenderBlocks.blockAccess = renderer.blockAccess
//    PatchedRenderBlocks.overrideBlockTexture = renderer.overrideBlockTexture
//    PatchedRenderBlocks.flipTexture = renderer.flipTexture
//    PatchedRenderBlocks.renderAllFaces = renderer.renderAllFaces
//    PatchedRenderBlocks.useInventoryTint = renderer.useInventoryTint
//    PatchedRenderBlocks.renderFromInside = renderer.renderFromInside
//    PatchedRenderBlocks.renderMinX = renderer.renderMinX
//    PatchedRenderBlocks.renderMaxX = renderer.renderMaxX
//    PatchedRenderBlocks.renderMinY = renderer.renderMinY
//    PatchedRenderBlocks.renderMaxY = renderer.renderMaxY
//    PatchedRenderBlocks.renderMinZ = renderer.renderMinZ
//    PatchedRenderBlocks.renderMaxZ = renderer.renderMaxZ
//    PatchedRenderBlocks.lockBlockBounds = renderer.lockBlockBounds
//    PatchedRenderBlocks.partialRenderBounds = renderer.partialRenderBounds
//    PatchedRenderBlocks.uvRotateEast = renderer.uvRotateEast
//    PatchedRenderBlocks.uvRotateWest = renderer.uvRotateWest
//    PatchedRenderBlocks.uvRotateSouth = renderer.uvRotateSouth
//    PatchedRenderBlocks.uvRotateNorth = renderer.uvRotateNorth
//    PatchedRenderBlocks.uvRotateTop = renderer.uvRotateTop
//    PatchedRenderBlocks.uvRotateBottom = renderer.uvRotateBottom
//    PatchedRenderBlocks
//  }
//  else renderer
//
//  object PatchedRenderBlocks extends RenderBlocks {
//    override def renderFaceXPos(block: Block, x: Double, y: Double, z: Double, texture: IIcon) {
//      flipTexture = !flipTexture
//      super.renderFaceXPos(block, x, y, z, texture)
//      flipTexture = !flipTexture
//    }
//
//    override def renderFaceZNeg(block: Block, x: Double, y: Double, z: Double, texture: IIcon) {
//      flipTexture = !flipTexture
//      super.renderFaceZNeg(block, x, y, z, texture)
//      flipTexture = !flipTexture
//    }
//  }
//
//  def renderFaceXPos(block: Block, metadata: Int, renderer: RenderBlocks) {
//    Tessellator.getInstance.setNormal(1, 0, 0)
//    renderer.renderFaceXPos(block, 0, 0, 0, renderer.getBlockIconFromSideAndMetadata(block, EnumFacing.EAST.ordinal, metadata))
//  }
//
//  def renderFaceXNeg(block: Block, metadata: Int, renderer: RenderBlocks) {
//    Tessellator.getInstance.setNormal(-1, 0, 0)
//    renderer.renderFaceXNeg(block, 0, 0, 0, renderer.getBlockIconFromSideAndMetadata(block, EnumFacing.WEST.ordinal, metadata))
//  }
//
//  def renderFaceYPos(block: Block, metadata: Int, renderer: RenderBlocks) {
//    Tessellator.getInstance.setNormal(0, 1, 0)
//    renderer.renderFaceYPos(block, 0, 0, 0, renderer.getBlockIconFromSideAndMetadata(block, EnumFacing.UP.ordinal, metadata))
//  }
//
//  def renderFaceYNeg(block: Block, metadata: Int, renderer: RenderBlocks) {
//    Tessellator.getInstance.setNormal(0, -1, 0)
//    renderer.renderFaceYNeg(block, 0, 0, 0, renderer.getBlockIconFromSideAndMetadata(block, EnumFacing.DOWN.ordinal, metadata))
//  }
//
//  def renderFaceZPos(block: Block, metadata: Int, renderer: RenderBlocks) {
//    Tessellator.getInstance.setNormal(0, 0, 1)
//    renderer.renderFaceZPos(block, 0, 0, 0, renderer.getBlockIconFromSideAndMetadata(block, EnumFacing.SOUTH.ordinal, metadata))
//  }
//
//  def renderFaceZNeg(block: Block, metadata: Int, renderer: RenderBlocks) {
//    Tessellator.getInstance.setNormal(0, 0, -1)
//    renderer.renderFaceZNeg(block, 0, 0, 0, renderer.getBlockIconFromSideAndMetadata(block, EnumFacing.NORTH.ordinal, metadata))
//  }
//}
