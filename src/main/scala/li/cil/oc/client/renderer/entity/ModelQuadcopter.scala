package li.cil.oc.client.renderer.entity

import li.cil.oc.Settings
import net.minecraft.client.model.ModelBase
import net.minecraft.client.model.ModelRenderer
import net.minecraft.entity.Entity
import net.minecraft.util.ResourceLocation

final class ModelQuadcopter extends ModelBase {
  val texture = new ResourceLocation(Settings.resourceDomain, "textures/entity/drone.png")

  val body = new ModelRenderer(this, "body")
  val wing0 = new ModelRenderer(this, "wing0")
  val wing1 = new ModelRenderer(this, "wing1")
  val wing2 = new ModelRenderer(this, "wing2")
  val wing3 = new ModelRenderer(this, "wing3")

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

  body.addBox("middle", -1F, 0F, -1F, 2, 1, 2)
  body.addBox("top", -3F, -1F, -3F, 6, 1, 6)
  body.addBox("bottom", -2F, 1F, -2F, 4, 1, 4)
  wing0.addBox("flap0", 1F, 0F, -7F, 6, 1, 6)
  wing0.addBox("pin0", 2F, -1F, -3F, 1, 3, 1)
  wing1.addBox("flap1", 1F, 0F, 1F, 6, 1, 6)
  wing1.addBox("pin1", 2F, -1F, 2F, 1, 3, 1)
  wing2.addBox("flap2", -7F, 0F, 1F, 6, 1, 6)
  wing2.addBox("pin2", -3F, -1F, 2F, 1, 3, 1)
  wing3.addBox("flap3", -7F, 0F, -7F, 6, 1, 6)
  wing3.addBox("pin3", -3F, -1F, -3F, 1, 3, 1)

  override def render(entity: Entity, f1: Float, f2: Float, f3: Float, f4: Float, f5: Float, scale: Float) {
    body.render(scale)
    wing0.render(scale)
    wing1.render(scale)
    wing2.render(scale)
    wing3.render(scale)
  }
}
