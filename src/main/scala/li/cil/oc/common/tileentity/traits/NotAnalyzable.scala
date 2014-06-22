package li.cil.oc.common.tileentity.traits

import li.cil.oc.api.network.{Analyzable, Node}
import net.minecraft.entity.player.EntityPlayer

trait NotAnalyzable extends Analyzable {
  override def onAnalyze(player: EntityPlayer, side: Int, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = null
}
