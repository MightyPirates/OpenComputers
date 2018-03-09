package li.cil.oc.common.recipe

import java.util.UUID

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.common.item.data.DroneData
import li.cil.oc.common.item.data.MicrocontrollerData
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.common.item.data.TabletData
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.SideTracker
import net.minecraft.init.Blocks
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.IRecipe
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsScala._
import scala.util.control.Breaks._

object ExtendedRecipe {
  private lazy val drone = api.Items.get(Constants.ItemName.Drone)
  private lazy val eeprom = api.Items.get(Constants.ItemName.EEPROM)
  private lazy val luaBios = api.Items.get(Constants.ItemName.LuaBios)
  private lazy val mcu = api.Items.get(Constants.BlockName.Microcontroller)
  private lazy val navigationUpgrade = api.Items.get(Constants.ItemName.NavigationUpgrade)
  private lazy val linkedCard = api.Items.get(Constants.ItemName.LinkedCard)
  private lazy val floppy = api.Items.get(Constants.ItemName.Floppy)
  private lazy val hdds = Array(
    api.Items.get(Constants.ItemName.HDDTier1),
    api.Items.get(Constants.ItemName.HDDTier2),
    api.Items.get(Constants.ItemName.HDDTier3)
  )
  private lazy val cpus = Array(
    api.Items.get(Constants.ItemName.CPUTier1),
    api.Items.get(Constants.ItemName.CPUTier2),
    api.Items.get(Constants.ItemName.CPUTier3),
    api.Items.get(Constants.ItemName.APUTier1),
    api.Items.get(Constants.ItemName.APUTier2)
  )
  private lazy val robot = api.Items.get(Constants.BlockName.Robot)
  private lazy val tablet = api.Items.get(Constants.ItemName.Tablet)
  private lazy val print = api.Items.get(Constants.BlockName.Print)
  private lazy val disabled = {
    val stack = new ItemStack(Blocks.DIRT)
    val tag = new NBTTagCompound()
    tag.setNewCompoundTag("display", _.setNewTagList("Lore", "Autocrafting of this item is disabled to avoid exploits."))
    stack.setTagCompound(tag)
    stack
  }

  def addNBTToResult(recipe: IRecipe, craftedStack: ItemStack, inventory: InventoryCrafting): ItemStack = {
    val craftedItemName = api.Items.get(craftedStack)

    if (craftedItemName == navigationUpgrade) {
      Option(api.Driver.driverFor(craftedStack)).foreach(driver =>
        for (stack <- getItems(inventory)) {
          if (stack.getItem == net.minecraft.init.Items.FILLED_MAP) {
            // Store information of the map used for crafting in the result.
            val nbt = driver.dataTag(craftedStack)
            nbt.setNewCompoundTag(Settings.namespace + "map", stack.writeToNBT)
          }
        })
    }

    if (craftedItemName == linkedCard) {
      if (SideTracker.isServer) {
        Option(api.Driver.driverFor(craftedStack)).foreach(driver => {
          val nbt = driver.dataTag(craftedStack)
          nbt.setString(Settings.namespace + "tunnel", UUID.randomUUID().toString)
        })
      }
    }

    if (cpus.contains(craftedItemName)) {
      LuaStateFactory.setDefaultArch(craftedStack)
    }

    if (craftedItemName == floppy || hdds.contains(craftedItemName)) {
      if (!craftedStack.hasTagCompound) {
        craftedStack.setTagCompound(new NBTTagCompound())
      }
      val nbt = craftedStack.getTagCompound
      if (recipe.canFit(1, 1)) {
        // Formatting / loot to normal disk conversion, only keep coloring.
        val colorKey = Settings.namespace + "color"
        for (stack <- getItems(inventory)) {
          if (api.Items.get(stack) != null && (api.Items.get(stack) == floppy || api.Items.get(stack).name == "lootDisk") && stack.hasTagCompound) {
            val oldData = stack.getTagCompound
            if (oldData.hasKey(colorKey) && oldData.getInteger(colorKey) != Color.dyes.indexOf("lightGray")) {
              nbt.setTag(colorKey, oldData.getTag(colorKey).copy())
            }
          }
        }
        if (nbt.hasNoTags) {
          craftedStack.setTagCompound(null)
        }
      }
      else if (getItems(inventory).forall(api.Items.get(_) == floppy)) {
        // Copy operation.
        for (stack <- getItems(inventory)) {
          if (api.Items.get(stack) == floppy && stack.hasTagCompound) {
            val oldData = stack.getTagCompound
            for (oldTagName <- oldData.getKeySet.map(_.asInstanceOf[String]) if !nbt.hasKey(oldTagName)) {
              nbt.setTag(oldTagName, oldData.getTag(oldTagName).copy())
            }
          }
        }
      }
    }

    if (craftedItemName == print &&
      recipe.isInstanceOf[ExtendedShapelessOreRecipe] &&
      recipe.asInstanceOf[ExtendedShapelessOreRecipe].getIngredients.size == 2) {
      // First, copy old data.
      val data = new PrintData(craftedStack)
      val inputs = getItems(inventory)
      for (stack <- inputs) {
        if (api.Items.get(stack) == print) {
          data.load(stack)
        }
      }

      // Then apply new data.
      val beaconBlocks = Array(
        new ItemStack(net.minecraft.init.Blocks.IRON_BLOCK),
        new ItemStack(net.minecraft.init.Blocks.GOLD_BLOCK),
        new ItemStack(net.minecraft.init.Blocks.EMERALD_BLOCK),
        new ItemStack(net.minecraft.init.Blocks.DIAMOND_BLOCK)
      )

      val glowstoneDust = new ItemStack(net.minecraft.init.Items.GLOWSTONE_DUST)
      val glowstone = new ItemStack(net.minecraft.init.Blocks.GLOWSTONE)
      for (stack <- inputs) {
        if (beaconBlocks.exists(_.isItemEqual(stack))) {
          if (data.isBeaconBase) {
            // Crafting wouldn't change anything, prevent accidental resource loss.
            return ItemStack.EMPTY
          }
          data.isBeaconBase = true
        }
        if (glowstoneDust.isItemEqual(stack)) {
          if (data.lightLevel == 15) {
            // Crafting wouldn't change anything, prevent accidental resource loss.
            return ItemStack.EMPTY
          }
          data.lightLevel = math.min(15, data.lightLevel + 1)
        }
        if (glowstone.isItemEqual(stack)) {
          if (data.lightLevel == 15) {
            // Crafting wouldn't change anything, prevent accidental resource loss.
            return ItemStack.EMPTY
          }
          data.lightLevel = math.min(15, data.lightLevel + 4)
        }
      }

      // Finally apply modified data.
      data.save(craftedStack)
    }

    // EEPROM copying.
    if (craftedItemName == eeprom &&
      craftedStack.getCount == 2 &&
      recipe.isInstanceOf[ExtendedShapelessOreRecipe] &&
      recipe.asInstanceOf[ExtendedShapelessOreRecipe].getIngredients.size == 2) breakable {
      for (stack <- getItems(inventory)) {
        if (api.Items.get(stack) == eeprom && stack.hasTagCompound) {
          val copy = stack.getTagCompound.copy.asInstanceOf[NBTTagCompound]
          // Erase node address, just in case.
          copy.getCompoundTag(Settings.namespace + "data").getCompoundTag("node").removeTag("address")
          craftedStack.setTagCompound(copy)
          break()
        }
      }
    }

    // Swapping EEPROM in devices.
    recraft(craftedStack, inventory, mcu, stack => new MCUDataWrapper(stack))
    recraft(craftedStack, inventory, drone, stack => new DroneDataWrapper(stack))
    recraft(craftedStack, inventory, robot, stack => new RobotDataWrapper(stack))
    recraft(craftedStack, inventory, tablet, stack => new TabletDataWrapper(stack))

    craftedStack
  }

