package li.cil.oc.common.template

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.tileentity
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList

object RobotTemplate extends Template {
  override protected def hostClass = classOf[tileentity.Robot]

  def selectTier1(stack: ItemStack) = ItemUtils.caseTier(stack) == Tier.One

  def selectTier2(stack: ItemStack) = ItemUtils.caseTier(stack) == Tier.Two

  def selectTier3(stack: ItemStack) = ItemUtils.caseTier(stack) == Tier.Three

  def selectCreative(stack: ItemStack) = ItemUtils.caseTier(stack) == Tier.Four

  def validate(inventory: IInventory): Array[AnyRef] = validateComputer(inventory)

  def assemble(inventory: IInventory) = {
    val items = (0 until inventory.getSizeInventory).map(inventory.getStackInSlot)
    val data = new ItemUtils.RobotData()
    data.tier = ItemUtils.caseTier(inventory.getStackInSlot(0))
    data.name = ItemUtils.RobotData.randomName
    data.robotEnergy = Settings.get.bufferRobot.toInt
    data.totalEnergy = data.robotEnergy
    data.containers = items.slice(1, 4).filter(_ != null).toArray
    data.components = items.drop(4).filter(_ != null).toArray
    val stack = api.Items.get("robot").createItemStack(1)
    data.save(stack)
    val energy = Settings.get.robotBaseCost + complexity(inventory) * Settings.get.robotComplexityCost

    Array(stack, double2Double(energy))
  }

  def register() {
    // Tier 1
    {
      val nbt = new NBTTagCompound()
      nbt.setString("select", "li.cil.oc.common.template.RobotTemplate.selectTier1")
      nbt.setString("validate", "li.cil.oc.common.template.RobotTemplate.validate")
      nbt.setString("assemble", "li.cil.oc.common.template.RobotTemplate.assemble")

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
      componentSlots.appendTag(Map("type" -> Slot.Floppy, "tier" -> Tier.Any))
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.One))
      nbt.setTag("componentSlots", componentSlots)

      FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
    }

    // Tier 2
    {
      val nbt = new NBTTagCompound()
      nbt.setString("select", "li.cil.oc.common.template.RobotTemplate.selectTier2")
      nbt.setString("validate", "li.cil.oc.common.template.RobotTemplate.validate")
      nbt.setString("assemble", "li.cil.oc.common.template.RobotTemplate.assemble")
      nbt.setString("hostClass", "li.cil.oc.api.tileentity.Robot")

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
      componentSlots.appendTag(Map("type" -> Slot.Floppy, "tier" -> Tier.Any))
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.Two))
      nbt.setTag("componentSlots", componentSlots)

      FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
    }

    // Tier 3
    {
      val nbt = new NBTTagCompound()
      nbt.setString("select", "li.cil.oc.common.template.RobotTemplate.selectTier3")
      nbt.setString("validate", "li.cil.oc.common.template.RobotTemplate.validate")
      nbt.setString("assemble", "li.cil.oc.common.template.RobotTemplate.assemble")

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
      componentSlots.appendTag(Map("type" -> Slot.Floppy, "tier" -> Tier.Any))
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.Two))
      nbt.setTag("componentSlots", componentSlots)

      FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
    }

    // Creative
    {
      val nbt = new NBTTagCompound()
      nbt.setString("select", "li.cil.oc.common.template.RobotTemplate.selectCreative")
      nbt.setString("validate", "li.cil.oc.common.template.RobotTemplate.validate")
      nbt.setString("assemble", "li.cil.oc.common.template.RobotTemplate.assemble")

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
      componentSlots.appendTag(Map("type" -> Slot.Floppy, "tier" -> Tier.Any))
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.Three))
      componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.Three))
      nbt.setTag("componentSlots", componentSlots)

      FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
    }
  }

  override protected def caseTier(inventory: IInventory) = ItemUtils.caseTier(inventory.getStackInSlot(0))
}
