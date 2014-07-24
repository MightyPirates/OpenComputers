package li.cil.oc.client.renderer

import java.util
import java.util.concurrent.{Callable, TimeUnit}

import com.google.common.cache.CacheBuilder
import cpw.mods.fml.common.{ITickHandler, TickType}
import li.cil.oc.api.event.RobotRenderEvent
import li.cil.oc.client.renderer.tileentity.RobotRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.Entity
import net.minecraftforge.client.event.RenderPlayerEvent
import net.minecraftforge.event.{EventPriority, ForgeSubscribe}
import org.lwjgl.opengl.{GL11, GL12}

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object PetRenderer extends ITickHandler {
  val hidden = mutable.Set.empty[String]

  private val entitledPlayers = Map(
    "Kethtar" -> (0.3, 0.9, 0.6),
    "Jodarion" -> (1.0, 0.0, 0.0),
    "DaKaTotal" -> (0.5, 0.7, 1.0)
  )

  private val petLocations = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(5, TimeUnit.SECONDS).
    asInstanceOf[CacheBuilder[Entity, PetLocation]].
    build[Entity, PetLocation]()

  private var rendering: Option[(Double, Double, Double)] = None

  @ForgeSubscribe
  def onPlayerRender(e: RenderPlayerEvent.Pre) {
    val name = e.entityPlayer.getCommandSenderName
    if (hidden.contains(name) || !entitledPlayers.contains(name)) return
    rendering = Some(entitledPlayers(name))

    val worldTime = e.entityPlayer.getEntityWorld.getTotalWorldTime
    val timeJitter = e.entityPlayer.hashCode ^ 0xFF
    val offset = timeJitter + worldTime / 20.0
    val hover = (math.sin(timeJitter + (worldTime + e.partialRenderTick) / 20.0) * 0.03).toFloat

    val location = petLocations.get(e.entityPlayer, new Callable[PetLocation] {
      override def call() = new PetLocation(e.entityPlayer)
    })

    GL11.glPushMatrix()
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)
    if (e.entityPlayer != Minecraft.getMinecraft.thePlayer) {
      val localPos = Minecraft.getMinecraft.thePlayer.getPosition(e.partialRenderTick)
      val playerPos = e.entityPlayer.getPosition(e.partialRenderTick)
      val correction = 1.62 - (if (e.entityPlayer.isSneaking) 0.125 else 0)
      GL11.glTranslated(
        playerPos.xCoord - localPos.xCoord,
        playerPos.yCoord - localPos.yCoord + correction,
        playerPos.zCoord - localPos.zCoord)
    }

    GL11.glEnable(GL11.GL_LIGHTING)
    GL11.glDisable(GL11.GL_BLEND)
    GL11.glEnable(GL12.GL_RESCALE_NORMAL)
    GL11.glColor4f(1, 1, 1, 1)

    location.applyInterpolatedTransformations(e.partialRenderTick)

    GL11.glScalef(0.3f, 0.3f, 0.3f)
    GL11.glTranslatef(0, hover, 0)

    RobotRenderer.renderChassis(null, offset, isRunningOverride = true)

    GL11.glPopAttrib()
    GL11.glPopMatrix()

    rendering = None
  }

  @ForgeSubscribe(priority = EventPriority.LOWEST)
  def onRobotRender(e: RobotRenderEvent) {
    rendering match {
      case Some((r, g, b)) => GL11.glColor3d(r, g, b)
      case _ =>
    }
  }

  private class PetLocation(val owner: Entity) {
    var x = 0.0
    var y = 0.0
    var z = 0.0
    var yaw = owner.rotationYaw

    var lastX = x
    var lastY = y
    var lastZ = z
    var lastYaw = yaw

    def update() {
      val dx = owner.lastTickPosX - owner.posX
      val dy = owner.lastTickPosY - owner.posY
      val dz = owner.lastTickPosZ - owner.posZ
      val dYaw = owner.rotationYaw - yaw
      lastX = x
      lastY = y
      lastZ = z
      lastYaw = yaw
      x += dx
      y += dy
      z += dz
      x *= 0.05
      y *= 0.05
      z *= 0.05
      yaw += dYaw * 0.2f
    }

    def applyInterpolatedTransformations(dt: Float) {
      val ix = lastX + (x - lastX) * dt
      val iy = lastY + (y - lastY) * dt
      val iz = lastZ + (z - lastZ) * dt
      val iYaw = lastYaw + (yaw - lastYaw) * dt

      GL11.glTranslated(ix, iy, iz)
      if (!isForInventory) {
        GL11.glRotatef(-iYaw, 0, 1, 0)
      }
      else {
        GL11.glRotatef(-owner.rotationYaw, 0, 1, 0)
      }
      GL11.glTranslated(0.3, -0.1, -0.2)
    }

    // Someone please tell me a cleaner solution than this...
    private def isForInventory = new Exception().getStackTrace.exists(_.getClassName == classOf[GuiContainer].getName)
  }

  override def getLabel = "OpenComputers.PetRenderer"

  override def ticks() = util.EnumSet.of(TickType.CLIENT)

  override def tickStart(tickType: util.EnumSet[TickType], tickData: AnyRef*) {
    petLocations.cleanUp()
    for (pet <- petLocations.asMap.values) {
      pet.update()
    }
  }

  override def tickEnd(tickType: util.EnumSet[TickType], tickData: AnyRef*) {}
}
