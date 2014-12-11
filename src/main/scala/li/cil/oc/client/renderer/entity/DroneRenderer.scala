package li.cil.oc.client.renderer.entity

import li.cil.oc.common.entity.Drone
import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity
import org.lwjgl.opengl.GL11

object DroneRenderer extends Render {
  val model = new ModelQuadcopter()

  private def doRender(drone: Drone) {
    GL11.glRotatef(drone.bodyAngle, 0, 1, 0)
    model.wing0.rotateAngleX = drone.flapAngles(0)(0)
    model.wing0.rotateAngleZ = drone.flapAngles(0)(1)
    model.wing1.rotateAngleX = drone.flapAngles(1)(0)
    model.wing1.rotateAngleZ = drone.flapAngles(1)(1)
    model.wing2.rotateAngleX = drone.flapAngles(2)(0)
    model.wing2.rotateAngleZ = drone.flapAngles(2)(1)
    model.wing3.rotateAngleX = drone.flapAngles(3)(0)
    model.wing3.rotateAngleZ = drone.flapAngles(3)(1)
    model.render(drone, 0, 0, 0, 0, 0, 1 / 16f)
  }

  override def doRender(entity: Entity, x: Double, y: Double, z: Double, yaw: Float, dt: Float) {
    bindEntityTexture(entity)
    GL11.glPushMatrix()
    GL11.glTranslated(x, y + 2 / 16f, z)

    doRender(entity.asInstanceOf[Drone])

    GL11.glPopMatrix()
  }

  override def getEntityTexture(entity: Entity) = model.texture
}
