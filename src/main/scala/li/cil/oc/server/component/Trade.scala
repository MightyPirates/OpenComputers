package li.cil.oc.server.component

import java.util.UUID

import li.cil.oc.Settings
import li.cil.oc.api.machine._
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.common.EventHandler
import li.cil.oc.util.InventoryUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.merchant.IMerchant
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.MerchantOffer
import net.minecraft.nbt.CompoundNBT
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.ResourceLocation
import net.minecraft.util.RegistryKey
import net.minecraft.util.math.BlockPos
import net.minecraft.util.registry.Registry
import net.minecraftforge.fml.server.ServerLifecycleHooks

import scala.collection.convert.ImplicitConversionsToScala._
import scala.ref.WeakReference

class Trade(val info: TradeInfo) extends AbstractValue {
  def this() = this(new TradeInfo())

  def this(upgrade: UpgradeTrading, merchant: IMerchant, recipeID: Int, merchantID: Int) =
    this(new TradeInfo(upgrade.host, merchant, recipeID, merchantID))

  def maxRange = Settings.get.tradingRange

  def isInRange = (info.merchant.get, info.host) match {
    case (Some(merchant: Entity), Some(host)) => merchant.distanceToSqr(host.xPosition, host.yPosition, host.zPosition) < maxRange * maxRange
    case _ => false
  }

  // Queue the load because when load is called we can't access the world yet
  // and we need to access it to get the Robot's TileEntity / Drone's Entity.
  override def loadData(nbt: CompoundNBT) = EventHandler.scheduleServer(() => info.loadData(nbt))

  override def saveData(nbt: CompoundNBT) = info.saveData(nbt)

  @Callback(doc = "function():number -- Returns a sort index of the merchant that provides this trade")
  def getMerchantId(context: Context, arguments: Arguments): Array[AnyRef] =
    result(info.merchantID)

  @Callback(doc = "function():table, table -- Returns the items the merchant wants for this trade.")
  def getInput(context: Context, arguments: Arguments): Array[AnyRef] =
    result(info.recipe.map(_.getCostA.copy()).orNull,
      if (info.recipe.exists(!_.getCostB.isEmpty)) info.recipe.map(_.getCostB.copy()).orNull else null)

  @Callback(doc = "function():table -- Returns the item the merchant offers for this trade.")
  def getOutput(context: Context, arguments: Arguments): Array[AnyRef] =
    result(info.recipe.map(_.getResult.copy()).orNull)

  @Callback(doc = "function():boolean -- Returns whether the merchant currently wants to trade this.")
  def isEnabled(context: Context, arguments: Arguments): Array[AnyRef] =
    result(info.merchant.get.exists(merchant => !info.recipe.exists(_.isOutOfStock))) // Make sure merchant is neither dead/gone nor the recipe has been disabled.

  @Callback(doc = "function():boolean, string -- Returns true when trade succeeds and nil, error when not.")
  def trade(context: Context, arguments: Arguments): Array[AnyRef] = {
    // Make sure we can access an inventory.
    info.inventory match {
      case Some(inventory) =>
        // Make sure merchant hasn't died, it somehow gone or moved out of range and still wants to trade this.
        info.merchant.get match {
          case Some(merchant: Entity) if merchant.isAlive && isInRange =>
            if (!merchant.isAlive) {
              result(false, "trader died")
            } else if (!isInRange) {
              result(false, "out of range")
            } else {
              info.recipe match {
                case Some(recipe) =>
                  if (recipe.isOutOfStock) {
                    result(false, "trade is disabled")
                  } else {
                    if (!hasRoomForRecipe(inventory, recipe)) {
                      result(false, "not enough inventory space to trade")
                    } else {
                      if (completeTrade(inventory, recipe, exact = true) || completeTrade(inventory, recipe, exact = false)) {
                        result(true)
                      } else {
                        result(false, "not enough items to trade")
                      }
                    }
                  }
                case _ => result(false, "trade has become invalid")
              }
            }
          case _ => result(false, "trade has become invalid")
        }
      case _ => result(false, "trading requires an inventory upgrade to be installed")
    }
  }

  def hasRoomForRecipe(inventory: IInventory, recipe: MerchantOffer) : Boolean = {
    val remainder = recipe.getResult.copy()
    InventoryUtils.insertIntoInventory(remainder, InventoryUtils.asItemHandler(inventory), remainder.getCount, simulate = true)
    remainder.getCount == 0
  }

  def completeTrade(inventory: IInventory, recipe: MerchantOffer, exact: Boolean) : Boolean = {
    // Now we'll check if we have enough items to perform the trade, caching first
    info.merchant.get match {
      case Some(merchant) => {
        val firstInputStack = recipe.getCostA
        val secondInputStack = if (!recipe.getCostB.isEmpty) Option(recipe.getCostB) else None

        def containsAccumulativeItemStack(stack: ItemStack) =
          InventoryUtils.extractFromInventory(stack, inventory, null, simulate = true, exact = exact).getCount == 0

        // Check if we have enough to perform the trade.
        if (!containsAccumulativeItemStack(firstInputStack) || !secondInputStack.forall(containsAccumulativeItemStack))
          return false

        // Now we need to check if we have enough inventory space to accept the item we get for the trade.
        val outputStack = recipe.getResult.copy()

        // We established that out inventory allows to perform the trade, now actually do the trade.
        InventoryUtils.extractFromInventory(firstInputStack, InventoryUtils.asItemHandler(inventory), exact = exact)
        secondInputStack.map(InventoryUtils.extractFromInventory(_, InventoryUtils.asItemHandler(inventory), exact = exact))
        InventoryUtils.insertIntoInventory(outputStack, InventoryUtils.asItemHandler(inventory), outputStack.getCount)

        // Tell the merchant we used the recipe, so MC can disable it and/or enable more recipes.
        merchant.notifyTrade(recipe)
        true
      }
      case _ => false
    }
  }
}

