package li.cil.oc.client.renderer.tileentity

import java.util.function.Function

import com.google.common.base.Strings
import com.google.common.collect.ImmutableList
import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.IVertexBuilder
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.driver.item.UpgradeRenderer
import li.cil.oc.api.driver.item.UpgradeRenderer.MountPointName
import li.cil.oc.api.event.RobotRenderEvent
import li.cil.oc.client.Textures
import li.cil.oc.common.EventHandler
import li.cil.oc.common.tileentity
import li.cil.oc.util.RenderState
import li.cil.oc.util.StackOption
import li.cil.oc.util.StackOption._
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer._
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType
import net.minecraft.client.renderer.tileentity.TileEntityRenderer
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.vertex.VertexFormat
import net.minecraft.client.renderer.vertex.VertexFormatElement
import net.minecraft.item.Items
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.util.Direction
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.util.math.vector.Vector3f
import net.minecraft.util.math.vector.Matrix3f
import net.minecraft.util.text.TextFormatting
import net.minecraftforge.client.ForgeHooksClient
import net.minecraftforge.common.MinecraftForge
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable
import scala.language.implicitConversions

object RobotRenderer extends Function[TileEntityRendererDispatcher, RobotRenderer] {
  override def apply(dispatch: TileEntityRendererDispatcher) = new RobotRenderer(dispatch)

  private val instance = new RobotRenderer(null)

  def renderChassis(stack: MatrixStack, offset: Double = 0, isRunningOverride: Boolean = false) =
    instance.renderChassis(stack, null, offset, isRunningOverride)
}

class RobotRenderer(dispatch: TileEntityRendererDispatcher) extends TileEntityRenderer[tileentity.RobotProxy](dispatch) {
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
  val NORMAL_3F = new VertexFormatElement(0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.NORMAL, 3)
  val POSITION_TEX_NORMALF = new VertexFormat(ImmutableList.builder()
    .add(DefaultVertexFormats.ELEMENT_POSITION)
    .add(DefaultVertexFormats.ELEMENT_UV0)
    .add(NORMAL_3F)
    .build())

  private implicit def extendWorldRenderer(self: IVertexBuilder): ExtendedWorldRenderer = new ExtendedWorldRenderer(self)

  private class ExtendedWorldRenderer(val buffer: IVertexBuilder) {
    def normal(matrix: Matrix3f, normal: Vector3d): IVertexBuilder = {
      val normalized = normal.normalize()
      buffer.normal(matrix, normalized.x.toFloat, normalized.y.toFloat, normalized.z.toFloat)
    }
  }

  private def drawTop(stack: MatrixStack): Unit = {
    val t = Tessellator.getInstance
    val r = t.getBuilder

    r.begin(GL11.GL_TRIANGLE_FAN, POSITION_TEX_NORMALF)

    r.vertex(stack.last.pose, 0.5f, 1, 0.5f).uv(0.25f, 0.25f).normal(stack.last.normal, new Vector3d(0, 0.2, 1)).endVertex()
    r.vertex(stack.last.pose, l, gt, h).uv(0, 0.5f).normal(stack.last.normal, new Vector3d(0, 0.2, 1)).endVertex()
    r.vertex(stack.last.pose, h, gt, h).uv(0.5f, 0.5f).normal(stack.last.normal, new Vector3d(0, 0.2, 1)).endVertex()
    r.vertex(stack.last.pose, h, gt, l).uv(0.5f, 0).normal(stack.last.normal, new Vector3d(1, 0.2, 0)).endVertex()
    r.vertex(stack.last.pose, l, gt, l).uv(0, 0).normal(stack.last.normal, new Vector3d(0, 0.2, -1)).endVertex()
    r.vertex(stack.last.pose, l, gt, h).uv(0, 0.5f).normal(stack.last.normal, new Vector3d(-1, 0.2, 0)).endVertex()

    t.end()

    r.begin(GL11.GL_QUADS, POSITION_TEX_NORMALF)

    r.vertex(stack.last.pose, l, gt, h).uv(0, 1).normal(stack.last.normal, 0, -1, 0).endVertex()
    r.vertex(stack.last.pose, l, gt, l).uv(0, 0.5f).normal(stack.last.normal, 0, -1, 0).endVertex()
    r.vertex(stack.last.pose, h, gt, l).uv(0.5f, 0.5f).normal(stack.last.normal, 0, -1, 0).endVertex()
    r.vertex(stack.last.pose, h, gt, h).uv(0.5f, 1).normal(stack.last.normal, 0, -1, 0).endVertex()

    t.end()
  }

