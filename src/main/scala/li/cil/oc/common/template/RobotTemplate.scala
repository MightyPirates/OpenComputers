package li.cil.oc.common.template

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.api.driver.{Inventory, Memory, Processor, UpgradeContainer}
import li.cil.oc.common.{Slot, Tier}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import li.cil.oc.{Localization, Settings, api}
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTTagCompound, NBTTagList}
import net.minecraft.util.ChatMessageComponent

import scala.collection.mutable

object RobotTemplate {
  def selectTier1(stack: ItemStack) = ItemUtils.caseTier(stack) == Tier.One

  def selectTier2(stack: ItemStack) = ItemUtils.caseTier(stack) == Tier.Two

  def selectTier3(stack: ItemStack) = ItemUtils.caseTier(stack) == Tier.Three

  def selectCreative(stack: ItemStack) = ItemUtils.caseTier(stack) == Tier.Four

  def validate(inventory: IInventory): Array[AnyRef] = {
    val hasCase = ItemUtils.caseTier(inventory.getStackInSlot(0)) != Tier.None
    val hasCPU = RobotTemplate.hasCPU(inventory)
    val hasRAM = RobotTemplate.hasRAM(inventory)
    val complexity = RobotTemplate.complexity(inventory)
    val maxComplexity = RobotTemplate.maxComplexity(inventory)

    val valid = hasCase && hasCPU && hasRAM && complexity <= maxComplexity

    val progress =
      if (!hasCPU) Localization.Assembler.InsertCPU
      else if (!hasRAM) Localization.Assembler.InsertRAM
      else Localization.Assembler.Complexity(complexity, maxComplexity)

    val warnings = mutable.ArrayBuffer.empty[ChatMessageComponent]
    for ((name, check) <- suggestedComponents) {
      if (!check(inventory)) {
        warnings += Localization.Assembler.Warning(name)
      }
    }
    if (warnings.length > 0) {
      warnings.prepend(Localization.Assembler.Warnings)
    }

    Array(valid: java.lang.Boolean, progress, warnings.toArray)
  }

  def assemble(inventory: IInventory) = {
    val items = (0 until inventory.getSizeInventory).map(inventory.getStackInSlot)
    val data = new ItemUtils.RobotData()
    data.tier = ItemUtils.caseTier(inventory.getStackInSlot(0))
    data.name = ItemUtils.RobotData.randomName
    data.robotEnergy = 50000
    data.totalEnergy = data.robotEnergy
    data.containers = items.slice(1, 4).filter(_ != null).toArray
    data.components = items.drop(4).filter(_ != null).toArray
    val stack = api.Items.get("robot").createItemStack(1)
    data.save(stack)
    val energy = Settings.get.robotBaseCost + complexity(inventory) * Settings.get.robotComplexityCost

    Array(stack, energy: java.lang.Double)
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

  private val suggestedComponents = Array(
    "Screen" -> hasComponent("screen1") _,
    "Keyboard" -> hasComponent("keyboard") _,
    "GraphicsCard" -> ((inventory: IInventory) => Array("graphicsCard1", "graphicsCard2", "graphicsCard3").exists(name => hasComponent(name)(inventory))),
    "Inventory" -> hasInventory _,
    "OS" -> hasFileSystem _)

  private def exists(inventory: IInventory, p: ItemStack => Boolean) = {
    (0 until inventory.getSizeInventory).exists(slot => Option(inventory.getStackInSlot(slot)) match {
      case Some(stack) => p(stack)
      case _ => false
    })
  }

  private def hasCPU(inventory: IInventory) = exists(inventory, api.Driver.driverFor(_) match {
    case _: Processor => true
    case _ => false
  })

  private def hasRAM(inventory: IInventory) = exists(inventory, api.Driver.driverFor(_) match {
    case _: Memory => true
    case _ => false
  })

  private def hasComponent(name: String)(inventory: IInventory) = exists(inventory, stack => Option(api.Items.get(stack)) match {
    case Some(descriptor) => descriptor.name == name
    case _ => false
  })

  private def hasInventory(inventory: IInventory) = exists(inventory, api.Driver.driverFor(_) match {
    case _: Inventory => true
    case _ => false
  })

  private def hasFileSystem(inventory: IInventory) = exists(inventory, stack => Option(api.Driver.driverFor(stack)) match {
    case Some(driver) => Slot.fromApi(driver.slot(stack)) == Slot.Floppy || Slot.fromApi(driver.slot(stack)) == Slot.HDD
    case _ => false
  })

  private def complexity(inventory: IInventory) = {
    var acc = 0
    for (slot <- 1 until inventory.getSizeInventory) {
      val stack = inventory.getStackInSlot(slot)
      acc += (Option(api.Driver.driverFor(stack)) match {
        case Some(driver: Processor) => 0 // CPUs are exempt, since they control the limit.
        case Some(driver: UpgradeContainer) => (1 + driver.tier(stack)) * 2
        case Some(driver) => 1 + driver.tier(stack)
        case _ => 0
      })
    }
    acc
  }

  private def maxComplexity(inventory: IInventory) = {
    val caseTier = ItemUtils.caseTier(inventory.getStackInSlot(0))
    val cpuTier = (0 until inventory.getSizeInventory).foldRight(0)((slot, acc) => {
      val stack = inventory.getStackInSlot(slot)
      acc + (api.Driver.driverFor(stack) match {
        case processor: Processor => processor.tier(stack)
        case _ => 0
      })
    })
    if (caseTier >= Tier.One && cpuTier >= Tier.One) {
      Settings.robotComplexityByTier(caseTier) - (caseTier - cpuTier) * 6
    }
    else 0
  }
}
