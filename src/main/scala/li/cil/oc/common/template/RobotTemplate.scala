package li.cil.oc.common.template

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.internal
import li.cil.oc.common.Slot
import li.cil.oc.common.Tier
import li.cil.oc.common.item.data.RobotData
import li.cil.oc.util.ItemUtils
import net.minecraft.inventory.IInventory
import net.minecraft.item.ItemStack

import scala.collection.JavaConverters.asJavaIterable
import scala.collection.convert.ImplicitConversionsToJava._

object RobotTemplate extends Template {
  override protected def hostClass = classOf[internal.Robot]

  def selectTier1(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.BlockName.CaseTier1)

  def selectTier2(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.BlockName.CaseTier2)

  def selectTier3(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.BlockName.CaseTier3)

  def selectCreative(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.BlockName.CaseCreative)

  def validate(inventory: IInventory): Array[AnyRef] = validateComputer(inventory)

  def assemble(inventory: IInventory) = {
    val items = (1 until inventory.getContainerSize).map(inventory.getItem)
    val data = new RobotData()
    data.tier = caseTier(inventory)
    data.name = RobotData.randomName
    data.robotEnergy = Settings.get.bufferRobot.toInt
    data.totalEnergy = data.robotEnergy
    data.containers = items.take(3).filter(!_.isEmpty).toArray
    data.components = items.drop(3).filter(!_.isEmpty).toArray
    val stack = data.createItemStack()
    val energy = Settings.get.robotBaseCost + complexity(inventory) * Settings.get.robotComplexityCost

    Array(stack, Double.box(energy))
  }

  def selectDisassembler(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.BlockName.Robot)

  def disassemble(stack: ItemStack, ingredients: Array[ItemStack]) = {
    val info = new RobotData(stack)
    val itemName = Constants.BlockName.Case(info.tier)

    Array(api.Items.get(itemName).createItemStack(1)) ++ info.containers ++ info.components
  }

  def register() {
    // Tier 1
    api.IMC.registerAssemblerTemplate(
      "Robot (Tier 1)",
      "li.cil.oc.common.template.RobotTemplate.selectTier1",
      "li.cil.oc.common.template.RobotTemplate.validate",
      "li.cil.oc.common.template.RobotTemplate.assemble",
      hostClass,
      Array(
        Tier.Two,
        Tier.One,
        Tier.One
      ),
      Array(
        Tier.One,
        Tier.One,
        Tier.One
      ),
      asJavaIterable(Iterable(
        (Slot.Card, Tier.One),
        null,
        null,
        (Slot.CPU, Tier.One),
        (Slot.Memory, Tier.One),
        (Slot.Memory, Tier.One),
        (Slot.EEPROM, Tier.Any),
        (Slot.HDD, Tier.One)
      ).map(toPair)))

    // Tier 2
    api.IMC.registerAssemblerTemplate(
      "Robot (Tier 2)",
      "li.cil.oc.common.template.RobotTemplate.selectTier2",
      "li.cil.oc.common.template.RobotTemplate.validate",
      "li.cil.oc.common.template.RobotTemplate.assemble",
      hostClass,
      Array(
        Tier.Three,
        Tier.Two,
        Tier.One
      ),
      Array(
        Tier.Two,
        Tier.Two,
        Tier.Two,
        Tier.One,
        Tier.One,
        Tier.One
      ),
      asJavaIterable(Iterable(
        (Slot.Card, Tier.Two),
        (Slot.Card, Tier.One),
        null,
        (Slot.CPU, Tier.Two),
        (Slot.Memory, Tier.Two),
        (Slot.Memory, Tier.Two),
        (Slot.EEPROM, Tier.Any),
        (Slot.HDD, Tier.Two)
      ).map(toPair)))

    // Tier 3
    api.IMC.registerAssemblerTemplate(
      "Robot (Tier 3)",
      "li.cil.oc.common.template.RobotTemplate.selectTier3",
      "li.cil.oc.common.template.RobotTemplate.validate",
      "li.cil.oc.common.template.RobotTemplate.assemble",
      hostClass,
      Array(
        Tier.Three,
        Tier.Two,
        Tier.Two
      ),
      Array(
        Tier.Three,
        Tier.Three,
        Tier.Three,
        Tier.Two,
        Tier.Two,
        Tier.Two,
        Tier.One,
        Tier.One,
        Tier.One
      ),
      asJavaIterable(Iterable(
        (Slot.Card, Tier.Three),
        (Slot.Card, Tier.Two),
        (Slot.Card, Tier.Two),
        (Slot.CPU, Tier.Three),
        (Slot.Memory, Tier.Three),
        (Slot.Memory, Tier.Three),
        (Slot.EEPROM, Tier.Any),
        (Slot.HDD, Tier.Three),
        (Slot.HDD, Tier.Two)
      ).map(toPair)))

    // Creative
    api.IMC.registerAssemblerTemplate(
      "Robot (Creative)",
      "li.cil.oc.common.template.RobotTemplate.selectCreative",
      "li.cil.oc.common.template.RobotTemplate.validate",
      "li.cil.oc.common.template.RobotTemplate.assemble",
      hostClass,
      Array(
        Tier.Three,
        Tier.Three,
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
        (Slot.HDD, Tier.Three),
        (Slot.HDD, Tier.Three)
      ).map(toPair)))

    // Disassembler
    api.IMC.registerDisassemblerTemplate(
      "Robot",
      "li.cil.oc.common.template.RobotTemplate.selectDisassembler",
      "li.cil.oc.common.template.RobotTemplate.disassemble")
  }

  override protected def caseTier(inventory: IInventory) = ItemUtils.caseTier(inventory.getItem(0))
}