  private def drawBottom(stack: MatrixStack): Unit = {
    val t = Tessellator.getInstance
    val r = t.getBuilder

    r.begin(GL11.GL_TRIANGLE_FAN, POSITION_TEX_NORMALF)

    r.vertex(stack.last.pose, 0.5f, 0.03f, 0.5f).uv(0.75f, 0.25f).normal(stack.last.normal, new Vector3d(0, -0.2, 1)).endVertex()
    r.vertex(stack.last.pose, l, gb, l).uv(0.5f, 0).normal(stack.last.normal, new Vector3d(0, -0.2, 1)).endVertex()
    r.vertex(stack.last.pose, h, gb, l).uv(1, 0).normal(stack.last.normal, new Vector3d(0, -0.2, 1)).endVertex()
    r.vertex(stack.last.pose, h, gb, h).uv(1, 0.5f).normal(stack.last.normal, new Vector3d(1, -0.2, 0)).endVertex()
    r.vertex(stack.last.pose, l, gb, h).uv(0.5f, 0.5f).normal(stack.last.normal, new Vector3d(0, -0.2, -1)).endVertex()
    r.vertex(stack.last.pose, l, gb, l).uv(0.5f, 0).normal(stack.last.normal, new Vector3d(-1, -0.2, 0)).endVertex()

    t.end()

    r.begin(GL11.GL_QUADS, POSITION_TEX_NORMALF)

    r.vertex(stack.last.pose, l, gb, l).uv(0, 0.5f).normal(stack.last.normal, 0, 1, 0).endVertex()
    r.vertex(stack.last.pose, l, gb, h).uv(0, 1).normal(stack.last.normal, 0, 1, 0).endVertex()
    r.vertex(stack.last.pose, h, gb, h).uv(0.5f, 1).normal(stack.last.normal, 0, 1, 0).endVertex()
    r.vertex(stack.last.pose, h, gb, l).uv(0.5f, 0.5f).normal(stack.last.normal, 0, 1, 0).endVertex()

    t.end()
  }

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

