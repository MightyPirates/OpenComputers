package li.cil.oc.common.template

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.api
import li.cil.oc.common.item.data.NavigationUpgradeData
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import scala.language.postfixOps

object NavigationUpgradeTemplate {
  def selectDisassembler(stack: ItemStack) = api.Items.get(stack) == api.Items.get("navigationUpgrade")

  def disassemble(stack: ItemStack, ingredients: Array[ItemStack]) = {
    val info = new NavigationUpgradeData(stack)
    ingredients.map {
      case part if part.getItem == net.minecraft.init.Items.filled_map => info.map
      case part => part
    }
  }

  def register() {
    // Disassembler
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Navigation Upgrade")
      nbt.setString("select", "li.cil.oc.common.template.NavigationUpgradeTemplate.selectDisassembler")
      nbt.setString("disassemble", "li.cil.oc.common.template.NavigationUpgradeTemplate.disassemble")

      FMLInterModComms.sendMessage("OpenComputers", "registerDisassemblerTemplate", nbt)
    }
  }
}
