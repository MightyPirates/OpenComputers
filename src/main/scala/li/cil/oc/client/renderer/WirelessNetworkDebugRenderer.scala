package li.cil.oc.client.renderer

import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.Settings
import li.cil.oc.server.network.WirelessNetwork
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.util.math.vector.Vector4f
import net.minecraft.util.math.vector.Matrix4f
import net.minecraft.world.World
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.lwjgl.opengl.GL11

object WirelessNetworkDebugRenderer {
  val colors = Array(0xFF0000, 0x00FFFF, 0x00FF00, 0x0000FF, 0xFF00FF, 0xFFFF00, 0xFFFFFF, 0x000000)

  @SubscribeEvent
  def onRenderWorldLastEvent(e: RenderWorldLastEvent) {
    if (Settings.rTreeDebugRenderer) {
      RenderState.checkError(getClass.getName + ".onRenderWorldLastEvent: entering (aka: wasntme)")

      val world = Minecraft.getInstance.level
      WirelessNetwork.dimensions.get(world.dimension) match {
        case Some(tree) =>
          val player = Minecraft.getInstance.player
          val px = player.xOld + (player.getX - player.xOld) * e.getPartialTicks
          val py = player.yOld + (player.getY - player.yOld) * e.getPartialTicks
          val pz = player.zOld + (player.getZ - player.zOld) * e.getPartialTicks

          val stack = e.getMatrixStack
          RenderState.pushAttrib()
          stack.pushPose()
          stack.translate(-px, -py, -pz)
          RenderState.makeItBlend()
          GL11.glDisable(GL11.GL_LIGHTING)
          GL11.glDisable(GL11.GL_TEXTURE_2D)
          GL11.glDisable(GL11.GL_DEPTH_TEST)
          GL11.glDisable(GL11.GL_CULL_FACE)

          def glVertex(matrix: Matrix4f, temp: Vector4f, x: Float, y: Float, z: Float) {
            temp.set(x, y, z, 1)
            temp.transform(matrix)
            GL11.glVertex3f(temp.x, temp.y, temp.z)
          }

          def drawBox(matrix: Matrix4f, temp: Vector4f, minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float) {
            GL11.glBegin(GL11.GL_QUADS)
            glVertex(matrix, temp, minX, minY, minZ)
            glVertex(matrix, temp, minX, minY, maxZ)
            glVertex(matrix, temp, maxX, minY, maxZ)
            glVertex(matrix, temp, maxX, minY, minZ)
            GL11.glEnd()
            GL11.glBegin(GL11.GL_QUADS)
            glVertex(matrix, temp, minX, minY, minZ)
            glVertex(matrix, temp, maxX, minY, minZ)
            glVertex(matrix, temp, maxX, maxY, minZ)
            glVertex(matrix, temp, minX, maxY, minZ)
            GL11.glEnd()
            GL11.glBegin(GL11.GL_QUADS)
            glVertex(matrix, temp, maxX, maxY, minZ)
            glVertex(matrix, temp, maxX, maxY, maxZ)
            glVertex(matrix, temp, minX, maxY, maxZ)
            glVertex(matrix, temp, minX, maxY, minZ)
            GL11.glEnd()
            GL11.glBegin(GL11.GL_QUADS)
            glVertex(matrix, temp, maxX, maxY, maxZ)
            glVertex(matrix, temp, maxX, minY, maxZ)
            glVertex(matrix, temp, minX, minY, maxZ)
            glVertex(matrix, temp, minX, maxY, maxZ)
            GL11.glEnd()
            GL11.glBegin(GL11.GL_QUADS)
            glVertex(matrix, temp, minX, minY, minZ)
            glVertex(matrix, temp, minX, maxY, minZ)
            glVertex(matrix, temp, minX, maxY, maxZ)
            glVertex(matrix, temp, minX, minY, maxZ)
            GL11.glEnd()
            GL11.glBegin(GL11.GL_QUADS)
            glVertex(matrix, temp, maxX, minY, minZ)
            glVertex(matrix, temp, maxX, minY, maxZ)
            glVertex(matrix, temp, maxX, maxY, maxZ)
            glVertex(matrix, temp, maxX, maxY, minZ)
            GL11.glEnd()
          }

          val temp = new Vector4f()
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
            val size = 0.5f - level * 0.05f
            drawBox(stack.last.pose, temp, minX.toFloat - size, minY.toFloat - size, minZ.toFloat - size, maxX.toFloat + size, maxY.toFloat + size, maxZ.toFloat + size)
          }
          GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)

          RenderState.popAttrib()
          stack.popPose()
        case _ =>
      }

      RenderState.checkError(getClass.getName + ".onRenderWorldLastEvent: leaving")
    }
  }

}
