package li.cil.oc.common.template

import java.lang.reflect.Method

import li.cil.oc.OpenComputers
import li.cil.oc.common.IMC
import net.minecraft.item.ItemStack
import net.minecraft.nbt.CompoundNBT

import scala.collection.mutable

object DisassemblerTemplates {
  private val templates = mutable.ArrayBuffer.empty[Template]

  def add(template: CompoundNBT): Unit = try {
    val selector = IMC.getStaticMethod(template.getString("select"), classOf[ItemStack])
    val disassembler = IMC.getStaticMethod(template.getString("disassemble"), classOf[ItemStack], classOf[Array[ItemStack]])

    templates += new Template(selector, disassembler)
  }
  catch {
    case t: Throwable => OpenComputers.log.warn("Failed registering disassembler template.", t)
  }

  def select(stack: ItemStack) = templates.find(_.select(stack))

  class Template(val selector: Method,
                 val disassembler: Method) {
    def select(stack: ItemStack) = IMC.tryInvokeStatic(selector, stack)(false)

    def disassemble(stack: ItemStack, ingredients: Array[ItemStack]) = IMC.tryInvokeStatic(disassembler, stack, ingredients)(null: Array[_]) match {
      case Array(stacks: Array[ItemStack], drops: Array[ItemStack]) => (Some(stacks), Some(drops))
      case Array(stack: ItemStack, drops: Array[ItemStack]) => (Some(Array(stack)), Some(drops))
      case Array(stacks: Array[ItemStack], drop: ItemStack) => (Some(stacks), Some(Array(drop)))
      case stacks: Array[ItemStack] => (Some(stacks), None)
      case _ => (None, None)
    }
  }

}