class TradeInfo(var host: Option[EnvironmentHost], var merchant: WeakReference[IMerchant], var recipeID: Int, var merchantID: Int) {
  def this() = this(None, new WeakReference[IMerchant](null), -1, -1)

  def this(host: EnvironmentHost, merchant: IMerchant, recipeID: Int, merchantID: Int) =
    this(Option(host), new WeakReference[IMerchant](merchant), recipeID, merchantID)

  def recipe = merchant.get.map(_.getOffers.get(recipeID))

  def inventory = host match {
    case Some(agent: li.cil.oc.api.internal.Agent) => Option(agent.mainInventory())
    case _ => None
  }

  private final val HostIsEntityTag = "hostIsEntity"
  private final val MerchantUUIDMostTag = "merchantUUIDMost"
  private final val MerchantUUIDLeastTag = "merchantUUIDLeast"
  private final val DimensionIDTag = "dimensionID"
  private final val HostUUIDMost = "hostUUIDMost"
  private final val HostUUIDLeast = "hostUUIDLeast"
  private final val HostXTag = "hostX"
  private final val HostYTag = "hostY"
  private final val HostZTag = "hostZ"
  private final val RecipeID = "recipeID"
  private final val MerchantID = "merchantID"

  def loadData(nbt: CompoundNBT): Unit = {
    val isEntity = nbt.getBoolean(HostIsEntityTag)
    // If drone we find it again by its UUID, if Robot we know the X/Y/Z of the TileEntity.
    host = if (isEntity) loadHostEntity(nbt) else loadHostTileEntity(nbt)
    merchant = new WeakReference[IMerchant](loadEntity(nbt, new UUID(nbt.getLong(MerchantUUIDMostTag), nbt.getLong(MerchantUUIDLeastTag))) match {
      case Some(merchant: IMerchant) => merchant
      case _ => null
    })
    recipeID = nbt.getInt(RecipeID)
    merchantID = if (nbt.contains(MerchantID)) nbt.getInt(MerchantID) else -1
  }

  def saveData(nbt: CompoundNBT): Unit = {
    host match {
      case Some(entity: Entity) =>
        nbt.putBoolean(HostIsEntityTag, true)
        nbt.putString(DimensionIDTag, entity.world.dimension.location.toString)
        nbt.putLong(HostUUIDLeast, entity.getUUID.getLeastSignificantBits)
        nbt.putLong(HostUUIDMost, entity.getUUID.getMostSignificantBits)
      case Some(tileEntity: TileEntity) =>
        nbt.putBoolean(HostIsEntityTag, false)
        nbt.putString(DimensionIDTag, tileEntity.getLevel.dimension.location.toString)
        nbt.putInt(HostXTag, tileEntity.getBlockPos.getX)
        nbt.putInt(HostYTag, tileEntity.getBlockPos.getY)
        nbt.putInt(HostZTag, tileEntity.getBlockPos.getZ)
      case _ => // Welp!
    }
    merchant.get match {
      case Some(entity: Entity) =>
        nbt.putLong(MerchantUUIDLeastTag, entity.getUUID.getLeastSignificantBits)
        nbt.putLong(MerchantUUIDMostTag, entity.getUUID.getMostSignificantBits)
      case _ =>
    }
    nbt.putInt(RecipeID, recipeID)
    nbt.putInt(MerchantID, merchantID)
  }

  private def loadEntity(nbt: CompoundNBT, uuid: UUID): Option[Entity] = {
    val dimension = new ResourceLocation(nbt.getString(DimensionIDTag))
    val dimKey = RegistryKey.create(Registry.DIMENSION_REGISTRY, dimension)
    val world = ServerLifecycleHooks.getCurrentServer.getLevel(dimKey)

    Option(world.getEntity(uuid))
  }

  private def loadHostEntity(nbt: CompoundNBT): Option[EnvironmentHost] = {
    loadEntity(nbt, new UUID(nbt.getLong(HostUUIDMost), nbt.getLong(HostUUIDLeast))) match {
      case Some(entity: Entity with li.cil.oc.api.internal.Agent) => Option(entity: EnvironmentHost)
      case _ => None
    }
  }

  private def loadHostTileEntity(nbt: CompoundNBT): Option[EnvironmentHost] = {
    val dimension = new ResourceLocation(nbt.getString(DimensionIDTag))
    val dimKey = RegistryKey.create(Registry.DIMENSION_REGISTRY, dimension)
    val world = ServerLifecycleHooks.getCurrentServer.getLevel(dimKey)

    val x = nbt.getInt(HostXTag)
    val y = nbt.getInt(HostYTag)
    val z = nbt.getInt(HostZTag)

    world.getBlockEntity(new BlockPos(x, y, z)) match {
      case robotProxy: li.cil.oc.common.tileentity.RobotProxy => Option(robotProxy.robot)
      case agent: li.cil.oc.api.internal.Agent => Option(agent)
      case null => None
    }
  }
}
