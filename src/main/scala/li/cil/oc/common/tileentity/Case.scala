package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.driver.Slot
import li.cil.oc.api.network.Connector
import li.cil.oc.api.{Driver, driver}
import li.cil.oc.common.InventorySlots
import li.cil.oc.util.Color
import li.cil.oc.{Settings, common}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.ForgeDirection

class Case(var tier: Int) extends traits.PowerAcceptor with traits.Computer with traits.Colored {
  def this() = this(0)

  color = Color.byTier(tier)

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: ForgeDirection) = side != facing

  override protected def connector(side: ForgeDirection) = Option(if (side != facing && computer != null) computer.node.asInstanceOf[Connector] else null)

  override def getWorld = world

  var maxComponents = 0

  def recomputeMaxComponents() {
    maxComponents = items.foldLeft(0)((sum, stack) => sum + (stack match {
      case Some(item) => Option(Driver.driverFor(item)) match {
        case Some(driver: driver.Processor) => driver.supportedComponents(item)
        case _ => 0
      }
      case _ => 0
    }))
  }

  override def installedMemory = items.foldLeft(0)((sum, stack) => sum + (stack match {
    case Some(item) => Option(Driver.driverFor(item)) match {
      case Some(driver: driver.Memory) => driver.amount(item)
      case _ => 0
    }
    case _ => 0
  }))

  def hasCPU = items.exists {
    case Some(stack) => Option(Driver.driverFor(stack)) match {
      case Some(driver) => driver.slot(stack) == Slot.Processor
      case _ => false
    }
    case _ => false
  }

  override def canUpdate = isServer

  override def updateEntity() {
    if (isServer && tier == 3 && world.getWorldTime % Settings.get.tickFrequency == 0) {
      // Creative case, make it generate power.
      node.asInstanceOf[Connector].changeBuffer(Double.PositiveInfinity)
    }
    super.updateEntity()
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    tier = nbt.getByte(Settings.namespace + "tier") max 0 min 3
    color = Color.byTier(tier)
    super.readFromNBT(nbt)
    recomputeMaxComponents()
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    nbt.setByte(Settings.namespace + "tier", tier.toByte)
    super.writeToNBT(nbt)
  }

  // ----------------------------------------------------------------------- //

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    if (InventorySlots.computer(tier)(slot).slot == Slot.Disk) {
      common.Sound.playDiskInsert(this)
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (InventorySlots.computer(tier)(slot).slot == Slot.Disk) {
      common.Sound.playDiskEject(this)
    }
  }

  override def markDirty() {
    super.markDirty()
    recomputeMaxComponents()
  }

  override def getSizeInventory = if (tier < 0 || tier >= InventorySlots.computer.length) 0 else InventorySlots.computer(tier).length

  override def isUseableByPlayer(player: EntityPlayer) =
    world.getTileEntity(x, y, z) match {
      case t: traits.TileEntity if t == this && computer.canInteract(player.getCommandSenderName) =>
        player.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64
      case _ => false
    }

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    Option(Driver.driverFor(stack)).fold(false)(driver => {
      val provided = InventorySlots.computer(tier)(slot)
      driver.slot(stack) == provided.slot && driver.tier(stack) <= provided.tier
    })
}