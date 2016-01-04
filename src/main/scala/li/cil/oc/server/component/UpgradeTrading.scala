package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.prefab
import li.cil.oc.api.Network
import li.cil.oc.api.network.Visibility
import li.cil.oc.util.BlockPosition
import li.cil.oc.api.machine.{Callback, Arguments, Context}
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.util.Vec3

import scala.collection.mutable.ArrayBuffer

class UpgradeTrading(val host: EnvironmentHost) extends prefab.ManagedEnvironment with traits.WorldAware {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("trading").
    create()

  override def position = BlockPosition(host)

  var maxRange = Settings.get.tradingRange

  def inRange(vil: EntityVillager): Boolean = {
    Vec3.createVectorHelper(vil.posX, vil.posY, vil.posZ).distanceTo(position.toVec3) <= maxRange
  }

  @Callback(doc = "function():table -- Returns a table of trades in range as userdata objects")
  def getTrades(context: Context, args: Arguments): Array[AnyRef] = {

    val boundsLow = position.bounds.offset(-maxRange, -maxRange, -maxRange)
    val boundsHigh = position.bounds.offset(maxRange, maxRange, maxRange)
    val bounds = boundsLow.func_111270_a(boundsHigh)

    val trades = ArrayBuffer[Trade]()
    entitiesInBounds[EntityVillager](bounds).foreach((vil: EntityVillager) => {
      if (inRange(vil)) {
        val merchantRecipes = vil.getRecipes(null)
        for (i <- 0 to merchantRecipes.size() - 1) {
          val trade = new Trade(this, vil, i)
          trades += trade
        }
      }
    })
    trades.toArray[Object]
  }
}
