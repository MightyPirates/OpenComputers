package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.driver.Container
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.component
import net.minecraft.world.World
import net.minecraft.world.biome.BiomeGenDesert

class UpgradeSolarGenerator(val owner: Container) extends component.ManagedComponent {
  val node = Network.newNode(this, Visibility.Network).
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
      isSunShining = isSunVisible(owner.world, math.round(owner.xPosition - 0.5).toInt, math.round(owner.yPosition - 0.5).toInt + 1, math.round(owner.zPosition - 0.5).toInt)
    }
    if (isSunShining) {
      node.changeBuffer(Settings.get.solarGeneratorEfficiency)
    }
  }

  private def isSunVisible(world: World, x: Int, y: Int, z: Int): Boolean =
    world.isDaytime &&
      (!world.provider.hasNoSky) &&
      world.canBlockSeeTheSky(x, y, z) &&
      (world.getWorldChunkManager.getBiomeGenAt(x, z).isInstanceOf[BiomeGenDesert] || (!world.isRaining && !world.isThundering))
}
