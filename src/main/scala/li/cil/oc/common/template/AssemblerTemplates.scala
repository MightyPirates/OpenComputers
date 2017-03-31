package li.cil.oc.common.template

import java.lang.reflect.Method

import com.google.common.base.Strings
import li.cil.oc.OpenComputers
import li.cil.oc.api
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.common.IMC
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.text.ITextComponent
import net.minecraftforge.common.util.Constants.NBT

import scala.collection.mutable
import scala.language.existentials

object AssemblerTemplates {
  val NoSlot = new Slot(Slot.None, Tier.None, None, None)

  private val templates = mutable.ArrayBuffer.empty[Template]

  private val templateFilters = mutable.ArrayBuffer.empty[Method]

  def add(template: NBTTagCompound): Unit = {
    val selector = IMC.getStaticMethod(template.getString("select"), classOf[ItemStack])
    val validator = IMC.getStaticMethod(template.getString("validate"), classOf[IInventory])
    val assembler = IMC.getStaticMethod(template.getString("assemble"), classOf[IInventory])
    val hostClass = tryGetHostClass(template.getString("hostClass"))
    val containerSlots = template.getTagList("containerSlots", NBT.TAG_COMPOUND).map((tag: NBTTagCompound) => parseSlot(tag, Some(Slot.Container), hostClass)).take(3).padTo(3, NoSlot).toArray
    val upgradeSlots = template.getTagList("upgradeSlots", NBT.TAG_COMPOUND).map((tag: NBTTagCompound) => parseSlot(tag, Some(Slot.Upgrade), hostClass)).take(9).padTo(9, NoSlot).toArray
    val componentSlots = template.getTagList("componentSlots", NBT.TAG_COMPOUND).map((tag: NBTTagCompound) => parseSlot(tag, None, hostClass)).take(9).padTo(9, NoSlot).toArray

    templates += new Template(selector, validator, assembler, containerSlots, upgradeSlots, componentSlots)
  }

  def addFilter(method: String): Unit = {
    templateFilters += IMC.getStaticMethod(method, classOf[ItemStack])
  }

  def select(stack: ItemStack) = {
    if (!stack.isEmpty && templateFilters.forall(IMC.tryInvokeStatic(_, stack)(true)))
      templates.find(_.select(stack))
    else
      None
  }

  class Template(val selector: Method,
                 val validator: Method,
                 val assembler: Method,
                 val containerSlots: Array[Slot],
                 val upgradeSlots: Array[Slot],
                 val componentSlots: Array[Slot]) {
    def select(stack: ItemStack) = IMC.tryInvokeStatic(selector, stack)(false)

    def validate(inventory: IInventory) = IMC.tryInvokeStatic(validator, inventory)(null: Array[AnyRef]) match {
      case Array(valid: java.lang.Boolean, progress: ITextComponent, warnings: Array[ITextComponent]) => (valid: Boolean, progress, warnings)
      case Array(valid: java.lang.Boolean, progress: ITextComponent) => (valid: Boolean, progress, Array.empty[ITextComponent])
      case Array(valid: java.lang.Boolean) => (valid: Boolean, null, Array.empty[ITextComponent])
      case _ => (false, null, Array.empty[ITextComponent])
    }

    def assemble(inventory: IInventory) = IMC.tryInvokeStatic(assembler, inventory)(null: Array[AnyRef]) match {
      case Array(stack: ItemStack, energy: java.lang.Number) => (stack, energy.doubleValue(): Double)
      case Array(stack: ItemStack) => (stack, 0.0)
      case _ => (null, 0.0)
    }
  }

  class Slot(val kind: String, val tier: Int, val validator: Option[Method], val hostClass: Option[Class[_ <: EnvironmentHost]]) {
    def validate(inventory: IInventory, slot: Int, stack: ItemStack) = validator match {
      case Some(method) => IMC.tryInvokeStatic(method, inventory, slot.underlying(), tier.underlying(), stack)(false)
      case _ => Option(hostClass.fold(api.Driver.driverFor(stack))(api.Driver.driverFor(stack, _))) match {
        case Some(driver) => try driver.slot(stack) == kind && driver.tier(stack) <= tier catch {
          case t: AbstractMethodError =>
            OpenComputers.log.warn(s"Error trying to query driver '${driver.getClass.getName}' for slot and/or tier information. Probably their fault. Yell at them before coming to OpenComputers for support. :P")
            false
        }
        case _ => false
      }
    }
  }

  private def parseSlot(nbt: NBTTagCompound, kindOverride: Option[String], hostClass: Option[Class[_ <: EnvironmentHost]]) = {
    val kind = kindOverride.getOrElse(if (nbt.hasKey("type")) nbt.getString("type") else Slot.None)
    val tier = if (nbt.hasKey("tier")) nbt.getInteger("tier") else Tier.Any
    val validator = if (nbt.hasKey("validate")) Option(IMC.getStaticMethod(nbt.getString("validate"), classOf[IInventory], classOf[Int], classOf[Int], classOf[ItemStack])) else None
    new Slot(kind, tier, validator, hostClass)
  }

  private def tryGetHostClass(name: String) =
    if (Strings.isNullOrEmpty(name)) None
    else Option(Class.forName(name).asSubclass(classOf[EnvironmentHost]))
}
