package li.cil.oc.common.item

import java.util.Random

import li.cil.oc.Constants
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.util.InventoryUtils
import li.cil.oc.util.ItemUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.SoundEvents
import net.minecraft.item.ItemStack
import net.minecraft.util.ActionResult
import net.minecraft.util.EnumActionResult
import net.minecraft.util.SoundCategory
import net.minecraft.world.World

import scala.collection.mutable

class Present(val parent: Delegator) extends traits.Delegate {
  showInItemList = false

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ActionResult[ItemStack] = {
    if (stack.stackSize > 0) {
      stack.stackSize -= 1
      if (!world.isRemote) {
        world.playSound(player, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.MASTER, 0.2f, 1f)
        val present = Present.nextPresent()
        InventoryUtils.addToPlayerInventory(present, player)
      }
    }
    ActionResult.newResult(EnumActionResult.SUCCESS, stack)
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

    add(Constants.ItemName.ArrowKeys, 520)
    add(Constants.ItemName.ButtonGroup, 460)
    add(Constants.ItemName.NumPad, 410)
    add(Constants.ItemName.Disk, 370)
    add(Constants.ItemName.Transistor, 350)
    add(Constants.ItemName.Floppy, 340)
    add(Constants.ItemName.PrintedCircuitBoard, 320)
    add(Constants.ItemName.ChipTier1, 290)
    add(Constants.ItemName.EEPROM, 250)
    add(Constants.ItemName.Interweb, 220)
    add(Constants.ItemName.Card, 190)
    add(Constants.ItemName.Analyzer, 170)
    add(Constants.ItemName.SignUpgrade, 150)
    add(Constants.ItemName.InventoryUpgrade, 130)
    add(Constants.ItemName.CraftingUpgrade, 110)
    add(Constants.ItemName.TankUpgrade, 90)
    add(Constants.ItemName.PistonUpgrade, 80)
    add(Constants.ItemName.LeashUpgrade, 70)
    add(Constants.ItemName.AngelUpgrade, 55)
    add(Constants.ItemName.RedstoneCardTier1, 50)
    add(Constants.ItemName.RAMTier1, 48)
    add(Constants.ItemName.ControlUnit, 46)
    add(Constants.ItemName.Alu, 45)
    add(Constants.ItemName.BatteryUpgradeTier1, 43)
    add(Constants.ItemName.NetworkCard, 38)
    add(Constants.ItemName.HDDTier1, 36)
    add(Constants.ItemName.GeneratorUpgrade, 35)
    add(Constants.ItemName.CPUTier1, 31)
    add(Constants.ItemName.MicrocontrollerCaseTier1, 30)
    add(Constants.ItemName.DroneCaseTier1, 25)
    add(Constants.ItemName.UpgradeContainerTier1, 23)
    add(Constants.ItemName.CardContainerTier1, 23)
    add(Constants.ItemName.GraphicsCardTier1, 19)
    add(Constants.ItemName.RedstoneCardTier2, 17)
    add(Constants.ItemName.RAMTier2, 15)
    add(Constants.ItemName.DatabaseUpgradeTier1, 15)
    add(Constants.ItemName.ChipTier2, 15)
    add(Constants.ItemName.ComponentBusTier1, 13)
    add(Constants.ItemName.BatteryUpgradeTier2, 12)
    add(Constants.ItemName.WirelessNetworkCard, 11)
    add(Constants.ItemName.RAMTier3, 10)
    add(Constants.ItemName.ServerTier1, 10)
    add(Constants.ItemName.InternetCard, 9)
    add(Constants.ItemName.Terminal, 9)
    add(Constants.ItemName.SolarGeneratorUpgrade, 9)
    add(Constants.ItemName.HDDTier2, 7)
    add(Constants.ItemName.NavigationUpgrade, 7)
    add(Constants.ItemName.InventoryControllerUpgrade, 7)
    add(Constants.ItemName.TankControllerUpgrade, 7)
    add(Constants.ItemName.CPUTier2, 6)
    add(Constants.ItemName.MicrocontrollerCaseTier2, 6)
    add(Constants.ItemName.ComponentBusTier2, 6)
    add(Constants.ItemName.TabletCaseTier1, 5)
    add(Constants.ItemName.UpgradeContainerTier2, 5)
    add(Constants.ItemName.CardContainerTier2, 5)
    add(Constants.ItemName.GraphicsCardTier2, 4)
    add(Constants.ItemName.RAMTier4, 4)
    add(Constants.ItemName.DroneCaseTier2, 4)
    add(Constants.ItemName.DatabaseUpgradeTier2, 4)
    add(Constants.ItemName.ServerTier2, 4)
    add(Constants.ItemName.ChipTier3, 3)
    add(Constants.ItemName.ComponentBusTier3, 3)
    add(Constants.ItemName.TractorBeamUpgrade, 3)
    add(Constants.ItemName.BatteryUpgradeTier3, 3)
    add(Constants.ItemName.ExperienceUpgrade, 2)
    add(Constants.ItemName.RAMTier5, 2)
    add(Constants.ItemName.UpgradeContainerTier3, 2)
    add(Constants.ItemName.CardContainerTier3, 2)
    add(Constants.ItemName.TabletCaseTier2, 1)
    add(Constants.ItemName.HDDTier3, 1)
    add(Constants.ItemName.ChunkloaderUpgrade, 1)
    add(Constants.ItemName.CPUTier3, 1)
    add(Constants.ItemName.GraphicsCardTier3, 1)
    add(Constants.ItemName.ServerTier3, 1)
    add(Constants.ItemName.DatabaseUpgradeTier3, 1)
    add(Constants.ItemName.RAMTier6, 1)

    result.toArray
  }

  private val rng = new Random()

  def nextPresent() = Presents(rng.nextInt(Presents.length)).copy()
}