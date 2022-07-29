package li.cil.oc.common.item

import li.cil.oc.CreativeTab
import li.cil.oc.Settings
import li.cil.oc.client.renderer.item.HoverBootRenderer
import li.cil.oc.common.init.Items
import li.cil.oc.common.item.data.HoverBootsData
import li.cil.oc.util.ItemColorizer
import net.minecraft.block.Blocks
import net.minecraft.block.CauldronBlock
import net.minecraft.client.renderer.entity.model.BipedModel
import net.minecraft.inventory.EquipmentSlotType
import net.minecraft.item.ArmorItem
import net.minecraft.item.ArmorMaterial
import net.minecraft.item.Item
import net.minecraft.item.Item.Properties
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Rarity
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.item.ItemEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.potion.Effect
import net.minecraft.potion.Effects
import net.minecraft.potion.EffectInstance
import net.minecraft.util.NonNullList
import net.minecraft.world.World
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.common.extensions.IForgeItem

class HoverBoots(props: Properties = new Properties().tab(CreativeTab).setNoRepair())
  extends ArmorItem(ArmorMaterial.DIAMOND, EquipmentSlotType.FEET, props) with IForgeItem with traits.SimpleItem with traits.Chargeable {

  @Deprecated
  override def getRarity(stack: ItemStack): Rarity = Rarity.UNCOMMON

  override def maxCharge(stack: ItemStack): Double = Settings.get.bufferHoverBoots

  override def getCharge(stack: ItemStack): Double =
    new HoverBootsData(stack).charge

  override def setCharge(stack: ItemStack, amount: Double): Unit = {
    val data = new HoverBootsData(stack)
    data.charge = math.min(maxCharge(stack), math.max(0, amount))
    data.saveData(stack)
  }

  override def canCharge(stack: ItemStack): Boolean = true

  override def charge(stack: ItemStack, amount: Double, simulate: Boolean): Double = {
    val data = new HoverBootsData(stack)
    traits.Chargeable.applyCharge(amount, data.charge, Settings.get.bufferHoverBoots, used => if (!simulate) {
      data.charge += used
      data.saveData(stack)
    })
  }

  override def fillItemCategory(tab: ItemGroup, list: NonNullList[ItemStack]): Unit = {
    super.fillItemCategory(tab, list)
    if (allowdedIn(tab)) list.add(Items.createChargedHoverBoots())
  }

  @OnlyIn(Dist.CLIENT)
  override def getArmorModel[A <: BipedModel[_]](entityLiving: LivingEntity, itemStack: ItemStack, armorSlot: EquipmentSlotType, _default: A): A = {
    if (armorSlot == slot) {
      HoverBootRenderer.lightColor = if (ItemColorizer.hasColor(itemStack)) ItemColorizer.getColor(itemStack) else 0x66DD55
      HoverBootRenderer.asInstanceOf[A]
    }
    else super.getArmorModel(entityLiving, itemStack, armorSlot, _default)
  }

  override def getArmorTexture(stack: ItemStack, entity: Entity, slot: EquipmentSlotType, subType: String): String = {
    if (entity.level.isClientSide) HoverBootRenderer.texture.toString
    else null
  }

  override def onArmorTick(stack: ItemStack, world: World, player: PlayerEntity): Unit = {
    super.onArmorTick(stack, world, player)
    if (!Settings.get.ignorePower && player.getEffect(Effects.MOVEMENT_SLOWDOWN) == null && getCharge(stack) == 0) {
      player.addEffect(new EffectInstance(Effects.MOVEMENT_SLOWDOWN, 20, 1))
    }
  }

  override def onEntityItemUpdate(stack: ItemStack, entity: ItemEntity): Boolean = {
    if (entity != null && entity.level != null && !entity.level.isClientSide && ItemColorizer.hasColor(stack)) {
      val pos = entity.blockPosition
      val state = entity.level.getBlockState(pos)
      if (state.getBlock == Blocks.CAULDRON) {
        val level = state.getValue(CauldronBlock.LEVEL).toInt
        if (level > 0) {
          ItemColorizer.removeColor(stack)
          entity.level.setBlock(pos, state.setValue(CauldronBlock.LEVEL, Int.box(level - 1)), 3)
          return true
        }
      }
    }
    super.onEntityItemUpdate(stack, entity)
  }

  override def showDurabilityBar(stack: ItemStack): Boolean = true

  override def getDurabilityForDisplay(stack: ItemStack): Double = {
    val data = new HoverBootsData(stack)
    1 - data.charge / Settings.get.bufferHoverBoots
  }

  override def getMaxDamage(stack: ItemStack): Int = Settings.get.bufferHoverBoots.toInt

  // Always show energy bar.
  override def isDamaged(stack: ItemStack): Boolean = true

  // Contradictory as it may seem with the above, this avoids actual damage value changing.
  override def canBeDepleted: Boolean = false

  override def setDamage(stack: ItemStack, damage: Int): Unit = {
    // Subtract energy when taking damage instead of actually damaging the item.
    charge(stack, -damage, simulate = false)

    // Set to 0 for old boots that may have been damaged before.
    super.setDamage(stack, 0)
  }
}
