package li.cil.oc.common.item

import cpw.mods.fml.relauncher.Side
import cpw.mods.fml.relauncher.SideOnly
import li.cil.oc.Settings
import li.cil.oc.client.renderer.item.HoverBootRenderer
import li.cil.oc.common.item.data.HoverBootsData
import net.minecraft.client.model.ModelBiped
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack

class HoverBoots extends ItemArmor(ItemArmor.ArmorMaterial.DIAMOND, 0, 3) with traits.SimpleItem with traits.Chargeable {
  setNoRepair()

  override def getRarity(stack: ItemStack): EnumRarity = EnumRarity.UNCOMMON

  override def maxCharge(stack: ItemStack) = Settings.get.bufferHoverBoots

  override def getCharge(stack: ItemStack): Double =
    new HoverBootsData(stack).charge

  override def setCharge(stack: ItemStack, amount: Double): Unit = {
    val data = new HoverBootsData(stack)
    data.charge = math.min(maxCharge(stack), math.max(0, amount))
    data.save(stack)
  }

  override def canCharge(stack: ItemStack): Boolean = true

  override def charge(stack: ItemStack, amount: Double, simulate: Boolean): Double = {
    val data = new HoverBootsData(stack)
    if (amount < 0) {
      val remainder = math.min(0, data.charge + amount)
      if (!simulate) {
        data.charge = math.max(0, data.charge + amount)
        data.save(stack)
      }
      remainder
    }
    else {
      val remainder = -math.min(0, Settings.get.bufferHoverBoots - (data.charge + amount))
      if (!simulate) {
        data.charge = math.min(Settings.get.bufferHoverBoots, data.charge + amount)
        data.save(stack)
      }
      remainder
    }
  }

  @SideOnly(Side.CLIENT)
  override def getArmorModel(entityLiving: EntityLivingBase, itemStack: ItemStack, armorSlot: Int): ModelBiped = {
    if (armorSlot == armorType) HoverBootRenderer
    else super.getArmorModel(entityLiving, itemStack, armorSlot)
  }

  override def getArmorTexture(stack: ItemStack, entity: Entity, slot: Int, subType: String): String = {
    if (entity.worldObj.isRemote) HoverBootRenderer.texture.toString
    else null
  }

  override def getDurabilityForDisplay(stack: ItemStack): Double = {
    val data = new HoverBootsData(stack)
    data.charge / Settings.get.bufferHoverBoots
  }

  override def getMaxDamage(stack: ItemStack): Int = Settings.get.bufferHoverBoots.toInt

  // Always show energy bar.
  override def isDamaged(stack: ItemStack): Boolean = true
}
