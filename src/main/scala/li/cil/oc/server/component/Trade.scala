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
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.DimensionManager

import scala.collection.convert.WrapAsScala._
import scala.ref.WeakReference

class Trade(val info: TradeInfo) extends AbstractValue {
  def this() = this(new TradeInfo())

  def this(upgrade: UpgradeTrading, merchant: IMerchant, recipeID: Int) =
    this(new TradeInfo(upgrade.host, merchant, recipeID))

  def maxRange = Settings.get.tradingRange

  def isInRange = (info.merchant.get, info.host) match {
    case (Some(merchant: Entity), Some(host)) => merchant.getDistanceSq(host.xPosition, host.yPosition, host.zPosition) < maxRange * maxRange
    case _ => false
  }

  // Queue the load because when load is called we can't access the world yet
  // and we need to access it to get the Robot's TileEntity / Drone's Entity.
  override def load(nbt: NBTTagCompound) = EventHandler.scheduleServer(() => info.load(nbt))

  override def save(nbt: NBTTagCompound) = info.save(nbt)

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
                    // Now we'll check if we have enough items to perform the trade, caching first
                    val firstInputStack = recipe.getItemToBuy
                    val secondInputStack = if (recipe.hasSecondItemToBuy) Option(recipe.getSecondItemToBuy) else None

                    def containsAccumulativeItemStack(stack: ItemStack) =
                      InventoryUtils.extractFromInventory(stack, inventory, null, simulate = true).stackSize == 0
                    def hasRoomForItemStack(stack: ItemStack) = {
                      val remainder = stack.copy()
                      InventoryUtils.insertIntoInventory(remainder, inventory, None, remainder.stackSize, simulate = true)
                      remainder.stackSize == 0
                    }

                    // Check if we have enough to perform the trade.
                    if (containsAccumulativeItemStack(firstInputStack) && secondInputStack.forall(containsAccumulativeItemStack)) {
                      // Now we need to check if we have enough inventory space to accept the item we get for the trade.
                      val outputStack = recipe.getItemToSell.copy()
                      if (hasRoomForItemStack(outputStack)) {
                        // We established that out inventory allows to perform the trade, now actually do the trade.
                        InventoryUtils.extractFromInventory(firstInputStack, inventory, null)
                        secondInputStack.map(InventoryUtils.extractFromInventory(_, inventory, null))
                        InventoryUtils.insertIntoInventory(outputStack, inventory, None, outputStack.stackSize)

                        // Tell the merchant we used the recipe, so MC can disable it and/or enable more recipes.
                        info.merchant.get.orNull.useRecipe(recipe)

                        result(true)
                      } else {
                        result(false, "not enough inventory space to trade")
                      }
                    } else {
                      result(false, "not enough items to trade")
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
}

class TradeInfo(var host: Option[EnvironmentHost], var merchant: WeakReference[IMerchant], var recipeID: Int) {
  def this() = this(None, new WeakReference[IMerchant](null), -1)

  def this(host: EnvironmentHost, merchant: IMerchant, recipeID: Int) =
    this(Option(host), new WeakReference[IMerchant](merchant), recipeID)

  def recipe = merchant.get.map(_.getRecipes(null).get(recipeID))

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

  def load(nbt: NBTTagCompound): Unit = {
    val isEntity = nbt.getBoolean(HostIsEntityTag)
    // If drone we find it again by its UUID, if Robot we know the X/Y/Z of the TileEntity.
    host = if (isEntity) loadHostEntity(nbt) else loadHostTileEntity(nbt)
    merchant = new WeakReference[IMerchant](loadEntity(nbt, new UUID(nbt.getLong(MerchantUUIDMostTag), nbt.getLong(MerchantUUIDLeastTag))) match {
      case Some(merchant: IMerchant) => merchant
      case _ => null
    })
    recipeID = nbt.getInteger(RecipeID)
  }

  def save(nbt: NBTTagCompound): Unit = {
    host match {
      case Some(entity: Entity) =>
        nbt.setBoolean(HostIsEntityTag, true)
        nbt.setInteger(DimensionIDTag, entity.world.provider.getDimension)
        nbt.setLong(HostUUIDLeast, entity.getPersistentID.getLeastSignificantBits)
        nbt.setLong(HostUUIDMost, entity.getPersistentID.getMostSignificantBits)
      case Some(tileEntity: TileEntity) =>
        nbt.setBoolean(HostIsEntityTag, false)
        nbt.setInteger(DimensionIDTag, tileEntity.getWorld.provider.getDimension)
        nbt.setInteger(HostXTag, tileEntity.getPos.getX)
        nbt.setInteger(HostYTag, tileEntity.getPos.getY)
        nbt.setInteger(HostZTag, tileEntity.getPos.getZ)
      case _ => // Welp!
    }
    merchant.get match {
      case Some(entity: Entity) =>
        nbt.setLong(MerchantUUIDLeastTag, entity.getPersistentID.getLeastSignificantBits)
        nbt.setLong(MerchantUUIDMostTag, entity.getPersistentID.getMostSignificantBits)
      case _ =>
    }
    nbt.setInteger(RecipeID, recipeID)
  }

  private def loadEntity(nbt: NBTTagCompound, uuid: UUID): Option[Entity] = {
    val dimension = nbt.getInteger(DimensionIDTag)
    val world = DimensionManager.getWorld(dimension)

    world.loadedEntityList.find {
      case entity: Entity if entity.getPersistentID == uuid => true
      case _ => false
    }
  }

  private def loadHostEntity(nbt: NBTTagCompound): Option[EnvironmentHost] = {
    loadEntity(nbt, new UUID(nbt.getLong(HostUUIDMost), nbt.getLong(HostUUIDLeast))) match {
      case Some(entity: Entity with li.cil.oc.api.internal.Agent) => Option(entity: EnvironmentHost)
      case _ => None
    }
  }

  private def loadHostTileEntity(nbt: NBTTagCompound): Option[EnvironmentHost] = {
    val dimension = nbt.getInteger(DimensionIDTag)
    val world = DimensionManager.getWorld(dimension)

    val x = nbt.getInteger(HostXTag)
    val y = nbt.getInteger(HostYTag)
    val z = nbt.getInteger(HostZTag)

    world.getTileEntity(new BlockPos(x, y, z)) match {
      case robotProxy: li.cil.oc.common.tileentity.RobotProxy => Option(robotProxy.robot)
      case agent: li.cil.oc.api.internal.Agent => Option(agent)
      case null => None
    }
  }
}
