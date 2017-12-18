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
import net.minecraft.inventory.{InventoryCraftResult, SlotCrafting}
import net.minecraft.item.crafting.CraftingManager

import scala.collection.convert.WrapAsJava._
import scala.util.control.Breaks._

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
      copyItemsFromHost()
      var countCrafted = 0
      val canCraft = CraftingManager.findMatchingRecipe(CraftingInventory, host.world) != null
      breakable {
        while (countCrafted < wantedCount) {
          val recipe = CraftingManager.findMatchingRecipe(CraftingInventory, host.world)
          if (recipe == null) break()
          val output = recipe.getCraftingResult(this)
          craftResult.setRecipeUsed(recipe)
          craftingSlot.onTake(host.player(), output)
          // Always count having done at least one craft, even if there are no crafting results.
          countCrafted += output.getCount max 1
          copyItemsToHost()
          InventoryUtils.addToPlayerInventory(output, host.player)
          copyItemsFromHost()
        }
      }
      Seq(canCraft, countCrafted)
    }

    def copyItemsFromHost() {
      val inventory = host.mainInventory()
      amountPossible = Int.MaxValue
      for (slot <- 0 until getSizeInventory) {
        val stack = inventory.getStackInSlot(toParentSlot(slot))
        setInventorySlotContents(slot, stack)
        if (!stack.isEmpty) {
          amountPossible = math.min(amountPossible, stack.getCount)
        }
      }
    }

    def copyItemsToHost() {
      val inventory = host.mainInventory()
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

  private val craftResult = new InventoryCraftResult
  private val craftingSlot = new SlotCrafting(host.player(), CraftingInventory, craftResult, 0, 0, 0)
}
