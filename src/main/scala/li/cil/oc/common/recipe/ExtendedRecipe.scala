package li.cil.oc.common.recipe

import java.util.UUID

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.integration.Mods
import li.cil.oc.util.Color
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.SideTracker
import net.minecraft.init.Blocks
import net.minecraft.inventory.InventoryCrafting
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import scala.collection.convert.WrapAsScala._

object ExtendedRecipe {
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
        for (i <- 0 until inventory.getSizeInventory) {
          val stack = inventory.getStackInSlot(i)
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
      for (i <- 0 until inventory.getSizeInventory) {
        val stack = inventory.getStackInSlot(i)
        if (stack != null) {
          Color.findDye(stack) match {
            case Some(oreDictName) =>
              nbt.setInteger(Settings.namespace + "color", Color.dyes.indexOf(oreDictName))
            case _ =>
          }
          if (api.Items.get(stack) == floppy && stack.hasTagCompound) {
            val oldData = stack.getTagCompound
            for (oldTagName <- oldData.func_150296_c().map(_.asInstanceOf[String])) {
              nbt.setTag(oldTagName, oldData.getTag(oldTagName).copy())
            }
          }
        }
      }
    }

    craftedStack
  }

  private def weAreBeingCalledFromAppliedEnergistics2 = Mods.AppliedEnergistics2.isAvailable && new Exception().getStackTrace.exists(_.getClassName == "appeng.container.implementations.ContainerPatternTerm")
}
