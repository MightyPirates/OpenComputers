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
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.client.renderer.entity.RendererLivingEntity
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer
import net.minecraft.init.Items
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumChatFormatting
import net.minecraft.util.Vec3
import net.minecraftforge.client.IItemRenderer.ItemRenderType._
import net.minecraftforge.client.IItemRenderer.ItemRendererHelper._
import net.minecraftforge.client.MinecraftForgeClient
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.common.util.ForgeDirection
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable

object RobotRenderer extends TileEntitySpecialRenderer {
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
      bindTexture(Textures.blockRobot)
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
        RenderState.disableLighting()

        // Additive blending for the light.
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
        // Light color.
        val lightColor = if (robot != null && robot.info != null) robot.info.lightColor else 0xF23030
        val r = ((lightColor >>> 16) & 0xFF).toByte
        val g = ((lightColor >>> 8) & 0xFF).toByte
        val b = ((lightColor >>> 0) & 0xFF).toByte
        GL11.glColor3ub(r, g, b)

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
      }
    }
  }

  override def renderTileEntityAt(entity: TileEntity, x: Double, y: Double, z: Double, f: Float) {
    RenderState.checkError(getClass.getName + ".renderTileEntityAt: entering (aka: wasntme)")

    val proxy = entity.asInstanceOf[tileentity.RobotProxy]
    val robot = proxy.robot
    val worldTime = entity.getWorldObj.getTotalWorldTime + f

    GL11.glPushMatrix()
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
    GL11.glTranslated(x + 0.5, y + 0.5, z + 0.5)

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

    val timeJitter = robot.hashCode ^ 0xFF
    val hover =
      if (robot.isRunning) (Math.sin(timeJitter + worldTime / 20.0) * 0.03).toFloat
      else -0.03f
    GL11.glTranslatef(0, hover, 0)

    GL11.glPushMatrix()

    GL11.glDepthMask(true)
    GL11.glEnable(GL11.GL_LIGHTING)
    GL11.glDisable(GL11.GL_BLEND)
    GL11.glColor4f(1, 1, 1, 1)

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

    val offset = timeJitter + worldTime / 20.0
    renderChassis(robot, offset)

    if (!robot.renderingErrored && x * x + y * y + z * z < 24 * 24) {
      Option(robot.getStackInSlot(0)) match {
        case Some(stack) =>
          val itemRenderer = RenderManager.instance.itemRenderer

          GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
          GL11.glPushMatrix()
          try {
            // Copy-paste from player render code, with minor adjustments for
            // robot scale.

            GL11.glDisable(GL11.GL_CULL_FACE)
            GL11.glEnable(GL12.GL_RESCALE_NORMAL)

            GL11.glScalef(1, -1, -1)
            GL11.glTranslatef(0, -8 * 0.0625F - 0.0078125F, -0.5F)

            if (robot.isAnimatingSwing) {
              val wantedTicksPerCycle = 10
              val cycles = math.max(robot.animationTicksTotal / wantedTicksPerCycle, 1)
              val ticksPerCycle = robot.animationTicksTotal / cycles
              val remaining = (robot.animationTicksLeft - f) / ticksPerCycle.toDouble
              GL11.glRotatef((Math.sin((remaining - remaining.toInt) * Math.PI) * 45).toFloat, 1, 0, 0)
            }

            val customRenderer = MinecraftForgeClient.getItemRenderer(stack, EQUIPPED)
            val is3D = customRenderer != null && customRenderer.shouldUseRenderHelper(EQUIPPED, stack, BLOCK_3D)

            if (is3D || (stack.getItem.isInstanceOf[ItemBlock] && RenderBlocks.renderItemIn3d(Block.getBlockFromItem(stack.getItem).getRenderType))) {
              val scale = 0.375f
              GL11.glTranslatef(0, 0.1875f, -0.3125f)
              GL11.glRotatef(20, 1, 0, 0)
              GL11.glRotatef(45, 0, 1, 0)
              GL11.glScalef(-scale, -scale, scale)
            }
            else if (stack.getItem == Items.bow) {
              val scale = 0.375f
              GL11.glTranslatef(0, 0.2f, -0.2f)
              GL11.glRotatef(-10, 0, 1, 0)
              GL11.glScalef(scale, -scale, scale)
              GL11.glRotatef(-20, 1, 0, 0)
              GL11.glRotatef(45, 0, 1, 0)
            }
            else if (stack.getItem.isFull3D) {
              val scale = 0.375f
              if (stack.getItem.shouldRotateAroundWhenRendering) {
                GL11.glRotatef(180, 0, 0, 1)
                GL11.glTranslatef(0, -0.125f, 0)
              }
              GL11.glTranslatef(0, 0.1f, 0)
              GL11.glScalef(scale, -scale, scale)
              GL11.glRotatef(-100, 1, 0, 0)
              GL11.glRotatef(45, 0, 1, 0)
            }
            else {
              val scale = 0.375f
              GL11.glTranslatef(0.25f, 0.1875f, -0.1875f)
              GL11.glScalef(scale, scale, scale)
              GL11.glRotatef(60, 0, 0, 1)
              GL11.glRotatef(-90, 1, 0, 0)
              GL11.glRotatef(20, 0, 0, 1)
            }

            val pass = MinecraftForgeClient.getRenderPass
            def renderPass(): Unit = {
              val tint = stack.getItem.getColorFromItemStack(stack, pass)
              val r = ((tint >> 16) & 0xFF) / 255f
              val g = ((tint >> 8) & 0xFF) / 255f
              val b = ((tint >> 0) & 0xFF) / 255f
              GL11.glColor4f(r, g, b, 1)
              itemRenderer.renderItem(Minecraft.getMinecraft.thePlayer, stack, pass)
            }

            if (stack.getItem.requiresMultipleRenderPasses()) {
              val passes = stack.getItem.getRenderPasses(stack.getItemDamage)
              if (pass < passes) {
                renderPass()
              }
              // Tile entities only get two render passes, so if items need
              // more, we have to fake them.
              if (pass == 1 && passes > 2) {
                for (fakePass <- 2 until passes) {
                  renderPass()
                }
              }
            }
            else if (pass == 0) {
              renderPass()
            }
          }
          catch {
            case e: Throwable =>
              OpenComputers.log.warn("Failed rendering equipped item.", e)
              robot.renderingErrored = true
          }
          GL11.glEnable(GL11.GL_CULL_FACE)
          GL11.glDisable(GL12.GL_RESCALE_NORMAL)
          GL11.glPopMatrix()
          GL11.glPopAttrib()
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
          GL11.glPushMatrix()
          GL11.glTranslatef(0.5f, 0.5f, 0.5f)
          renderer.render(stack, mountPoint, robot, f)
          GL11.glPopMatrix()
        }
        catch {
          case e: Throwable =>
            OpenComputers.log.warn("Failed rendering equipped upgrade.", e)
            robot.renderingErrored = true
        }
      }
    }
    GL11.glPopMatrix()

    val name = robot.name
    if (Settings.get.robotLabels && MinecraftForgeClient.getRenderPass == 1 && !Strings.isNullOrEmpty(name) && x * x + y * y + z * z < RendererLivingEntity.NAME_TAG_RANGE) {
      GL11.glPushMatrix()

      // This is pretty much copy-pasta from the entity's label renderer.
      val t = Tessellator.instance
      val f = func_147498_b
      val scale = 1.6f / 60f
      val width = f.getStringWidth(name)
      val halfWidth = width / 2

      GL11.glTranslated(0, 0.8, 0)
      GL11.glNormal3f(0, 1, 0)
      GL11.glColor3f(1, 1, 1)

      GL11.glRotatef(-field_147501_a.field_147562_h, 0, 1, 0)
      GL11.glRotatef(field_147501_a.field_147563_i, 1, 0, 0)
      GL11.glScalef(-scale, -scale, scale)

      RenderState.makeItBlend()
      GL11.glDepthMask(false)
      GL11.glDisable(GL11.GL_LIGHTING)
      GL11.glDisable(GL11.GL_TEXTURE_2D)

      t.startDrawingQuads()
      t.setColorRGBA_F(0, 0, 0, 0.5f)
      t.addVertex(-halfWidth - 1, -1, 0)
      t.addVertex(-halfWidth - 1, 8, 0)
      t.addVertex(halfWidth + 1, 8, 0)
      t.addVertex(halfWidth + 1, -1, 0)
      t.draw

      GL11.glEnable(GL11.GL_TEXTURE_2D) // For the font.
      f.drawString((if (EventHandler.isItTime) EnumChatFormatting.OBFUSCATED.toString else "") + name, -halfWidth, 0, 0xFFFFFFFF)

      GL11.glDepthMask(true)
      GL11.glEnable(GL11.GL_LIGHTING)
      GL11.glDisable(GL11.GL_BLEND)

      GL11.glPopMatrix()
    }

    GL11.glPopMatrix()
    GL11.glPopAttrib()

    RenderState.checkError(getClass.getName + ".renderTileEntityAt: leaving")
  }
}
