package li.cil.oc.client.renderer.entity

import li.cil.oc.common.entity.Drone
import li.cil.oc.util.RenderState
import net.minecraft.client.model.ModelBase
import net.minecraft.client.model.ModelRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11

final class ModelQuadcopter extends ModelBase {
  val body = new ModelRenderer(this, "body")
  val wing0 = new ModelRenderer(this, "wing0")
  val wing1 = new ModelRenderer(this, "wing1")
  val wing2 = new ModelRenderer(this, "wing2")
  val wing3 = new ModelRenderer(this, "wing3")
  val light0 = new ModelRenderer(this, "light0")
  val light1 = new ModelRenderer(this, "light1")
  val light2 = new ModelRenderer(this, "light2")
  val light3 = new ModelRenderer(this, "light3")

  textureWidth = 64
  textureHeight = 32

  setTextureOffset("body.middle", 0, 23)
  setTextureOffset("body.top", 0, 1)
  setTextureOffset("body.bottom", 0, 17)
  setTextureOffset("wing0.flap0", 0, 9)
  setTextureOffset("wing0.pin0", 0, 27)
  setTextureOffset("wing1.flap1", 0, 9)
  setTextureOffset("wing1.pin1", 0, 27)
  setTextureOffset("wing2.flap2", 0, 9)
  setTextureOffset("wing2.pin2", 0, 27)
  setTextureOffset("wing3.flap3", 0, 9)
  setTextureOffset("wing3.pin3", 0, 27)

  setTextureOffset("light0.flap0", 24, 0)
  setTextureOffset("light1.flap1", 24, 0)
  setTextureOffset("light2.flap2", 24, 0)
  setTextureOffset("light3.flap3", 24, 0)

  body.addBox("top", -3, 1, -3, 6, 1, 6).rotateAngleY = math.toRadians(45).toFloat
  body.addBox("middle", -1, 0, -1, 2, 1, 2).rotateAngleY = math.toRadians(45).toFloat
  body.addBox("bottom", -2, -1, -2, 4, 1, 4).rotateAngleY = math.toRadians(45).toFloat
  wing0.addBox("flap0", 1, 0, -7, 6, 1, 6)
  wing0.addBox("pin0", 2, -1, -3, 1, 3, 1)
  wing1.addBox("flap1", 1, 0, 1, 6, 1, 6)
  wing1.addBox("pin1", 2, -1, 2, 1, 3, 1)
  wing2.addBox("flap2", -7, 0, 1, 6, 1, 6)
  wing2.addBox("pin2", -3, -1, 2, 1, 3, 1)
  wing3.addBox("flap3", -7, 0, -7, 6, 1, 6)
  wing3.addBox("pin3", -3, -1, -3, 1, 3, 1)

  light0.addBox("flap0", 1, 0, -7, 6, 1, 6)
  light1.addBox("flap1", 1, 0, 1, 6, 1, 6)
  light2.addBox("flap2", -7, 0, 1, 6, 1, 6)
  light3.addBox("flap3", -7, 0, -7, 6, 1, 6)

  private val scale = 1 / 16f
  private val up = new Vec3(0, 1, 0)

