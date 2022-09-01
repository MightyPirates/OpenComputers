package li.cil.oc.client.renderer.entity

import li.cil.oc.client.Textures
import li.cil.oc.common.entity.Drone
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.{Render, RenderManager}

class DroneRenderer(manager: RenderManager) extends Render[Drone](manager) {
  val model = new ModelQuadcopter()

  override def doRender(entity: Drone, x: Double, y: Double, z: Double, yaw: Float, dt: Float) {
    bindEntityTexture(entity)
    GlStateManager.pushMatrix()
    RenderState.pushAttrib()

    GlStateManager.translate(x, y + 2 / 16f, z)

    model.render(entity, 0, 0, 0, 0, 0, dt)

    RenderState.popAttrib()
    GlStateManager.popMatrix()
  }

  override def getEntityTexture(entity: Drone) = Textures.Model.Drone
}
