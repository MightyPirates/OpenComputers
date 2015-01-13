package li.cil.oc.common.template

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.internal
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraftforge.fml.common.event.FMLInterModComms

object DroneTemplate extends Template {
  override protected val suggestedComponents = Array(
    "BIOS" -> hasComponent("eeprom") _)

  override protected def hostClass = classOf[internal.Drone]

  def selectTier1(stack: ItemStack) = api.Items.get(stack) == api.Items.get("droneCase1")

  def selectTier2(stack: ItemStack) = api.Items.get(stack) == api.Items.get("droneCase2")

  def selectTierCreative(stack: ItemStack) = api.Items.get(stack) == api.Items.get("droneCaseCreative")

  def validate(inventory: IInventory): Array[AnyRef] = validateComputer(inventory)

  def assemble(inventory: IInventory) = {
    val items = (0 until inventory.getSizeInventory).map(inventory.getStackInSlot)
    val data = new ItemUtils.MicrocontrollerData()
    data.tier = caseTier(inventory)
    data.components = items.drop(1).filter(_ != null).toArray
    data.storedEnergy = Settings.get.bufferDrone.toInt
    val stack = api.Items.get("drone").createItemStack(1)
    data.save(stack)
    val energy = Settings.get.droneBaseCost + complexity(inventory) * Settings.get.droneComplexityCost

    Array(stack, double2Double(energy))
  }

  def selectDisassembler(stack: ItemStack) = api.Items.get(stack) == api.Items.get("drone")

  def disassemble(stack: ItemStack, ingredients: Array[ItemStack]) = {
    val info = new ItemUtils.MicrocontrollerData(stack)
    val itemName = ItemUtils.caseNameWithTierSuffix("droneCase", info.tier)

    Array(api.Items.get(itemName).createItemStack(1)) ++ info.components
  }

  def register() {
    // Tier 1
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Drone (Tier 1)")
      nbt.setString("select", "li.cil.oc.common.template.DroneTemplate.selectTier1")
      nbt.setString("validate", "li.cil.oc.common.template.DroneTemplate.validate")
      nbt.setString("assemble", "li.cil.oc.common.template.DroneTemplate.assemble")
      nbt.setString("hostClass", "li.cil.oc.api.internal.Drone")

      val upgradeSlots = new NBTTagList()
      upgradeSlots.appendTag(Map("tier" -> Tier.Two))
      upgradeSlots.appendTag(Map("tier" -> Tier.One))
      nbt.setTag("upgradeSlots", upgradeSlots)

      val componentSlots = new NBTTagList()
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Two))
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.One))
      componentSlots.appendTag(new NBTTagCompound())
      componentSlots.appendTag(Map("type" -> Slot.CPU, "tier" -> Tier.One))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.One))
      componentSlots.appendTag(new NBTTagCompound())
      componentSlots.appendTag(Map("type" -> Slot.EEPROM, "tier" -> Tier.Any))
      nbt.setTag("componentSlots", componentSlots)

      FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
    }

    // Tier 2
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Drone (Tier 2)")
      nbt.setString("select", "li.cil.oc.common.template.DroneTemplate.selectTier2")
      nbt.setString("validate", "li.cil.oc.common.template.DroneTemplate.validate")
      nbt.setString("assemble", "li.cil.oc.common.template.DroneTemplate.assemble")
      nbt.setString("hostClass", "li.cil.oc.api.internal.Drone")

      val upgradeSlots = new NBTTagList()
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Two))
      upgradeSlots.appendTag(Map("tier" -> Tier.One))
      nbt.setTag("upgradeSlots", upgradeSlots)

      val componentSlots = new NBTTagList()
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Two))
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Two))
      componentSlots.appendTag(new NBTTagCompound())
      componentSlots.appendTag(Map("type" -> Slot.CPU, "tier" -> Tier.One))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.One))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.One))
      componentSlots.appendTag(Map("type" -> Slot.EEPROM, "tier" -> Tier.Any))
      nbt.setTag("componentSlots", componentSlots)

      FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
    }

    // Creative
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Drone (Creative)")
      nbt.setString("select", "li.cil.oc.common.template.DroneTemplate.selectTierCreative")
      nbt.setString("validate", "li.cil.oc.common.template.DroneTemplate.validate")
      nbt.setString("assemble", "li.cil.oc.common.template.DroneTemplate.assemble")
      nbt.setString("hostClass", "li.cil.oc.api.internal.Drone")

      val upgradeSlots = new NBTTagList()
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      nbt.setTag("upgradeSlots", upgradeSlots)

      val componentSlots = new NBTTagList()
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.CPU, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.EEPROM, "tier" -> Tier.Any))
      nbt.setTag("componentSlots", componentSlots)

      FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
    }

    // Disassembler
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Drone")
      nbt.setString("select", "li.cil.oc.common.template.DroneTemplate.selectDisassembler")
      nbt.setString("disassemble", "li.cil.oc.common.template.DroneTemplate.disassemble")

      FMLInterModComms.sendMessage("OpenComputers", "registerDisassemblerTemplate", nbt)
    }
  }

  override protected def maxComplexity(inventory: IInventory) =
    if (caseTier(inventory) == Tier.Two) 8
    else if (caseTier(inventory) == Tier.Four) 9001 // Creative
    else 5

  override protected def caseTier(inventory: IInventory) = ItemUtils.caseTier(inventory.getStackInSlot(0))
}
