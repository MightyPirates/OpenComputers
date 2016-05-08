package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.util.BlockPosition
import net.minecraft.util.EnumFacing

class UpgradeSolarGenerator(val host: EnvironmentHost) extends prefab.ManagedEnvironment {
  override val node = Network.newNode(this, Visibility.Network).
    withConnector().
    create()

  var ticksUntilCheck = 0

  var isSunShining = false

  // ----------------------------------------------------------------------- //

  override val canUpdate = true

  override def update() {
    super.update()

    ticksUntilCheck -= 1
    if (ticksUntilCheck <= 0) {
      ticksUntilCheck = 100
      isSunShining = isSunVisible
    }
    if (isSunShining) {
      node.changeBuffer(Settings.get.solarGeneratorEfficiency)
    }
  }

  private def isSunVisible = {
    val blockPos = BlockPosition(host).offset(EnumFacing.UP)
    host.world.isDaytime &&
      (!host.world.provider.getHasNoSky) &&
      host.world.canBlockSeeSky(blockPos.toBlockPos) &&
      (!host.world.getBiomeGenForCoords(blockPos.toBlockPos).canRain || (!host.world.isRaining && !host.world.isThundering))
  }
}
