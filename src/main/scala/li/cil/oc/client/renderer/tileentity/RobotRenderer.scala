package li.cil.oc.client.renderer.tileentity

import com.google.common.base.Strings
import com.mojang.realmsclient.gui.ChatFormatting
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.driver.item.UpgradeRenderer
import li.cil.oc.api.driver.item.UpgradeRenderer.MountPointName
import li.cil.oc.api.event.RobotRenderEvent
import li.cil.oc.client.Textures
import li.cil.oc.common.EventHandler
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType
import net.minecraft.client.renderer.entity.RenderLivingBase
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.vertex.VertexFormat
import net.minecraft.client.renderer.vertex.VertexFormatElement
import net.minecraft.init.Items
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.Vec3d
import net.minecraftforge.client.MinecraftForgeClient
import net.minecraftforge.common.MinecraftForge
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable
import scala.language.implicitConversions

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

  private val size = 0.4f
  private val l = 0.5f - size
  private val h = 0.5f + size
  private val gap = 1.0f / 28.0f
  private val gt = 0.5f + gap
  private val gb = 0.5f - gap

  // https://github.com/MinecraftForge/MinecraftForge/issues/2321
  val POSITION_TEX_NORMALF = new VertexFormat()
  val NORMAL_3F = new VertexFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.NORMAL, 3)
  POSITION_TEX_NORMALF.addElement(DefaultVertexFormats.POSITION_3F)
  POSITION_TEX_NORMALF.addElement(DefaultVertexFormats.TEX_2F)
  POSITION_TEX_NORMALF.addElement(NORMAL_3F)

  private implicit def extendWorldRenderer(self: VertexBuffer): ExtendedWorldRenderer = new ExtendedWorldRenderer(self)

  private class ExtendedWorldRenderer(val buffer: VertexBuffer) {
    def normal(normal: Vec3d): VertexBuffer = {
      val normalized = normal.normalize()
      buffer.normal(normalized.xCoord.toFloat, normalized.yCoord.toFloat, normalized.zCoord.toFloat)
    }
  }

  private def drawTop(): Unit = {
    val t = Tessellator.getInstance
    val r = t.getBuffer

    r.begin(GL11.GL_TRIANGLE_FAN, POSITION_TEX_NORMALF)

    r.pos(0.5f, 1, 0.5f).tex(0.25f, 0.25f).normal(new Vec3d(0, 0.2, 1)).endVertex()
    r.pos(l, gt, h).tex(0, 0.5f).normal(new Vec3d(0, 0.2, 1)).endVertex()
    r.pos(h, gt, h).tex(0.5f, 0.5f).normal(new Vec3d(0, 0.2, 1)).endVertex()
    r.pos(h, gt, l).tex(0.5f, 0).normal(new Vec3d(1, 0.2, 0)).endVertex()
    r.pos(l, gt, l).tex(0, 0).normal(new Vec3d(0, 0.2, -1)).endVertex()
    r.pos(l, gt, h).tex(0, 0.5f).normal(new Vec3d(-1, 0.2, 0)).endVertex()

    t.draw()

    r.begin(GL11.GL_QUADS, POSITION_TEX_NORMALF)

    r.pos(l, gt, h).tex(0, 1).normal(0, -1, 0).endVertex()
    r.pos(l, gt, l).tex(0, 0.5).normal(0, -1, 0).endVertex()
    r.pos(h, gt, l).tex(0.5, 0.5).normal(0, -1, 0).endVertex()
    r.pos(h, gt, h).tex(0.5, 1).normal(0, -1, 0).endVertex()

    t.draw()
  }

  private def drawBottom(): Unit = {
    val t = Tessellator.getInstance
    val r = t.getBuffer

    r.begin(GL11.GL_TRIANGLE_FAN, POSITION_TEX_NORMALF)

    r.pos(0.5f, 0.03f, 0.5f).tex(0.75f, 0.25f).normal(new Vec3d(0, -0.2, 1)).endVertex()
    r.pos(l, gb, l).tex(0.5f, 0).normal(new Vec3d(0, -0.2, 1)).endVertex()
    r.pos(h, gb, l).tex(1, 0).normal(new Vec3d(0, -0.2, 1)).endVertex()
    r.pos(h, gb, h).tex(1, 0.5f).normal(new Vec3d(1, -0.2, 0)).endVertex()
    r.pos(l, gb, h).tex(0.5f, 0.5f).normal(new Vec3d(0, -0.2, -1)).endVertex()
    r.pos(l, gb, l).tex(0.5f, 0).normal(new Vec3d(-1, -0.2, 0)).endVertex()

    t.draw()

    r.begin(GL11.GL_QUADS, POSITION_TEX_NORMALF)

    r.pos(l, gb, l).tex(0, 0.5).normal(0, 1, 0).endVertex()
    r.pos(l, gb, h).tex(0, 1).normal(0, 1, 0).endVertex()
    r.pos(h, gb, h).tex(0.5, 1).normal(0, 1, 0).endVertex()
    r.pos(h, gb, l).tex(0.5, 0.5).normal(0, 1, 0).endVertex()

    t.draw()
  }

  def compileList() {
    GL11.glNewList(displayList, GL11.GL_COMPILE)

    drawTop()

    GL11.glEndList()

    GL11.glNewList(displayList + 1, GL11.GL_COMPILE)

    drawBottom()

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
        GlStateManager.translate(0, -2 * gap, 0)
      }
      //GlStateManager.callList(displayList + 1)
      drawBottom()
      if (!isRunning) {
        GlStateManager.translate(0, -2 * gap, 0)
      }

      if (MinecraftForgeClient.getRenderPass > 0) return

      //GlStateManager.callList(displayList)
      drawTop()
      GlStateManager.color(1, 1, 1)

      if (isRunning) {
        RenderState.disableEntityLighting()

        {
          // Additive blending for the light.
          GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
          // Light color.
          val lightColor = if (robot != null && robot.info != null) robot.info.lightColor else 0xF23030
          val r = (lightColor >>> 16) & 0xFF
          val g = (lightColor >>> 8) & 0xFF
          val b = (lightColor >>> 0) & 0xFF
          GlStateManager.color(r / 255f, g / 255f, b / 255f)
        }

        val t = Tessellator.getInstance
        val r = t.getBuffer
        r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
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
      GlStateManager.color(1, 1, 1, 1)
      GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
    }
  }

  override def renderTileEntityAt(proxy: tileentity.RobotProxy, x: Double, y: Double, z: Double, f: Float, damage: Int) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val robot = proxy.robot
    val worldTime = robot.getWorld.getTotalWorldTime + f

    GlStateManager.pushMatrix()
    //GlStateManager.pushAttrib()
    GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5)

    // If the move started while we were rendering and we have a reference to
    // the *old* proxy the robot would be rendered at the wrong position, so we
    // correct for the offset.
    if (robot.proxy != proxy) {
      GlStateManager.translate(robot.proxy.x - proxy.x, robot.proxy.y - proxy.y, robot.proxy.z - proxy.z)
    }

    if (robot.isAnimatingMove) {
      val remaining = (robot.animationTicksLeft - f) / robot.animationTicksTotal.toDouble
      val delta = robot.moveFrom.get.subtract(robot.getPos)
      GlStateManager.translate(delta.getX * remaining, delta.getY * remaining, delta.getZ * remaining)
    }

    val timeJitter = robot.hashCode ^ 0xFF
    val hover =
      if (robot.isRunning) (Math.sin(timeJitter + worldTime / 20.0) * 0.03).toFloat
      else -0.03f
    GlStateManager.translate(0, hover, 0)

    GlStateManager.pushMatrix()

    GlStateManager.depthMask(true)
    RenderState.enableEntityLighting()
    GlStateManager.disableBlend()

    if (robot.isAnimatingTurn) {
      val remaining = (robot.animationTicksLeft - f) / robot.animationTicksTotal.toFloat
      GlStateManager.rotate(90 * remaining, 0, robot.turnAxis, 0)
    }

    robot.yaw match {
      case EnumFacing.WEST => GlStateManager.rotate(-90, 0, 1, 0)
      case EnumFacing.NORTH => GlStateManager.rotate(180, 0, 1, 0)
      case EnumFacing.EAST => GlStateManager.rotate(90, 0, 1, 0)
      case _ => // No yaw.
    }

    GlStateManager.translate(-0.5f, -0.5f, -0.5f)

    val offset = timeJitter + worldTime / 20.0
    renderChassis(robot, offset)

    if (MinecraftForgeClient.getRenderPass == 0 && !robot.renderingErrored && x * x + y * y + z * z < 24 * 24) {
      val itemRenderer = Minecraft.getMinecraft.getItemRenderer
      Option(robot.getStackInSlot(0)) match {
        case Some(stack) =>

          //GlStateManager.pushAttrib()
          GlStateManager.pushMatrix()
          try {
            // Copy-paste from player render code, with minor adjustments for
            // robot scale.

            GlStateManager.disableCull()
            GlStateManager.enableRescaleNormal()

            GlStateManager.scale(1, -1, -1)
            GlStateManager.translate(0, -8 * 0.0625F - 0.0078125F, -0.5F)

            if (robot.isAnimatingSwing) {
              val wantedTicksPerCycle = 10
              val cycles = math.max(robot.animationTicksTotal / wantedTicksPerCycle, 1)
              val ticksPerCycle = robot.animationTicksTotal / cycles
              val remaining = (robot.animationTicksLeft - f) / ticksPerCycle.toDouble
              GlStateManager.rotate((Math.sin((remaining - remaining.toInt) * Math.PI) * 45).toFloat, 1, 0, 0)
            }

            val item = stack.getItem
            if (item.isInstanceOf[ItemBlock]) {
              GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F)
              GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F)
              val scale = 0.625F
              GlStateManager.scale(scale, scale, scale)
            }
            else if (item == Items.BOW) {
              GlStateManager.translate(1.5f/16f, -0.125F, -0.125F)
              GlStateManager.rotate(10.0F, 0.0F, 0.0F, 1.0F)
              val scale = 0.625F
              GlStateManager.scale(scale, -scale, scale)
            }
            else if (item.isFull3D) {
              if (item.shouldRotateAroundWhenRendering) {
                GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F)
                GlStateManager.translate(0.0F, -0.0625F, 0.0F)
              }

              GlStateManager.translate(0.0F, 0.1875F, 0.0F)
              GlStateManager.translate(0.0625F, -0.125F, -2/16F)
              val scale = 0.625F
              GlStateManager.scale(scale, -scale, scale)
              GlStateManager.rotate(0.0F, 1.0F, 0.0F, 0.0F)
              GlStateManager.rotate(0.0F, 0.0F, 1.0F, 0.0F)
            }
            else {
              GlStateManager.translate(0, 2f/16f, 0)
              val scale = 0.875F
              GlStateManager.scale(scale, scale, scale)
              GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F)
              GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F)
            }

            itemRenderer.renderItem(Minecraft.getMinecraft.thePlayer, stack, TransformType.THIRD_PERSON_RIGHT_HAND)
          }
          catch {
            case e: Throwable =>
              OpenComputers.log.warn("Failed rendering equipped item.", e)
              robot.renderingErrored = true
          }
          GlStateManager.enableCull()
          GlStateManager.disableRescaleNormal()
          GlStateManager.popMatrix()
          //GlStateManager.popAttrib()
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
          GlStateManager.pushMatrix()
          GlStateManager.translate(0.5f, 0.5f, 0.5f)
          renderer.render(stack, mountPoint, robot, f)
          GlStateManager.popMatrix()
        }
        catch {
          case e: Throwable =>
            OpenComputers.log.warn("Failed rendering equipped upgrade.", e)
            robot.renderingErrored = true
        }
      }
    }
    GlStateManager.popMatrix()

    val name = robot.name
    if (Settings.get.robotLabels && MinecraftForgeClient.getRenderPass == 1 && !Strings.isNullOrEmpty(name) && x * x + y * y + z * z < RenderLivingBase.NAME_TAG_RANGE) {
      GlStateManager.pushMatrix()

      // This is pretty much copy-pasta from the entity's label renderer.
      val t = Tessellator.getInstance
      val r = t.getBuffer
      val f = getFontRenderer
      val scale = 1.6f / 60f
      val width = f.getStringWidth(name)
      val halfWidth = width / 2

      GlStateManager.translate(0, 0.8, 0)
      GL11.glNormal3f(0, 1, 0)
      GlStateManager.color(1, 1, 1)

      GlStateManager.rotate(-rendererDispatcher.entityYaw, 0, 1, 0)
      GlStateManager.rotate(rendererDispatcher.entityPitch, 1, 0, 0)
      GlStateManager.scale(-scale, -scale, scale)

      RenderState.makeItBlend()
      GlStateManager.depthMask(false)
      GlStateManager.disableLighting()
      GlStateManager.disableTexture2D()

      r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR)
      r.pos(-halfWidth - 1, -1, 0).color(0, 0, 0, 0.5f).endVertex()
      r.pos(-halfWidth - 1, 8, 0).color(0, 0, 0, 0.5f).endVertex()
      r.pos(halfWidth + 1, 8, 0).color(0, 0, 0, 0.5f).endVertex()
      r.pos(halfWidth + 1, -1, 0).color(0, 0, 0, 0.5f).endVertex()
      t.draw()

      GlStateManager.enableTexture2D() // For the font.
      f.drawString((if (EventHandler.isItTime) ChatFormatting.OBFUSCATED.toString else "") + name, -halfWidth, 0, 0xFFFFFFFF)

      GlStateManager.depthMask(true)
      GlStateManager.enableLighting()
      GlStateManager.disableBlend()

      GlStateManager.popMatrix()
    }

    GlStateManager.popMatrix()
    //GlStateManager.popAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
