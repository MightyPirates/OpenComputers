package li.cil.oc.client.renderer.tileentity

import li.cil.oc.Settings
import li.cil.oc.common.tileentity.Rack
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.GL11

object RackRenderer extends TileEntitySpecialRenderer {
  private val frontOn = new ResourceLocation(Settings.resourceDomain, "textures/blocks/rack_front_on.png")

  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float) = {
    val rack = tileEntity.asInstanceOf[Rack]
    if (rack.anyRunning) {
      GL11.glPushAttrib(0xFFFFFF)

      RenderState.disableLighting()
      RenderState.makeItBlend()

      GL11.glPushMatrix()

      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

      rack.yaw match {
        case ForgeDirection.WEST => GL11.glRotatef(-90, 0, 1, 0)
        case ForgeDirection.NORTH => GL11.glRotatef(180, 0, 1, 0)
        case ForgeDirection.EAST => GL11.glRotatef(90, 0, 1, 0)
        case _ => // No yaw.
      }

      GL11.glTranslatef(-0.5f, 0.5f, 0.501f)
      GL11.glScalef(1, -1, 1)

      bindTexture(frontOn)

      val v1 = 2 / 16f
      val fs = 3 / 16f
      for (i <- 0 until 4 if rack.isRunning(i)) {
        val l = v1 +i * fs
        val h = v1 + (i + 1) * fs
        val t = Tessellator.instance
        t.startDrawingQuads()
        t.addVertexWithUV(0, h, 0, 0, h)
        t.addVertexWithUV(1, h, 0, 1, h)
        t.addVertexWithUV(1, l, 0, 1, l)
        t.addVertexWithUV(0, l, 0, 0, l)
        t.draw()
      }

      GL11.glPopMatrix()
      GL11.glPopAttrib()
    }
  }
}