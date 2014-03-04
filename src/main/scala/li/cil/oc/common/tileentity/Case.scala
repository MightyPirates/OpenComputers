package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.Settings
import li.cil.oc.api.driver
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network.Connector
import li.cil.oc.server.driver.Registry
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

class Case(var tier: Int, isRemote: Boolean) extends Computer(isRemote) {
  def this() = this(0, false)

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: ForgeDirection) = side != facing

  override protected def connector(side: ForgeDirection) = Option(if (side != facing && computer != null) computer.node.asInstanceOf[Connector] else null)

  var maxComponents = 0

  def recomputeMaxComponents() {
    maxComponents = items.foldLeft(0)((sum, stack) => sum + (stack match {
      case Some(item) => Registry.itemDriverFor(item) match {
        case Some(driver: driver.Processor) => driver.supportedComponents(item)
        case _ => 0
      }
      case _ => 0
    }))
  }

  override def installedMemory = items.foldLeft(0)((sum, stack) => sum + (stack match {
    case Some(item) => Registry.itemDriverFor(item) match {
      case Some(driver: driver.Memory) => driver.amount(item)
      case _ => 0
    }
    case _ => 0
  }))

  def hasCPU = items.exists {
    case Some(stack) => Registry.itemDriverFor(stack) match {
      case Some(driver) => driver.slot(stack) == Slot.Processor
      case _ => false
    }
    case _ => false
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    tier = nbt.getByte(Settings.namespace + "tier") max 0 min 2
    super.readFromNBT(nbt)
    recomputeMaxComponents()
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    nbt.setByte(Settings.namespace + "tier", tier.toByte)
    super.writeToNBT(nbt)
  }

  // ----------------------------------------------------------------------- //

  override def markDirty() {
    super.markDirty()
    recomputeMaxComponents()
  }

  override def getInventoryName = Settings.namespace + "container.Case"

  override def getSizeInventory = tier match {
    case 0 => 6
    case 1 => 7
    case 2 => 9
    case _ => 0
  }

  override def isUseableByPlayer(player: EntityPlayer) =
    world.getTileEntity(x, y, z) match {
      case t: TileEntity if t == this && computer.canInteract(player.getCommandSenderName) =>
        player.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64
      case _ => false
    }

  override def isItemValidForSlot(slot: Int, stack: ItemStack) = tier match {
    case 0 => (slot, Registry.itemDriverFor(stack)) match {
      case (_, None) => false // Invalid item.
      case (0 | 1, Some(driver)) => driver.slot(stack) == Slot.Card && driver.tier(stack) <= maxComponentTierForSlot(slot)
      case (2 | 5, Some(driver)) => driver.slot(stack) == Slot.Memory && driver.tier(stack) <= maxComponentTierForSlot(slot)
      case (3, Some(driver)) => driver.slot(stack) == Slot.HardDiskDrive && driver.tier(stack) <= maxComponentTierForSlot(slot)
      case (4, Some(driver)) => driver.slot(stack) == Slot.Processor && driver.tier(stack) <= maxComponentTierForSlot(slot)
      case _ => false // Invalid slot.
    }
    case 1 => (slot, Registry.itemDriverFor(stack)) match {
      case (_, None) => false // Invalid item.
      case (0 | 1, Some(driver)) => driver.slot(stack) == Slot.Card && driver.tier(stack) <= maxComponentTierForSlot(slot)
      case (2 | 3, Some(driver)) => driver.slot(stack) == Slot.Memory && driver.tier(stack) <= maxComponentTierForSlot(slot)
      case (4 | 5, Some(driver)) => driver.slot(stack) == Slot.HardDiskDrive && driver.tier(stack) <= maxComponentTierForSlot(slot)
      case (6, Some(driver)) => driver.slot(stack) == Slot.Processor && driver.tier(stack) <= maxComponentTierForSlot(slot)
      case _ => false // Invalid slot.
    }
    case 2 => (slot, Registry.itemDriverFor(stack)) match {
      case (_, None) => false // Invalid item.
      case (0 | 1 | 2, Some(driver)) => driver.slot(stack) == Slot.Card && driver.tier(stack) <= maxComponentTierForSlot(slot)
      case (3 | 4, Some(driver)) => driver.slot(stack) == Slot.Memory && driver.tier(stack) <= maxComponentTierForSlot(slot)
      case (5 | 6, Some(driver)) => driver.slot(stack) == Slot.HardDiskDrive && driver.tier(stack) <= maxComponentTierForSlot(slot)
      case (7, Some(driver)) => driver.slot(stack) == Slot.Disk
      case (8, Some(driver)) => driver.slot(stack) == Slot.Processor && driver.tier(stack) <= maxComponentTierForSlot(slot)
      case _ => false // Invalid slot.
    }
    case _ => false
  }

  def maxComponentTierForSlot(slot: Int) = tier match {
    case 0 if slot >= 0 && slot < getSizeInventory => 0
    case 1 => slot match {
      case 0 => 1
      case 1 => 0
      case (2 | 3) => 1
      case 4 => 1
      case 5 => 0
      case 6 => 1
      case _ => -1 // Invalid slot.
    }
    case 2 => slot match {
      case 0 => 2
      case 1 | 2 => 1
      case (3 | 4) => 2
      case 5 => 2
      case 6 => 1
      case 7 => 0
      case 8 => 2
      case _ => -1 // Invalid slot.
    }
    case _ => -1
  }
}