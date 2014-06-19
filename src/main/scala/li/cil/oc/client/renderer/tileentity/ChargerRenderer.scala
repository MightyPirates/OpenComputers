package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Charger
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.texture.TextureMap
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.GL11

object ChargerRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val charger = tileEntity.asInstanceOf[Charger]
    if (charger.chargeSpeed > 0) {
      GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

      RenderState.disableLighting()
      RenderState.makeItBlend()

      GL11.glPushMatrix()

      GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

      charger.yaw match {
        case ForgeDirection.WEST => GL11.glRotatef(-90, 0, 1, 0)
        case ForgeDirection.NORTH => GL11.glRotatef(180, 0, 1, 0)
        case ForgeDirection.EAST => GL11.glRotatef(90, 0, 1, 0)
        case _ => // No yaw.
      }

      GL11.glTranslatef(-0.5f, 0.5f, 0.5f)
      GL11.glScalef(1, -1, 1)

      val t = Tessellator.instance

      val frontIcon = Textures.Charger.iconFrontCharging
      bindTexture(TextureMap.locationBlocksTexture)
      t.startDrawingQuads()

      val inverse = 1 - charger.chargeSpeed
      t.addVertexWithUV(0, 1, 0.005, frontIcon.getMinU, frontIcon.getMaxV)
      t.addVertexWithUV(1, 1, 0.005, frontIcon.getMaxU, frontIcon.getMaxV)
      t.addVertexWithUV(1, inverse, 0.005, frontIcon.getMaxU, frontIcon.getInterpolatedV(inverse * 16))
      t.addVertexWithUV(0, inverse, 0.005, frontIcon.getMinU, frontIcon.getInterpolatedV(inverse * 16))

      if (charger.hasPower) {
        val sideIcon = Textures.Charger.iconSideCharging
        t.addVertexWithUV(-0.005, 1, -1, sideIcon.getMinU, sideIcon.getMaxV)
        t.addVertexWithUV(-0.005, 1, 0, sideIcon.getMaxU, sideIcon.getMaxV)
        t.addVertexWithUV(-0.005, 0, 0, sideIcon.getMaxU, sideIcon.getMinV)
        t.addVertexWithUV(-0.005, 0, -1, sideIcon.getMinU, sideIcon.getMinV)

        t.addVertexWithUV(1, 1, -1.005, sideIcon.getMinU, sideIcon.getMaxV)
        t.addVertexWithUV(0, 1, -1.005, sideIcon.getMaxU, sideIcon.getMaxV)
        t.addVertexWithUV(0, 0, -1.005, sideIcon.getMaxU, sideIcon.getMinV)
        t.addVertexWithUV(1, 0, -1.005, sideIcon.getMinU, sideIcon.getMinV)

        t.addVertexWithUV(1.005, 1, 0, sideIcon.getMinU, sideIcon.getMaxV)
        t.addVertexWithUV(1.005, 1, -1, sideIcon.getMaxU, sideIcon.getMaxV)
        t.addVertexWithUV(1.005, 0, -1, sideIcon.getMaxU, sideIcon.getMinV)
        t.addVertexWithUV(1.005, 0, 0, sideIcon.getMinU, sideIcon.getMinV)
      }

      t.draw()

      GL11.glPopMatrix()
      GL11.glPopAttrib()
    }

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
