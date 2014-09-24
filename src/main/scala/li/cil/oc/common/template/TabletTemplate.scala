package li.cil.oc.common.template

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.{Settings, api}
import li.cil.oc.common.{Slot, Tier}
import li.cil.oc.server.driver.item
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.ItemUtils
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTTagCompound, NBTTagList}

import scala.collection.mutable

object TabletTemplate extends Template {
  override protected val suggestedComponents = Array(
    "GraphicsCard" -> ((inventory: IInventory) => Array("graphicsCard1", "graphicsCard2", "graphicsCard3").exists(name => hasComponent(name)(inventory))),
    "OS" -> hasFileSystem _)

  def select(stack: ItemStack) = api.Items.get(stack) == api.Items.get("tabletCase")

  def validate(inventory: IInventory): Array[AnyRef] = validateComputer(inventory)

  def validateUpgrade(inventory: IInventory, slot: Int, tier: Int, stack: ItemStack): Boolean = Option(api.Driver.driverFor(stack)) match {
    case Some(driver) if Slot(driver, stack) == Slot.Upgrade =>
      driver != item.Screen &&
        Slot(driver, stack) == Slot.Upgrade && driver.tier(stack) <= tier
    case _ => false
  }

  def assemble(inventory: IInventory): Array[AnyRef] = {
    val items = mutable.ArrayBuffer(
      Option(api.Items.get("screen1").createItemStack(1))
    ) ++ (1 until inventory.getSizeInventory).map(slot => Option(inventory.getStackInSlot(slot)))
    val data = new ItemUtils.TabletData()
    data.items = items.filter(_.isDefined).toArray
    data.energy = Settings.get.bufferTablet
    data.maxEnergy = data.energy
    val stack = api.Items.get("tablet").createItemStack(1)
    data.save(stack)
    val energy = Settings.get.tabletBaseCost + complexity(inventory) * Settings.get.tabletComplexityCost

    Array(stack, double2Double(energy))
  }

  def register() {
    val nbt = new NBTTagCompound()
    nbt.setString("select", "li.cil.oc.common.template.TabletTemplate.select")
    nbt.setString("validate", "li.cil.oc.common.template.TabletTemplate.validate")
    nbt.setString("assemble", "li.cil.oc.common.template.TabletTemplate.assemble")

    val upgradeSlots = new NBTTagList()
    upgradeSlots.appendTag(Map("tier" -> Tier.Three, "validate" -> "li.cil.oc.common.template.TabletTemplate.validateUpgrade"))
    upgradeSlots.appendTag(Map("tier" -> Tier.Two, "validate" -> "li.cil.oc.common.template.TabletTemplate.validateUpgrade"))
    upgradeSlots.appendTag(Map("tier" -> Tier.One, "validate" -> "li.cil.oc.common.template.TabletTemplate.validateUpgrade"))
    nbt.setTag("upgradeSlots", upgradeSlots)

    val componentSlots = new NBTTagList()
    componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Two))
    componentSlots.appendTag(Map("type" -> Slot.Card, "tier" -> Tier.Two))
    componentSlots.appendTag(new NBTTagCompound())
    componentSlots.appendTag(Map("type" -> Slot.CPU, "tier" -> Tier.Two))
    componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.Two))
    componentSlots.appendTag(Map("type" -> Slot.Memory, "tier" -> Tier.Two))
    componentSlots.appendTag(Map("type" -> Slot.HDD, "tier" -> Tier.Two))
    nbt.setTag("componentSlots", componentSlots)

    FMLInterModComms.sendMessage("OpenComputers", "registerAssemblerTemplate", nbt)
  }

  override protected def maxComplexity(inventory: IInventory) = super.maxComplexity(inventory) - 10

  override protected def caseTier(inventory: IInventory) = if (select(inventory.getStackInSlot(0))) Tier.Two else Tier.None
}
