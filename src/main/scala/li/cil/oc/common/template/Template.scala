package li.cil.oc.common.template

import li.cil.oc.Constants
import li.cil.oc.Localization
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.network.Environment
import li.cil.oc.api.util.Location
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.util.text.ITextComponent
import org.apache.commons.lang3.tuple

import scala.collection.mutable

abstract class Template {
  protected val suggestedComponents = Array(
    "BIOS" -> hasComponent(Constants.ItemName.EEPROM) _,
    "Screen" -> hasComponent(Constants.BlockName.ScreenTier1) _,
    "Keyboard" -> hasComponent(Constants.BlockName.Keyboard) _,
    "GraphicsCard" -> ((inventory: IInventory) => Array(
      Constants.ItemName.APUCreative,
      Constants.ItemName.APUTier1,
      Constants.ItemName.APUTier2,
      Constants.ItemName.GraphicsCardTier1,
      Constants.ItemName.GraphicsCardTier2,
      Constants.ItemName.GraphicsCardTier3).
      exists(name => hasComponent(name)(inventory))),
    "Inventory" -> hasInventory _,
    "OS" -> hasFileSystem _)

  protected def hostClass: Class[_ <: Location]

  protected def validateComputer(inventory: IInventory): Array[AnyRef] = {
    val hasCase = caseTier(inventory) != Tier.None
    val hasCPU = this.hasCPU(inventory)
    val hasRAM = this.hasRAM(inventory)
    val requiresRAM = this.requiresRAM(inventory)
    val complexity = this.complexity(inventory)
    val maxComplexity = this.maxComplexity(inventory)

    val valid = hasCase && hasCPU && (hasRAM || !requiresRAM) && complexity <= maxComplexity

    val progress =
      if (!hasCPU) Localization.Assembler.InsertCPU
      else if (!hasRAM && requiresRAM) Localization.Assembler.InsertRAM
      else Localization.Assembler.Complexity(complexity, maxComplexity)

    val warnings = mutable.ArrayBuffer.empty[ITextComponent]
    for ((name, check) <- suggestedComponents) {
      if (!check(inventory)) {
        warnings += Localization.Assembler.Warning(name)
      }
    }
    if (warnings.nonEmpty) {
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

  protected def hasCPU(inventory: IInventory) = exists(inventory, api.Driver.driverFor(_, hostClass) match {
    case _: api.driver.item.Processor => true
    case _ => false
  })

  protected def hasRAM(inventory: IInventory) = exists(inventory, api.Driver.driverFor(_, hostClass) match {
    case _: api.driver.item.Memory => true
    case _ => false
  })

  protected def requiresRAM(inventory: IInventory) = !(0 until inventory.getSizeInventory).
    map(inventory.getStackInSlot).
    exists(stack => api.Driver.driverFor(stack, hostClass) match {
      case driver: api.driver.item.Processor =>
        val architecture = driver.architecture(stack)
        architecture != null && architecture.getAnnotation(classOf[api.machine.Architecture.NoMemoryRequirements]) != null
      case _ => false
    })

  protected def hasComponent(name: String)(inventory: IInventory) = exists(inventory, stack => Option(api.Items.get(stack)) match {
    case Some(descriptor) => descriptor.name == name
    case _ => false
  })

  protected def hasInventory(inventory: IInventory) = exists(inventory, api.Driver.driverFor(_, hostClass) match {
    case _: api.driver.item.Inventory => true
    case _ => false
  })

  protected def hasFileSystem(inventory: IInventory) = exists(inventory, stack => Option(api.Driver.driverFor(stack, hostClass)) match {
    case Some(driver) => driver.slot(stack) == Slot.Floppy || driver.slot(stack) == Slot.HDD
    case _ => false
  })

  protected def complexity(inventory: IInventory) = {
    var acc = 0
    for (slot <- 1 until inventory.getSizeInventory) {
      val stack = inventory.getStackInSlot(slot)
      acc += (Option(api.Driver.driverFor(stack, hostClass)) match {
        case Some(driver: api.driver.item.Processor) => 0 // CPUs are exempt, since they control the limit.
        case Some(driver: api.driver.item.Container) => (1 + driver.tier(stack)) * 2
        case Some(driver) if driver.slot(stack) != Slot.EEPROM => 1 + driver.tier(stack)
        case _ => 0
      })
    }
    acc
  }

  protected def maxComplexity(inventory: IInventory) = {
    val caseTier = this.caseTier(inventory)
    val cpuTier = (0 until inventory.getSizeInventory).foldRight(0)((slot, acc) => {
      val stack = inventory.getStackInSlot(slot)
      acc + (api.Driver.driverFor(stack, hostClass) match {
        case processor: api.driver.item.Processor => processor.tier(stack)
        case _ => 0
      })
    })
    if (caseTier >= Tier.One && cpuTier >= Tier.One) {
      Settings.deviceComplexityByTier(caseTier) - (math.min(2, caseTier) - cpuTier) * 6
    }
    else 0
  }

  protected def caseTier(inventory: IInventory): Int

  protected def toPair(t: (String, Int)): tuple.Pair[String, java.lang.Integer] =
    if (t == null) null
    else tuple.Pair.of(t._1, t._2)
}
