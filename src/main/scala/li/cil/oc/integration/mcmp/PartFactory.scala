package li.cil.oc.integration.mcmp

import li.cil.oc.Constants
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.common.item.data.PrintData
import li.cil.oc.integration.Mods
import mcmultipart.item.IItemMultipartFactory
import mcmultipart.multipart.IMultipart
import mcmultipart.multipart.IPartFactory
import mcmultipart.multipart.MultipartHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import net.minecraft.world.World

import scala.collection.convert.WrapAsScala._

object PartFactory extends IPartFactory with IItemMultipartFactory {
  final val PartTypeCable = Mods.IDs.OpenComputers + ":" + Constants.BlockName.Cable
  final val PartTypePrint = Mods.IDs.OpenComputers + ":" + Constants.BlockName.Print

  final lazy val CableDescriptor = api.Items.get(Constants.BlockName.Cable)
  final lazy val PrintDescriptor = api.Items.get(Constants.BlockName.Print)

  override def createPart(partType: String, client: Boolean): IMultipart = {
    if (partType == PartTypeCable) new PartCable()
    else if (partType == PartTypePrint) new PartPrint()
    else null
  }

  override def createPart(world: World, pos: BlockPos, side: EnumFacing, hit: Vec3, stack: ItemStack, player: EntityPlayer): IMultipart = {
    val descriptor = api.Items.get(stack)
    if (descriptor == CableDescriptor) new PartCable()
    else if (descriptor == PrintDescriptor && canAddPrint(world, pos, stack)) {
      val part = new PartPrint()
      part.wrapped.data.load(stack)
      part.wrapped.setFromEntityPitchAndYaw(player)
      if (part.wrapped.validFacings.contains(part.wrapped.pitch)) {
        part.wrapped.pitch = part.wrapped.validFacings.headOption.getOrElse(EnumFacing.NORTH)
      }
      part
    }
    else null
  }

  private def canAddPrint(world: World, pos: BlockPos, stack: ItemStack): Boolean = {
    val container = MultipartHelper.getPartContainer(world, pos)
    container == null || {
      val complexity = container.getParts.collect {
        case print: PartPrint => print.wrapped.data.complexity
      }.sum
      val data = new PrintData(stack)
      data.complexity + complexity <= Settings.get.maxPrintComplexity
    }
  }
}
