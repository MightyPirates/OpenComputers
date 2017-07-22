package li.cil.oc.integration.util

import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.ChatComponentTranslation
import net.minecraft.util.DamageSource
import net.minecraft.util.IChatComponent
import net.minecraft.util.StatCollector

class DamageSourceWithRandomCause(name: String, numCauses: Int) extends DamageSource(name) {
  override def func_151519_b(damagee: EntityLivingBase): IChatComponent = {
    val damager = damagee.func_94060_bK
    val format = "death.attack." + damageType + "." + (damagee.worldObj.rand.nextInt(numCauses) + 1)
    val withCauseFormat = format + ".player"
    if (damager != null && StatCollector.canTranslate(withCauseFormat))
      new ChatComponentTranslation(withCauseFormat, damagee.func_145748_c_, damager.func_145748_c_)
    else
      new ChatComponentTranslation(format, damagee.func_145748_c_)
  }
}
