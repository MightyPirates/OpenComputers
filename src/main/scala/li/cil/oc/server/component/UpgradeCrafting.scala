package li.cil.oc.server.component

import li.cil.oc.api.Network
import li.cil.oc.api.driver.EnvironmentHost
import li.cil.oc.api.internal.Robot
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.api.prefab
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.item.crafting.IRecipe
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.entity.player.PlayerDestroyItemEvent
import net.minecraftforge.fml.common.FMLCommonHandler

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.util.control.Breaks._

class UpgradeCrafting(val host: EnvironmentHost with Robot) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withComponent("crafting").
    create()

  @Callback(doc = """function([count:number]):number -- Tries to craft the specified number of items in the top left area of the inventory.""")
  def craft(context: Context, args: Arguments): Array[AnyRef] = {
    val count = args.optInteger(0, Int.MaxValue)
    result(CraftingInventory.craft(count): _*)
  }

  private object CraftingInventory extends inventory.InventoryCrafting(new inventory.Container {
    override def canInteractWith(player: EntityPlayer) = true
  }, 3, 3) {
    var amountPossible = 0

    def craft(wantedCount: Int): Seq[_] = {
      load()
      CraftingManager.getInstance.getRecipeList.find {
        case recipe: IRecipe => recipe.matches(CraftingInventory, host.world)
        case _ => false // Shouldn't ever happen, but...
      } match {
        case Some(recipe: IRecipe) =>
          var countCrafted = 0
          breakable {
            while (countCrafted < wantedCount && recipe.matches(this, host.world)) {
              val result = recipe.getCraftingResult(CraftingInventory)
              if (result == null || result.stackSize < 1) break()
              countCrafted += result.stackSize
              FMLCommonHandler.instance.firePlayerCraftingEvent(host.player, result, this)
              val surplus = mutable.ArrayBuffer.empty[ItemStack]
              for (slot <- 0 until getSizeInventory) {
                val stack = getStackInSlot(slot)
                if (stack != null) {
                  decrStackSize(slot, 1)
                  val item = stack.getItem
                  if (item.hasContainerItem(stack)) {
                    val container = item.getContainerItem(stack)
                    if (container.isItemStackDamageable && container.getItemDamage > container.getMaxDamage) {
                      MinecraftForge.EVENT_BUS.post(new PlayerDestroyItemEvent(host.player, container))
                    }
                    else if ( /* container.getItem.doesContainerItemLeaveCraftingGrid(container) || TODO */ getStackInSlot(slot) != null) {
                      surplus += container
                    }
                    else {
                      setInventorySlotContents(slot, container)
                    }
                  }
                }
              }
              save()
              val inventory = host.player.inventory
              inventory.addItemStackToInventory(result)
              for (stack <- surplus) {
                inventory.addItemStackToInventory(stack)
              }
              load()
            }
          }
          Seq(true, countCrafted)
        case _ => Seq(false, 0)
      }
    }

    def load() {
      val inventory = host.player.inventory
      amountPossible = Int.MaxValue
      for (slot <- 0 until getSizeInventory) {
        val stack = inventory.getStackInSlot(toParentSlot(slot))
        setInventorySlotContents(slot, stack)
        if (stack != null) {
          amountPossible = math.min(amountPossible, stack.stackSize)
        }
      }
    }

    def save() {
      val inventory = host.player.inventory
      for (slot <- 0 until getSizeInventory) {
        inventory.setInventorySlotContents(toParentSlot(slot), getStackInSlot(slot))
      }
    }

    private def toParentSlot(slot: Int) = {
      val col = slot % 3
      val row = slot / 3
      row * 4 + col + 4 // first four are always: tool, card, disk, upgrade
    }
  }

}
