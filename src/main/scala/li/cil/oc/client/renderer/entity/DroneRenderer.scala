package li.cil.oc.client.renderer.entity

import net.minecraft.client.renderer.entity.Render
import net.minecraft.entity.Entity
import org.lwjgl.opengl.GL11

object DroneRenderer extends Render {
  val model = new ModelQuadcopter()

  override def doRender(entity: Entity, x: Double, y: Double, z: Double, yaw: Float, dt: Float) {
    bindEntityTexture(entity)
    GL11.glPushMatrix()
    GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

    GL11.glTranslated(x, y + 2 / 16f, z)

    model.render(entity, 0, 0, 0, 0, 0, dt)

    GL11.glPopAttrib()
    GL11.glPopMatrix()
  }

  override def getEntityTexture(entity: Entity) = model.texture
}
