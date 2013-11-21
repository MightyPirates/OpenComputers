package li.cil.oc.client.renderer.tileentity

import java.util.logging.Level
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import li.cil.oc.{OpenComputers, Config}
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.{Tessellator, GLAllocation}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{Vec3, ResourceLocation}
import net.minecraftforge.client.MinecraftForgeClient
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.GL11

object RobotRenderer extends TileEntitySpecialRenderer {
  private val texture = new ResourceLocation(Config.resourceDomain, "textures/blocks/robot.png")

  private val displayList = GLAllocation.generateDisplayLists(1)

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

  def renderChassis(powered: Boolean = false, offset: Double = 0) {
    val size = 0.3f
    val l = 0.5f - size
    val h = 0.5f + size
    val vStep = 1.0f / 32.0f

    val offsetV = ((offset - offset.toInt) * 16).toInt * vStep
    val (u0, u1, v0, v1) = {
      if (powered)
        (0.5f, 1f, 0.5f + offsetV, 0.5f + vStep + offsetV)
      else
        (0.25f - vStep, 0.25f + vStep, 0.75f - vStep, 0.75f + vStep)
    }

    bindTexture(texture)
    GL11.glCallList(displayList)

    if (MinecraftForgeClient.getRenderPass == 0) {
      RenderState.disableLighting()
    }

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

    if (MinecraftForgeClient.getRenderPass == 0) {
      RenderState.enableLighting()
    }
  }

  def renderTileEntityAt(entity: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    val proxy = entity.asInstanceOf[tileentity.RobotProxy]
    val robot = proxy.robot
    val worldTime = entity.getWorldObj.getTotalWorldTime + f

    GL11.glPushMatrix()
    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    if (robot.isAnimatingMove) {
      val remaining = (robot.animationTicksLeft - f) / robot.animationTicksTotal.toDouble
      GL11.glTranslated(
        -robot.moveDirection.offsetX * remaining,
        -robot.moveDirection.offsetY * remaining,
        -robot.moveDirection.offsetZ * remaining)
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
      if (robot.isOn) (Math.sin(timeJitter + worldTime / 20.0) * 0.03).toFloat
      else -0.03f
    GL11.glTranslatef(0, hover, 0)

    if (MinecraftForgeClient.getRenderPass == 0) {
      val offset = timeJitter + worldTime / 20.0
      renderChassis(robot.isOn, offset)
    }

    robot.equippedItem match {
      case Some(stack) =>
        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glTranslatef(0.1f, 0.25f, 0.75f)
        GL11.glScalef(0.4f, 0.4f, -0.4f)
        if (robot.isAnimatingSwing) {
          val remaining = (robot.animationTicksLeft - f) / robot.animationTicksTotal.toDouble
          GL11.glRotatef((Math.sin(remaining * Math.PI) * 45).toFloat, -1, 0, 0)
        }
        GL11.glRotatef(-30, 1, 0, 0)
        GL11.glRotatef(40, 0, 1, 0)
        try {
          RenderManager.instance.itemRenderer.renderItem(robot.player(), stack, MinecraftForgeClient.getRenderPass)
        }
        catch {
          case e: Throwable =>
            OpenComputers.log.log(Level.WARNING, "Failed rendering equipped item.", e)
            robot.equippedItem = None
        }
        GL11.glEnable(GL11.GL_CULL_FACE)
      case _ =>
    }

    GL11.glPopMatrix()
  }
}
