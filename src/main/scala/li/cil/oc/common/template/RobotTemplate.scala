package li.cil.oc.common.template

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.internal
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraftforge.fml.common.event.FMLInterModComms

object RobotTemplate extends Template {
  override protected def hostClass = classOf[internal.Robot]

  def selectTier1(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.BlockName.CaseTier1)

  def selectTier2(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.BlockName.CaseTier2)

  def selectTier3(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.BlockName.CaseTier3)

  def selectCreative(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.BlockName.CaseCreative)

  def validate(inventory: IInventory): Array[AnyRef] = validateComputer(inventory)

  def assemble(inventory: IInventory) = {
    val items = (1 until inventory.getSizeInventory).map(inventory.getStackInSlot)
    val data = new RobotData()
    data.tier = caseTier(inventory)
    data.name = RobotData.randomName
    data.robotEnergy = Settings.get.bufferRobot.toInt
    data.totalEnergy = data.robotEnergy
    data.containers = items.take(3).filter(_ != null).toArray
    data.components = items.drop(3).filter(_ != null).toArray
    val stack = data.createItemStack()
    val energy = Settings.get.robotBaseCost + complexity(inventory) * Settings.get.robotComplexityCost

    Array(stack, double2Double(energy))
  }

  def selectDisassembler(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.BlockName.Robot)

  def disassemble(stack: ItemStack, ingredients: Array[ItemStack]) = {
    val info = new RobotData(stack)
    val itemName = Constants.BlockName.Case(info.tier)

    Array(api.Items.get(itemName).createItemStack(1)) ++ info.containers ++ info.components
  }

