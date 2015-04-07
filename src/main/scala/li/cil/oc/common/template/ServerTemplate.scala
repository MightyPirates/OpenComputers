package li.cil.oc.common.template

import cpw.mods.fml.common.event.FMLInterModComms
import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.util.ItemUtils
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound

import scala.language.postfixOps

object ServerTemplate {
  def selectDisassembler(stack: ItemStack) =
    api.Items.get(stack) == api.Items.get(Constants.ItemName.ServerTier1) ||
      api.Items.get(stack) == api.Items.get(Constants.ItemName.ServerTier2) ||
      api.Items.get(stack) == api.Items.get(Constants.ItemName.ServerTier3)

  def disassemble(stack: ItemStack, ingredients: Array[ItemStack]) = {
    val info = new ServerInventory {
      override def tier = ItemUtils.caseTier(stack)

      override def container = stack
    }
    Array(ingredients, (0 until info.getSizeInventory).map(info.getStackInSlot).filter(null !=).toArray)
  }

  def register() {
    // Disassembler
    {
      val nbt = new NBTTagCompound()
      nbt.setString("name", "Server")
      nbt.setString("select", "li.cil.oc.common.template.ServerTemplate.selectDisassembler")
      nbt.setString("disassemble", "li.cil.oc.common.template.ServerTemplate.disassemble")

      FMLInterModComms.sendMessage("OpenComputers", "registerDisassemblerTemplate", nbt)
    }
  }
}
