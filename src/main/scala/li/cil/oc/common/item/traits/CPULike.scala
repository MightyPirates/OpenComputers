package li.cil.oc.common.item.traits

import java.util

import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.machine.Architecture
import li.cil.oc.util.Tooltip
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.ChatComponentTranslation
import net.minecraft.world.World

import scala.collection.convert.WrapAsScala._
import scala.language.existentials

trait CPULike extends Delegate {
  def cpuTier: Int

  override protected def tooltipData: Seq[Any] = Seq(Settings.get.cpuComponentSupport(cpuTier))

  override protected def tooltipExtended(stack: ItemStack, tooltip: util.List[String]) {
    (if (stack.hasTagCompound) {
      Option(stack.getTagCompound.getString(Settings.namespace + "archName"))
    }
    else {
      val architectures = allArchitectures
      architectures.headOption.map(_._2)
    }) match {
      case Some(archName) if !archName.isEmpty => tooltip.addAll(Tooltip.get("CPU.Architecture", archName))
      case _ => allArchitectures.headOption.collect {
        case ((_, name)) => tooltip.addAll(Tooltip.get("CPU.Architecture", name))
      }
    }
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer) = {
    if (player.isSneaking) {
      if (!world.isRemote) {
        val architectures = allArchitectures
        if (architectures.length > 0) {
          val currentIndex = if (stack.hasTagCompound) {
            val currentArch = stack.getTagCompound.getString(Settings.namespace + "archClass")
            architectures.indexWhere(_._1.getName == currentArch)
          }
          else {
            stack.setTagCompound(new NBTTagCompound())
            -1
          }
          val index = (currentIndex + 1) % architectures.length
          val (archClass, archName) = architectures(index)
          stack.getTagCompound.setString(Settings.namespace + "archClass", archClass.getName)
          stack.getTagCompound.setString(Settings.namespace + "archName", archName)
          player.addChatMessage(new ChatComponentTranslation(Settings.namespace + "tooltip.CPU.Architecture", archName))
        }
        player.swingItem()
      }
    }
    stack
  }

  private def allArchitectures = api.Machine.architectures.map { arch =>
    arch.getAnnotation(classOf[Architecture.Name]) match {
      case annotation: Architecture.Name => (arch, annotation.value)
      case _ => (arch, arch.getSimpleName)
    }
  }.toArray
}
