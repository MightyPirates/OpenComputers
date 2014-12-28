package li.cil.oc.common.template

import li.cil.oc.api
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.util.ItemUtils
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.common.event.FMLInterModComms

import scala.language.postfixOps

object ServerTemplate {
  def selectDisassembler(stack: ItemStack) =
    api.Items.get(stack) == api.Items.get("server1") ||
      api.Items.get(stack) == api.Items.get("server2") ||
      api.Items.get(stack) == api.Items.get("server3")

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
