//package li.cil.oc.client.renderer.block
//
///* TODO FMP
//import codechicken.multipart.TileMultipart
//*/
//import li.cil.oc.client.Textures
//import li.cil.oc.common
//import li.cil.oc.integration.Mods
//import net.minecraft.block.Block
//import net.minecraft.client.renderer.RenderBlocks
//import net.minecraft.tileentity.TileEntity
//import net.minecraft.util.AxisAlignedBB
//import net.minecraft.world.IBlockAccess
//import net.minecraft.util.EnumFacing
//
//object Cable {
//  private val baseSize = 4.0 / 16.0 / 2.0
//
//  private val plugSize = 6.0 / 16.0 / 2.0 - 10e-5
//
//  def render(world: IBlockAccess, x: Int, y: Int, z: Int, block: Block, renderer: RenderBlocks) {
//    // Center part.
//    val bounds = AxisAlignedBB.fromBounds(-baseSize, -baseSize, -baseSize, baseSize, baseSize, baseSize)
//    bounds.offset(0.5, 0.5, 0.5)
//    renderer.setRenderBounds(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ)
//    renderer.renderStandardBlock(block, x, y, z)
//
//    // Connections.
//    def renderPart(side: EnumFacing, size: Double, boundSetter: (AxisAlignedBB, EnumFacing) => AxisAlignedBB) {
//      val baseBounds = AxisAlignedBB.fromBounds(-size, -size, -size, size, size, size).offset(side.getFrontOffsetX * 0.25, side.getFrontOffsetY * 0.25, side.getFrontOffsetZ * 0.25)
//      val realBounds = boundSetter(baseBounds, side).offset(0.5, 0.5, 0.5)
//      renderer.setRenderBounds(realBounds.minX, realBounds.minY, realBounds.minZ, realBounds.maxX, realBounds.maxY, realBounds.maxZ)
//      renderer.partialRenderBounds = false
//      renderer.renderStandardBlock(block, x, y, z)
//    }
//
//    val mask = common.block.Cable.neighbors(world, x, y, z)
//    for (side <- EnumFacing.values) {
//      if ((side.flag & mask) != 0) {
//        renderPart(side, baseSize, setConnectedBounds)
//      }
//      renderer.overrideBlockTexture = Textures.Cable.iconCap
//      if ((side.flag & mask) != 0 && !isCable(world, x + side.getFrontOffsetX, y + side.getFrontOffsetY, z + side.getFrontOffsetZ)) {
//        utilForTrickingTheRendererIntoUsingUnclampedTextureCoordinates(renderer, 1)
//        renderPart(side, plugSize, setPlugBounds)
//        utilForTrickingTheRendererIntoUsingUnclampedTextureCoordinates(renderer, 0)
//      }
//      else if ((side.getOpposite.flag & mask) == mask || mask == 0) {
//        renderPart(side, baseSize, setUnconnectedBounds)
//      }
//      renderer.clearOverrideBlockTexture()
//    }
//  }
//
//  def render(block: Block, metadata: Int, renderer: RenderBlocks) {
//    val previousRenderAllFaces = renderer.renderAllFaces
//    renderer.renderAllFaces = true
//
//    renderer.setRenderBounds(0.375, 3 / 16f, 0.375, 0.625, 13 / 16f, 0.625)
//    BlockRenderer.renderFaceXNeg(block, metadata, renderer)
//    BlockRenderer.renderFaceXPos(block, metadata, renderer)
//    BlockRenderer.renderFaceZNeg(block, metadata, renderer)
//    BlockRenderer.renderFaceZPos(block, metadata, renderer)
//
//    renderer.overrideBlockTexture = Textures.Cable.iconCap
//    renderer.setRenderBounds(0.375, 2 / 16f, 0.375, 0.625, 3 / 16f, 0.625)
//    BlockRenderer.renderFaceYNeg(block, metadata, renderer)
//    BlockRenderer.renderFaceXNeg(block, metadata, renderer)
//    BlockRenderer.renderFaceXPos(block, metadata, renderer)
//    BlockRenderer.renderFaceZNeg(block, metadata, renderer)
//    BlockRenderer.renderFaceZPos(block, metadata, renderer)
//    renderer.setRenderBounds(0.375, 13 / 16f, 0.375, 0.625, 14 / 16f, 0.625)
//    BlockRenderer.renderFaceYPos(block, metadata, renderer)
//    BlockRenderer.renderFaceXNeg(block, metadata, renderer)
//    BlockRenderer.renderFaceXPos(block, metadata, renderer)
//    BlockRenderer.renderFaceZNeg(block, metadata, renderer)
//    BlockRenderer.renderFaceZPos(block, metadata, renderer)
//    renderer.clearOverrideBlockTexture()
//
//    renderer.renderAllFaces = previousRenderAllFaces
//  }
//
//  private def isCable(world: IBlockAccess, x: Int, y: Int, z: Int) = {
//    val tileEntity = world.getTileEntity(x, y, z)
//    tileEntity.isInstanceOf[common.tileentity.Cable] || (Mods.ForgeMultipart.isAvailable && isCableFMP(tileEntity))
//  }
//
//  private def isCableFMP(tileEntity: TileEntity) = false
//  /* TODO FMP
//    tileEntity.isInstanceOf[TileMultipart]
//  */
//
//  private def utilForTrickingTheRendererIntoUsingUnclampedTextureCoordinates(renderer: RenderBlocks, value: Int) {
//    renderer.uvRotateBottom = value
//    renderer.uvRotateEast = value
//    renderer.uvRotateNorth = value
//    renderer.uvRotateSouth = value
//    renderer.uvRotateTop = value
//    renderer.uvRotateWest = value
//  }
//
//  private def setConnectedBounds(bounds: AxisAlignedBB, side: EnumFacing) = {
//    AxisAlignedBB.fromBounds(
//      math.min(bounds.minX, side.getFrontOffsetX * 0.5),
//      math.max(bounds.maxX, side.getFrontOffsetX * 0.5),
//      math.min(bounds.minY, side.getFrontOffsetY * 0.5),
//      math.max(bounds.maxY, side.getFrontOffsetY * 0.5),
//      math.min(bounds.minZ, side.getFrontOffsetZ * 0.5),
//      math.max(bounds.maxZ, side.getFrontOffsetZ * 0.5)
//    )
//  }
//
//  private def setPlugBounds(bounds: AxisAlignedBB, side: EnumFacing) = {
//    AxisAlignedBB.fromBounds(
//      math.max(math.min(bounds.minX + side.getFrontOffsetX * 10.0 / 16.0, 7.0 / 16.0), -0.5 - 10e-5),
//      math.min(math.max(bounds.maxX + side.getFrontOffsetX * 10.0 / 16.0, -7.0 / 16.0), 0.5 + 10e-5),
//      math.max(math.min(bounds.minY + side.getFrontOffsetY * 10.0 / 16.0, 7.0 / 16.0), -0.5 - 10e-5),
//      math.min(math.max(bounds.maxY + side.getFrontOffsetY * 10.0 / 16.0, -7.0 / 16.0), 0.5 + 10e-5),
//      math.max(math.min(bounds.minZ + side.getFrontOffsetZ * 10.0 / 16.0, 7.0 / 16.0), -0.5 - 10e-5),
//      math.min(math.max(bounds.maxZ + side.getFrontOffsetZ * 10.0 / 16.0, -7.0 / 16.0), 0.5 + 10e-5)
//    )
//  }
//
//  private def setUnconnectedBounds(bounds: AxisAlignedBB, side: EnumFacing) = {
//    AxisAlignedBB.fromBounds(
//      math.max(bounds.minX, -plugSize),
//      math.min(bounds.maxX, plugSize),
//      math.max(bounds.minY, -plugSize),
//      math.min(bounds.maxY, plugSize),
//      math.max(bounds.minZ, -plugSize),
//      math.min(bounds.maxZ, plugSize)
//    )
//  }
//}
