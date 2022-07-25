package li.cil.oc.client.renderer.item

import com.mojang.blaze3d.matrix.MatrixStack
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.IVertexBuilder
import li.cil.oc.Settings
import li.cil.oc.util.RenderState
import net.minecraft.client.renderer.model.Model
import net.minecraft.client.renderer.model.ModelRenderer
import net.minecraft.client.renderer.entity.model.BipedModel
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

object HoverBootRenderer extends BipedModel[LivingEntity](0.5f) {
  val texture = new ResourceLocation(Settings.resourceDomain, "textures/model/drone.png")

  val bootLeft = new ModelRenderer(this)
  val bootRight = new ModelRenderer(this)
  val droneBody = new ModelRenderer(this)
  val wing0 = new ModelRenderer(this)
  val wing1 = new ModelRenderer(this)
  val wing2 = new ModelRenderer(this)
  val wing3 = new ModelRenderer(this)
  val light0 = new LightModelRenderer(this)
  val light1 = new LightModelRenderer(this)
  val light2 = new LightModelRenderer(this)
  val light3 = new LightModelRenderer(this)

  bootLeft.addChild(droneBody)
  bootLeft.addChild(wing0)
  bootLeft.addChild(wing1)

  bootRight.addChild(droneBody)
  bootRight.addChild(wing2)
  bootRight.addChild(wing3)

  wing0.addChild(light0)
  wing1.addChild(light1)
  wing2.addChild(light2)
  wing3.addChild(light3)

  texWidth = 64
  texHeight = 32

  bootRight.y = 10.1f / 16
  bootLeft.y = 10.11f / 16f

  droneBody.texOffs(0, 23).addBox(-3, 1, -3, 6, 1, 6).yRot = math.toRadians(45).toFloat // top
  droneBody.texOffs(0, 1).addBox(-1, 0, -1, 2, 1, 2).yRot = math.toRadians(45).toFloat // middle
  droneBody.texOffs(0, 17).addBox(-2, -1, -2, 4, 1, 4).yRot = math.toRadians(45).toFloat // bottom
  wing0.texOffs(0, 9).addBox(-1, 0, -7, 6, 1, 6) // flap0
  wing0.texOffs(0, 27).addBox(0, -1, -3, 1, 3, 1) // pin0
  wing1.texOffs(0, 9).addBox(-1, 0, 1, 6, 1, 6) // flap1
  wing1.texOffs(0, 27).addBox(0, -1, 2, 1, 3, 1) // pin1
  wing2.texOffs(0, 9).addBox(-5, 0, 1, 6, 1, 6) // flap2
  wing2.texOffs(0, 27).addBox(-1, -1, 2, 1, 3, 1) // pin2
  wing3.texOffs(0, 9).addBox(-5, 0, -7, 6, 1, 6) // flap3
  wing3.texOffs(0, 27).addBox(-1, -1, -3, 1, 3, 1) // pin3

  light0.texOffs(24, 0).addBox(-1, 0, -7, 6, 1, 6) // flap0
  light1.texOffs(24, 0).addBox(-1, 0, 1, 6, 1, 6) // flap1
  light2.texOffs(24, 0).addBox(-5, 0, 1, 6, 1, 6) // flap2
  light3.texOffs(24, 0).addBox(-5, 0, -7, 6, 1, 6) // flap3

  // No drone textured legs, thank you very much.
  leftLeg = leftLeg.createShallowCopy()
  rightLeg = rightLeg.createShallowCopy()

  leftLeg.addChild(bootLeft)
  rightLeg.addChild(bootRight)

  head.visible = false
  hat.visible = false
  body.visible = false
  rightArm.visible = false
  leftArm.visible = false

  var lightColor = 0x66DD55

  override def setupAnim(entity: LivingEntity, f1: Float, f2: Float, f3: Float, f4: Float, f5: Float): Unit = {
    super.setupAnim(entity, f1, f2, f3, f4, f5)
    // Because Forge is being a dummy...
    crouching = entity.isCrouching
    // Because Forge is being an even bigger dummy...
    young = false
  }

  class LightModelRenderer(modelBase: Model) extends ModelRenderer(modelBase) {
    override def render(stack: MatrixStack, builder: IVertexBuilder, light: Int, overlay: Int, r: Float, g: Float, b: Float, a: Float): Unit = {
      RenderState.pushAttrib()
      RenderSystem.disableLighting()
      RenderState.disableEntityLighting()
      RenderSystem.depthFunc(GL11.GL_LEQUAL)
      RenderState.makeItBlend()
      RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
      val rm = ((lightColor >>> 16) & 0xFF) / 255f
      val gm = ((lightColor >>> 8) & 0xFF) / 255f
      val bm = ((lightColor >>> 0) & 0xFF) / 255f

      super.render(stack, builder, light, overlay, r * rm, g * gm, b * bm, a)

      RenderState.disableBlend()
      RenderSystem.enableLighting()
      RenderState.enableEntityLighting()
      RenderState.popAttrib()
    }
  }

}
