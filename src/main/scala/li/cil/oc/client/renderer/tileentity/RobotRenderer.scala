package li.cil.oc.client.renderer.tileentity

import com.google.common.base.Strings
import java.util.logging.Level
import li.cil.oc.client.Textures
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import li.cil.oc.{Settings, OpenComputers}
import net.minecraft.client.renderer.entity.{RendererLivingEntity, RenderManager}
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.{Tessellator, GLAllocation}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Vec3
import net.minecraftforge.client.IItemRenderer.ItemRenderType
import net.minecraftforge.client.MinecraftForgeClient
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.opengl.GL11

object RobotRenderer extends TileEntitySpecialRenderer {
  private val displayList = GLAllocation.generateDisplayLists(2)

  private val gap = 1.0f / 28.0f
  private val gt = 0.5f + gap
  private val gb = 0.5f - gap

  private def normal(v: Vec3) {
    val n = v.normalize()
    GL11.glNormal3f(n.xCoord.toFloat, n.yCoord.toFloat, n.zCoord.toFloat)
  }

  def compileList() {
    val t = Tessellator.instance

    val size = 0.4f
    val l = 0.5f - size
    val h = 0.5f + size

    GL11.glNewList(displayList, GL11.GL_COMPILE)

    GL11.glBegin(GL11.GL_TRIANGLE_FAN)
    GL11.glTexCoord2f(0.25f, 0.25f)
    GL11.glVertex3f(0.5f, 1, 0.5f)
    GL11.glTexCoord2f(0, 0.5f)
    GL11.glVertex3f(l, gt, h)
    normal(Vec3.createVectorHelper(0, 0.2, 1))
    GL11.glTexCoord2f(0.5f, 0.5f)
    GL11.glVertex3f(h, gt, h)
    normal(Vec3.createVectorHelper(1, 0.2, 0))
    GL11.glTexCoord2f(0.5f, 0)
    GL11.glVertex3f(h, gt, l)
    normal(Vec3.createVectorHelper(0, 0.2, -1))
    GL11.glTexCoord2f(0, 0)
    GL11.glVertex3f(l, gt, l)
    normal(Vec3.createVectorHelper(-1, 0.2, 0))
    GL11.glTexCoord2f(0, 0.5f)
    GL11.glVertex3f(l, gt, h)
    GL11.glEnd()

    t.startDrawingQuads()
    t.setNormal(0, -1, 0)
    t.addVertexWithUV(l, gt, h, 0, 1)
    t.addVertexWithUV(l, gt, l, 0, 0.5)
    t.addVertexWithUV(h, gt, l, 0.5, 0.5)
    t.addVertexWithUV(h, gt, h, 0.5, 1)
    t.draw()

    GL11.glEndList()

    GL11.glNewList(displayList + 1, GL11.GL_COMPILE)

    GL11.glBegin(GL11.GL_TRIANGLE_FAN)
    GL11.glTexCoord2f(0.75f, 0.25f)
    GL11.glVertex3f(0.5f, 0.03f, 0.5f)
    GL11.glTexCoord2f(0.5f, 0)
    GL11.glVertex3f(l, gb, l)
    normal(Vec3.createVectorHelper(0, -0.2, 1))
    GL11.glTexCoord2f(1, 0)
    GL11.glVertex3f(h, gb, l)
    normal(Vec3.createVectorHelper(1, -0.2, 0))
    GL11.glTexCoord2f(1, 0.5f)
    GL11.glVertex3f(h, gb, h)
    normal(Vec3.createVectorHelper(0, -0.2, -1))
    GL11.glTexCoord2f(0.5f, 0.5f)
    GL11.glVertex3f(l, gb, h)
    normal(Vec3.createVectorHelper(-1, -0.2, 0))
    GL11.glTexCoord2f(0.5f, 0)
    GL11.glVertex3f(l, gb, l)
    GL11.glEnd()

    t.startDrawingQuads()
    t.setNormal(0, 1, 0)
    t.addVertexWithUV(l, gb, l, 0, 0.5)
    t.addVertexWithUV(l, gb, h, 0, 1)
    t.addVertexWithUV(h, gb, h, 0.5, 1)
    t.addVertexWithUV(h, gb, l, 0.5, 0.5)
    t.draw()

    GL11.glEndList()
  }

