package li.cil.oc.integration.util

import net.minecraft.client.resources.I18n
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.DamageSource
import net.minecraft.util.text.ITextComponent
import net.minecraft.util.text.TextComponentTranslation

class DamageSourceWithRandomCause(name: String, numCauses: Int) extends DamageSource(name) {
  override def getDeathMessage(damagee: EntityLivingBase): ITextComponent = {
    val damager = damagee.getAttackingEntity
    val format = "death.attack." + damageType + "." + (damagee.world.rand.nextInt(numCauses) + 1)
    val withCauseFormat = format + ".player"
    if (damager != null && I18n.hasKey(withCauseFormat))
      new TextComponentTranslation(withCauseFormat, damagee.getDisplayName, damager.getDisplayName)
    else
      new TextComponentTranslation(format, damagee.getDisplayName)
  }
}
