package li.cil.oc.client.renderer.entity

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import li.cil.oc.client.Textures
import li.cil.oc.common.entity.Drone
import li.cil.oc.util.RenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.IRenderTypeBuffer
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererManager
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraftforge.fml.client.registry.IRenderFactory

object DroneRenderer extends IRenderFactory[Drone] {
  val model = new ModelQuadcopter()

  override def createRenderFor(manager: EntityRendererManager) = new DroneRenderer(manager)
}

class DroneRenderer(manager: EntityRendererManager) extends EntityRenderer[Drone](manager) {
  override def render(entity: Drone, yaw: Float, dt: Float, stack: MatrixStack, buffer: IRenderTypeBuffer, light: Int): Unit = {
    val renderType = getRenderType(entity)
    if (renderType != null) {
      stack.pushPose()
      stack.translate(0, 2f / 16f, 0)
      val builder = buffer.getBuffer(renderType)
      DroneRenderer.model.renderToBuffer(stack, builder, light, OverlayTexture.NO_OVERLAY, 1, 1, 1, 1)
      stack.popPose()
    }
    super.render(entity, yaw, dt, stack, buffer, light)
  }

  override def getTextureLocation(entity: Drone) = Textures.Model.Drone

  def getRenderType(entity: Drone): RenderType = {
    val mc = Minecraft.getInstance
    val texture = getTextureLocation(entity)
    if (!entity.isInvisible) DroneRenderer.model.renderType(texture)
    else if (!entity.isInvisibleTo(mc.player)) RenderType.itemEntityTranslucentCull(texture)
    else if (mc.shouldEntityAppearGlowing(entity)) RenderType.outline(texture)
    else null
  }
}
