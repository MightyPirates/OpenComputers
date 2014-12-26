package li.cil.oc.common.tileentity.traits

import li.cil.oc.api.network.Analyzable
import li.cil.oc.api.network.Node
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumFacing

trait NotAnalyzable extends Analyzable {
  override def onAnalyze(player: EntityPlayer, side: EnumFacing, hitX: Float, hitY: Float, hitZ: Float): Array[Node] = null
}