  private def getItems(inventory: InventoryCrafting) = (0 until inventory.getSizeInventory).map(inventory.getStackInSlot).filter(!_.isEmpty)

  private def recraft(craftedStack: ItemStack, inventory: InventoryCrafting, descriptor: ItemInfo, dataFactory: (ItemStack) => ItemDataWrapper) {
    if (api.Items.get(craftedStack) == descriptor) {
      // Find old Microcontroller.
      getItems(inventory).find(api.Items.get(_) == descriptor) match {
        case Some(oldMcu) =>
          val data = dataFactory(oldMcu)

          // Remove old EEPROM.
          val oldRom = data.components.filter(api.Items.get(_) == eeprom)
          data.components = data.components.diff(oldRom)

          // Insert new EEPROM.
          for (stack <- getItems(inventory)) {
            if (api.Items.get(stack) == eeprom) {
              data.components :+= stack
            }
          }

          data.save(craftedStack)
        case _ =>
      }
    }
  }

  private trait ItemDataWrapper {
    def components: Array[ItemStack]

    def components_=(value: Array[ItemStack]): Unit

    def save(stack: ItemStack): Unit
  }

  private class MCUDataWrapper(val stack: ItemStack) extends ItemDataWrapper {
    val data = new MicrocontrollerData(stack)

    override def components: Array[ItemStack] = data.components

    override def components_=(value: Array[ItemStack]): Unit = data.components = value

    override def save(stack: ItemStack): Unit = data.save(stack)
  }

  private class DroneDataWrapper(val stack: ItemStack) extends ItemDataWrapper {
    val data = new DroneData(stack)

    override def components: Array[ItemStack] = data.components

    override def components_=(value: Array[ItemStack]): Unit = data.components = value

    override def save(stack: ItemStack): Unit = data.save(stack)
  }

  private class RobotDataWrapper(val stack: ItemStack) extends ItemDataWrapper {
    val data = new RobotData(stack)

    override def components: Array[ItemStack] = data.components

    override def components_=(value: Array[ItemStack]): Unit = data.components = value

    override def save(stack: ItemStack): Unit = data.save(stack)
  }

  private class TabletDataWrapper(val stack: ItemStack) extends ItemDataWrapper {
    val data = new TabletData(stack)

    var components: Array[ItemStack] = data.items.filter(!_.isEmpty)

    override def save(stack: ItemStack): Unit = {
      data.items = components.clone()
      data.save(stack)
    }
  }

}
