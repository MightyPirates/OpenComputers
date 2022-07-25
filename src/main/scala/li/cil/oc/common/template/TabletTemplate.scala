package li.cil.oc.common.template

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.internal
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item.data.TabletData
import li.cil.oc.util.ItemUtils
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

import scala.collection.JavaConverters.asJavaIterable
import scala.collection.convert.ImplicitConversionsToJava._

object TabletTemplate extends Template {
  override protected val suggestedComponents = Array(
    "BIOS" -> hasComponent(Constants.ItemName.EEPROM) _,
    "Keyboard" -> hasComponent(Constants.BlockName.Keyboard) _,
    "GraphicsCard" -> ((inventory: IInventory) => Array(
      Constants.ItemName.APUCreative,
      Constants.ItemName.APUTier1,
      Constants.ItemName.APUTier2,
      Constants.ItemName.GraphicsCardTier1,
      Constants.ItemName.GraphicsCardTier2,
      Constants.ItemName.GraphicsCardTier3).
      exists(name => hasComponent(name)(inventory))),
    "OS" -> hasFileSystem _)

  override protected def hostClass = classOf[internal.Tablet]

  def selectTier1(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.ItemName.TabletCaseTier1)

  def selectTier2(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.ItemName.TabletCaseTier2)

  def selectCreative(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.ItemName.TabletCaseCreative)

  def validate(inventory: IInventory): Array[AnyRef] = validateComputer(inventory)

  def assemble(inventory: IInventory): Array[AnyRef] = {
    val items = (1 until inventory.getContainerSize).map(slot => inventory.getItem(slot))
    val data = new TabletData()
    data.tier = ItemUtils.caseTier(inventory.getItem(0))
    data.container = items.headOption.getOrElse(ItemStack.EMPTY)
    data.items = Array(api.Items.get(Constants.BlockName.ScreenTier1).createItemStack(1)) ++ items.drop(if (data.tier == Tier.One) 0 else 1).filter(!_.isEmpty)
    data.energy = Settings.get.bufferTablet
    data.maxEnergy = data.energy
    val stack = api.Items.get(Constants.ItemName.Tablet).createItemStack(1)
    data.saveData(stack)
    val energy = Settings.get.tabletBaseCost + complexity(inventory) * Settings.get.tabletComplexityCost

    Array(stack, Double.box(energy))
  }

  def selectDisassembler(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.ItemName.Tablet)

  def disassemble(stack: ItemStack, ingredients: Array[ItemStack]) = {
    val info = new TabletData(stack)
    val itemName = Constants.ItemName.TabletCase(info.tier)
    (Array(api.Items.get(itemName).createItemStack(1), info.container) ++ info.items.filter(!_.isEmpty).drop(1) /* Screen */).filter(!_.isEmpty)
  }

  def register() {
    // Tier 1
    api.IMC.registerAssemblerTemplate(
      "Tablet (Tier 1)",
      "li.cil.oc.common.template.TabletTemplate.selectTier1",
      "li.cil.oc.common.template.TabletTemplate.validate",
      "li.cil.oc.common.template.TabletTemplate.assemble",
      hostClass,
      null,
      Array(
        Tier.Three,
        Tier.Two,
        Tier.One
      ),
      asJavaIterable(Iterable(
        (Slot.Card, Tier.Two),
        (Slot.Card, Tier.Two),
        null,
        (Slot.CPU, Tier.Two),
        (Slot.Memory, Tier.Two),
        (Slot.Memory, Tier.Two),
        (Slot.EEPROM, Tier.Any),
        (Slot.HDD, Tier.Two)
      ).map(toPair)))

    // Tier 2
    api.IMC.registerAssemblerTemplate(
      "Tablet (Tier 2)",
      "li.cil.oc.common.template.TabletTemplate.selectTier2",
      "li.cil.oc.common.template.TabletTemplate.validate",
      "li.cil.oc.common.template.TabletTemplate.assemble",
      hostClass,
      Array(
        Tier.Two
      ),
      Array(
        Tier.Three,
        Tier.Two,
        Tier.Two
      ),
      asJavaIterable(Iterable(
        (Slot.Card, Tier.Three),
        (Slot.Card, Tier.Two),
        null,
        (Slot.CPU, Tier.Three),
        (Slot.Memory, Tier.Two),
        (Slot.Memory, Tier.Two),
        (Slot.EEPROM, Tier.Any),
        (Slot.HDD, Tier.Two)
      ).map(toPair)))

    // Creative
    api.IMC.registerAssemblerTemplate(
      "Tablet (Creative)",
      "li.cil.oc.common.template.TabletTemplate.selectCreative",
      "li.cil.oc.common.template.TabletTemplate.validate",
      "li.cil.oc.common.template.TabletTemplate.assemble",
      hostClass,
      Array(
        Tier.Three
      ),
      Array(
        Tier.Three,
        Tier.Three,
        Tier.Three,
        Tier.Three,
        Tier.Three,
        Tier.Three,
        Tier.Three,
        Tier.Three,
        Tier.Three
      ),
      asJavaIterable(Iterable(
        (Slot.Card, Tier.Three),
        (Slot.Card, Tier.Three),
        (Slot.Card, Tier.Three),
        (Slot.CPU, Tier.Three),
        (Slot.Memory, Tier.Three),
        (Slot.Memory, Tier.Three),
        (Slot.EEPROM, Tier.Any),
        (Slot.HDD, Tier.Three)
      ).map(toPair)))

    // Disassembler
    api.IMC.registerDisassemblerTemplate(
      "Tablet",
      "li.cil.oc.common.template.TabletTemplate.selectDisassembler",
      "li.cil.oc.common.template.TabletTemplate.disassemble")
  }

  override protected def maxComplexity(inventory: IInventory) = super.maxComplexity(inventory) / 2 + 5

  override protected def caseTier(inventory: IInventory) = ItemUtils.caseTier(inventory.getItem(0))
}
