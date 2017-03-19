package li.cil.oc.client.renderer.item

import li.cil.oc.Settings
import li.cil.oc.util.RenderState
import net.minecraft.client.model.ModelBase
import net.minecraft.client.model.ModelBiped
import net.minecraft.client.model.ModelRenderer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

object HoverBootRenderer extends ModelBiped {
  val texture = new ResourceLocation(Constants.resourceDomain, "textures/model/drone.png")

  val bootLeft = new ModelRenderer(this, "bootLeft")
  val bootRight = new ModelRenderer(this, "bootRight")
  val body = new ModelRenderer(this, "body")
  val wing0 = new ModelRenderer(this, "wing0")
  val wing1 = new ModelRenderer(this, "wing1")
  val wing2 = new ModelRenderer(this, "wing2")
  val wing3 = new ModelRenderer(this, "wing3")
  val light0 = new LightModelRenderer(this, "light0")
  val light1 = new LightModelRenderer(this, "light1")
  val light2 = new LightModelRenderer(this, "light2")
  val light3 = new LightModelRenderer(this, "light3")

  bootLeft.addChild(body)
  bootLeft.addChild(wing0)
  bootLeft.addChild(wing1)

  bootRight.addChild(body)
  bootRight.addChild(wing2)
  bootRight.addChild(wing3)

  wing0.addChild(light0)
  wing1.addChild(light1)
  wing2.addChild(light2)
  wing3.addChild(light3)

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

  bootRight.offsetY = 10.1f / 16
  bootLeft.offsetY = 10.11f / 16f

  body.addBox("top", -3, 1, -3, 6, 1, 6).rotateAngleY = math.toRadians(45).toFloat
  body.addBox("middle", -1, 0, -1, 2, 1, 2).rotateAngleY = math.toRadians(45).toFloat
  body.addBox("bottom", -2, -1, -2, 4, 1, 4).rotateAngleY = math.toRadians(45).toFloat
  wing0.addBox("flap0", -1, 0, -7, 6, 1, 6)
  wing0.addBox("pin0", 0, -1, -3, 1, 3, 1)
  wing1.addBox("flap1", -1, 0, 1, 6, 1, 6)
  wing1.addBox("pin1", 0, -1, 2, 1, 3, 1)
  wing2.addBox("flap2", -5, 0, 1, 6, 1, 6)
  wing2.addBox("pin2", -1, -1, 2, 1, 3, 1)
  wing3.addBox("flap3", -5, 0, -7, 6, 1, 6)
  wing3.addBox("pin3", -1, -1, -3, 1, 3, 1)

  light0.addBox("flap0", -1, 0, -7, 6, 1, 6)
  light1.addBox("flap1", -1, 0, 1, 6, 1, 6)
  light2.addBox("flap2", -5, 0, 1, 6, 1, 6)
  light3.addBox("flap3", -5, 0, -7, 6, 1, 6)

  // No drone textured legs, thank you very much.
  bipedLeftLeg.cubeList.clear()
  bipedRightLeg.cubeList.clear()

  bipedLeftLeg.addChild(bootLeft)
  bipedRightLeg.addChild(bootRight)

  bipedHead.isHidden = true
  bipedHeadwear.isHidden = true
  bipedBody.isHidden = true
  bipedRightArm.isHidden = true
  bipedLeftArm.isHidden = true

  var lightColor = 0x66DD55

  override def render(entity: Entity, f0: Float, f1: Float, f2: Float, f3: Float, f4: Float, f5: Float): Unit = {
    // Because Forge is being a dummy...
    isSneak = entity.isSneaking
    // Because Forge is being an even bigger dummy...
    isChild = false
    super.render(entity, f0, f1, f2, f3, f4, f5)
  }

  class LightModelRenderer(modelBase: ModelBase, name: String) extends ModelRenderer(modelBase, name) {
    override def render(dt: Float): Unit = {
      RenderState.pushAttrib()
      GlStateManager.disableLighting()
      RenderState.disableEntityLighting()
      GlStateManager.depthFunc(GL11.GL_LEQUAL)
      RenderState.makeItBlend()
      GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE)
      val r = ((lightColor >>> 16) & 0xFF) / 255f
      val g = ((lightColor >>> 8) & 0xFF) / 255f
      val b = ((lightColor >>> 0) & 0xFF) / 255f
      GlStateManager.color(r, g, b)

      super.render(dt)

      RenderState.disableBlend()
      GlStateManager.enableLighting()
      RenderState.enableEntityLighting()
      GlStateManager.color(1, 1, 1)
      RenderState.popAttrib()
    }
  }

}
