package li.cil.oc.client.renderer.tileentity

import net.minecraft.util.ResourceLocation
import li.cil.oc.Config
import net.minecraft.tileentity.TileEntity
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.{RenderBlocks, Tessellator}
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler
import net.minecraft.block.Block
import net.minecraft.world.IBlockAccess


//object KeyboardRenderer extends ISimpleBlockRenderingHandler {
//
//  private val frontOn = new ResourceLocation(Config.resourceDomain, "textures/blocks/computer_front.png")
//
//  override def renderInventoryBlock(block: Block, metadata: Int, modelID: Int, renderer: RenderBlocks) {
//
//  }
//
// override def renderWorldBlock(world: IBlockAccess, x: Int, y: Int, z: Int, block: Block, modelId: Int, renderer: RenderBlocks): Boolean = {
////
////
////    // dont create them here, create them in your constructor and save a reference as a member variable please
////    val tessellator = Tessellator.instance;
////
////
////
////
////    if (this.hasOverrideBlockTexture) {
////      icon = this.overrideBlockTexture
////    }
////
////
////
////    val d5: Double = icon.getMinU.asInstanceOf[Double]
////    val d6: Double = icon.getMinV.asInstanceOf[Double]
////    val d7: Double = icon.getMaxU.asInstanceOf[Double]
////    val d8: Double = icon.getMaxV.asInstanceOf[Double]
////    val d9: Double = icon.getInterpolatedU(7.0D).asInstanceOf[Double]
////    val d10: Double = icon.getInterpolatedV(6.0D).asInstanceOf[Double]
////    val d11: Double = icon.getInterpolatedU(9.0D).asInstanceOf[Double]
////    val d12: Double = icon.getInterpolatedV(8.0D).asInstanceOf[Double]
////    val d13: Double = icon.getInterpolatedU(7.0D).asInstanceOf[Double]
////    val d14: Double = icon.getInterpolatedV(13.0D).asInstanceOf[Double]
////    val d15: Double = icon.getInterpolatedU(9.0D).asInstanceOf[Double]
////    val d16: Double = icon.getInterpolatedV(15.0D).asInstanceOf[Double]
////    tessellator.setBrightness(par1Block.getMixedBrightnessForBlock(renderer.blockAccess, x, y, z))
////    tessellator.setColorOpaque_F(1.0F, 1.0F, 1.0F)
////
////    //+1 so that our "drawing" appears 1 block over our block (to get a better view)
////    // tessellator.startDrawingQuads()
////    //back
////    tessellator.addVertexWithUV(0, 0, 0, 0, 0)
////    tessellator.addVertexWithUV(0, 1, 0, 0, 1)
////    tessellator.addVertexWithUV(1, 1, 0, 1, 1)
////    tessellator.addVertexWithUV(1, 0, 0, 1, 0)
////
////
////    //front
////    tessellator.addVertexWithUV(0, 0, 0.5, 0, 0)
////    tessellator.addVertexWithUV(1, 0, 0.5, 1, 0)
////    tessellator.addVertexWithUV(1, 1, 0.5, 1, 1)
////    tessellator.addVertexWithUV(0, 1, 0.5, 0, 1)
////
////    //top
////    tessellator.addVertexWithUV(0, 1, 0.5, 0, 0)
////    tessellator.addVertexWithUV(1, 1, 0.5, 1, 0)
////    tessellator.addVertexWithUV(1, 1, 0, 1, 1)
////    tessellator.addVertexWithUV(0, 1, 0, 0, 1)
////
////    //bottom
////    tessellator.addVertexWithUV(0, 0, 0, 0, 1)
////    tessellator.addVertexWithUV(1, 0, 0, 1, 1)
////    tessellator.addVertexWithUV(1, 0, 0.5, 1, 0)
////    tessellator.addVertexWithUV(0, 0, 0.5, 0, 0)
////
////
////    //left
////    tessellator.addVertexWithUV(0, 0, 0.5, 0, 0)
////    tessellator.addVertexWithUV(0, 1, 0.5, 1, 0)
////    tessellator.addVertexWithUV(0, 1, 0, 1, 1)
////    tessellator.addVertexWithUV(0, 0, 0, 0, 1)
////
////    //right
////    tessellator.addVertexWithUV(1, 0, 0.5, 0, 0)
////    tessellator.addVertexWithUV(1, 0, 0, 0, 1)
////    tessellator.addVertexWithUV(1, 1, 0, 1, 1)
////    tessellator.addVertexWithUV(1, 1, 0.5, 1, 0)
////
////
////    //tessellator.draw()
////
//    true
//  }
//
//  override def shouldRender3DInInventory = false
//
//  def getRenderId: Int = Config.blockRenderId
//
//
//  def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float) = {
//
//  }
//
//}
