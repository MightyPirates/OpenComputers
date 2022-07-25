package li.cil.oc.common.nanomachines.provider

import li.cil.oc.api.nanomachines.Behavior
import li.cil.oc.api.prefab.AbstractProvider
import net.minecraft.entity.player.PlayerEntity

import scala.collection.convert.WrapAsJava._

abstract class ScalaProvider(id: String) extends AbstractProvider(id) {
  def createScalaBehaviors(player: PlayerEntity): Iterable[Behavior]

  override def createBehaviors(player: PlayerEntity): java.lang.Iterable[Behavior] = asJavaIterable(createScalaBehaviors(player))
}
