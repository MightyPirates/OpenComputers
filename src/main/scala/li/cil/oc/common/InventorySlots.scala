package li.cil.oc.common

object InventorySlots {
  val computer = Array(
    Array(
      InventorySlot(Slot.Card, Tier.One),
      InventorySlot(Slot.Card, Tier.One),
      InventorySlot(Slot.Memory, Tier.One),
      InventorySlot(Slot.HDD, Tier.One),
      InventorySlot(Slot.CPU, Tier.One),
      InventorySlot(Slot.Memory, Tier.One)
    ),

    Array(
      InventorySlot(Slot.Card, Tier.Two),
      InventorySlot(Slot.Card, Tier.One),
      InventorySlot(Slot.Memory, Tier.Two),
      InventorySlot(Slot.Memory, Tier.Two),
      InventorySlot(Slot.HDD, Tier.Two),
      InventorySlot(Slot.HDD, Tier.One),
      InventorySlot(Slot.CPU, Tier.Two)
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
      InventorySlot(Slot.CPU, Tier.Three)
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
      InventorySlot(Slot.CPU, Tier.Three)
    )
  )

  val assembler = Array(
    Array(
      InventorySlot(Slot.None, Tier.None), // Reserved for computer case.
      InventorySlot(Slot.Container, Tier.Two),
      InventorySlot(Slot.Container, Tier.One),
      InventorySlot(Slot.Container, Tier.One),
      InventorySlot(Slot.Upgrade, Tier.One),
      InventorySlot(Slot.Upgrade, Tier.One),
      InventorySlot(Slot.Upgrade, Tier.One),
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.Card, Tier.One),
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.CPU, Tier.One),
      InventorySlot(Slot.Memory, Tier.One),
      InventorySlot(Slot.Memory, Tier.One),
      InventorySlot(Slot.Floppy, Tier.Any),
      InventorySlot(Slot.HDD, Tier.One),
      InventorySlot(Slot.None, Tier.None)
    ),

    Array(
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.Container, Tier.Three),
      InventorySlot(Slot.Container, Tier.Two),
      InventorySlot(Slot.Container, Tier.One),
      InventorySlot(Slot.Upgrade, Tier.Two),
      InventorySlot(Slot.Upgrade, Tier.Two),
      InventorySlot(Slot.Upgrade, Tier.Two),
      InventorySlot(Slot.Upgrade, Tier.One),
      InventorySlot(Slot.Upgrade, Tier.One),
      InventorySlot(Slot.Upgrade, Tier.One),
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.Card, Tier.Two),
      InventorySlot(Slot.Card, Tier.One),
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.CPU, Tier.Two),
      InventorySlot(Slot.Memory, Tier.Two),
      InventorySlot(Slot.Memory, Tier.Two),
      InventorySlot(Slot.Floppy, Tier.Any),
      InventorySlot(Slot.HDD, Tier.Two),
      InventorySlot(Slot.None, Tier.None)
    ),

    Array(
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.Container, Tier.Three),
      InventorySlot(Slot.Container, Tier.Three),
      InventorySlot(Slot.Container, Tier.Two),
      InventorySlot(Slot.Upgrade, Tier.Three),
      InventorySlot(Slot.Upgrade, Tier.Three),
      InventorySlot(Slot.Upgrade, Tier.Three),
      InventorySlot(Slot.Upgrade, Tier.Two),
      InventorySlot(Slot.Upgrade, Tier.Two),
      InventorySlot(Slot.Upgrade, Tier.Two),
      InventorySlot(Slot.Upgrade, Tier.One),
      InventorySlot(Slot.Upgrade, Tier.One),
      InventorySlot(Slot.Upgrade, Tier.One),
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.Card, Tier.Two),
      InventorySlot(Slot.Card, Tier.Two),
      InventorySlot(Slot.CPU, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.Floppy, Tier.Any),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Two)
    ),

    Array(
      InventorySlot(Slot.None, Tier.None),
      InventorySlot(Slot.Container, Tier.Three),
      InventorySlot(Slot.Container, Tier.Three),
      InventorySlot(Slot.Container, Tier.Three),
      InventorySlot(Slot.Upgrade, Tier.Three),
      InventorySlot(Slot.Upgrade, Tier.Three),
      InventorySlot(Slot.Upgrade, Tier.Three),
      InventorySlot(Slot.Upgrade, Tier.Three),
      InventorySlot(Slot.Upgrade, Tier.Three),
      InventorySlot(Slot.Upgrade, Tier.Three),
      InventorySlot(Slot.Upgrade, Tier.Three),
      InventorySlot(Slot.Upgrade, Tier.Three),
      InventorySlot(Slot.Upgrade, Tier.Three),
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.Card, Tier.Three),
      InventorySlot(Slot.CPU, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.Memory, Tier.Three),
      InventorySlot(Slot.Floppy, Tier.Any),
      InventorySlot(Slot.HDD, Tier.Three),
      InventorySlot(Slot.HDD, Tier.Three)
    )
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
      InventorySlot(Slot.HDD, Tier.Two)
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
      InventorySlot(Slot.Card, Tier.Two)
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
      InventorySlot(Slot.Card, Tier.Two)
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
      InventorySlot(Slot.Card, Tier.Three)
    )
  )

  val switch = Array(
    InventorySlot(Slot.CPU, Tier.Three),
    InventorySlot(Slot.Memory, Tier.Three),
    InventorySlot(Slot.HDD, Tier.Three)
  )

  case class InventorySlot(slot: String, tier: Int)

}
