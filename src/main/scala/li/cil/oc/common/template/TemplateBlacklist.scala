package li.cil.oc.common.template

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.ResourceLocation

import scala.collection.convert.WrapAsScala._

object TemplateBlacklist {
  private lazy val TheBlacklist = { // scnr
    val pattern = """^([^@]+)(?:@(\d+))?$""".r
    def parseDescriptor(id: String, meta: Int) = {
      val item = Item.REGISTRY.getObject(new ResourceLocation(id))
      if (item == null) {
        OpenComputers.log.warn(s"Bad assembler blacklist entry '$id', unknown item id.")
        None
      }
      else {
        Option(new ItemStack(item, 1, meta))
      }
    }
    Settings.get.assemblerBlacklist.map {
      case pattern(id, null) => parseDescriptor(id, 0)
      case pattern(id, meta) => try parseDescriptor(id, meta.toInt) catch {
        case _: NumberFormatException =>
          OpenComputers.log.warn(s"Bad assembler blacklist entry '$id@$meta', invalid damage value.")
          None
      }
      case badFormat =>
        OpenComputers.log.warn(s"Bad assembler blacklist entry '$badFormat', invalid format (should be 'id' or 'id@damage').")
        None
    }.collect {
      case Some(stack) => stack
    }.toArray
  }

  def register(): Unit = {
    api.IMC.registerAssemblerFilter("li.cil.oc.common.template.TemplateBlacklist.filter")
  }

  def filter(stack: ItemStack): Boolean = {
    !TheBlacklist.exists(_.isItemEqual(stack))
  }
}
