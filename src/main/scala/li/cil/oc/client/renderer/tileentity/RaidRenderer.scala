package li.cil.oc.client.renderer.tileentity

import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity.Raid
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.tileentity.TileEntity
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.opengl.GL11

object RaidRenderer extends TileEntitySpecialRenderer {
  override def renderTileEntityAt(tileEntity: TileEntity, x: Double, y: Double, z: Double, f: Float) = {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val raid = tileEntity.asInstanceOf[Raid]
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

    RenderState.disableLighting()
    RenderState.makeItBlend()

    GL11.glPushMatrix()

    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    raid.yaw match {
      case ForgeDirection.WEST => GL11.glRotatef(-90, 0, 1, 0)
      case ForgeDirection.NORTH => GL11.glRotatef(180, 0, 1, 0)
      case ForgeDirection.EAST => GL11.glRotatef(90, 0, 1, 0)
      case _ => // No yaw.
    }

    GL11.glTranslated(-0.5, 0.5, 0.505)
    GL11.glScalef(1, -1, 1)

    for (slot <- 0 until raid.getSizeInventory) {

      if (!raid.presence(slot)) {
        bindTexture(Textures.blockRaidFrontError)
        renderSlot(slot)
      }
      else if (System.currentTimeMillis() - raid.lastAccess < 400 && raid.world.rand.nextDouble() > 0.1 && slot == raid.lastAccess % raid.getSizeInventory) {
        bindTexture(Textures.blockRaidFrontActivity)
        renderSlot(slot)
      }
    }

    RenderState.enableLighting()

    GL11.glPopMatrix()
    GL11.glPopAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }

  private val u1 = 2 / 16f
  private val fs = 4 / 16f

  private def renderSlot(slot: Int) {
    val l = u1 + slot * fs
    val h = u1 + (slot + 1) * fs
    val t = Tessellator.instance
    t.startDrawingQuads()
    t.addVertexWithUV(l, 1, 0, l, 1)
    t.addVertexWithUV(h, 1, 0, h, 1)
    t.addVertexWithUV(h, 0, 0, h, 0)
    t.addVertexWithUV(l, 0, 0, l, 0)
    t.draw()
  }
}
