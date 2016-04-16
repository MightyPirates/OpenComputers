package li.cil.oc.common.tileentity

import li.cil.oc.Settings
import li.cil.oc.api.Driver
import li.cil.oc.api.internal
import li.cil.oc.api.network.Connector
import li.cil.oc.common
import li.cil.oc.common.InventorySlots
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.block.property.PropertyRunning
import li.cil.oc.util.Color
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly

class Case(var tier: Int) extends traits.PowerAcceptor with traits.Computer with traits.Colored with internal.Case {
  def this() = this(0)

  // Used on client side to check whether to render disk activity/network indicators.
  var lastFileSystemAccess = 0L
  var lastNetworkActivity = 0L

  setColor(Color.rgbValues(Color.byTier(tier)))

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: EnumFacing) = side != facing

  override protected def connector(side: EnumFacing) = Option(if (side != facing && machine != null) machine.node.asInstanceOf[Connector] else null)

  override def energyThroughput = Settings.get.caseRate(tier)


  def isCreative = tier == Tier.Four

  // ----------------------------------------------------------------------- //

  override def componentSlot(address: String) = components.indexWhere(_.exists(env => env.node != null && env.node.address == address))

  // ----------------------------------------------------------------------- //

  override def canUpdate = isServer

  override def updateEntity() {
    if (isServer && isCreative && world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      // Creative case, make it generate power.
      node.asInstanceOf[Connector].changeBuffer(Double.PositiveInfinity)
    }
    super.updateEntity()
  }

  // ----------------------------------------------------------------------- //

  override protected def onRunningChanged(): Unit = {
    super.onRunningChanged()
    getBlockType match {
      case block: common.block.Case => world.setBlockState(getPos, world.getBlockState(getPos).withProperty(PropertyRunning.Running, Boolean.box(isRunning)))
      case _ =>
    }
  }

  override def readFromNBTForServer(nbt: NBTTagCompound) {
    tier = nbt.getByte(Settings.namespace + "tier") max 0 min 3
    setColor(Color.rgbValues(Color.byTier(tier)))
    super.readFromNBTForServer(nbt)
  }

  override def writeToNBTForServer(nbt: NBTTagCompound) {
    nbt.setByte(Settings.namespace + "tier", tier.toByte)
    super.writeToNBTForServer(nbt)
  }

  // ----------------------------------------------------------------------- //

  override protected def onItemAdded(slot: Int, stack: ItemStack) {
    super.onItemAdded(slot, stack)
    if (isServer) {
      if (InventorySlots.computer(tier)(slot).slot == Slot.Floppy) {
        common.Sound.playDiskInsert(this)
      }
    }
  }

  override protected def onItemRemoved(slot: Int, stack: ItemStack) {
    super.onItemRemoved(slot, stack)
    if (isServer) {
      val slotType = InventorySlots.computer(tier)(slot).slot
      if (slotType == Slot.Floppy) {
        common.Sound.playDiskEject(this)
      }
      if (slotType == Slot.CPU) {
        machine.stop()
      }
    }
  }

  override def getSizeInventory = if (tier < 0 || tier >= InventorySlots.computer.length) 0 else InventorySlots.computer(tier).length

  override def isUseableByPlayer(player: EntityPlayer) =
    super.isUseableByPlayer(player) && (!isCreative || player.capabilities.isCreativeMode)

  override def isItemValidForSlot(slot: Int, stack: ItemStack) =
    Option(Driver.driverFor(stack, getClass)).fold(false)(driver => {
      val provided = InventorySlots.computer(tier)(slot)
      driver.slot(stack) == provided.slot && driver.tier(stack) <= provided.tier
    })
}