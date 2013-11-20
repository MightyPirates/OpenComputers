package li.cil.oc.client.renderer.tileentity

import java.util.logging.Level
import li.cil.oc.common.tileentity
import li.cil.oc.{OpenComputers, Config}
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.{OpenGlHelper, Tessellator, GLAllocation}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.ForgeDirection
import org.lwjgl.opengl.GL11
import li.cil.oc.util.RenderState

object RobotRenderer extends TileEntitySpecialRenderer {
  private val texture = new ResourceLocation(Config.resourceDomain, "textures/blocks/robot.png")

  private val displayList = GLAllocation.generateDisplayLists(1)

  private val gap = 1.0 / 28.0
  private val gt = 0.5 + gap
  private val gb = 0.5 - gap

  def compileList() {
    val t = Tessellator.instance

    val size = 0.4
    val l = 0.5 - size
    val h = 0.5 + size

    GL11.glNewList(displayList, GL11.GL_COMPILE)

    t.startDrawing(GL11.GL_TRIANGLE_FAN)
    t.addVertexWithUV(0.5, 1, 0.5, 0.25, 0.25)
    t.addVertexWithUV(l, gt, h, 0, 0.5)
    t.addVertexWithUV(h, gt, h, 0.5, 0.5)
    t.addVertexWithUV(h, gt, l, 0.5, 0)
    t.addVertexWithUV(l, gt, l, 0, 0)
    t.addVertexWithUV(l, gt, h, 0, 0.5)
    t.draw()

    t.startDrawingQuads()
    t.addVertexWithUV(l, gt, h, 0, 1)
    t.addVertexWithUV(l, gt, l, 0, 0.5)
    t.addVertexWithUV(h, gt, l, 0.5, 0.5)
    t.addVertexWithUV(h, gt, h, 0.5, 1)
    t.draw()

    t.startDrawing(GL11.GL_TRIANGLE_FAN)
    t.addVertexWithUV(0.5, 0.03, 0.5, 0.75, 0.25)
    t.addVertexWithUV(l, gb, l, 0.5, 0)
    t.addVertexWithUV(h, gb, l, 1, 0)
    t.addVertexWithUV(h, gb, h, 1, 0.5)
    t.addVertexWithUV(l, gb, h, 0.5, 0.5)
    t.addVertexWithUV(l, gb, l, 0.5, 0)
    t.draw()

    t.startDrawingQuads()
    t.addVertexWithUV(l, gb, l, 0, 0.5)
    t.addVertexWithUV(l, gb, h, 0, 1)
    t.addVertexWithUV(h, gb, h, 0.5, 1)
    t.addVertexWithUV(h, gb, l, 0.5, 0.5)
    t.draw()

    GL11.glEndList()
  }

  compileList()

  def renderTileEntityAt(entity: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    val proxy = entity.asInstanceOf[tileentity.RobotProxy]
    val robot = proxy.robot
    val worldTime = entity.getWorldObj.getTotalWorldTime + f

    {
      val l = robot.world.getLightBrightnessForSkyBlocks(robot.x, robot.y, robot.z, 0)
      val l1 = l % 65536
      val l2 = l / 65536
      OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, l1, l2)
    }

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

    GL11.glTranslated(-0.5, -0.5, -0.5)

    val timeJitter = robot.hashCode
    val hover =
      if (robot.isOn) Math.sin(timeJitter + worldTime / 20.0) * 0.03
      else -0.03
    GL11.glTranslated(0, hover, 0)

    bindTexture(texture)
    GL11.glCallList(displayList)

    val size = 0.3
    val l = 0.5 - size
    val h = 0.5 + size
    val vStep = 1.0 / 32.0

    val strip = timeJitter + worldTime / 20.0
    val offsetV = ((strip - strip.toInt) * 16).toInt * vStep
    val (u0, u1, v0, v1) = {
      if (robot.isOn)
        (0.5, 1.0, 0.5 + offsetV, 0.5 + vStep + offsetV)
      else
        (0.25 - vStep, 0.25 + vStep, 0.75 - vStep, 0.75 + vStep)
    }

    RenderState.disableLighting()
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
    RenderState.enableLighting()

    robot.equippedItem match {
      case Some(stack) =>
        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glTranslated(0.1, 0.25, 0.75)
        GL11.glScaled(0.4, 0.4, -0.4)
        if (robot.isAnimatingSwing) {
          val remaining = (robot.animationTicksLeft - f) / robot.animationTicksTotal.toDouble
          GL11.glRotated(Math.sin(remaining * Math.PI) * 45, -1, 0, 0)
        }
        GL11.glRotatef(-30, 1, 0, 0)
        GL11.glRotatef(40, 0, 1, 0)
        try {
          RenderManager.instance.itemRenderer.renderItem(robot.player(), stack, 0)
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
