package li.cil.oc.server.component

import java.util.UUID

import li.cil.oc.Settings
import li.cil.oc.api.machine._
import li.cil.oc.api.prefab.AbstractValue
import li.cil.oc.common.EventHandler
import li.cil.oc.util.InventoryUtils
import net.minecraft.entity.passive.EntityVillager
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.village.MerchantRecipe
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.common.util.ForgeDirection
import ref.WeakReference
import li.cil.oc.api.network.EnvironmentHost
import scala.collection.JavaConverters._

class Trade(info: TradeInfo) extends AbstractValue {

  def this() = this(new TradeInfo())
  def this(upgr: UpgradeTrading, villager: EntityVillager, recipeID: Int) = {
    this(new TradeInfo(upgr.host, villager, recipeID))
  }


  override def load(nbt: NBTTagCompound) = {
    EventHandler.scheduleServer(() => {
      //Tell the info to load from NBT, behind EventHandler because when load is called we can't access the world yet
      //and we need to access it to get the Robot/Drone TileEntity/Entity
      info.load(nbt)
    })
  }

  override def save(nbt: NBTTagCompound) = {
    //Tell info to save to nbt
    info.save(nbt)
  }

  def inventory = info.inventory

  @Callback(doc="function():table, table -- returns the items the villager wants for this trade")
  def getInput(context: Context, arguments: Arguments): Array[AnyRef] = {
    Array(info.recipe.getItemToBuy.copy(),
      info.recipe.hasSecondItemToBuy match {
        case true => info.recipe.getSecondItemToBuy.copy()
        case false => null
      })
  }

  @Callback(doc = "function():table -- returns the item the villager offers for this trade")
  def getOutput(context: Context, arguments: Arguments): Array[AnyRef] = {
    Array(info.recipe.getItemToSell.copy())
  }

  @Callback(doc="function():boolean -- returns whether the villager currently wants to trade this")
  def isEnabled(context: Context, arguments: Arguments): Array[AnyRef] = {
    Array(info.villager.exists((villager: EntityVillager) => // Make sure villager is neither dead/gone nor the recipe
      !info.recipe.isRecipeDisabled                          // has been disabled
    ).asInstanceOf[AnyRef])
  }

  val maxRange = Settings.get.tradingRange
  def inRange = info.villager.isDefined && distance.exists((distance: Double) => distance < maxRange)
  def distance = info.villager match {
    case Some(villager: EntityVillager) =>
      info.host match {
        case Some(h: EnvironmentHost) => Some(Math.sqrt(Math.pow(villager.posX - h.xPosition, 2) + Math.pow(villager.posY - h.yPosition, 2) + Math.pow(villager.posZ - h.zPosition, 2)))
        case _ => None
      }
    case None => None
  }

  @Callback(doc="function():boolean, string -- returns true when trade succeeds and nil, error when not")
  def trade(context: Context, arguments: Arguments): Array[AnyRef] = {
    //Make sure we can access an inventory
    val inventory = info.inventory match {
      case Some(i) => i
      case None => return result(false, "trading requires an inventory upgrade to be installed")
    }

    //Make sure villager hasn't died, it somehow gone or moved out of range
    if (info.villager.isEmpty)
      return result(false, "trade has become invalid")
    else if (!info.villager.get.isEntityAlive)
      return result(false, "trader died")
    if (!inRange) {
      return result(false, "out of range")
    }

    //Make sure villager wants to trade this
    if (info.recipe.isRecipeDisabled)
      return result(false, "recipe is disabled")

    //Now we'll check if we have enough items to perform the trade, caching first
    val firstItem = info.recipe.getItemToBuy
    val secondItem = info.recipe.hasSecondItemToBuy match {
      case true => Some(info.recipe.getSecondItemToBuy)
      case false => None
    }

    //Check if we have enough of the first item
    var extracting: Int = firstItem.stackSize
    for (slot <- 0 until inventory.getSizeInventory) {
      val stack = inventory.getStackInSlot(slot)
      if (stack != null && stack.isItemEqual(firstItem) && extracting > 0)
        //Takes the stack in the slot, extracts up to limit and calls the function in the first argument
        //We don't actually consume anything, we just count that we have extracted as much as we need
        InventoryUtils.extractFromInventorySlot((stack: ItemStack) => extracting -= stack.stackSize, inventory, ForgeDirection.UNKNOWN, slot, extracting)
    }
    //If we had enough, the left-over amount will be 0
    if (extracting != 0)
      return result(false, "not enough items to trade")

    //Do the same with the second item if there is one
    if (secondItem.isDefined) {
      extracting = secondItem.orNull.stackSize
      for (slot <- 0 until inventory.getSizeInventory) {
        val stack = inventory.getStackInSlot(slot)
        if (stack != null && stack.isItemEqual(secondItem.orNull) && extracting > 0)
          InventoryUtils.extractFromInventorySlot((stack: ItemStack) => extracting -= stack.stackSize, inventory, ForgeDirection.UNKNOWN, slot, extracting)
      }
      if (extracting != 0)
        return result(false, "not enough items to trade")
    }

    //Now we need to check if we have enough inventory space to accept the item we get for the trade
    val outputItemSim = info.recipe.getItemToSell.copy()
    InventoryUtils.insertIntoInventory(outputItemSim, inventory, None, 64, simulate = true)
    if (outputItemSim.stackSize != 0)
      return result(false, "not enough inventory space to trade")

    //We established that out inventory allows to perform the trade, now actaully do the trade
    extracting = firstItem.stackSize
    for (slot <- 0 until inventory.getSizeInventory) {
      if (extracting != 0) {
        val stack = inventory.getStackInSlot(slot)
        if (stack != null && stack.isItemEqual(firstItem))
          //Pretty much the same as earlier (but counting down, and not up now)
          //but this time we actually consume the stack we get
          InventoryUtils.extractFromInventorySlot((stack: ItemStack) => {
            extracting -= stack.stackSize
            stack.stackSize = 0
          }, inventory, ForgeDirection.UNKNOWN, slot, extracting)
      }
    }

    //Do the same for the second item
    if (secondItem.isDefined) {
      extracting = secondItem.orNull.stackSize
      for (slot <- 0 until inventory.getSizeInventory) {
        if (extracting != 0) {
          val stack = inventory.getStackInSlot(slot)
          if (stack != null && stack.isItemEqual(secondItem.orNull))
            InventoryUtils.extractFromInventorySlot((stack: ItemStack) => {
              extracting -= stack.stackSize
              stack.stackSize = 0
            }, inventory, ForgeDirection.UNKNOWN, slot, extracting)
        }
      }
    }

    //Now put our output item into the inventory
    val outputItem = info.recipe.getItemToSell.copy()
    while (outputItem.stackSize != 0)
      InventoryUtils.insertIntoInventory(outputItem, inventory, None, outputItem.stackSize)

    //Tell the villager we used the recipe, so MC can disable it and/or enable more recipes
    info.villager.orNull.useRecipe(info.recipe)
    result(true)
  }
}

