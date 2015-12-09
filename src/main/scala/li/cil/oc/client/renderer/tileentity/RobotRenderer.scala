package li.cil.oc.client.renderer.tileentity

import com.google.common.base.Strings
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.driver.item.UpgradeRenderer
import li.cil.oc.api.driver.item.UpgradeRenderer.MountPointName
import li.cil.oc.api.event.RobotRenderEvent
import li.cil.oc.client.Textures
import li.cil.oc.common.EventHandler
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GLAllocation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType
import net.minecraft.client.renderer.entity.RendererLivingEntity
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.init.Items
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import net.minecraftforge.client.MinecraftForgeClient
import net.minecraftforge.common.MinecraftForge
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

object RobotRenderer extends TileEntitySpecialRenderer[tileentity.RobotProxy] {
  private val displayList = GLAllocation.generateDisplayLists(2)

  private val mountPoints = new Array[RobotRenderEvent.MountPoint](7)

  private val slotNameMapping = Map(
    UpgradeRenderer.MountPointName.TopLeft -> 0,
    UpgradeRenderer.MountPointName.TopRight -> 1,
    UpgradeRenderer.MountPointName.TopBack -> 2,
    UpgradeRenderer.MountPointName.BottomLeft -> 3,
    UpgradeRenderer.MountPointName.BottomRight -> 4,
    UpgradeRenderer.MountPointName.BottomBack -> 5,
    UpgradeRenderer.MountPointName.BottomFront -> 6
  )

  for ((name, index) <- slotNameMapping) {
    mountPoints(index) = new RobotRenderEvent.MountPoint(name)
  }

  private val gap = 1.0f / 28.0f
  private val gt = 0.5f + gap
  private val gb = 0.5f - gap

  private def normal(v: Vec3) {
    val n = v.normalize()
    GL11.glNormal3f(n.xCoord.toFloat, n.yCoord.toFloat, n.zCoord.toFloat)
  }

  def compileList() {
    val t = Tessellator.getInstance
    val r = t.getWorldRenderer

    val size = 0.4f
    val l = 0.5f - size
    val h = 0.5f + size

    GL11.glNewList(displayList, GL11.GL_COMPILE)

    GL11.glBegin(GL11.GL_TRIANGLE_FAN)
    GL11.glTexCoord2f(0.25f, 0.25f)
    GL11.glVertex3f(0.5f, 1, 0.5f)
    GL11.glTexCoord2f(0, 0.5f)
    GL11.glVertex3f(l, gt, h)
    normal(new Vec3(0, 0.2, 1))
    GL11.glTexCoord2f(0.5f, 0.5f)
    GL11.glVertex3f(h, gt, h)
    normal(new Vec3(1, 0.2, 0))
    GL11.glTexCoord2f(0.5f, 0)
    GL11.glVertex3f(h, gt, l)
    normal(new Vec3(0, 0.2, -1))
    GL11.glTexCoord2f(0, 0)
    GL11.glVertex3f(l, gt, l)
    normal(new Vec3(-1, 0.2, 0))
    GL11.glTexCoord2f(0, 0.5f)
    GL11.glVertex3f(l, gt, h)
    GL11.glEnd()

    r.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL)

    r.pos(l, gt, h).tex(0, 1).normal(0, -1, 0).endVertex()
    r.pos(l, gt, l).tex(0, 0.5).normal(0, -1, 0).endVertex()
    r.pos(h, gt, l).tex(0.5, 0.5).normal(0, -1, 0).endVertex()
    r.pos(h, gt, h).tex(0.5, 1).normal(0, -1, 0).endVertex()

    t.draw()

    GL11.glEndList()

    GL11.glNewList(displayList + 1, GL11.GL_COMPILE)

