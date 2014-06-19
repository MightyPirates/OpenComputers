package li.cil.oc.common.multipart

import codechicken.multipart.{TIconHitEffects, TMultiPart}
import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.common.block.Delegate
import net.minecraft.entity.Entity
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.common.ForgeDirection

import scala.collection.convert.WrapAsJava

abstract class DelegatePart extends TMultiPart with TIconHitEffects {
  def delegate: Delegate

  override def pickItem(hit: MovingObjectPosition) = delegate.createItemStack()

  override def getDrops = WrapAsJava.asJavaIterable(Iterable(delegate.createItemStack()))

  override def explosionResistance(entity: Entity) = delegate.explosionResistance(entity)

  @SideOnly(Side.CLIENT)
  override def getBrokenIcon(side: Int) = delegate.icon(ForgeDirection.getOrientation(side)).orNull
}
