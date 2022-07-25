package li.cil.oc.integration.appeng

import java.util.Optional

import appeng.api.networking.security.{IActionHost, IActionSource}
import net.minecraft.entity.player.PlayerEntity

class MachineSource(val via: IActionHost) extends IActionSource {
  def player: Optional[PlayerEntity] = Optional.empty[PlayerEntity]

  def machine: Optional[IActionHost] = Optional.of(this.via)

  def context[T](key: Class[T]): Optional[T] = Optional.empty[T]
}