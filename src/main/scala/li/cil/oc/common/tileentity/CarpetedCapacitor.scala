package li.cil.oc.common.tileentity

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.passive.{EntityOcelot, EntitySheep}
import net.minecraft.util.DamageSource
import net.minecraft.util.EnumFacing

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import li.cil.oc.common.tileentity.traits.Tickable

class CarpetedCapacitor extends Capacitor with Tickable {
  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Power,
    DeviceAttribute.Description -> "Battery",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "CarpetedCapBank3x",
    DeviceAttribute.Capacity -> maxCapacity.toString
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  private val rng = scala.util.Random
  private val chance: Double = Settings.get.carpetDamageChance
  private var nextChanceTime: Long = 0

  private def energyFromGroup(entities: Set[EntityLivingBase], power: Double): Double = {
    if (entities.size < 2) return 0
    def tryDamageOne(): Unit = {
      for (ent <- entities) {
        if (rng.nextDouble() < chance) {
          ent.attackEntityFrom(DamageSource.generic, 1)
          ent.setRevengeTarget(ent) // panic
          ent.knockBack(ent, 0, .25, 0)
          // wait a minute before the next possible shock
          nextChanceTime = world.getTotalWorldTime + (20 * 60)
          return
        }
      }
    }
    if (chance > 0 && nextChanceTime < world.getTotalWorldTime) {
      tryDamageOne()
    }
    power
  }

  override def updateEntity() {
    if (node != null && (world.getTotalWorldTime + hashCode) % 20 == 0) {
      val entities = world.getEntitiesWithinAABB(classOf[EntityLivingBase], capacitorPowerBounds)
        .filter(entity => entity.isEntityAlive)
        .toSet
      val sheepPower = energyFromGroup(entities.filter(_.isInstanceOf[EntitySheep]), Settings.get.sheepPower)
      val ocelotPower = energyFromGroup(entities.filter(_.isInstanceOf[EntityOcelot]), Settings.get.ocelotPower)
      val totalPower = sheepPower + ocelotPower
      if (totalPower > 0) {
        node.changeBuffer(totalPower)
      }
    }
  }

  private def capacitorPowerBounds = position.offset(EnumFacing.UP).bounds
}
