package li.cil.oc.server.component

import java.util.UUID

import li.cil.oc.Settings
import li.cil.oc.api.machine._
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.common.EventHandler
import li.cil.oc.util.InventoryUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.IMerchant
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.village.MerchantRecipe
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.convert.WrapAsScala._
import scala.ref.WeakReference

class Trade(val info: TradeInfo) extends AbstractValue {
  def this() = this(new TradeInfo())

  def this(upgrade: UpgradeTrading, merchant: IMerchant, recipeID: Int, merchantID: Int) =
    this(new TradeInfo(upgrade.host, merchant, recipeID, merchantID))

  def maxRange = Settings.get.tradingRange

  def isInRange = (info.merchant.get, info.host) match {
    case (Some(merchant: Entity), Some(host)) => merchant.getDistanceSq(host.xPosition, host.yPosition, host.zPosition) < maxRange * maxRange
    case _ => false
  }

  // Queue the load because when load is called we can't access the world yet
  // and we need to access it to get the Robot's TileEntity / Drone's Entity.
  override def load(nbt: NBTTagCompound) = EventHandler.scheduleServer(() => info.load(nbt))

  override def save(nbt: NBTTagCompound) = info.save(nbt)

  @Callback(doc = "function():number -- Returns a sort index of the merchant that provides this trade")
  def getMerchantId(context: Context, arguments: Arguments): Array[AnyRef] =
    result(info.merchantID)

  @Callback(doc = "function():table, table -- Returns the items the merchant wants for this trade.")
  def getInput(context: Context, arguments: Arguments): Array[AnyRef] =
    result(info.recipe.map(_.getItemToBuy.copy()).orNull,
      if (info.recipe.exists(_.hasSecondItemToBuy)) info.recipe.map(_.getSecondItemToBuy.copy()).orNull else null)

  @Callback(doc = "function():table -- Returns the item the merchant offers for this trade.")
  def getOutput(context: Context, arguments: Arguments): Array[AnyRef] =
    result(info.recipe.map(_.getItemToSell.copy()).orNull)

  @Callback(doc = "function():boolean -- Returns whether the merchant currently wants to trade this.")
  def isEnabled(context: Context, arguments: Arguments): Array[AnyRef] =
    result(info.merchant.get.exists(merchant => !info.recipe.exists(_.isRecipeDisabled))) // Make sure merchant is neither dead/gone nor the recipe has been disabled.

