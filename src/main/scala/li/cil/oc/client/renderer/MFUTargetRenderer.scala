package li.cil.oc.client.renderer

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.util.Hand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.vector.Vector4f
import net.minecraft.util.math.vector.Matrix4f
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.lwjgl.opengl.GL11

object MFUTargetRenderer {
  private val color = 0x00FF00
  private lazy val mfu = api.Items.get(Constants.ItemName.MFU)

  @SubscribeEvent
  def onRenderWorldLastEvent(e: RenderWorldLastEvent) {
    val mc = Minecraft.getInstance
    val player = mc.player
    if (player == null) return
    player.getItemInHand(Hand.MAIN_HAND) match {
      case stack: ItemStack if api.Items.get(stack) == mfu && stack.hasTag =>
        val data = stack.getTag
        if (data.contains(Settings.namespace + "coord", NBT.TAG_INT_ARRAY)) {
          val dimension = new ResourceLocation(data.getString(Settings.namespace + "dimension"))
          if (!player.level.dimension.location.equals(dimension)) return
          val Array(x, y, z, side) = data.getIntArray(Settings.namespace + "coord")
          if (player.distanceToSqr(x, y, z) > 64 * 64) return

          val bounds = BlockPosition(x, y, z).bounds.inflate(0.1, 0.1, 0.1)

          val px = player.xOld + (player.getX - player.xOld) * e.getPartialTicks
          val py = player.yOld + (player.getY - player.yOld) * e.getPartialTicks
          val pz = player.zOld + (player.getZ - player.zOld) * e.getPartialTicks

          RenderState.checkError(getClass.getName + ".onRenderWorldLastEvent: entering (aka: wasntme)")

          GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
          val matrix = e.getMatrixStack
          matrix.pushPose()
          matrix.translate(-px, -py, -pz)
          RenderState.makeItBlend()
          GL11.glDisable(GL11.GL_LIGHTING)
          GL11.glDisable(GL11.GL_TEXTURE_2D)
          GL11.glDisable(GL11.GL_DEPTH_TEST)
          GL11.glDisable(GL11.GL_CULL_FACE)

          GL11.glColor4f(
            ((color >> 16) & 0xFF) / 255f,
            ((color >> 8) & 0xFF) / 255f,
            ((color >> 0) & 0xFF) / 255f,
            0.25f)
          GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
          drawBox(matrix.last.pose, new Vector4f(), bounds.minX.toFloat, bounds.minY.toFloat, bounds.minZ.toFloat, bounds.maxX.toFloat, bounds.maxY.toFloat, bounds.maxZ.toFloat)
          GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)
          drawFace(matrix.last.pose, new Vector4f(), bounds.minX.toFloat, bounds.minY.toFloat, bounds.minZ.toFloat, bounds.maxX.toFloat, bounds.maxY.toFloat, bounds.maxZ.toFloat, side)

          matrix.popPose()
          GL11.glPopAttrib()

          RenderState.checkError(getClass.getName + ".onRenderWorldLastEvent: leaving")
        }
      case _ => // Nothing
    }
  }

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

  private def drawFace(matrix: Matrix4f, temp: Vector4f, minX: Float, minY: Float, minZ: Float, maxX: Float, maxY: Float, maxZ: Float, side: Int): Unit = {
    side match {
      case 0 => // Down
        GL11.glBegin(GL11.GL_QUADS)
        glVertex(matrix, temp, minX, minY, minZ)
        glVertex(matrix, temp, minX, minY, maxZ)
        glVertex(matrix, temp, maxX, minY, maxZ)
        glVertex(matrix, temp, maxX, minY, minZ)
        GL11.glEnd()
      case 1 => // Up
        GL11.glBegin(GL11.GL_QUADS)
        glVertex(matrix, temp, maxX, maxY, minZ)
        glVertex(matrix, temp, maxX, maxY, maxZ)
        glVertex(matrix, temp, minX, maxY, maxZ)
        glVertex(matrix, temp, minX, maxY, minZ)
        GL11.glEnd()
      case 2 => // North
        GL11.glBegin(GL11.GL_QUADS)
        glVertex(matrix, temp, minX, minY, minZ)
        glVertex(matrix, temp, maxX, minY, minZ)
        glVertex(matrix, temp, maxX, maxY, minZ)
        glVertex(matrix, temp, minX, maxY, minZ)
        GL11.glEnd()
      case 3 => // South
        GL11.glBegin(GL11.GL_QUADS)
        glVertex(matrix, temp, maxX, maxY, maxZ)
        glVertex(matrix, temp, maxX, minY, maxZ)
        glVertex(matrix, temp, minX, minY, maxZ)
        glVertex(matrix, temp, minX, maxY, maxZ)
        GL11.glEnd()
      case 4 => // East
        GL11.glBegin(GL11.GL_QUADS)
        glVertex(matrix, temp, minX, minY, minZ)
        glVertex(matrix, temp, minX, maxY, minZ)
        glVertex(matrix, temp, minX, maxY, maxZ)
        glVertex(matrix, temp, minX, minY, maxZ)
        GL11.glEnd()
      case 5 => // West
        GL11.glBegin(GL11.GL_QUADS)
        glVertex(matrix, temp, maxX, minY, minZ)
        glVertex(matrix, temp, maxX, minY, maxZ)
        glVertex(matrix, temp, maxX, maxY, maxZ)
        glVertex(matrix, temp, maxX, maxY, minZ)
        GL11.glEnd()
      case _ => // WTF?
    }
  }

}
