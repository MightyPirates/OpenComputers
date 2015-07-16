package li.cil.oc.client.renderer

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import li.cil.oc.api.event.RobotRenderEvent
import li.cil.oc.client.renderer.tileentity.RobotRenderer
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.entity.Entity
import net.minecraftforge.client.event.RenderPlayerEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.opengl.GL11

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

object PetRenderer {
  val hidden = mutable.Set.empty[String]

  var isInitialized = false

  // http://goo.gl/frLWYR
  private val entitledPlayers = Map(
    "Sangar" ->(0.3, 0.9, 0.6),
    "Jodarion" ->(1.0, 0.0, 0.0),
    "DaKaTotal" ->(0.5, 0.7, 1.0),
    "MichiRavencroft" ->(1.0, 0.0, 0.0),
    "Vexatos" ->(0.18, 0.95, 0.922),
    "StoneNomad" ->(0.8, 0.77, 0.75),
    "LizzyTheSiren" ->(0.3, 0.3, 1.0),
    "vifino" ->(0.2, 1.0, 0.1),
    "Izaya" ->(0.0, 0.2, 0.6)
  )

  private val petLocations = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(5, TimeUnit.SECONDS).
    asInstanceOf[CacheBuilder[Entity, PetLocation]].
    build[Entity, PetLocation]()

  private var rendering: Option[(Double, Double, Double)] = None

  @SubscribeEvent
  def onPlayerRender(e: RenderPlayerEvent.Pre) {
    val name = e.entityPlayer.getName
    if (hidden.contains(name) || !entitledPlayers.contains(name)) return
    rendering = Some(entitledPlayers(name))

    val worldTime = e.entityPlayer.getEntityWorld.getTotalWorldTime
    val timeJitter = e.entityPlayer.hashCode ^ 0xFF
    val offset = timeJitter + worldTime / 20.0
    val hover = (math.sin(timeJitter + (worldTime + e.partialRenderTick) / 20.0) * 0.03).toFloat

    val location = petLocations.get(e.entityPlayer, new Callable[PetLocation] {
      override def call() = new PetLocation(e.entityPlayer)
    })

    RenderState.pushMatrix()
    RenderState.pushAttrib(GL11.GL_ALL_ATTRIB_BITS)
    val localPos = Minecraft.getMinecraft.thePlayer.getPositionEyes(e.partialRenderTick)
    val playerPos = e.entityPlayer.getPositionEyes(e.partialRenderTick)
    val correction = 1.62 - (if (e.entityPlayer.isSneaking) 0.125 else 0)
    GL11.glTranslated(
      playerPos.xCoord - localPos.xCoord,
      playerPos.yCoord - localPos.yCoord + correction,
      playerPos.zCoord - localPos.zCoord)

    RenderState.enableEntityLighting()
    RenderState.disableBlend()
    RenderState.enableRescaleNormal()
    RenderState.color(1, 1, 1, 1)

    location.applyInterpolatedTransformations(e.partialRenderTick)

    GL11.glScalef(0.3f, 0.3f, 0.3f)
    GL11.glTranslatef(0, hover, 0)

    RobotRenderer.renderChassis(null, offset, isRunningOverride = true)

    RenderState.disableEntityLighting()
    RenderState.disableRescaleNormal()

    RenderState.popAttrib()
    RenderState.popMatrix()

    rendering = None
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  def onRobotRender(e: RobotRenderEvent) {
    rendering match {
      case Some((r, g, b)) => RenderState.color(r.toFloat, g.toFloat, b.toFloat)
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

  @SubscribeEvent
  def tickStart(e: ClientTickEvent) {
    petLocations.cleanUp()
    for (pet <- petLocations.asMap.values) {
      pet.update()
    }
  }
}