  @Callback(doc = "function():boolean, string -- Returns true when trade succeeds and nil, error when not.")
  def trade(context: Context, arguments: Arguments): Array[AnyRef] = {
    // Make sure we can access an inventory.
    info.inventory match {
      case Some(inventory) =>
        // Make sure merchant hasn't died, it somehow gone or moved out of range and still wants to trade this.
        info.merchant.get match {
          case Some(merchant: Entity) if merchant.isEntityAlive && isInRange =>
            if (!merchant.isEntityAlive) {
              result(false, "trader died")
            } else if (!isInRange) {
              result(false, "out of range")
            } else {
              info.recipe match {
                case Some(recipe) =>
                  if (recipe.isRecipeDisabled) {
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

  def hasRoomForRecipe(inventory: IInventory, recipe: MerchantRecipe) : Boolean = {
    val remainder = recipe.getItemToSell.copy()
    InventoryUtils.insertIntoInventory(remainder, inventory, None, remainder.stackSize, simulate = true)
    remainder.stackSize == 0
  }

  def completeTrade(inventory: IInventory, recipe: MerchantRecipe, exact: Boolean) : Boolean = {
    // Now we'll check if we have enough items to perform the trade, caching first
    val firstInputStack = recipe.getItemToBuy
    val secondInputStack = if (recipe.hasSecondItemToBuy) Option(recipe.getSecondItemToBuy) else None

    def containsAccumulativeItemStack(stack: ItemStack) =
      InventoryUtils.extractFromInventory(stack, inventory, ForgeDirection.UNKNOWN, simulate = true, exact = exact).stackSize == 0

    // Check if we have enough to perform the trade.
    if (!containsAccumulativeItemStack(firstInputStack) || !secondInputStack.forall(containsAccumulativeItemStack))
      return false

    // Now we need to check if we have enough inventory space to accept the item we get for the trade.
    val outputStack = recipe.getItemToSell.copy()

    // We established that out inventory allows to perform the trade, now actually do the trade.
    InventoryUtils.extractFromInventory(firstInputStack, inventory, ForgeDirection.UNKNOWN, exact = exact)
    secondInputStack.map(InventoryUtils.extractFromInventory(_, inventory, ForgeDirection.UNKNOWN, exact = exact))
    InventoryUtils.insertIntoInventory(outputStack, inventory, None, outputStack.stackSize)

    // Tell the merchant we used the recipe, so MC can disable it and/or enable more recipes.
    info.merchant.get.orNull.useRecipe(recipe)
    true
  }
}

class TradeInfo(var host: Option[EnvironmentHost], var merchant: WeakReference[IMerchant], var recipeID: Int, var merchantID: Int) {
  def this() = this(None, new WeakReference[IMerchant](null), -1, -1)

  def this(host: EnvironmentHost, merchant: IMerchant, recipeID: Int, merchantID: Int) =
    this(Option(host), new WeakReference[IMerchant](merchant), recipeID, merchantID)

  def recipe = merchant.get.map(_.getRecipes(null).get(recipeID).asInstanceOf[MerchantRecipe])

  def inventory = host match {
    case Some(agent: li.cil.oc.api.internal.Agent) => Option(agent.mainInventory())
    case _ => None
  }

  def load(nbt: NBTTagCompound): Unit = {
    val isEntity = nbt.getBoolean("hostIsEntity")
    // If drone we find it again by its UUID, if Robot we know the X/Y/Z of the TileEntity.
    host = if (isEntity) loadHostEntity(nbt) else loadHostTileEntity(nbt)
    merchant = new WeakReference[IMerchant](loadEntity(nbt, new UUID(nbt.getLong("merchantUUIDMost"), nbt.getLong("merchantUUIDLeast"))) match {
      case Some(merchant: IMerchant) => merchant
      case _ => null
    })
    recipeID = nbt.getInteger("recipeID")
    merchantID = if (nbt.hasKey("merchantID")) nbt.getInteger("merchantID") else -1
  }

  def save(nbt: NBTTagCompound): Unit = {
    host match {
      case Some(entity: Entity) =>
        nbt.setBoolean("hostIsEntity", true)
        nbt.setInteger("dimensionID", entity.world.provider.dimensionId)
        nbt.setLong("hostUUIDLeast", entity.getPersistentID.getLeastSignificantBits)
        nbt.setLong("hostUUIDMost", entity.getPersistentID.getMostSignificantBits)
      case Some(tileEntity: TileEntity) =>
        nbt.setBoolean("hostIsEntity", false)
        nbt.setInteger("dimensionID", tileEntity.getWorldObj.provider.dimensionId)
        nbt.setInteger("hostX", tileEntity.xCoord)
        nbt.setInteger("hostY", tileEntity.yCoord)
        nbt.setInteger("hostZ", tileEntity.zCoord)
      case _ => // Welp!
    }
    merchant.get match {
      case Some(entity: Entity) =>
        nbt.setLong("merchantUUIDLeast", entity.getPersistentID.getLeastSignificantBits)
        nbt.setLong("merchantUUIDMost", entity.getPersistentID.getMostSignificantBits)
      case _ =>
    }
    nbt.setInteger("recipeID", recipeID)
    nbt.setInteger("merchantID", merchantID)
  }

  private def loadEntity(nbt: NBTTagCompound, uuid: UUID): Option[Entity] = {
    val dimension = nbt.getInteger("dimensionID")
    val world = DimensionManager.getProvider(dimension).worldObj

    world.loadedEntityList.find {
      case entity: Entity if entity.getPersistentID == uuid => true
      case _ => false
    }.map(_.asInstanceOf[Entity])
  }

  private def loadHostEntity(nbt: NBTTagCompound): Option[EnvironmentHost] = {
    loadEntity(nbt, new UUID(nbt.getLong("hostUUIDMost"), nbt.getLong("hostUUIDLeast"))) match {
      case Some(entity: Entity with li.cil.oc.api.internal.Agent) => Option(entity: EnvironmentHost)
      case _ => None
    }
  }

  private def loadHostTileEntity(nbt: NBTTagCompound): Option[EnvironmentHost] = {
    val dimension = nbt.getInteger("dimensionID")
    val world = DimensionManager.getProvider(dimension).worldObj

    val x = nbt.getInteger("hostX")
    val y = nbt.getInteger("hostY")
    val z = nbt.getInteger("hostZ")

    world.getTileEntity(x, y, z) match {
      case robotProxy: li.cil.oc.common.tileentity.RobotProxy => Option(robotProxy.robot)
      case agent: li.cil.oc.api.internal.Agent => Option(agent)
      case null => None
    }
  }
}