  private def doRender(drone: Drone, dt: Float) {
    if (drone.isRunning) {
      val timeJitter = drone.hashCode() ^ 0xFF
      GlStateManager.translate(0, (math.sin(timeJitter + (drone.worldObj.getTotalWorldTime + dt) / 20.0) * (1 / 16f)).toFloat, 0)
    }

    val velocity = new Vec3(drone.motionX, drone.motionY, drone.motionZ)
    val direction = velocity.normalize()
    if (direction.dotProduct(up) < 0.99) {
      // Flying sideways.
      val rotationAxis = direction.crossProduct(up)
      val relativeSpeed = velocity.lengthVector().toFloat / drone.maxVelocity
      GlStateManager.rotate(relativeSpeed * -20, rotationAxis.xCoord.toFloat, rotationAxis.yCoord.toFloat, rotationAxis.zCoord.toFloat)
    }

    GlStateManager.rotate(drone.bodyAngle, 0, 1, 0)

    body.render(scale)

    wing0.rotateAngleX = drone.flapAngles(0)(0)
    wing0.rotateAngleZ = drone.flapAngles(0)(1)
    wing1.rotateAngleX = drone.flapAngles(1)(0)
    wing1.rotateAngleZ = drone.flapAngles(1)(1)
    wing2.rotateAngleX = drone.flapAngles(2)(0)
    wing2.rotateAngleZ = drone.flapAngles(2)(1)
    wing3.rotateAngleX = drone.flapAngles(3)(0)
    wing3.rotateAngleZ = drone.flapAngles(3)(1)

    wing0.render(scale)
    wing1.render(scale)
    wing2.render(scale)
    wing3.render(scale)

    if (drone.isRunning) {
      RenderState.disableEntityLighting()
      GlStateManager.depthFunc(GL11.GL_LEQUAL)

      light0.rotateAngleX = drone.flapAngles(0)(0)
      light0.rotateAngleZ = drone.flapAngles(0)(1)
      light1.rotateAngleX = drone.flapAngles(1)(0)
      light1.rotateAngleZ = drone.flapAngles(1)(1)
      light2.rotateAngleX = drone.flapAngles(2)(0)
      light2.rotateAngleZ = drone.flapAngles(2)(1)
      light3.rotateAngleX = drone.flapAngles(3)(0)
      light3.rotateAngleZ = drone.flapAngles(3)(1)

      // Additive blending for the lights.
      RenderState.makeItBlend()
      GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
      // Light color.
      val lightColor = drone.lightColor
      val r = (lightColor >>> 16) & 0xFF
      val g = (lightColor >>> 8) & 0xFF
      val b = (lightColor >>> 0) & 0xFF
      GlStateManager.color(r / 255f, g / 255f, b / 255f)

      light0.render(scale)
      light1.render(scale)
      light2.render(scale)
      light3.render(scale)

      RenderState.disableBlend()
      RenderState.enableEntityLighting()
      GlStateManager.color(1, 1, 1, 1)
    }
  }

  // For inventory rendering.
  def render() {
    body.render(scale)

    val tilt = math.toRadians(2).toFloat
    wing0.rotateAngleX = tilt
    wing0.rotateAngleZ = tilt
    wing1.rotateAngleX = -tilt
    wing1.rotateAngleZ = tilt
    wing2.rotateAngleX = -tilt
    wing2.rotateAngleZ = -tilt
    wing3.rotateAngleX = tilt
    wing3.rotateAngleZ = -tilt

    wing0.render(scale)
    wing1.render(scale)
    wing2.render(scale)
    wing3.render(scale)

    RenderState.disableEntityLighting()
    GlStateManager.depthFunc(GL11.GL_LEQUAL)

    light0.rotateAngleX = tilt
    light0.rotateAngleZ = tilt
    light1.rotateAngleX = -tilt
    light1.rotateAngleZ = tilt
    light2.rotateAngleX = -tilt
    light2.rotateAngleZ = -tilt
    light3.rotateAngleX = tilt
    light3.rotateAngleZ = -tilt

    RenderState.makeItBlend()
    GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
    GlStateManager.color(0x66 / 255f, 0xDD / 255f, 0x55 / 255f)

    light0.render(scale)
    light1.render(scale)
    light2.render(scale)
    light3.render(scale)

    RenderState.disableBlend()
    RenderState.enableEntityLighting()
    GlStateManager.color(1, 1, 1, 1)
  }

  override def render(entity: Entity, f1: Float, f2: Float, f3: Float, f4: Float, f5: Float, f6: Float): Unit = {
    doRender(entity.asInstanceOf[Drone], f6)
  }
}
