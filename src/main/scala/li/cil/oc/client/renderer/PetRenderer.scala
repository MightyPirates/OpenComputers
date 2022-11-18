package li.cil.oc.client.renderer

import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit

import com.google.common.cache.CacheBuilder
import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.api.event.RobotRenderEvent
import li.cil.oc.client.renderer.tileentity.RobotRenderer
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher
import net.minecraft.entity.Entity
import net.minecraft.util.math.vector.Vector3d
import net.minecraft.util.math.vector.Vector3f
import net.minecraftforge.client.event.RenderPlayerEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.event.TickEvent.ClientTickEvent

import scala.collection.convert.ImplicitConversionsToScala._
import scala.collection.mutable

object PetRenderer {
  val hidden = mutable.Set.empty[String]

  var isInitialized = false

  // http://goo.gl/frLWYR
  private val entitledPlayers = Map(
    "9f1f262f-0d68-4e13-9161-9eeaf4a0a1a8" ->(0.3, 0.9, 0.6), // Sangar
    "18f8bed4-f027-44af-8947-6a3a2317645a" ->(1.0, 0.0, 0.0), // Jodarion
    "36123742-2cf6-4cfc-8b65-278581b3caeb" ->(0.5, 0.7, 1.0), // DaKaTotal
    "2c0c214b-96f4-4565-b513-de90d5fbc977" ->(1.0, 0.0, 0.0), // MichiRavencroft
    "f3ba6ec8-c280-4950-bb08-1fcb2eab3a9c" ->(0.18, 0.95, 0.922), // Vexatos
    "9d636bdd-b9f4-4b80-b9ce-586ca04bd4f3" ->(0.8, 0.77, 0.75), // StoneNomad
    "23c7ed71-fb13-4abe-abe7-f355e1de6e62" ->(0.3, 0.3, 1.0), // LizzyTheSiren
    "076541f1-f10a-46de-a127-dfab8adfbb75" ->(0.2, 1.0, 0.1), // vifino
    "e7e90198-0ccf-4662-a827-192ec8f4419d" ->(0.0, 0.2, 0.6), // Izaya
    "f514ee69-7bbb-4e46-9e94-d8176324cec2" ->(0.098, 0.471, 0.784), // Wobbo
    "f812c043-78ba-4324-82ae-e8f05c52ae6e" ->(0.1, 0.8, 0.5), // payonel
    "1db17ee7-8830-4bac-8018-de154340aae6" ->(0.0, 0.5, 1.0) // Kosmos
  )

  private val petLocations = com.google.common.cache.CacheBuilder.newBuilder().
    expireAfterAccess(5, TimeUnit.SECONDS).
    asInstanceOf[CacheBuilder[Entity, PetLocation]].
    build[Entity, PetLocation]()

  private var rendering: Option[(Double, Double, Double)] = None

  @SubscribeEvent
  def onPlayerRender(e: RenderPlayerEvent.Pre) {
    val uuid = e.getPlayer.getUUID.toString
    if (hidden.contains(uuid) || !entitledPlayers.contains(uuid)) return
    rendering = Some(entitledPlayers(uuid))

    val worldTime = e.getPlayer.level.getGameTime
    val timeJitter = e.getPlayer.hashCode ^ 0xFF
    val offset = timeJitter + worldTime / 20.0
    val hover = (math.sin(timeJitter + (worldTime + e.getPartialRenderTick) / 20.0) * 0.03).toFloat

    val location = petLocations.get(e.getPlayer, new Callable[PetLocation] {
      override def call() = new PetLocation(e.getPlayer)
    })

    val stack = e.getMatrixStack
    stack.pushPose()
    val self = Minecraft.getInstance.player
    val other = e.getPlayer
    val px = other.xOld + (other.getX - other.xOld) * e.getPartialRenderTick
    val py = other.yOld + (other.getY - other.yOld) * e.getPartialRenderTick + other.getEyeHeight(other.getPose)
    val pz = other.zOld + (other.getZ - other.zOld) * e.getPartialRenderTick
    stack.translate(px - self.getX, py - self.getY, pz - self.getZ)

    location.applyInterpolatedTransformations(stack, e.getPartialRenderTick)

    stack.scale(0.3f, 0.3f, 0.3f)
    stack.translate(0, hover, 0)

    RobotRenderer.renderChassis(stack, e.getBuffers, e.getLight, offset, isRunningOverride = true)

    stack.popPose()

    rendering = None
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  def onRobotRender(e: RobotRenderEvent) {
    rendering match {
      case Some((r, g, b)) => {
        e.setLightColor(r.toFloat, g.toFloat, b.toFloat)
        e.multiplyColors(r.toFloat, g.toFloat, b.toFloat)
      }
      case _ =>
    }
  }

  private class PetLocation(val owner: Entity) {
    var x = 0.0
    var y = 0.0
    var z = 0.0
    var yaw = owner.yRot

    var lastX = x
    var lastY = y
    var lastZ = z
    var lastYaw = yaw

    def update() {
      val dx = owner.xOld - owner.getX
      val dy = owner.yOld - owner.getY
      val dz = owner.zOld - owner.getZ
      val dYaw = owner.yRot - yaw
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

    def applyInterpolatedTransformations(stack: MatrixStack, dt: Float) {
      val ix = lastX + (x - lastX) * dt
      val iy = lastY + (y - lastY) * dt
      val iz = lastZ + (z - lastZ) * dt
      val iYaw = lastYaw + (yaw - lastYaw) * dt

      stack.translate(ix, iy, iz)
      if (!isForInventory) {
        stack.mulPose(Vector3f.YP.rotationDegrees(-iYaw))
      }
      else {
        stack.mulPose(Vector3f.YP.rotationDegrees(-owner.yRot))
      }
      stack.translate(0.3, -0.1, -0.2)
    }

    private def isForInventory = Minecraft.getInstance.screen != null && owner == Minecraft.getInstance.player
  }

  @SubscribeEvent
  def tickStart(e: ClientTickEvent) {
    petLocations.cleanUp()
    for (pet <- petLocations.asMap.values) {
      pet.update()
    }
  }
}
