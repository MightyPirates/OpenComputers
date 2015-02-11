package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.ServerRack
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import org.lwjgl.opengl.GL11

object ServerRackRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val rack = tileEntity.asInstanceOf[ServerRack]
    GlStateManager.pushAttrib()

    RenderState.disableLighting()
    RenderState.makeItBlend()
    GlStateManager.color(1, 1, 1, 1)

    GL11.glPushMatrix()

    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    rack.yaw match {
      case EnumFacing.WEST => GL11.glRotatef(-90, 0, 1, 0)
      case EnumFacing.NORTH => GL11.glRotatef(180, 0, 1, 0)
      case EnumFacing.EAST => GL11.glRotatef(90, 0, 1, 0)
      case _ => // No yaw.
    }

    GL11.glTranslated(-0.5, 0.5, 0.505 - 0.5f / 16f)
    GL11.glScalef(1, -1, 1)

    if (rack.anyRunning) {
      val t = Tessellator.getInstance
      val r = t.getWorldRenderer

      Textures.Block.bind()
      r.startDrawingQuads()

      val v1 = 2 / 16f
      val fs = 3 / 16f

      {
        val icon = Textures.getSprite(Textures.Block.RackFrontOn)
        for (i <- 0 until 4 if rack.isRunning(i)) {
          val l = v1 + i * fs
          val h = v1 + (i + 1) * fs

          r.addVertexWithUV(0, h, 0, icon.getMinU, icon.getInterpolatedV(h * 16))
          r.addVertexWithUV(1, h, 0, icon.getMaxU, icon.getInterpolatedV(h * 16))
          r.addVertexWithUV(1, l, 0, icon.getMaxU, icon.getInterpolatedV(l * 16))
          r.addVertexWithUV(0, l, 0, icon.getMinU, icon.getInterpolatedV(l * 16))
        }
      }

      {
        val icon = Textures.getSprite(Textures.Block.RackFrontActivity)
        for (i <- 0 until 4 if System.currentTimeMillis() - rack.lastAccess(i) < 400 && rack.world.rand.nextDouble() > 0.1) {
          val l = v1 + i * fs
          val h = v1 + (i + 1) * fs

          r.addVertexWithUV(0, h, 0, icon.getMinU, icon.getInterpolatedV(h * 16))
          r.addVertexWithUV(1, h, 0, icon.getMaxU, icon.getInterpolatedV(h * 16))
          r.addVertexWithUV(1, l, 0, icon.getMaxU, icon.getInterpolatedV(l * 16))
          r.addVertexWithUV(0, l, 0, icon.getMinU, icon.getInterpolatedV(l * 16))
        }
      }

      t.draw()
    }

    RenderState.enableLighting()

    GL11.glPopMatrix()
    GlStateManager.popAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}