package li.cil.oc.common.nanomachines.provider

import li.cil.oc.api.nanomachines.Behavior
import li.cil.oc.api.prefab.nanomachines.AbstractProvider
import net.minecraft.entity.player.EntityPlayer

import scala.collection.convert.WrapAsJava._

abstract class ScalaProvider(id: String) extends AbstractProvider(id) {
  def createScalaBehaviors(player: EntityPlayer): Iterable[Behavior]

  override def createBehaviors(player: EntityPlayer): java.lang.Iterable[Behavior] = asJavaIterable(createScalaBehaviors(player))
}
