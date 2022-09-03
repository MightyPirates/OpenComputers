package li.cil.oc.client.renderer.block

import codechicken.multipart.TileMultipart
import li.cil.oc.client.Textures
import li.cil.oc.common
import li.cil.oc.integration.Mods
import net.minecraft.block.Block
import net.minecraft.client.renderer.{RenderBlocks, Tessellator}
import net.minecraft.item.{ItemBlock, ItemStack}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.IBlockAccess
import net.minecraftforge.common.util.ForgeDirection

object Cable {
  private val baseSize = 4.0 / 16.0 / 2.0

  private val plugSize = 6.0 / 16.0 / 2.0 - 10e-5

  def render(world: IBlockAccess, x: Int, y: Int, z: Int, block: Block, renderer: RenderBlocks) {
    // Center part.
    val bounds = AxisAlignedBB.getBoundingBox(-baseSize, -baseSize, -baseSize, baseSize, baseSize, baseSize)
    bounds.offset(0.5, 0.5, 0.5)
    renderer.setRenderBounds(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ)
    renderer.renderStandardBlock(block, x, y, z)

    // Connections.
    def renderPart(side: ForgeDirection, size: Double, boundSetter: (AxisAlignedBB, ForgeDirection) => Unit) {
      bounds.setBounds(-size, -size, -size, size, size, size)
      bounds.offset(side.offsetX * 0.25, side.offsetY * 0.25, side.offsetZ * 0.25)
      boundSetter(bounds, side)
      bounds.offset(0.5, 0.5, 0.5)
      renderer.setRenderBounds(bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ)
      renderer.partialRenderBounds = false
      renderer.renderStandardBlock(block, x, y, z)
    }

    val mask = common.block.Cable.neighbors(world, x, y, z)
    for (side <- ForgeDirection.VALID_DIRECTIONS) {
      if ((side.flag & mask) != 0) {
        renderPart(side, baseSize, setConnectedBounds)
      }
      renderer.overrideBlockTexture = Textures.Cable.iconCap
      if ((side.flag & mask) != 0 && !isCable(world, x + side.offsetX, y + side.offsetY, z + side.offsetZ)) {
        utilForTrickingTheRendererIntoUsingUnclampedTextureCoordinates(renderer, 1)
        renderPart(side, plugSize, setPlugBounds)
        utilForTrickingTheRendererIntoUsingUnclampedTextureCoordinates(renderer, 0)
      }
      else if ((side.getOpposite.flag & mask) == mask || mask == 0) {
        renderPart(side, baseSize, setUnconnectedBounds)
      }
      renderer.clearOverrideBlockTexture()
    }
  }

  def render(stack: ItemStack, renderer: RenderBlocks) {
    val block = stack.getItem.asInstanceOf[ItemBlock].field_150939_a
    val metadata = 0

    val previousRenderAllFaces = renderer.renderAllFaces
    renderer.renderAllFaces = true

    renderer.setRenderBounds(0.375, 3 / 16f, 0.375, 0.625, 13 / 16f, 0.625)
    BlockRenderer.renderFaceXNeg(block, metadata, renderer)
    BlockRenderer.renderFaceXPos(block, metadata, renderer)
    BlockRenderer.renderFaceZNeg(block, metadata, renderer)
    BlockRenderer.renderFaceZPos(block, metadata, renderer)

    renderer.overrideBlockTexture = Textures.Cable.iconCap
    renderer.setRenderBounds(0.375, 2 / 16f, 0.375, 0.625, 3 / 16f, 0.625)
    BlockRenderer.renderFaceYNeg(block, metadata, renderer)
    BlockRenderer.renderFaceXNeg(block, metadata, renderer)
    BlockRenderer.renderFaceXPos(block, metadata, renderer)
    BlockRenderer.renderFaceZNeg(block, metadata, renderer)
    BlockRenderer.renderFaceZPos(block, metadata, renderer)
    renderer.setRenderBounds(0.375, 13 / 16f, 0.375, 0.625, 14 / 16f, 0.625)
    BlockRenderer.renderFaceYPos(block, metadata, renderer)
    BlockRenderer.renderFaceXNeg(block, metadata, renderer)
    BlockRenderer.renderFaceXPos(block, metadata, renderer)
    BlockRenderer.renderFaceZNeg(block, metadata, renderer)
    BlockRenderer.renderFaceZPos(block, metadata, renderer)
    renderer.clearOverrideBlockTexture()

    renderer.renderAllFaces = previousRenderAllFaces
  }

  private def isCable(world: IBlockAccess, x: Int, y: Int, z: Int) = {
    val tileEntity = world.getTileEntity(x, y, z)
    tileEntity.isInstanceOf[common.tileentity.Cable] || (Mods.ForgeMultipart.isAvailable && isCableFMP(tileEntity))
  }

  private def isCableFMP(tileEntity: TileEntity) = {
    tileEntity.isInstanceOf[TileMultipart]
  }

  private def utilForTrickingTheRendererIntoUsingUnclampedTextureCoordinates(renderer: RenderBlocks, value: Int) {
    renderer.uvRotateBottom = value
    renderer.uvRotateEast = value
    renderer.uvRotateNorth = value
    renderer.uvRotateSouth = value
    renderer.uvRotateTop = value
    renderer.uvRotateWest = value
  }

  private def setConnectedBounds(bounds: AxisAlignedBB, side: ForgeDirection) {
    bounds.minX = math.min(bounds.minX, side.offsetX * 0.5)
    bounds.maxX = math.max(bounds.maxX, side.offsetX * 0.5)
    bounds.minY = math.min(bounds.minY, side.offsetY * 0.5)
    bounds.maxY = math.max(bounds.maxY, side.offsetY * 0.5)
    bounds.minZ = math.min(bounds.minZ, side.offsetZ * 0.5)
    bounds.maxZ = math.max(bounds.maxZ, side.offsetZ * 0.5)
  }

  private def setPlugBounds(bounds: AxisAlignedBB, side: ForgeDirection) {
    bounds.minX = math.max(math.min(bounds.minX + side.offsetX * 10.0 / 16.0, 7.0 / 16.0), -0.5 - 10e-5)
    bounds.maxX = math.min(math.max(bounds.maxX + side.offsetX * 10.0 / 16.0, -7.0 / 16.0), 0.5 + 10e-5)
    bounds.minY = math.max(math.min(bounds.minY + side.offsetY * 10.0 / 16.0, 7.0 / 16.0), -0.5 - 10e-5)
    bounds.maxY = math.min(math.max(bounds.maxY + side.offsetY * 10.0 / 16.0, -7.0 / 16.0), 0.5 + 10e-5)
    bounds.minZ = math.max(math.min(bounds.minZ + side.offsetZ * 10.0 / 16.0, 7.0 / 16.0), -0.5 - 10e-5)
    bounds.maxZ = math.min(math.max(bounds.maxZ + side.offsetZ * 10.0 / 16.0, -7.0 / 16.0), 0.5 + 10e-5)
  }

  private def setUnconnectedBounds(bounds: AxisAlignedBB, side: ForgeDirection) {
    bounds.minX = math.max(bounds.minX, -plugSize)
    bounds.maxX = math.min(bounds.maxX, plugSize)
    bounds.minY = math.max(bounds.minY, -plugSize)
    bounds.maxY = math.min(bounds.maxY, plugSize)
    bounds.minZ = math.max(bounds.minZ, -plugSize)
    bounds.maxZ = math.min(bounds.maxZ, plugSize)
  }
}
