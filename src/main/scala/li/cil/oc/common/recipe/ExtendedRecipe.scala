package li.cil.oc.common.recipe

import java.util.UUID

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.detail.ItemInfo
import li.cil.oc.integration.Mods
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import li.cil.oc.util.SideTracker
import net.minecraft.init.Blocks
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsScala._

object ExtendedRecipe {
  private lazy val drone = api.Items.get("drone")
  private lazy val eeprom = api.Items.get("eeprom")
  private lazy val mcu = api.Items.get("microcontroller")
  private lazy val navigationUpgrade = api.Items.get("navigationUpgrade")
  private lazy val linkedCard = api.Items.get("linkedCard")
  private lazy val floppy = api.Items.get("floppy")
  private lazy val disabled = {
    val stack = new ItemStack(Blocks.dirt)
    val tag = new NBTTagCompound()
    tag.setNewCompoundTag("display", _.setNewTagList("Lore", "Autocrafting of this item is disabled to avoid exploits."))
    stack.setTagCompound(tag)
    stack
  }

  def addNBTToResult(craftedStack: ItemStack, inventory: InventoryCrafting): ItemStack = {
    if (api.Items.get(craftedStack) == navigationUpgrade) {
      Option(api.Driver.driverFor(craftedStack)).foreach(driver =>
        for (slot <- 0 until inventory.getSizeInventory) {
          val stack = inventory.getStackInSlot(slot)
          if (stack != null && stack.getItem == net.minecraft.init.Items.filled_map) {
            // Store information of the map used for crafting in the result.
            val nbt = driver.dataTag(craftedStack)
            nbt.setNewCompoundTag(Settings.namespace + "map", stack.writeToNBT)
          }
        })
    }

    if (api.Items.get(craftedStack) == linkedCard) {
      if (weAreBeingCalledFromAppliedEnergistics2) return disabled.copy()
      if (SideTracker.isServer) {
        Option(api.Driver.driverFor(craftedStack)).foreach(driver => {
          val nbt = driver.dataTag(craftedStack)
          nbt.setString(Settings.namespace + "tunnel", UUID.randomUUID().toString)
        })
      }
    }

    if (api.Items.get(craftedStack) == floppy) {
      if (!craftedStack.hasTagCompound) {
        craftedStack.setTagCompound(new NBTTagCompound())
      }
      val nbt = craftedStack.getTagCompound
      for (slot <- 0 until inventory.getSizeInventory) {
        val stack = inventory.getStackInSlot(slot)
        if (stack != null) {
          if (api.Items.get(stack) == floppy && stack.hasTagCompound) {
            val oldData = stack.getTagCompound
            for (oldTagName <- oldData.func_150296_c().map(_.asInstanceOf[String])) {
              nbt.setTag(oldTagName, oldData.getTag(oldTagName).copy())
            }
          }
        }
      }
    }

    recraftMCU(craftedStack, inventory, mcu)
    recraftMCU(craftedStack, inventory, drone)

    craftedStack
  }

  private def recraftMCU(craftedStack: ItemStack, inventory: InventoryCrafting, descriptor: ItemInfo) {
    if (api.Items.get(craftedStack) == descriptor) {
      // Find old Microcontroller.
      (0 until inventory.getSizeInventory).map(inventory.getStackInSlot).find(api.Items.get(_) == descriptor) match {
        case Some(oldMcu) =>
          val data = new ItemUtils.MicrocontrollerData(oldMcu)

          // Remove old EEPROM.
          val oldRom = data.components.filter(api.Items.get(_) == eeprom)
          data.components = data.components.diff(oldRom)

          // Insert new EEPROM.
          for (slot <- 0 until inventory.getSizeInventory) {
            val stack = inventory.getStackInSlot(slot)
            if (api.Items.get(stack) == eeprom) {
              data.components :+= stack
            }
          }

          data.save(craftedStack)
        case _ =>
      }
    }
  }

  private def weAreBeingCalledFromAppliedEnergistics2 = Mods.AppliedEnergistics2.isAvailable && new Exception().getStackTrace.exists(_.getClassName == "appeng.container.implementations.ContainerPatternTerm")
}
