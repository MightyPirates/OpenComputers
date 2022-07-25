package li.cil.oc.server.component

import java.util
import java.util.UUID

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.util.BlockPosition
import net.minecraft.entity.Entity
import net.minecraft.entity.merchant.IMerchant
import net.minecraft.util.math.vector.Vector3d

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class UpgradeTrading(val host: EnvironmentHost) extends AbstractManagedEnvironment with traits.WorldAware with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("trading").
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Trading upgrade",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "Capitalism H.O. 1200T"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  override def position = BlockPosition(host)

  def maxRange = Settings.get.tradingRange

  def isInRange(entity: Entity) = new Vector3d(entity.getX, entity.getY, entity.getZ).distanceTo(position.toVec3) <= maxRange

  @Callback(doc = "function():table -- Returns a table of trades in range as userdata objects.")
  def getTrades(context: Context, args: Arguments): Array[AnyRef] = {
    val merchants = entitiesInBounds[Entity](classOf[Entity], position.bounds.inflate(maxRange, maxRange, maxRange)).
      filter(isInRange).
      collect { case merchant: Entity with IMerchant => merchant }
    var nextId = 1
    val idMap = mutable.Map[UUID, Int]()
    for (id: UUID <- merchants.collect { case merchant: IMerchant => merchant.getUUID }.sorted) {
      idMap.put(id, nextId)
      nextId += 1
    }
    // sorting the result is not necessary, but will help the merchant trades line up nicely by merchant
    result(merchants.sortBy(m => m.getUUID).flatMap(merchant => merchant.getOffers.indices.map(index => {
      new Trade(this, merchant, index, idMap(merchant.getUUID))
    })))
  }
}