  def register() {
    // Tier 1
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Robot (Tier 1)")
      nbt.setString("select", "li.cil.oc.common.template.RobotTemplate.selectTier1")
      nbt.setString("validate", "li.cil.oc.common.template.RobotTemplate.validate")
      nbt.setString("assemble", "li.cil.oc.common.template.RobotTemplate.assemble")
      nbt.setString("hostClass", "li.cil.oc.api.internal.Robot")

      val containerSlots = new NBTTagList()
      containerSlots.appendTag(Map("tier" -> Tier.Two))
      containerSlots.appendTag(Map("tier" -> Tier.One))
      containerSlots.appendTag(Map("tier" -> Tier.One))
      nbt.setTag("containerSlots", containerSlots)

      val upgradeSlots = new NBTTagList()
      upgradeSlots.appendTag(Map("tier" -> Tier.One))
      upgradeSlots.appendTag(Map("tier" -> Tier.One))
      upgradeSlots.appendTag(Map("tier" -> Tier.One))
      nbt.setTag("upgradeSlots", upgradeSlots)

      val componentSlots = new NBTTagList()
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.One))
      componentSlots.appendTag(new NBTTagCompound())
      componentSlots.appendTag(new NBTTagCompound())
      componentSlots.appendTag(Map("type" -> Slot.CPU, "tier" -> Tier.One))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.One))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.One))
      componentSlots.appendTag(Map("type" -> Slot.EEPROM, "tier" -> Tier.Any))
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.One))
      nbt.setTag("componentSlots", componentSlots)

      FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
    }

    // Tier 2
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Robot (Tier 2)")
      nbt.setString("select", "li.cil.oc.common.template.RobotTemplate.selectTier2")
      nbt.setString("validate", "li.cil.oc.common.template.RobotTemplate.validate")
      nbt.setString("assemble", "li.cil.oc.common.template.RobotTemplate.assemble")
      nbt.setString("hostClass", "li.cil.oc.api.internal.Robot")

      val containerSlots = new NBTTagList()
      containerSlots.appendTag(Map("tier" -> Tier.Three))
      containerSlots.appendTag(Map("tier" -> Tier.Two))
      containerSlots.appendTag(Map("tier" -> Tier.One))
      nbt.setTag("containerSlots", containerSlots)

      val upgradeSlots = new NBTTagList()
      upgradeSlots.appendTag(Map("tier" -> Tier.Two))
      upgradeSlots.appendTag(Map("tier" -> Tier.Two))
      upgradeSlots.appendTag(Map("tier" -> Tier.Two))
      upgradeSlots.appendTag(Map("tier" -> Tier.One))
      upgradeSlots.appendTag(Map("tier" -> Tier.One))
      upgradeSlots.appendTag(Map("tier" -> Tier.One))
      nbt.setTag("upgradeSlots", upgradeSlots)

      val componentSlots = new NBTTagList()
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Two))
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.One))
      componentSlots.appendTag(new NBTTagCompound())
      componentSlots.appendTag(Map("type" -> Slot.CPU, "tier" -> Tier.Two))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.Two))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.Two))
      componentSlots.appendTag(Map("type" -> Slot.EEPROM, "tier" -> Tier.Any))
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.Two))
      nbt.setTag("componentSlots", componentSlots)

      FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
    }

    // Tier 3
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Robot (Tier 3)")
      nbt.setString("select", "li.cil.oc.common.template.RobotTemplate.selectTier3")
      nbt.setString("validate", "li.cil.oc.common.template.RobotTemplate.validate")
      nbt.setString("assemble", "li.cil.oc.common.template.RobotTemplate.assemble")
      nbt.setString("hostClass", "li.cil.oc.api.internal.Robot")

      val containerSlots = new NBTTagList()
      containerSlots.appendTag(Map("tier" -> Tier.Three))
      containerSlots.appendTag(Map("tier" -> Tier.Two))
      containerSlots.appendTag(Map("tier" -> Tier.Two))
      nbt.setTag("containerSlots", containerSlots)

      val upgradeSlots = new NBTTagList()
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Three))
      upgradeSlots.appendTag(Map("tier" -> Tier.Two))
      upgradeSlots.appendTag(Map("tier" -> Tier.Two))
      upgradeSlots.appendTag(Map("tier" -> Tier.Two))
      upgradeSlots.appendTag(Map("tier" -> Tier.One))
      upgradeSlots.appendTag(Map("tier" -> Tier.One))
      upgradeSlots.appendTag(Map("tier" -> Tier.One))
      nbt.setTag("upgradeSlots", upgradeSlots)

      val componentSlots = new NBTTagList()
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Two))
      componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Two))
      componentSlots.appendTag(Map("type" -> Slot.CPU, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.EEPROM, "tier" -> Tier.Any))
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.Two))
      nbt.setTag("componentSlots", componentSlots)

      FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
    }

    // Creative
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Robot (Creative)")
      nbt.setString("select", "li.cil.oc.common.template.RobotTemplate.selectCreative")
      nbt.setString("validate", "li.cil.oc.common.template.RobotTemplate.validate")
      nbt.setString("assemble", "li.cil.oc.common.template.RobotTemplate.assemble")
      nbt.setString("hostClass", "li.cil.oc.api.internal.Robot")

      val containerSlots = new NBTTagList()
      containerSlots.appendTag(Map("tier" -> Tier.Three))
      containerSlots.appendTag(Map("tier" -> Tier.Three))
      containerSlots.appendTag(Map("tier" -> Tier.Three))
      nbt.setTag("containerSlots", containerSlots)

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
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.Three))
      nbt.setTag("componentSlots", componentSlots)

      FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
    }

    // Disassembler
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Robot")
      nbt.setString("select", "li.cil.oc.common.template.RobotTemplate.selectDisassembler")
      nbt.setString("disassemble", "li.cil.oc.common.template.RobotTemplate.disassemble")

      FMLInterModComms.sendMessage("OpenComputers", "registerDisassemblerTemplate", nbt)
    }
  }

  override protected def caseTier(inventory: IInventory) = ItemUtils.caseTier(inventory.getStackInSlot(0))
}
