package li.cil.oc.server.component

import li.cil.oc.Settings
import li.cil.oc.api.Network
import li.cil.oc.api.network.Visibility
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraft.world.biome.BiomeGenDesert

class UpgradeSolarGenerator(val owner: TileEntity) extends ManagedComponent {
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
      val world = owner.getWorldObj
      val x = owner.xCoord
      val y = owner.yCoord
      val z = owner.zCoord
      isSunShining = isSunVisible(world, x, y + 1, z)
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
