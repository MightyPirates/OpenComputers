package li.cil.oc.integration.util

import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.ChatComponentTranslation
import net.minecraft.util.DamageSource
import net.minecraft.util.IChatComponent
import net.minecraft.util.StatCollector

class DamageSourceWithRandomCause(name: String, numCauses: Int) extends DamageSource(name) {
  override def getDeathMessage(damagee: EntityLivingBase): IChatComponent = {
    val damager = damagee.getAttackingEntity
    val format = "death.attack." + damageType + "." + (damagee.worldObj.rand.nextInt(numCauses) + 1)
    val withCauseFormat = format + ".player"
    if (damager != null && StatCollector.canTranslate(withCauseFormat))
      new ChatComponentTranslation(withCauseFormat, damagee.getDisplayName, damager.getDisplayName)
    else
      new ChatComponentTranslation(format, damagee.getDisplayName)
  }
}
