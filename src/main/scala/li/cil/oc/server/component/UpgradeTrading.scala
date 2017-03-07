package li.cil.oc.server.component

import java.util

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
import li.cil.oc.api.prefab.network.AbstractManagedEnvironment
import li.cil.oc.util.BlockPosition
import net.minecraft.entity.Entity
import net.minecraft.entity.IMerchant
import net.minecraft.util.math.Vec3d

import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

class UpgradeTrading(val host: EnvironmentHost) extends AbstractManagedEnvironment with traits.WorldAware with DeviceInfo {
  override val getNode = Network.newNode(this, Visibility.NETWORK).
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

  def isInRange(entity: Entity) = new Vec3d(entity.posX, entity.posY, entity.posZ).distanceTo(position.toVec3) <= maxRange

  @Callback(doc = "function():table -- Returns a table of trades in range as userdata objects.")
  def getTrades(context: Context, args: Arguments): Array[AnyRef] = {
    result(entitiesInBounds[Entity](classOf[Entity], position.bounds.expand(maxRange, maxRange, maxRange)).
      filter(isInRange).
      collect { case merchant: IMerchant => merchant }.
      flatMap(merchant => merchant.getRecipes(null).indices.map(new Trade(this, merchant, _))))
  }
}