  def renderChassis(stack: MatrixStack, robot: tileentity.Robot = null, offset: Double = 0, isRunningOverride: Boolean = false) {
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
      Textures.bind(Textures.Model.Robot)
      if (!isRunning) {
        stack.translate(0, -2 * gap, 0)
      }
      drawBottom(stack)
      if (!isRunning) {
        stack.translate(0, -2 * gap, 0)
      }

      if (ForgeHooksClient.getWorldRenderPass > 0) return

      drawTop(stack)
      RenderSystem.color3f(1, 1, 1)

      if (isRunning) {
        RenderState.disableEntityLighting()

        {
          // Additive blending for the light.
          RenderState.makeItBlend()
          RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
          // Light color.
          val lightColor = if (robot != null && robot.info != null) robot.info.lightColor else 0xF23030
          val r = (lightColor >>> 16) & 0xFF
          val g = (lightColor >>> 8) & 0xFF
          val b = (lightColor >>> 0) & 0xFF
          RenderSystem.color3f(r / 255f, g / 255f, b / 255f)
        }

        val t = Tessellator.getInstance
        val r = t.getBuilder
        r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        r.vertex(stack.last.pose, l, gt, l).uv(u0, v0).endVertex()
        r.vertex(stack.last.pose, l, gb, l).uv(u0, v1).endVertex()
        r.vertex(stack.last.pose, l, gb, h).uv(u1, v1).endVertex()
        r.vertex(stack.last.pose, l, gt, h).uv(u1, v0).endVertex()

        r.vertex(stack.last.pose, l, gt, h).uv(u0, v0).endVertex()
        r.vertex(stack.last.pose, l, gb, h).uv(u0, v1).endVertex()
        r.vertex(stack.last.pose, h, gb, h).uv(u1, v1).endVertex()
        r.vertex(stack.last.pose, h, gt, h).uv(u1, v0).endVertex()

        r.vertex(stack.last.pose, h, gt, h).uv(u0, v0).endVertex()
        r.vertex(stack.last.pose, h, gb, h).uv(u0, v1).endVertex()
        r.vertex(stack.last.pose, h, gb, l).uv(u1, v1).endVertex()
        r.vertex(stack.last.pose, h, gt, l).uv(u1, v0).endVertex()

        r.vertex(stack.last.pose, h, gt, l).uv(u0, v0).endVertex()
        r.vertex(stack.last.pose, h, gb, l).uv(u0, v1).endVertex()
        r.vertex(stack.last.pose, l, gb, l).uv(u1, v1).endVertex()
        r.vertex(stack.last.pose, l, gt, l).uv(u1, v0).endVertex()
        t.end()

        RenderState.disableBlend()
        RenderState.enableEntityLighting()
      }
      RenderSystem.color4f(1, 1, 1, 1)
    }
  }

  override def render(proxy: tileentity.RobotProxy, f: Float, matrix: MatrixStack, buffer: IRenderTypeBuffer, light: Int, overlay: Int) {
    RenderState.checkError(getClass.getName + ".render: entering (aka: wasntme)")

    val robot = proxy.robot
    val worldTime = proxy.getLevel.getGameTime + f

    matrix.pushPose()
    RenderState.pushAttrib()

    matrix.translate(0.5, 0.5, 0.5)

    // If the move started while we were rendering and we have a reference to
    // the *old* proxy the robot would be rendered at the wrong position, so we
    // correct for the offset.
    if (robot.proxy != proxy) {
      matrix.translate(robot.proxy.x - proxy.x, robot.proxy.y - proxy.y, robot.proxy.z - proxy.z)
    }

    if (robot.isAnimatingMove) {
      val remaining = (robot.animationTicksLeft - f) / robot.animationTicksTotal.toDouble
      val delta = robot.moveFrom.get.subtract(robot.getBlockPos)
      matrix.translate(delta.getX * remaining, delta.getY * remaining, delta.getZ * remaining)
    }

    val timeJitter = robot.hashCode ^ 0xFF
    val hover =
      if (robot.isRunning) (Math.sin(timeJitter + worldTime / 20.0) * 0.03).toFloat
      else -0.03f
    matrix.translate(0, hover, 0)

    matrix.pushPose()

    RenderSystem.depthMask(true)
    RenderState.enableEntityLighting()
    RenderSystem.disableBlend()

    if (robot.isAnimatingTurn) {
      val remaining = (robot.animationTicksLeft - f) / robot.animationTicksTotal.toFloat
      val axis = if (robot.turnAxis < 0) Vector3f.YN else Vector3f.YP
      matrix.mulPose(axis.rotationDegrees(90 * remaining))
    }

    robot.yaw match {
      case Direction.WEST => matrix.mulPose(Vector3f.YP.rotationDegrees(-90))
      case Direction.NORTH => matrix.mulPose(Vector3f.YP.rotationDegrees(180))
      case Direction.EAST => matrix.mulPose(Vector3f.YP.rotationDegrees(90))
      case _ => // No yaw.
    }

    matrix.translate(-0.5f, -0.5f, -0.5f)

    val offset = timeJitter + worldTime / 20.0
    renderChassis(matrix, robot, offset)

    val pos = proxy.getBlockPos
    val dist = Minecraft.getInstance.player.position.distanceToSqr(pos.getX + 0.5, pos.getY + 0.5, pos.getZ + 0.5)
    if (ForgeHooksClient.getWorldRenderPass == 0 && !robot.renderingErrored && dist < 24 * 24) {
      val itemRenderer = Minecraft.getInstance.getItemRenderer
      StackOption(robot.getItem(0)) match {
        case SomeStack(stack) =>

          RenderState.pushAttrib()
          matrix.pushPose()
          try {
            // Copy-paste from player render code, with minor adjustments for
            // robot scale.

            RenderSystem.disableCull()
            RenderSystem.enableRescaleNormal()

            matrix.scale(1, -1, -1)
            matrix.translate(0, -8 * 0.0625F - 0.0078125F, -0.5F)

            if (robot.isAnimatingSwing) {
              val wantedTicksPerCycle = 10
              val cycles = math.max(robot.animationTicksTotal / wantedTicksPerCycle, 1)
              val ticksPerCycle = robot.animationTicksTotal / cycles
              val remaining = (robot.animationTicksLeft - f) / ticksPerCycle.toDouble
              matrix.mulPose(Vector3f.XP.rotationDegrees((Math.sin((remaining - remaining.toInt) * Math.PI) * 45).toFloat))
            }

            val item = stack.getItem
            if (item.isInstanceOf[BlockItem]) {
              matrix.mulPose(Vector3f.XP.rotationDegrees(-90.0F))
              matrix.mulPose(Vector3f.YP.rotationDegrees(180.0F))
              val scale = 0.625F
              matrix.scale(scale, scale, scale)
            }
            else if (item == Items.BOW) {
              matrix.translate(1.5f/16f, -0.125F, -0.125F)
              matrix.mulPose(Vector3f.ZP.rotationDegrees(10.0F))
              val scale = 0.625F
              matrix.scale(scale, -scale, scale)
            }
            else {
              matrix.translate(0, 2f/16f, 0)
              val scale = 0.875F
              matrix.scale(scale, scale, scale)
              matrix.mulPose(Vector3f.YP.rotationDegrees(90.0F))
              matrix.mulPose(Vector3f.ZP.rotationDegrees(180.0F))
            }

            itemRenderer.renderStatic(Minecraft.getInstance.player, stack, TransformType.THIRD_PERSON_RIGHT_HAND, false, matrix, buffer, proxy.getLevel, light, overlay)
          }
          catch {
            case e: Throwable =>
              OpenComputers.log.warn("Failed rendering equipped item.", e)
              robot.renderingErrored = true
          }
          RenderSystem.enableCull()
          RenderSystem.disableRescaleNormal()
          matrix.popPose()
          RenderState.popAttrib()
        case _ =>
      }

      if (ForgeHooksClient.getWorldRenderPass == 0) {
        lazy val availableSlots = slotNameMapping.keys.to[mutable.Set]
        lazy val wildcardRenderers = mutable.Buffer.empty[(ItemStack, UpgradeRenderer)]
        lazy val slotMapping = Array.fill(mountPoints.length)(null: (ItemStack, UpgradeRenderer))

        val renderers = (robot.componentSlots ++ robot.containerSlots).map(robot.getItem).
          collect { case stack if !stack.isEmpty && stack.getItem.isInstanceOf[UpgradeRenderer] => (stack, stack.getItem.asInstanceOf[UpgradeRenderer]) }

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
          matrix.pushPose()
          matrix.translate(0.5f, 0.5f, 0.5f)
          renderer.render(matrix, stack, mountPoint, robot, f)
          matrix.popPose()
        }
        catch {
          case e: Throwable =>
            OpenComputers.log.warn("Failed rendering equipped upgrade.", e)
            robot.renderingErrored = true
        }
      }
    }
    matrix.popPose()

    val name = robot.name
    if (Settings.get.robotLabels && ForgeHooksClient.getWorldRenderPass == 1 && !Strings.isNullOrEmpty(name) && ForgeHooksClient.isNameplateInRenderDistance(null, dist)) {
      matrix.pushPose()

      // This is pretty much copy-pasta from the entity's label renderer.
      val t = Tessellator.getInstance
      val r = t.getBuilder
      val f = Minecraft.getInstance.font
      val scale = 1.6f / 60f
      val width = f.width(name)
      val halfWidth = width / 2

      matrix.translate(0, 0.8, 0)
      GL11.glNormal3f(0, 1, 0)
      RenderSystem.color3f(1, 1, 1)

      matrix.mulPose(Vector3f.YP.rotationDegrees(-renderer.camera.getYRot))
      matrix.mulPose(Vector3f.XP.rotationDegrees(renderer.camera.getXRot))
      matrix.scale(-scale, -scale, scale)

      RenderState.makeItBlend()
      RenderSystem.depthMask(false)
      RenderSystem.disableLighting()
      RenderSystem.disableTexture()

      r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR)
      r.vertex(matrix.last.pose, -halfWidth - 1, -1, 0).color(0, 0, 0, 0.5f).endVertex()
      r.vertex(matrix.last.pose, -halfWidth - 1, 8, 0).color(0, 0, 0, 0.5f).endVertex()
      r.vertex(matrix.last.pose, halfWidth + 1, 8, 0).color(0, 0, 0, 0.5f).endVertex()
      r.vertex(matrix.last.pose, halfWidth + 1, -1, 0).color(0, 0, 0, 0.5f).endVertex()
      t.end()

      RenderSystem.enableTexture() // For the font.
      f.draw(matrix, (if (EventHandler.isItTime) TextFormatting.OBFUSCATED.toString else "") + name, -halfWidth, 0, 0xFFFFFFFF)

      RenderSystem.depthMask(true)
      RenderSystem.enableLighting()
      RenderState.disableBlend()

      matrix.popPose()
    }

    matrix.popPose()
    RenderState.popAttrib()

    RenderState.checkError(getClass.getName + ".render: leaving")
  }
}
