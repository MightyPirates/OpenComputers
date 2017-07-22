package li.cil.oc.common.event

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import li.cil.oc.Settings
import li.cil.oc.api.event.RobotMoveEvent
import li.cil.oc.api.event.RobotUsedToolEvent
import li.cil.oc.api.internal.Robot
import li.cil.oc.common.item.Delegator
import li.cil.oc.common.item.UpgradeHover
import li.cil.oc.util.BlockPosition
import li.cil.oc.util.ExtendedWorld._
import net.minecraftforge.common.util.ForgeDirection

object RobotCommonHandler {
  @SubscribeEvent
  def onRobotApplyDamageRate(e: RobotUsedToolEvent.ApplyDamageRate) {
    if (e.toolAfterUse.isItemStackDamageable) {
      val damage = e.toolAfterUse.getItemDamage - e.toolBeforeUse.getItemDamage
      if (damage > 0) {
        val actualDamage = damage * e.getDamageRate
        val repairedDamage = if (e.agent.player.getRNG.nextDouble() > 0.5) damage - math.floor(actualDamage).toInt else damage - math.ceil(actualDamage).toInt
        e.toolAfterUse.setItemDamage(e.toolAfterUse.getItemDamage - repairedDamage)
      }
    }
  }

  @SubscribeEvent
  def onRobotMove(e: RobotMoveEvent.Pre): Unit = {
    if (Settings.get.limitFlightHeight < 256) e.agent match {
      case robot: Robot =>
        val world = robot.world
        var maxFlyingHeight = Settings.get.limitFlightHeight

        (0 until robot.equipmentInventory.getSizeInventory).
          map(robot.equipmentInventory.getStackInSlot).
          map(Delegator.subItem).
          collect { case Some(item: UpgradeHover) => maxFlyingHeight = math.max(maxFlyingHeight, Settings.get.upgradeFlightHeight(item.tier)) }

        (0 until robot.componentCount).
          map(_ + robot.mainInventory.getSizeInventory + robot.equipmentInventory.getSizeInventory).
          map(robot.getStackInSlot).
          map(Delegator.subItem).
          collect { case Some(item: UpgradeHover) => maxFlyingHeight = math.max(maxFlyingHeight, Settings.get.upgradeFlightHeight(item.tier)) }

        def isMovingDown = e.direction == ForgeDirection.DOWN
        def hasAdjacentBlock(pos: BlockPosition) = ForgeDirection.VALID_DIRECTIONS.exists(side => world.isSideSolid(pos.offset(side), side.getOpposite))
        def isWithinFlyingHeight(pos: BlockPosition) = maxFlyingHeight >= world.getHeight || (1 to maxFlyingHeight).exists(n => !world.isAirBlock(pos.offset(ForgeDirection.DOWN, n)))
        val startPos = BlockPosition(robot)
        val targetPos = startPos.offset(e.direction)
        // New movement rules as of 1.5:
        // 1. Robots may only move if the start or target position is valid (e.g. to allow building bridges).
        // 2. The position below a robot is always valid (can always move down).
        // 3. Positions up to <flightHeight> above a block are valid (limited flight capabilities).
        // 4. Any position that has an adjacent block with a solid face towards the position is valid (robots can "climb").
        val validMove = isMovingDown ||
          hasAdjacentBlock(startPos) ||
          hasAdjacentBlock(targetPos) ||
          isWithinFlyingHeight(startPos)

        if (!validMove) {
          e.setCanceled(true)
        }
      case _ =>
    }
  }
}
