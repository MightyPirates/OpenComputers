package li.cil.oc.common

import li.cil.oc.api.Driver
import net.minecraft.item.ItemStack

object InventorySlots {
  val computer = Array(
    Array(
      InventorySlot(Slot.Card, Tier.One),
      InventorySlot(Slot.Card, Tier.One),
      InventorySlot(Slot.Memory, Tier.One),
      InventorySlot(Slot.HDD, Tier.One),
      InventorySlot(Slot.CPU, Tier.One),
      InventorySlot(Slot.Memory, Tier.One),
      InventorySlot(Slot.EEPROM, Tier.Any)
    ),

    Array(
      InventorySlot(Slot.Card, Tier.Two),
      InventorySlot(Slot.Card, Tier.One),
      InventorySlot(Slot.Memory, Tier.Two),
      InventorySlot(Slot.Memory, Tier.Two),
      InventorySlot(Slot.HDD, Tier.Two),
      InventorySlot(Slot.HDD, Tier.One),
      InventorySlot(Slot.CPU, Tier.Two),
      InventorySlot(Slot.EEPROM, Tier.Any)
    ),

    Array(
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.Card, Tier.Two),
      InventorySlot(Slot.Card, Tier.Two),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Two),
      InventorySlot(Slot.Floppy, Tier.One),
      InventorySlot(Slot.CPU, Tier.Three),
      InventorySlot(Slot.EEPROM, Tier.Any)
    ),

    Array(
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.Floppy, Tier.One),
      InventorySlot(Slot.CPU, Tier.Three),
      InventorySlot(Slot.EEPROM, Tier.Any)
    )
  )

  val diskDrive = Array(
    InventorySlot(Slot.Floppy, Tier.Any)
  )

  val charger = Array(
    InventorySlot(Slot.Tablet, Tier.Any)
  )

  val server = Array(
    Array(
      InventorySlot(Slot.Card, Tier.Two),
      InventorySlot(Slot.Card, Tier.Two),
      InventorySlot(Slot.CPU, Tier.Two),
      InventorySlot(Slot.ComponentBus, Tier.Two),
      InventorySlot(Slot.Memory, Tier.Two),
      InventorySlot(Slot.Memory, Tier.Two),
      InventorySlot(Slot.HDD, Tier.Two),
      InventorySlot(Slot.HDD, Tier.Two),
      InventorySlot(Slot.EEPROM, Tier.Any)
    ),

    Array(
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.Card, Tier.Two),
      InventorySlot(Slot.CPU, Tier.Three),
      InventorySlot(Slot.ComponentBus, Tier.Three),
      InventorySlot(Slot.ComponentBus, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.Card, Tier.Two),
      InventorySlot(Slot.EEPROM, Tier.Any)
    ),

    Array(
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.CPU, Tier.Three),
      InventorySlot(Slot.ComponentBus, Tier.Three),
      InventorySlot(Slot.ComponentBus, Tier.Three),
      InventorySlot(Slot.ComponentBus, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.Card, Tier.Two),
      InventorySlot(Slot.Card, Tier.Two),
      InventorySlot(Slot.EEPROM, Tier.Any)
    ),

    Array(
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.CPU, Tier.Three),
      InventorySlot(Slot.ComponentBus, Tier.Three),
      InventorySlot(Slot.ComponentBus, Tier.Three),
      InventorySlot(Slot.ComponentBus, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.EEPROM, Tier.Any)
    )
  )

  val relay = Array(
    InventorySlot(Slot.CPU, Tier.Three),
    InventorySlot(Slot.Memory, Tier.Three),
    InventorySlot(Slot.HDD, Tier.Three),
    InventorySlot(Slot.Card, Tier.Three)
  )

  val switch = Array(
    InventorySlot(Slot.CPU, Tier.Three),
    InventorySlot(Slot.Memory, Tier.Three),
    InventorySlot(Slot.HDD, Tier.Three)
  )

  def isValidForSlot(config:Array[InventorySlot], slot:Int, stack:ItemStack, clazz:Class[_]): Boolean = {
    if (slot < 0 || slot >= config.length) {
      return false
    }

    val driver = Driver.driverFor(stack, clazz)
    if (driver == null) {
      return false
    }

    val haveSlot = config(slot).slot
    val wantSlot = driver.slot(stack)
    if (haveSlot != wantSlot) {
      return false
    }

    val haveTier = config(slot).tier
    val wantTier = driver.tier(stack)
    if (haveTier < wantTier) {
      return false
    }

    return true
  }

  case class InventorySlot(slot: String, tier: Int)

}
