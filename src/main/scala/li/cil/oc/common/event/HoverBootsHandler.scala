package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Settings
import li.cil.oc.common.item.HoverBoots
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent
import net.minecraftforge.event.entity.living.LivingFallEvent

object HoverBootsHandler {
  @SubscribeEvent
  def onLivingUpdate(e: LivingUpdateEvent): Unit = e.entity match {
    case player: EntityPlayer if !player.isInstanceOf[FakePlayer] =>
      val nbt = player.getEntityData
      val hadHoverBoots = nbt.getBoolean(Settings.namespace + "hasHoverBoots")
      val hasHoverBoots = !player.isSneaking && equippedArmor(player).exists(stack => stack.getItem match {
        case boots: HoverBoots =>
          Settings.get.ignorePower || {
            if (player.onGround && !player.capabilities.isCreativeMode && player.worldObj.getTotalWorldTime % Settings.get.tickFrequency == 0) {
              val velocity = player.motionX * player.motionX + player.motionY * player.motionY + player.motionZ * player.motionZ
              if (velocity > 0.015f) {
                boots.charge(stack, -Settings.get.hoverBootMove, simulate = false)
              }
            }
            boots.getCharge(stack) > 0
          }
        case _ => false
      })
      if (hasHoverBoots != hadHoverBoots) {
        nbt.setBoolean(Settings.namespace + "hasHoverBoots", hasHoverBoots)
        player.stepHeight = if (hasHoverBoots) 1f else 0.5f
      }
      if (hasHoverBoots && !player.onGround && player.fallDistance < 5 && player.motionY < 0) {
        player.motionY *= 0.9f
      }
    case _ => // Ignore.
  }

  @SubscribeEvent
  def onLivingJump(e: LivingJumpEvent): Unit = e.entity match {
    case player: EntityPlayer if !player.isInstanceOf[FakePlayer] && !player.isSneaking =>
      equippedArmor(player).collectFirst {
        case stack if stack.getItem.isInstanceOf[HoverBoots] =>
          val boots = stack.getItem.asInstanceOf[HoverBoots]
          val hoverJumpCost = -Settings.get.hoverBootJump
          val isCreative = Settings.get.ignorePower || player.capabilities.isCreativeMode
          if (isCreative || boots.charge(stack, hoverJumpCost, simulate = true) == 0) {
            if (!isCreative) boots.charge(stack, hoverJumpCost, simulate = false)
            if (player.isSprinting)
              player.addVelocity(player.motionX * 0.5, 0.4, player.motionZ * 0.5)
            else
              player.addVelocity(0, 0.4, 0)
          }
      }
    case _ => // Ignore.
  }

  @SubscribeEvent
  def onLivingFall(e: LivingFallEvent): Unit = if (e.distance > 3) e.entity match {
    case player: EntityPlayer if !player.isInstanceOf[FakePlayer] =>
      equippedArmor(player).collectFirst {
        case stack if stack.getItem.isInstanceOf[HoverBoots] =>
          val boots = stack.getItem.asInstanceOf[HoverBoots]
          val hoverFallCost = -Settings.get.hoverBootAbsorb
          val isCreative = Settings.get.ignorePower || player.capabilities.isCreativeMode
          if (isCreative || boots.charge(stack, hoverFallCost, simulate = true) == 0) {
            if (!isCreative) boots.charge(stack, hoverFallCost, simulate = false)
            e.distance *= 0.3f
          }
      }
    case _ => // Ignore.
  }

  private def equippedArmor(player: EntityPlayer) = (1 to 4).map(player.getEquipmentInSlot).filter(_ != null)
}
