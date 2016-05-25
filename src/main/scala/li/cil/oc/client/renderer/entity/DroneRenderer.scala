package li.cil.oc.client.renderer.entity

import li.cil.oc.client.Textures
import li.cil.oc.common.entity.Drone
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.entity.Render

object DroneRenderer extends Render[Drone](Minecraft.getMinecraft.getRenderManager) {
  val model = new ModelQuadcopter()

  override def doRender(entity: Drone, x: Double, y: Double, z: Double, yaw: Float, dt: Float) {
    bindEntityTexture(entity)
    GlStateManager.pushMatrix()
    //GlStateManager.pushAttrib()

    GlStateManager.translate(x, y + 2 / 16f, z)

    model.render(entity, 0, 0, 0, 0, 0, dt)

    //GlStateManager.popAttrib()
    GlStateManager.popMatrix()
  }

  override def getEntityTexture(entity: Drone) = Textures.Model.Drone
}
