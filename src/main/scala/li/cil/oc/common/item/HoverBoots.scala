package li.cil.oc.common.item

import li.cil.oc.Settings
import li.cil.oc.client.renderer.item.HoverBootRenderer
import li.cil.oc.common.item.data.HoverBootsData
import li.cil.oc.util.ItemColorizer
import net.minecraft.block.BlockCauldron
import net.minecraft.client.model.ModelBiped
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.EnumRarity
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect
import net.minecraft.world.World
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class HoverBoots extends ItemArmor(ItemArmor.ArmorMaterial.DIAMOND, 0, EntityEquipmentSlot.FEET) with traits.SimpleItem with traits.Chargeable {
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
  override def getArmorModel(entityLiving: EntityLivingBase, itemStack: ItemStack, armorSlot: EntityEquipmentSlot, _default: ModelBiped): ModelBiped = {
    if (armorSlot == armorType) {
      HoverBootRenderer.lightColor = if (ItemColorizer.hasColor(itemStack)) ItemColorizer.getColor(itemStack) else 0x66DD55
      HoverBootRenderer
    }
    else super.getArmorModel(entityLiving, itemStack, armorSlot, _default)
  }

  override def getArmorTexture(stack: ItemStack, entity: Entity, slot: EntityEquipmentSlot, subType: String): String = {
    if (entity.world.isRemote) HoverBootRenderer.texture.toString
    else null
  }

  override def onArmorTick(world: World, player: EntityPlayer, stack: ItemStack): Unit = {
    super.onArmorTick(world, player, stack)
    if (!Settings.get.ignorePower && player.getActivePotionEffect(Potion.getPotionFromResourceLocation("slowness")) == null && getCharge(stack) == 0) {
      player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("slowness"), 20, 1))
    }
  }

  override def onEntityItemUpdate(entity: EntityItem): Boolean = {
    if (entity != null && entity.world != null && !entity.world.isRemote && ItemColorizer.hasColor(entity.getEntityItem)) {
      val pos = entity.getPosition
      val state = entity.world.getBlockState(pos)
      if (state.getBlock == Blocks.CAULDRON) {
        val level = state.getValue(BlockCauldron.LEVEL).toInt
        if (level > 0) {
          ItemColorizer.removeColor(entity.getEntityItem)
          entity.world.setBlockState(pos, state.withProperty(BlockCauldron.LEVEL, Int.box(level - 1)), 3)
          return true
        }
      }
    }
    super.onEntityItemUpdate(entity)
  }

//  override def getColorFromItemStack(itemStack: ItemStack, pass: Int): Int = {
//    if (pass == 1) {
//      return if (ItemColorizer.hasColor(itemStack)) ItemColorizer.getColor(itemStack) else 0x66DD55
//    }
//    super.getColorFromItemStack(itemStack, pass)
//  }

  override def showDurabilityBar(stack: ItemStack): Boolean = true

  override def getDurabilityForDisplay(stack: ItemStack): Double = {
    val data = new HoverBootsData(stack)
    1 - data.charge / Settings.get.bufferHoverBoots
  }

  override def getMaxDamage(stack: ItemStack): Int = Settings.get.bufferHoverBoots.toInt

  // Always show energy bar.
  override def isDamaged(stack: ItemStack): Boolean = true

  // Contradictory as it may seem with the above, this avoids actual damage value changing.
  override def isDamageable: Boolean = false

  override def setDamage(stack: ItemStack, damage: Int): Unit = {
    // Subtract energy when taking damage instead of actually damaging the item.
    charge(stack, -damage, simulate = false)

    // Set to 0 for old boots that may have been damaged before.
    super.setDamage(stack, 0)
  }
}
