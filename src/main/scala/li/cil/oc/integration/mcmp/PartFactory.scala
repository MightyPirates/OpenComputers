package li.cil.oc.integration.mcmp

import li.cil.oc.Constants
import li.cil.oc.api
import li.cil.oc.integration.Mods
import mcmultipart.item.IItemMultipartFactory
import mcmultipart.multipart.IMultipart
import mcmultipart.multipart.IPartFactory
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import net.minecraft.world.World

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
    else if (descriptor == PrintDescriptor) {
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
}
