package li.cil.oc.common.template

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.util.ItemUtils
import net.minecraft.item.ItemStack

import scala.language.postfixOps

object ServerTemplate {
  def selectDisassembler(stack: ItemStack) =
    api.Items.get(stack) == api.Items.get(Constants.ItemName.ServerTier1) ||
      api.Items.get(stack) == api.Items.get(Constants.ItemName.ServerTier2) ||
      api.Items.get(stack) == api.Items.get(Constants.ItemName.ServerTier3)

  def disassemble(stack: ItemStack, ingredients: Array[ItemStack]) = {
    val info = new ServerInventory {
      override def container = stack
    }
    Array(ingredients, (0 until info.getContainerSize).map(info.getItem).filter(null !=).toArray)
  }

  def register() {
    // Disassembler
    api.IMC.registerDisassemblerTemplate("Server",
      "li.cil.oc.common.template.ServerTemplate.selectDisassembler",
      "li.cil.oc.common.template.ServerTemplate.disassemble")
  }
}
