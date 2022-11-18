package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.passive.{OcelotEntity, SheepEntity}
import net.minecraft.tileentity.TileEntityType
import net.minecraft.util.DamageSource
import net.minecraft.util.Direction

import scala.collection.convert.ImplicitConversionsToJava._
import scala.collection.convert.ImplicitConversionsToScala._

class CarpetedCapacitor(selfType: TileEntityType[_ <: CarpetedCapacitor]) extends Capacitor(selfType) with traits.Tickable {
  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Power,
    DeviceAttribute.Description -> "Battery",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "CarpetedCapBank3x",
    DeviceAttribute.Capacity -> maxCapacity.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  private def _world: net.minecraft.world.World = getLevel
  private val rng = scala.util.Random
  private val chance: Double = Settings.get.carpetDamageChance
  private var nextChanceTime: Long = 0

  private def energyFromGroup(entities: Set[LivingEntity], power: Double): Double = {
    if (entities.size < 2) return 0
    def tryDamageOne(): Unit = {
      for (ent <- entities) {
        if (rng.nextDouble() < chance) {
          ent.hurt(DamageSource.GENERIC, 1)
          ent.setLastHurtByMob(ent) // panic
          ent.knockback(0, .25, 0)
          // wait a minute before the next possible shock
          nextChanceTime = _world.getGameTime + (20 * 60)
          return
        }
      }
    }
    if (chance > 0 && nextChanceTime < _world.getGameTime) {
      tryDamageOne()
    }
    power
  }

  override def updateEntity() {
    if (node != null && (_world.getGameTime + hashCode) % 20 == 0) {
      val entities = _world.getEntitiesOfClass(classOf[LivingEntity], capacitorPowerBounds)
        .filter(entity => entity.isAlive)
        .toSet
      val sheepPower = energyFromGroup(entities.filter(_.isInstanceOf[SheepEntity]), Settings.get.sheepPower)
      val ocelotPower = energyFromGroup(entities.filter(_.isInstanceOf[OcelotEntity]), Settings.get.ocelotPower)
      val totalPower = sheepPower + ocelotPower
      if (totalPower > 0) {
        node.changeBuffer(totalPower)
      }
    }
  }

  private def capacitorPowerBounds = position.offset(Direction.UP).bounds
}