    GL11.glBegin(GL11.GL_TRIANGLE_FAN)
    GL11.glTexCoord2f(0.75f, 0.25f)
    GL11.glVertex3f(0.5f, 0.03f, 0.5f)
    GL11.glTexCoord2f(0.5f, 0)
    GL11.glVertex3f(l, gb, l)
    normal(new Vec3(0, -0.2, 1))
    GL11.glTexCoord2f(1, 0)
    GL11.glVertex3f(h, gb, l)
    normal(new Vec3(1, -0.2, 0))
    GL11.glTexCoord2f(1, 0.5f)
    GL11.glVertex3f(h, gb, h)
    normal(new Vec3(0, -0.2, -1))
    GL11.glTexCoord2f(0.5f, 0.5f)
    GL11.glVertex3f(l, gb, h)
    normal(new Vec3(-1, -0.2, 0))
    GL11.glTexCoord2f(0.5f, 0)
    GL11.glVertex3f(l, gb, l)
    GL11.glEnd()

    r.begin(7, DefaultVertexFormats.POSITION_TEX_NORMAL)

    r.pos(l, gb, l).tex(0, 0.5).normal(0, 1, 0).endVertex()
    r.pos(l, gb, h).tex(0, 1).normal(0, 1, 0).endVertex()
    r.pos(h, gb, h).tex(0.5, 1).normal(0, 1, 0).endVertex()
    r.pos(h, gb, l).tex(0.5, 0.5).normal(0, 1, 0).endVertex()

    t.draw()

