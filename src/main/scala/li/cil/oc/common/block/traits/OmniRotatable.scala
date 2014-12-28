package li.cil.oc.common.block.traits

import com.google.common.base.Predicate
import com.google.common.base.Predicates
import li.cil.oc.common.tileentity
import net.minecraft.block.Block
import net.minecraft.block.properties.IProperty
import net.minecraft.block.properties.PropertyDirection
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.world.IBlockAccess
import net.minecraftforge.common.property.IExtendedBlockState
import net.minecraftforge.common.property.IUnlistedProperty
import net.minecraftforge.common.property.Properties

import scala.collection.mutable

trait OmniRotatable extends Block with Extended {
  final lazy val PitchRaw = PropertyDirection.create("pitch", Predicates.instanceOf(classOf[EnumFacing]))
  final lazy val Pitch: IUnlistedProperty[EnumFacing] = Properties.toUnlisted(PitchRaw)
  final lazy val YawRaw = PropertyDirection.create("yaw", EnumFacing.Plane.HORIZONTAL.asInstanceOf[Predicate[EnumFacing]])
  final lazy val Yaw: IUnlistedProperty[EnumFacing] = Properties.toUnlisted(YawRaw)

  override protected def addExtendedState(state: IExtendedBlockState, world: IBlockAccess, pos: BlockPos) =
    (world.getTileEntity(pos), state) match {
      case rotatable: tileentity.traits.Rotatable =>
        super.addExtendedState(state.withProperty(Pitch, rotatable.pitch).withProperty(Yaw, rotatable.yaw), world, pos)
      case _ =>
        None
    }

  override protected def addExtendedProperties(listed: mutable.ArrayBuffer[IProperty], unlisted: mutable.ArrayBuffer[IUnlistedProperty[_]]): Unit = {
    super.addExtendedProperties(listed, unlisted)
    unlisted += Pitch
    unlisted += Yaw
  }

  override protected def addExtendedRawProperties(unlisted: mutable.Map[IUnlistedProperty[_], IProperty]): Unit = {
    super.addExtendedRawProperties(unlisted)
    unlisted += Pitch -> PitchRaw
    unlisted += Yaw -> YawRaw
  }
}