  compileList()

  def renderChassis(isRunning: Boolean = false, level: Int = 0, offset: Double = 0) {
    val size = 0.3f
    val l = 0.5f - size
    val h = 0.5f + size
    val vStep = 1.0f / 32.0f

    val offsetV = ((offset - offset.toInt) * 16).toInt * vStep
    val (u0, u1, v0, v1) = {
      if (isRunning)
        (0.5f, 1f, 0.5f + offsetV, 0.5f + vStep + offsetV)
      else
        (0.25f - vStep, 0.25f + vStep, 0.75f - vStep, 0.75f + vStep)
    }

    bindTexture(Textures.blockRobot)
    if (level > 19) {
      GL11.glColor3f(0.4f, 1, 1)
    }
    else if (level > 9) {
      GL11.glColor3f(1, 1, 0.4f)
    }
    else {
      GL11.glColor3f(0.5f, 0.5f, 0.5f)
    }
    if (!isRunning) {
      GL11.glTranslatef(0, -2 * gap, 0)
    }
    GL11.glCallList(displayList)
    if (!isRunning) {
      GL11.glTranslatef(0, 2 * gap, 0)
    }
    GL11.glCallList(displayList + 1)
    GL11.glColor3f(1, 1, 1)

    if (MinecraftForgeClient.getRenderPass == 0) {
      RenderState.disableLighting()
    }

    if (isRunning) {
      val t = Tessellator.instance
      t.startDrawingQuads()
      t.addVertexWithUV(l, gt, l, u0, v0)
      t.addVertexWithUV(l, gb, l, u0, v1)
      t.addVertexWithUV(l, gb, h, u1, v1)
      t.addVertexWithUV(l, gt, h, u1, v0)

      t.addVertexWithUV(l, gt, h, u0, v0)
      t.addVertexWithUV(l, gb, h, u0, v1)
      t.addVertexWithUV(h, gb, h, u1, v1)
      t.addVertexWithUV(h, gt, h, u1, v0)

      t.addVertexWithUV(h, gt, h, u0, v0)
      t.addVertexWithUV(h, gb, h, u0, v1)
      t.addVertexWithUV(h, gb, l, u1, v1)
      t.addVertexWithUV(h, gt, l, u1, v0)

      t.addVertexWithUV(h, gt, l, u0, v0)
      t.addVertexWithUV(h, gb, l, u0, v1)
      t.addVertexWithUV(l, gb, l, u1, v1)
      t.addVertexWithUV(l, gt, l, u1, v0)
      t.draw()
    }

    if (MinecraftForgeClient.getRenderPass == 0) {
      RenderState.enableLighting()
    }
  }