    GL11.glEndList()
  }

  compileList()

  def resetMountPoints(running: Boolean) {
    val offset = if (running) 0 else -0.06f

    // Left top.
    mountPoints(0).offset.setX(0)
    mountPoints(0).offset.setY(0.2f)
    mountPoints(0).offset.setZ(0.24f)
    mountPoints(0).rotation.setX(0)
    mountPoints(0).rotation.setY(1)
    mountPoints(0).rotation.setZ(0)
    mountPoints(0).rotation.setW(90)

    // Right top.
    mountPoints(1).offset.setX(0)
    mountPoints(1).offset.setY(0.2f)
    mountPoints(1).offset.setZ(0.24f)
    mountPoints(1).rotation.setX(0)
    mountPoints(1).rotation.setY(1)
    mountPoints(1).rotation.setZ(0)
    mountPoints(1).rotation.setW(-90)

    // Back top.
    mountPoints(2).offset.setX(0)
    mountPoints(2).offset.setY(0.2f)
    mountPoints(2).offset.setZ(0.24f)
    mountPoints(2).rotation.setX(0)
    mountPoints(2).rotation.setY(1)
    mountPoints(2).rotation.setZ(0)
    mountPoints(2).rotation.setW(180)

    // Left bottom.
    mountPoints(3).offset.setX(0)
    mountPoints(3).offset.setY(-0.2f - offset)
    mountPoints(3).offset.setZ(0.24f)
    mountPoints(3).rotation.setX(0)
    mountPoints(3).rotation.setY(1)
    mountPoints(3).rotation.setZ(0)
    mountPoints(3).rotation.setW(90)

    // Right bottom.
    mountPoints(4).offset.setX(0)
    mountPoints(4).offset.setY(-0.2f - offset)
    mountPoints(4).offset.setZ(0.24f)
    mountPoints(4).rotation.setX(0)
    mountPoints(4).rotation.setY(1)
    mountPoints(4).rotation.setZ(0)
    mountPoints(4).rotation.setW(-90)

    // Back bottom.
    mountPoints(5).offset.setX(0)
    mountPoints(5).offset.setY(-0.2f - offset)
    mountPoints(5).offset.setZ(0.24f)
    mountPoints(5).rotation.setX(0)
    mountPoints(5).rotation.setY(1)
    mountPoints(5).rotation.setZ(0)
    mountPoints(5).rotation.setW(180)

    // Front bottom.
    mountPoints(6).offset.setX(0)
    mountPoints(6).offset.setY(-0.2f - offset)
    mountPoints(6).offset.setZ(0.24f)
    mountPoints(6).rotation.setX(0)
    mountPoints(6).rotation.setY(1)
    mountPoints(6).rotation.setZ(0)
    mountPoints(6).rotation.setW(0)
  }

  def renderChassis(robot: tileentity.Robot = null, offset: Double = 0, isRunningOverride: Boolean = false) {
    val isRunning = if (robot == null) isRunningOverride else robot.isRunning

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

    resetMountPoints(robot != null && robot.isRunning)
    val event = new RobotRenderEvent(robot, mountPoints)
    MinecraftForge.EVENT_BUS.post(event)
    if (!event.isCanceled) {
      bindTexture(Textures.Model.Robot)
      if (!isRunning) {
        GL11.glTranslatef(0, -2 * gap, 0)
      }
      GL11.glCallList(displayList + 1)
      if (!isRunning) {
        GL11.glTranslatef(0, -2 * gap, 0)
      }

      if (MinecraftForgeClient.getRenderPass > 0) return

      GL11.glCallList(displayList)
      GL11.glColor3f(1, 1, 1)

      if (isRunning) {
        RenderState.disableEntityLighting()

        {
          // Additive blending for the light.
          RenderState.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
          // Light color.
          val lightColor = if (robot != null && robot.info != null) robot.info.lightColor else 0xF23030
          val r = ((lightColor >>> 16) & 0xFF).toByte
          val g = ((lightColor >>> 8) & 0xFF).toByte
          val b = ((lightColor >>> 0) & 0xFF).toByte
          GL11.glColor3ub(r, g, b)
        }

        val t = Tessellator.getInstance
        val r = t.getWorldRenderer
        r.begin(7, DefaultVertexFormats.POSITION_TEX)
        r.pos(l, gt, l).tex(u0, v0).endVertex()
        r.pos(l, gb, l).tex(u0, v1).endVertex()
        r.pos(l, gb, h).tex(u1, v1).endVertex()
        r.pos(l, gt, h).tex(u1, v0).endVertex()

        r.pos(l, gt, h).tex(u0, v0).endVertex()
        r.pos(l, gb, h).tex(u0, v1).endVertex()
        r.pos(h, gb, h).tex(u1, v1).endVertex()
        r.pos(h, gt, h).tex(u1, v0).endVertex()

        r.pos(h, gt, h).tex(u0, v0).endVertex()
        r.pos(h, gb, h).tex(u0, v1).endVertex()
        r.pos(h, gb, l).tex(u1, v1).endVertex()
        r.pos(h, gt, l).tex(u1, v0).endVertex()

        r.pos(h, gt, l).tex(u0, v0).endVertex()
        r.pos(h, gb, l).tex(u0, v1).endVertex()
        r.pos(l, gb, l).tex(u1, v1).endVertex()
        r.pos(l, gt, l).tex(u1, v0).endVertex()
        t.draw()

        RenderState.enableEntityLighting()
      }
      RenderState.color(1, 1, 1, 1)
    }
  }

  override def renderTileEntityAt(proxy: tileentity.RobotProxy, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val robot = proxy.robot
    val worldTime = robot.getWorld.getTotalWorldTime + f

    RenderState.pushMatrix()
    RenderState.pushAttrib()
    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

    // If the move started while we were rendering and we have a reference to
    // the *old* proxy the robot would be rendered at the wrong position, so we
    // correct for the offset.
    if (robot.proxy != proxy) {
      GL11.glTranslated(robot.proxy.x - proxy.x, robot.proxy.y - proxy.y, robot.proxy.z - proxy.z)
    }

    if (robot.isAnimatingMove) {
      val remaining = (robot.animationTicksLeft - f) / robot.animationTicksTotal.toDouble
      val delta = robot.moveFrom.get.subtract(robot.getPos)
      GL11.glTranslated(delta.getX * remaining, delta.getY * remaining, delta.getZ * remaining)
    }

    val timeJitter = robot.hashCode ^ 0xFF
    val hover =
      if (robot.isRunning) (Math.sin(timeJitter + worldTime / 20.0) * 0.03).toFloat
      else -0.03f
    GL11.glTranslatef(0, hover, 0)

    RenderState.pushMatrix()

    RenderState.enableDepthMask()
    RenderState.enableEntityLighting()
    RenderState.disableBlend()

    if (robot.isAnimatingTurn) {
      val remaining = (robot.animationTicksLeft - f) / robot.animationTicksTotal.toDouble
      GL11.glRotated(90 * remaining, 0, robot.turnAxis, 0)
    }

    robot.yaw match {
      case EnumFacing.WEST => GL11.glRotatef(-90, 0, 1, 0)
      case EnumFacing.NORTH => GL11.glRotatef(180, 0, 1, 0)
      case EnumFacing.EAST => GL11.glRotatef(90, 0, 1, 0)
      case _ => // No yaw.
    }

    GL11.glTranslatef(-0.5f, -0.5f, -0.5f)

    val offset = timeJitter + worldTime / 20.0
    renderChassis(robot, offset)

    if (MinecraftForgeClient.getRenderPass == 0 && !robot.renderingErrored && x * x + y * y + z * z < 24 * 24) {
      val itemRenderer = Minecraft.getMinecraft.getItemRenderer
      Option(robot.getStackInSlot(0)) match {
        case Some(stack) =>

          RenderState.pushAttrib()
          RenderState.pushMatrix()
          try {
            // Copy-paste from player render code, with minor adjustments for
            // robot scale.

            RenderState.disableCullFace()
            RenderState.enableRescaleNormal()

            GL11.glScalef(1, -1, -1)
            GL11.glTranslatef(0, -8 * 0.0625F - 0.0078125F, -0.5F)

            if (robot.isAnimatingSwing) {
              val wantedTicksPerCycle = 10
              val cycles = math.max(robot.animationTicksTotal / wantedTicksPerCycle, 1)
              val ticksPerCycle = robot.animationTicksTotal / cycles
              val remaining = (robot.animationTicksLeft - f) / ticksPerCycle.toDouble
              GL11.glRotatef((Math.sin((remaining - remaining.toInt) * Math.PI) * 45).toFloat, 1, 0, 0)
            }

            val item = stack.getItem
            val minecraft = Minecraft.getMinecraft

            if (item.isInstanceOf[ItemBlock] && minecraft.getBlockRendererDispatcher.isRenderTypeChest(Block.getBlockFromItem(item), stack.getMetadata)) {
              GlStateManager.translate(0.0F, 0.1875F, -0.3125F)
              GlStateManager.rotate(20.0F, 1.0F, 0.0F, 0.0F)
              GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F)
              val scale = 0.375F
              GlStateManager.scale(scale, -scale, scale)
            }
            else if (item eq Items.bow) {
              GlStateManager.translate(-0.1F, -0.125F, -0.1f)
              val scale = 0.625F
              GlStateManager.scale(scale, -scale, scale)
              GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F)
              GlStateManager.rotate(10.0F, 0.0F, 1.0F, 0.0F)
            }
            else if (item.isFull3D) {
              if (item.shouldRotateAroundWhenRendering) {
                GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F)
                GlStateManager.translate(0.0F, -0.125F, 0.0F)
              }
              GlStateManager.translate(0.0F, 0.1F, 0.0F)
              val scale = 0.625F
              GlStateManager.scale(scale, -scale, scale)
              GlStateManager.rotate(-2.0F, 0.0F, 1.0F, 0.0F)
              GlStateManager.rotate(-5.0F, 0.0F, 0.0F, 1.0F)
            }

            itemRenderer.renderItem(Minecraft.getMinecraft.thePlayer, stack, TransformType.THIRD_PERSON)
          }
          catch {
            case e: Throwable =>
              OpenComputers.log.warn("Failed rendering equipped item.", e)
              robot.renderingErrored = true
          }
          RenderState.enableCullFace()
          RenderState.disableRescaleNormal()
          RenderState.popMatrix()
          RenderState.popAttrib()
        case _ =>
      }

      if (MinecraftForgeClient.getRenderPass == 0) {
        lazy val availableSlots = slotNameMapping.keys.to[mutable.Set]
        lazy val wildcardRenderers = mutable.Buffer.empty[(ItemStack, UpgradeRenderer)]
        lazy val slotMapping = Array.fill(mountPoints.length)(null: (ItemStack, UpgradeRenderer))

        val renderers = (robot.componentSlots ++ robot.containerSlots).map(robot.getStackInSlot).
          collect { case stack if stack != null && stack.getItem.isInstanceOf[UpgradeRenderer] => (stack, stack.getItem.asInstanceOf[UpgradeRenderer]) }

        for ((stack, renderer) <- renderers) {
          val preferredSlot = renderer.computePreferredMountPoint(stack, robot, availableSlots)
          if (availableSlots.remove(preferredSlot)) {
            slotMapping(slotNameMapping(preferredSlot)) = (stack, renderer)
          }
          else if (preferredSlot == MountPointName.Any) {
            wildcardRenderers += ((stack, renderer))
          }
        }

        var firstEmpty = slotMapping.indexOf(null)
        for (entry <- wildcardRenderers if firstEmpty >= 0) {
          slotMapping(firstEmpty) = entry
          firstEmpty = slotMapping.indexOf(null)
        }

        for ((info, mountPoint) <- (slotMapping, mountPoints).zipped if info != null) try {
          val (stack, renderer) = info
          RenderState.pushMatrix()
          GL11.glTranslatef(0.5f, 0.5f, 0.5f)
          renderer.render(stack, mountPoint, robot, f)
          RenderState.popMatrix()
        }
        catch {
          case e: Throwable =>
            OpenComputers.log.warn("Failed rendering equipped upgrade.", e)
            robot.renderingErrored = true
        }
      }
    }
    RenderState.popMatrix()

    val name = robot.name
    if (Settings.get.robotLabels && MinecraftForgeClient.getRenderPass == 1 && !Strings.isNullOrEmpty(name) && x * x + y * y + z * z < RendererLivingEntity.NAME_TAG_RANGE) {
      RenderState.pushMatrix()

      // This is pretty much copy-pasta from the entity's label renderer.
      val t = Tessellator.getInstance
      val r = t.getWorldRenderer
      val f = getFontRenderer
      val scale = 1.6f / 60f
      val width = f.getStringWidth(name)
      val halfWidth = width / 2

      GL11.glTranslated(0, 0.8, 0)
      GL11.glNormal3f(0, 1, 0)
      GL11.glColor3f(1, 1, 1)

      GL11.glRotatef(-rendererDispatcher.entityYaw, 0, 1, 0)
      GL11.glRotatef(rendererDispatcher.entityPitch, 1, 0, 0)
      GL11.glScalef(-scale, -scale, scale)

      RenderState.makeItBlend()
      RenderState.disableDepthMask()
      RenderState.disableLighting()
      GL11.glDisable(GL11.GL_TEXTURE_2D)

      r.begin(7, DefaultVertexFormats.POSITION_COLOR)
      r.pos(-halfWidth - 1, -1, 0).color(0, 0, 0, 0.5f).endVertex()
      r.pos(-halfWidth - 1, 8, 0).color(0, 0, 0, 0.5f).endVertex()
      r.pos(halfWidth + 1, 8, 0).color(0, 0, 0, 0.5f).endVertex()
      r.pos(halfWidth + 1, -1, 0).color(0, 0, 0, 0.5f).endVertex()
      t.draw()

      GL11.glEnable(GL11.GL_TEXTURE_2D) // For the font.
      f.drawString((if (EventHandler.isItTime) EnumChatFormatting.OBFUSCATED.toString else "") + name, -halfWidth, 0, 0xFFFFFFFF)

      RenderState.enableDepthMask()
      RenderState.enableLighting()
      RenderState.disableBlend()

      RenderState.popMatrix()
    }

    RenderState.popMatrix()
    RenderState.popAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