class TradeInfo() {
  def this(host: EnvironmentHost, villager: EntityVillager, recipeID: Int) = {
    this()
    _vilRef = new WeakReference[EntityVillager](villager)
    _recipeID = recipeID
    this.host = host
  }

  def getEntityByUUID(dimID: Int, uuid: UUID) = DimensionManager.getProvider(dimID).worldObj.getLoadedEntityList.asScala.find {
    case entAny: net.minecraft.entity.Entity if entAny.getPersistentID == uuid => true
    case _ => false
  }

  def getTileEntity(dimID: Int, posX: Int, posY: Int, posZ: Int) = Option(DimensionManager.getProvider(dimID).worldObj.getTileEntity(posX, posY, posZ) match {
    case robot : li.cil.oc.common.tileentity.Robot => robot
    case robotProxy : li.cil.oc.common.tileentity.RobotProxy => robotProxy.robot
    case null => None
  })

  def load(nbt: NBTTagCompound): Unit = {
    val dimID = nbt.getInteger("dimensionID")
    val _hostIsDrone = nbt.getBoolean("hostIsDrone")
    //If drone we find it again by its UUID, if Robot we know the X/Y/Z of the TileEntity
    host = Option(_hostIsDrone match {
      case true => getEntityByUUID(dimID, UUID.fromString(nbt.getString("hostUUID"))).orNull.asInstanceOf[EnvironmentHost]
      case false => getTileEntity(
        dimID,
        nbt.getInteger("hostX"),
        nbt.getInteger("hostY"),
        nbt.getInteger("hostZ")
      ).orNull.asInstanceOf[EnvironmentHost]
    })
    _recipeID = nbt.getInteger("recipeID")
    _vilRef = new WeakReference[EntityVillager](getEntityByUUID(dimID, UUID.fromString(nbt.getString("villagerUUID"))).orNull.asInstanceOf[EntityVillager])
  }

  def save(nbt: NBTTagCompound): Unit = {
    host match {
      case Some(h) =>
        nbt.setInteger("dimensionID", h.world.provider.dimensionId)
        hostIsDrone match {
          case true => nbt.setString("hostUUID", h.asInstanceOf[li.cil.oc.common.entity.Drone].getPersistentID.toString)
          case false =>
            nbt.setInteger("hostX", h.xPosition.floor.toInt)
            nbt.setInteger("hostY", h.yPosition.floor.toInt)
            nbt.setInteger("hostZ", h.zPosition.floor.toInt)
        }
      case None =>
    }
    villager match {
      case Some(v) => nbt.setString("villagerUUID", v.getPersistentID.toString)
      case None =>
    }
    nbt.setBoolean("hostIsDrone", hostIsDrone)
    nbt.setInteger("recipeID", _recipeID)
  }

  def hostIsDrone = host.orNull match {
    case h : li.cil.oc.common.tileentity.Robot => false
    case h : li.cil.oc.common.entity.Drone => true
    case _ => false
  }
  private var _host: Option[EnvironmentHost] = None
  private var _recipeID: Int = _
  private var _vilRef: WeakReference[EntityVillager] = new WeakReference[EntityVillager](null)

  def host = _host
  private def host_= (value: EnvironmentHost): Unit = _host = Option(value)
  private def host_= (value: Option[EnvironmentHost]): Unit = _host = value

  def villager = _vilRef.get
  def recipeID = _recipeID
  def recipe : MerchantRecipe = villager.orNull.getRecipes(null).get(recipeID).asInstanceOf[MerchantRecipe]

  def inventory = host match {
    case Some(h) => hostIsDrone match {
      case true => Some(h.asInstanceOf[li.cil.oc.common.entity.Drone].mainInventory)
      case false => Some(h.asInstanceOf[li.cil.oc.common.tileentity.Robot].mainInventory)
    }
    case None => None
  }
}