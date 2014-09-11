package li.cil.oc.common.template

import li.cil.oc.api.driver.{Inventory, Memory, Processor, UpgradeContainer}
import li.cil.oc.common.{Slot, Tier}
import li.cil.oc.{Localization, Settings, api}
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.IChatComponent

import scala.collection.mutable

abstract class Template {
  protected val suggestedComponents = Array(
    "Screen" -> hasComponent("screen1") _,
    "Keyboard" -> hasComponent("keyboard") _,
    "GraphicsCard" -> ((inventory: IInventory) => Array("graphicsCard1", "graphicsCard2", "graphicsCard3").exists(name => hasComponent(name)(inventory))),
    "Inventory" -> hasInventory _,
    "OS" -> hasFileSystem _)

  protected def validateComputer(inventory: IInventory): Array[AnyRef] = {
    val hasCase = caseTier(inventory) != Tier.None
    val hasCPU = this.hasCPU(inventory)
    val hasRAM = this.hasRAM(inventory)
    val complexity = this.complexity(inventory)
    val maxComplexity = this.maxComplexity(inventory)

    val valid = hasCase && hasCPU && hasRAM && complexity <= maxComplexity

    val progress =
      if (!hasCPU) Localization.Assembler.InsertCPU
      else if (!hasRAM) Localization.Assembler.InsertRAM
      else Localization.Assembler.Complexity(complexity, maxComplexity)

    val warnings = mutable.ArrayBuffer.empty[IChatComponent]
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

  protected def exists(inventory: IInventory, p: ItemStack => Boolean) = {
    (0 until inventory.getSizeInventory).exists(slot => Option(inventory.getStackInSlot(slot)) match {
      case Some(stack) => p(stack)
      case _ => false
    })
  }

  protected def hasCPU(inventory: IInventory) = exists(inventory, api.Driver.driverFor(_) match {
    case _: Processor => true
    case _ => false
  })

  protected def hasRAM(inventory: IInventory) = exists(inventory, api.Driver.driverFor(_) match {
    case _: Memory => true
    case _ => false
  })

  protected def hasComponent(name: String)(inventory: IInventory) = exists(inventory, stack => Option(api.Items.get(stack)) match {
    case Some(descriptor) => descriptor.name == name
    case _ => false
  })

  protected def hasInventory(inventory: IInventory) = exists(inventory, api.Driver.driverFor(_) match {
    case _: Inventory => true
    case _ => false
  })

  protected def hasFileSystem(inventory: IInventory) = exists(inventory, stack => Option(api.Driver.driverFor(stack)) match {
    case Some(driver) => Slot(driver, stack) == Slot.Floppy || Slot(driver, stack) == Slot.HDD
    case _ => false
  })

  protected def complexity(inventory: IInventory) = {
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

  protected def maxComplexity(inventory: IInventory) = {
    val caseTier = this.caseTier(inventory)
    val cpuTier = (0 until inventory.getSizeInventory).foldRight(0)((slot, acc) => {
      val stack = inventory.getStackInSlot(slot)
      acc + (api.Driver.driverFor(stack) match {
        case processor: Processor => processor.tier(stack)
        case _ => 0
      })
    })
    if (caseTier >= Tier.One && cpuTier >= Tier.One) {
      Settings.deviceComplexityByTier(caseTier) - (math.min(2, caseTier) - cpuTier) * 6
    }
    else 0
  }

  protected def caseTier(inventory: IInventory): Int
}
