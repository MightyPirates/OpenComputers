package li.cil.oc.server.component

import java.util

import li.cil.oc.Constants
import li.cil.oc.api.driver.DeviceInfo.DeviceAttribute
import li.cil.oc.api.driver.DeviceInfo.DeviceClass
import li.cil.oc.api.Network
import li.cil.oc.api.driver.DeviceInfo
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.internal
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab.AbstractManagedEnvironment
import li.cil.oc.util.InventoryUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory
import net.minecraft.inventory.{IInventory, InventoryCraftResult, SlotCrafting}
import net.minecraft.item.crafting.CraftingManager

import scala.collection.convert.WrapAsJava._
import scala.util.control.Breaks._
import net.minecraft.item.ItemStack

class UpgradeCrafting(val host: EnvironmentHost with internal.Robot) extends AbstractManagedEnvironment with DeviceInfo {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("crafting").
    create()

  private final lazy val deviceInfo = Map(
    DeviceAttribute.Class -> DeviceClass.Generic,
    DeviceAttribute.Description -> "Assembly controller",
    DeviceAttribute.Vendor -> Constants.DeviceInfo.DefaultVendor,
    DeviceAttribute.Product -> "MultiCombinator-9S"
  )

  override def getDeviceInfo: util.Map[String, String] = deviceInfo

  @Callback(doc = """function([count:number]):number -- Tries to craft the specified number of items in the top left area of the inventory.""")
  def craft(context: Context, args: Arguments): Array[AnyRef] = {
    val count = args.optInteger(0, 64) max 0 min 64
    result(CraftingInventory.craft(count): _*)
  }

  private object CraftingInventory extends inventory.InventoryCrafting(new inventory.Container {
    override def canInteractWith(player: EntityPlayer) = true
  }, 3, 3) {
    var amountPossible = 0
    def craft(wantedCount: Int): Seq[_] = {
      var player = host.player
      copyItemsFromHost(player.inventory)
      var countCrafted = 0
      val initialCraft = CraftingManager.findMatchingRecipe(CraftingInventory, host.world)
      if (initialCraft != null) {
        def tryCraft() : Boolean = {
          val craft = CraftingManager.findMatchingRecipe(CraftingInventory, host.world)
          if (craft == null || craft != initialCraft) {
            return false
          }

          val craftResult = new InventoryCraftResult
          val craftingSlot = new SlotCrafting(player, CraftingInventory, craftResult, 0, 0, 0)
          val craftedResult = craft.getCraftingResult(this)
          craftResult.setInventorySlotContents(0, craftedResult)
          if (!craftingSlot.getHasStack)
            return false

          val stack = craftingSlot.getStack
          countCrafted += stack.getCount max 1
          val taken = craftingSlot.onTake(player, stack)
          if (taken.getCount > 0) {
            copyItemsToHost(player.inventory)
            InventoryUtils.addToPlayerInventory(taken, player)
            copyItemsFromHost(player.inventory)
          }
          true
        }
        while (countCrafted < wantedCount && tryCraft()) {
          //
        }
      }
      Seq(countCrafted > 0, countCrafted)
    }

    def copyItemsFromHost(inventory: IInventory) {
      amountPossible = Int.MaxValue
      for (slot <- 0 until getSizeInventory) {
        val stack = inventory.getStackInSlot(toParentSlot(slot))
        setInventorySlotContents(slot, stack)
        if (!stack.isEmpty) {
          amountPossible = math.min(amountPossible, stack.getCount)
        }
      }
    }

    def copyItemsToHost(inventory: IInventory) {
      for (slot <- 0 until getSizeInventory) {
        inventory.setInventorySlotContents(toParentSlot(slot), getStackInSlot(slot))
      }
    }

    private def toParentSlot(slot: Int) = {
      val col = slot % 3
      val row = slot / 3
      row * 4 + col
    }
  }
}
