package li.cil.oc.client.renderer

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.util.{WirelessNetwork, RenderState}
import net.minecraft.client.Minecraft
import net.minecraftforge.client.event.RenderWorldLastEvent
import org.lwjgl.opengl.GL11

object WirelessNetworkDebugRenderer {
  val colors = Array(0xFF0000, 0x00FFFF, 0x00FF00, 0x0000FF, 0xFF00FF, 0xFFFF00, 0xFFFFFF, 0x000000)

  @SubscribeEvent
  def onRenderWorldLastEvent(e: RenderWorldLastEvent) {
    WirelessNetwork.dimensions.get(Minecraft.getMinecraft.theWorld.provider.dimensionId) match {
      case Some(tree) =>
        val mc = Minecraft.getMinecraft
        val player = mc.thePlayer
        val px = player.lastTickPosX + (player.posX - player.lastTickPosX) * e.partialTicks
        val py = player.lastTickPosY + (player.posY - player.lastTickPosY) * e.partialTicks
        val pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * e.partialTicks

        GL11.glPushAttrib(0xFFFFFFFF)
        GL11.glPushMatrix()
        GL11.glTranslated(-px, -py, -pz)
        RenderState.makeItBlend()
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_CULL_FACE)

        def drawBox(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double) {
          GL11.glBegin(GL11.GL_QUADS)
          GL11.glVertex3d(minX, minY, minZ)
          GL11.glVertex3d(minX, minY, maxZ)
          GL11.glVertex3d(maxX, minY, maxZ)
          GL11.glVertex3d(maxX, minY, minZ)
          GL11.glEnd()
          GL11.glBegin(GL11.GL_QUADS)
          GL11.glVertex3d(minX, minY, minZ)
          GL11.glVertex3d(maxX, minY, minZ)
          GL11.glVertex3d(maxX, maxY, minZ)
          GL11.glVertex3d(minX, maxY, minZ)
          GL11.glEnd()
          GL11.glBegin(GL11.GL_QUADS)
          GL11.glVertex3d(maxX, maxY, minZ)
          GL11.glVertex3d(maxX, maxY, maxZ)
          GL11.glVertex3d(minX, maxY, maxZ)
          GL11.glVertex3d(minX, maxY, minZ)
          GL11.glEnd()
          GL11.glBegin(GL11.GL_QUADS)
          GL11.glVertex3d(maxX, maxY, maxZ)
          GL11.glVertex3d(maxX, minY, maxZ)
          GL11.glVertex3d(minX, minY, maxZ)
          GL11.glVertex3d(minX, maxY, maxZ)
          GL11.glEnd()
          GL11.glBegin(GL11.GL_QUADS)
          GL11.glVertex3d(minX, minY, minZ)
          GL11.glVertex3d(minX, maxY, minZ)
          GL11.glVertex3d(minX, maxY, maxZ)
          GL11.glVertex3d(minX, minY, maxZ)
          GL11.glEnd()
          GL11.glBegin(GL11.GL_QUADS)
          GL11.glVertex3d(maxX, minY, minZ)
          GL11.glVertex3d(maxX, minY, maxZ)
          GL11.glVertex3d(maxX, maxY, maxZ)
          GL11.glVertex3d(maxX, maxY, minZ)
          GL11.glEnd()
        }

        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
        for (((min, max), level) <- tree.allBounds) {
          val (minX, minY, minZ) = min
          val (maxX, maxY, maxZ) = max
          val color = colors(level % colors.length)
          GL11.glColor4f(
            ((color >> 16) & 0xFF) / 255f,
            ((color >> 8) & 0xFF) / 255f,
            ((color >> 0) & 0xFF) / 255f,
            0.25f)
          val size = 0.5 - level * 0.05
          drawBox(minX - size, minY - size, minZ - size, maxX + size, maxY + size, maxZ + size)
        }
        GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)

        GL11.glPopMatrix()
        GL11.glPopAttrib()
      case _ =>
    }
  }

}
