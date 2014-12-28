package li.cil.oc.common.block.traits

import com.google.common.base.Predicate
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

trait Rotatable extends Block with Extended {
  final lazy val FacingRaw = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL.asInstanceOf[Predicate[EnumFacing]])
  final lazy val Facing: IUnlistedProperty[EnumFacing] = Properties.toUnlisted(FacingRaw)

  override protected def addExtendedState(state: IExtendedBlockState, world: IBlockAccess, pos: BlockPos) = {
    world.getTileEntity(pos) match {
      case rotatable: tileentity.traits.Rotatable =>
        super.addExtendedState(state.withProperty(Facing, rotatable.facing), world, pos)
      case _ =>
        None
    }
  }

  override protected def addExtendedProperties(listed: mutable.ArrayBuffer[IProperty], unlisted: mutable.ArrayBuffer[IUnlistedProperty[_]]): Unit = {
    super.addExtendedProperties(listed, unlisted)
    unlisted += Facing
  }

  override protected def addExtendedRawProperties(unlisted: mutable.Map[IUnlistedProperty[_], IProperty]): Unit = {
    super.addExtendedRawProperties(unlisted)
    unlisted += Facing -> FacingRaw
  }
}
