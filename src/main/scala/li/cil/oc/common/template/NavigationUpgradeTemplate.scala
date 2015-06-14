package li.cil.oc.common.template

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.common.item.data.NavigationUpgradeData
import net.minecraft.item.ItemStack

import scala.language.postfixOps

object NavigationUpgradeTemplate {
  def selectDisassembler(stack: ItemStack) = api.Items.get(stack) == api.Items.get(Constants.ItemName.NavigationUpgrade)

  def disassemble(stack: ItemStack, ingredients: Array[ItemStack]) = {
    val info = new NavigationUpgradeData(stack)
    ingredients.map {
      case part if part.getItem == net.minecraft.init.Items.filled_map => info.map
      case part => part
    }
  }

  def register() {
    // Disassembler
    api.IMC.registerDisassemblerTemplate(
      "Navigation Upgrade",
      "li.cil.oc.common.template.NavigationUpgradeTemplate.selectDisassembler",
      "li.cil.oc.common.template.NavigationUpgradeTemplate.disassemble")
  }
}
