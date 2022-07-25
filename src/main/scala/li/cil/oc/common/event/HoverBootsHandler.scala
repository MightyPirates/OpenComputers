package li.cil.oc.common.event

import com.mojang.blaze3d.matrix.MatrixStack
import li.cil.oc.Settings
import li.cil.oc.common.item.HoverBoots
import net.minecraft.entity.player.PlayerEntity
import net.minecraftforge.common.util.FakePlayer
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent
import net.minecraftforge.event.entity.living.LivingFallEvent
import net.minecraftforge.eventbus.api.SubscribeEvent

import scala.collection.convert.WrapAsScala._

object HoverBootsHandler {
  @SubscribeEvent
  def onLivingUpdate(e: LivingUpdateEvent): Unit = e.getEntity match {
    case player: PlayerEntity if !player.isInstanceOf[FakePlayer] =>
      val nbt = player.getPersistentData
      val hadHoverBoots = nbt.getBoolean(Settings.namespace + "hasHoverBoots")
      val hasHoverBoots = !player.isCrouching && equippedArmor(player).exists(stack => stack.getItem match {
        case boots: HoverBoots =>
          Settings.get.ignorePower || {
            if (player.isOnGround && !player.isCreative && player.level.getGameTime % Settings.get.tickFrequency == 0) {
              val velocity = player.getDeltaMovement.lengthSqr
              if (velocity > 0.015f) {
                boots.charge(stack, -Settings.get.hoverBootMove, simulate = false)
              }
            }
            boots.getCharge(stack) > 0
          }
        case _ => false
      })
      if (hasHoverBoots != hadHoverBoots) {
        nbt.putBoolean(Settings.namespace + "hasHoverBoots", hasHoverBoots)
        player.maxUpStep = if (hasHoverBoots) 1f else 0.5f
      }
      if (hasHoverBoots && !player.isOnGround && player.fallDistance < 5 && player.getDeltaMovement.y < 0) {
        player.setDeltaMovement(player.getDeltaMovement.multiply(1, 0.9, 1))
      }
    case _ => // Ignore.
  }

  @SubscribeEvent
  def onLivingJump(e: LivingJumpEvent): Unit = e.getEntity match {
    case player: PlayerEntity if !player.isInstanceOf[FakePlayer] && !player.isCrouching =>
      equippedArmor(player).collectFirst {
        case stack if stack.getItem.isInstanceOf[HoverBoots] =>
          val boots = stack.getItem.asInstanceOf[HoverBoots]
          val hoverJumpCost = -Settings.get.hoverBootJump
          val isCreative = Settings.get.ignorePower || player.isCreative
          if (isCreative || boots.charge(stack, hoverJumpCost, simulate = true) == 0) {
            if (!isCreative) boots.charge(stack, hoverJumpCost, simulate = false)
            val motion = player.getDeltaMovement
            if (player.isSprinting)
              player.push(motion.x * 0.5, 0.4, motion.z * 0.5)
            else
              player.push(0, 0.4, 0)
          }
      }
    case _ => // Ignore.
  }

  @SubscribeEvent
  def onLivingFall(e: LivingFallEvent): Unit = if (e.getDistance > 3) e.getEntity match {
    case player: PlayerEntity if !player.isInstanceOf[FakePlayer] =>
      equippedArmor(player).collectFirst {
        case stack if stack.getItem.isInstanceOf[HoverBoots] =>
          val boots = stack.getItem.asInstanceOf[HoverBoots]
          val hoverFallCost = -Settings.get.hoverBootAbsorb
          val isCreative = Settings.get.ignorePower || player.isCreative
          if (isCreative || boots.charge(stack, hoverFallCost, simulate = true) == 0) {
            if (!isCreative) boots.charge(stack, hoverFallCost, simulate = false)
            e.setDistance(e.getDistance * 0.3f)
          }
      }
    case _ => // Ignore.
  }

  private def equippedArmor(player: PlayerEntity) = player.inventory.armor.filter(!_.isEmpty)
}
