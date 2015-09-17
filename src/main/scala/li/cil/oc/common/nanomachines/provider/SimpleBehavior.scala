package li.cil.oc.common.nanomachines.provider

import li.cil.oc.api.nanomachines.Behavior
import net.minecraft.entity.player.EntityPlayer

class SimpleBehavior(val player: EntityPlayer) extends Behavior {
  override def getNameHint: String = null

  override def onEnable(): Unit = {}

  override def onDisable(): Unit = {}

  override def update(): Unit = {}
}
