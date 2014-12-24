package li.cil.oc.common.item

import java.util.Random

import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.util.ItemUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.world.World

import scala.collection.mutable

class Present(val parent: Delegator) extends Delegate {
  showInItemList = false

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
    if (stack.stackSize > 0) {
      stack.stackSize -= 1
      if (!world.isRemote) {
        world.playSoundAtEntity(player, "random.levelup", 0.2f, 1f)
        val present = Present.nextPresent()
        if (player.inventory.addItemStackToInventory(present)) {
          player.inventory.markDirty()
          if (player.openContainer != null) {
            player.openContainer.detectAndSendChanges()
          }
        }
        else {
          player.dropPlayerItemWithRandomChoice(present, false)
        }
      }
    }
    stack
  }
}

object Present {
  private lazy val Presents = {
    val result = mutable.ArrayBuffer.empty[ItemStack]

    def add(name: String, weight: Int): Unit = {
      val item = api.Items.get(name)
      if (item != null) {
        val stack = item.createItemStack(1)
        // Only if it can be crafted (wasn't disabled in the config).
        if (ItemUtils.getIngredients(stack).nonEmpty) {
          for (i <- 0 until weight) result += stack
        }
      }
      else {
        OpenComputers.log.warn(s"Oops, trying to add '$name' as a present even though it doesn't exist!")
      }
    }

    add("arrowKeys", 520)
    add("buttonGroup", 460)
    add("numPad", 410)
    add("disk", 370)
    add("transistor", 350)
    add("floppy", 340)
    add("printedCircuitBoard", 320)
    add("chip1", 290)
    add("eeprom", 250)
    add("interweb", 220)
    add("card", 190)
    add("analyzer", 170)
    add("signUpgrade", 150)
    add("inventoryUpgrade", 130)
    add("craftingUpgrade", 110)
    add("tankUpgrade", 90)
    add("pistonUpgrade", 80)
    add("leashUpgrade", 70)
    add("angelUpgrade", 55)
    add("redstoneCard1", 50)
    add("ram1", 48)
    add("cu", 46)
    add("alu", 45)
    add("batteryUpgrade1", 43)
    add("lanCard", 38)
    add("hdd1", 36)
    add("generatorUpgrade", 35)
    add("cpu1", 31)
    add("microcontrollerCase1", 30)
    add("droneCase1", 25)
    add("upgradeContainer1", 23)
    add("cardContainer1", 23)
    add("graphicsCard1", 19)
    add("redstoneCard2", 17)
    add("ram2", 15)
    add("databaseUpgrade1", 15)
    add("chip2", 15)
    add("componentBus1", 13)
    add("batteryUpgrade2", 12)
    add("wlanCard", 11)
    add("ram3", 10)
    add("server1", 10)
    add("internetCard", 9)
    add("terminal", 9)
    add("solarGeneratorUpgrade", 9)
    add("hdd2", 7)
    add("navigationUpgrade", 7)
    add("inventoryControllerUpgrade", 7)
    add("tankControllerUpgrade", 7)
    add("cpu2", 6)
    add("microcontrollerCase2", 6)
    add("componentBus2", 6)
    add("tabletCase", 5)
    add("upgradeContainer2", 5)
    add("cardContainer2", 5)
    add("graphicsCard2", 4)
    add("ram4", 4)
    add("droneCase2", 4)
    add("databaseUpgrade2", 4)
    add("server2", 4)
    add("chip3", 3)
    add("componentBus3", 3)
    add("tractorBeamUpgrade", 3)
    add("batteryUpgrade3", 3)
    add("experienceUpgrade", 2)
    add("ram5", 2)
    add("upgradeContainer3", 2)
    add("cardContainer3", 2)
    add("hdd3", 1)
    add("chunkloaderUpgrade", 1)
    add("cpu3", 1)
    add("graphicsCard3", 1)
    add("server3", 1)
    add("databaseUpgrade3", 1)
    add("ram6", 1)

    result.toArray
  }

  private val rng = new Random()

  def nextPresent() = Presents(rng.nextInt(Presents.length)).copy()
}