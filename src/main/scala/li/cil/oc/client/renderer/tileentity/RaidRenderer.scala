package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Raid
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import org.lwjgl.opengl.GL11

object RaidRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val raid = tileEntity.asInstanceOf[Raid]
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

    RenderState.disableLighting()
    RenderState.makeItBlend()
    GL11.glColor4f(1, 1, 1, 1)

    GL11.glPushMatrix()

    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    raid.yaw match {
      case EnumFacing.WEST => GL11.glRotatef(-90, 0, 1, 0)
      case EnumFacing.NORTH => GL11.glRotatef(180, 0, 1, 0)
      case EnumFacing.EAST => GL11.glRotatef(90, 0, 1, 0)
      case _ => // No yaw.
    }

    GL11.glTranslated(-0.5, 0.5, 0.505)
    GL11.glScalef(1, -1, 1)

    val t = Tessellator.getInstance
    val r = t.getWorldRenderer

    Textures.Block.bind()
    r.startDrawingQuads()

    {
      val icon = Textures.Block.getSprite(Textures.Block.RaidFrontError)
      for (slot <- 0 until raid.getSizeInventory) {
        if (!raid.presence(slot)) {
          renderSlot(r, slot, icon)
        }
      }
    }

    {
      val icon = Textures.Block.getSprite(Textures.Block.RaidFrontActivity)
      for (slot <- 0 until raid.getSizeInventory) {
        if (System.currentTimeMillis() - raid.lastAccess < 400 && raid.world.rand.nextDouble() > 0.1 && slot == raid.lastAccess % raid.getSizeInventory) {
          renderSlot(r, slot, icon)
        }
      }
    }

    t.draw()

    GL11.glPopMatrix()
    GL11.glPopAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }

  private val u1 = 2 / 16f
  private val fs = 4 / 16f

  private def renderSlot(r: WorldRenderer, slot: Int, icon: TextureAtlasSprite) {
    val l = u1 + slot * fs
    val h = u1 + (slot + 1) * fs
    r.addVertexWithUV(l, 1, 0, icon.getInterpolatedU(l * 16), icon.getMaxV)
    r.addVertexWithUV(h, 1, 0, icon.getInterpolatedU(h * 16), icon.getMaxV)
    r.addVertexWithUV(h, 0, 0, icon.getInterpolatedU(h * 16), icon.getMinV)
    r.addVertexWithUV(l, 0, 0, icon.getInterpolatedU(l * 16), icon.getMinV)
  }
}