  override def renderTileEntityAt(entity: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    val proxy = entity.asInstanceOf[tileentity.RobotProxy]
    val robot = proxy.robot
    val worldTime = entity.getWorldObj.getTotalWorldTime + f

    GL11.glPushMatrix()
    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    val name = robot.name
    if (Settings.get.robotLabels && !Strings.isNullOrEmpty(name) && x * x + y * y + z * z < RendererLivingEntity.NAME_TAG_RANGE) {
      GL11.glPushMatrix()

      // This is pretty much copy-pasta from the entity's label renderer.
      val t = Tessellator.instance
      val f = func_147498_b
      val scale = 1.6f / 60f
      val width = f.getStringWidth(name)
      val halfWidth = width / 2

      GL11.glTranslated(0, 0.7, 0)
      GL11.glNormal3f(0, 1, 0)

      GL11.glRotatef(-field_147501_a.field_147562_h, 0, 1, 0)
      GL11.glRotatef(field_147501_a.field_147563_i, 1, 0, 0)
      GL11.glScalef(-scale, -scale, scale)

      RenderState.makeItBlend()
      GL11.glDisable(GL11.GL_LIGHTING)
      GL11.glDepthMask(false)
      GL11.glDisable(GL11.GL_TEXTURE_2D)

      t.startDrawingQuads()
      t.setColorRGBA_F(0, 0, 0, 0.25f)
      t.addVertex(-halfWidth - 1, -1, 0)
      t.addVertex(-halfWidth - 1, 8, 0)
      t.addVertex(halfWidth + 1, 8, 0)
      t.addVertex(halfWidth + 1, -1, 0)
      t.draw

      GL11.glEnable(GL11.GL_TEXTURE_2D) // For the font.
      f.drawString(name, -halfWidth, 0, 0xFFFFFFFF)

      GL11.glPopMatrix()
    }

    GL11.glDepthMask(true)
    GL11.glEnable(GL11.GL_LIGHTING)
    GL11.glDisable(GL11.GL_BLEND)
    GL11.glColor4f(1, 1, 1, 1)

    // If the move started while we were rendering and we have a reference to
    // the *old* proxy the robot would be rendered at the wrong position, so we
    // correct for the offset.
    if (robot.proxy != proxy) {
      GL11.glTranslated(robot.proxy.x - proxy.x, robot.proxy.y - proxy.y, robot.proxy.z - proxy.z)
    }

    if (robot.isAnimatingMove) {
      val remaining = (robot.animationTicksLeft - f) / robot.animationTicksTotal.toDouble
      val dx = robot.moveFromX - robot.x
      val dy = robot.moveFromY - robot.y
      val dz = robot.moveFromZ - robot.z
      GL11.glTranslated(dx * remaining, dy * remaining, dz * remaining)
    }

    if (robot.isAnimatingTurn) {
      val remaining = (robot.animationTicksLeft - f) / robot.animationTicksTotal.toDouble
      GL11.glRotated(90 * remaining, 0, robot.turnAxis, 0)
    }

    robot.yaw match {
      case ForgeDirection.WEST => GL11.glRotatef(-90, 0, 1, 0)
      case ForgeDirection.NORTH => GL11.glRotatef(180, 0, 1, 0)
      case ForgeDirection.EAST => GL11.glRotatef(90, 0, 1, 0)
      case _ => // No yaw.
    }

    GL11.glTranslatef(-0.5f, -0.5f, -0.5f)

    val timeJitter = robot.hashCode
    val hover =
      if (robot.isRunning) (Math.sin(timeJitter + worldTime / 20.0) * 0.03).toFloat
      else -0.03f
    GL11.glTranslatef(0, hover, 0)

    if (MinecraftForgeClient.getRenderPass == 0) {
      val offset = timeJitter + worldTime / 20.0
      renderChassis(robot.isRunning, robot.level, offset)
    }

    robot.equippedItem match {
      case Some(stack) =>
        GL11.glPushMatrix()
        GL11.glTranslatef(0.1f, 0.25f, 0.75f)
        GL11.glScalef(0.4f, 0.4f, -0.4f)
        if (robot.isAnimatingSwing) {
          val remaining = (robot.animationTicksLeft - f) / robot.animationTicksTotal.toDouble
          GL11.glRotatef((Math.sin(remaining * Math.PI) * 45).toFloat, -1, 0, 0)
        }
        GL11.glRotatef(-30, 1, 0, 0)
        GL11.glRotatef(40, 0, 1, 0)
        GL11.glDisable(GL11.GL_CULL_FACE)
        try {
          RenderManager.instance.itemRenderer.renderItem(robot.player(), stack, MinecraftForgeClient.getRenderPass)
        }
        catch {
          case e: Throwable =>
            OpenComputers.log.log(Level.WARNING, "Failed rendering equipped item.", e)
            robot.equippedItem = None
        }
        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glPopMatrix()
      case _ =>
    }

    robot.equippedUpgrade match {
      case Some(stack) =>
        try {
          if (MinecraftForgeClient.getItemRenderer(stack, ItemRenderType.EQUIPPED) != null) {
            RenderManager.instance.itemRenderer.renderItem(robot.player(), stack, MinecraftForgeClient.getRenderPass, ItemRenderType.EQUIPPED)
          }
        }
        catch {
          case e: Throwable =>
            OpenComputers.log.log(Level.WARNING, "Failed rendering equipped upgrade.", e)
            robot.equippedUpgrade = None
        }
      case _ =>
    }

    GL11.glPopMatrix()
  }
}
