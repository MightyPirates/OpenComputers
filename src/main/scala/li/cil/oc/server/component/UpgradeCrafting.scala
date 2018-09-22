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
import li.cil.oc.api.prefab
import li.cil.oc.util.InventoryUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent
import net.minecraftforge.fml.common.FMLCommonHandler

import scala.collection.convert.WrapAsJava._
import scala.collection.mutable
import scala.util.control.Breaks._

class UpgradeCrafting(val host: EnvironmentHost with internal.Robot) extends prefab.ManagedEnvironment with DeviceInfo {
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
      val player = host.player
      load(player.inventory)
      val cm = CraftingManager.getInstance
      var countCrafted = 0
      val originalCraft = cm.findMatchingRecipe(CraftingInventory, host.world)
      if (originalCraft == null) {
        return Seq(false, 0)
      }
      breakable {
        while (countCrafted < wantedCount) {
          val result = cm.findMatchingRecipe(CraftingInventory, host.world)
          if (result == null || result.stackSize < 1) break()
          if (!originalCraft.isItemEqual(result)) {
            break()
          }
          countCrafted += result.stackSize

          FMLCommonHandler.instance.firePlayerCraftingEvent(player, result, this)
          result.onCrafting(host.world, player, result.stackSize)
          ForgeHooks.setCraftingPlayer(player)
          val aitemstack = cm.getRemainingItems(this, host.world)
          ForgeHooks.setCraftingPlayer(null)

          val surplus = mutable.ArrayBuffer.empty[ItemStack]
          for (slot <- 0 until aitemstack.size) {
            var clean = getStackInSlot(slot)
            var used = aitemstack(slot)

            // use 1 of this item
            if (clean != null) {
              decrStackSize(slot, 1)
              clean = getStackInSlot(slot)
            }

            // the item remains
            if (used != null) {
              // but we thought we used it up
              if (clean == null) {
                // put it back
                setInventorySlotContents(slot, used)
              }
              else if (ItemStack.areItemsEqual(clean, used) && ItemStack.areItemStackTagsEqual(clean, used)) {
                // the recipe doesn't actually consume this item
                used.stackSize += used.stackSize
                setInventorySlotContents(slot, used)
              }
              else {
                // the used item doesn't stack with the clean (could be a container, could be a damaged tool)
                surplus += used
              }
            }
          }

          save(player.inventory)
          InventoryUtils.addToPlayerInventory(result, player)
          for (stack <- surplus) {
            InventoryUtils.addToPlayerInventory(stack, player)
          }
          load(player.inventory)
        }
      }
      Seq(originalCraft != null, countCrafted)
    }

    def load(inventory: IInventory) {
      amountPossible = Int.MaxValue
      for (slot <- 0 until getSizeInventory) {
        val stack = inventory.getStackInSlot(toParentSlot(slot))
        setInventorySlotContents(slot, stack)
        if (stack != null) {
          amountPossible = math.min(amountPossible, stack.stackSize)
        }
      }
    }

    def save(inventory: IInventory) {
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
