package li.cil.oc.server.component

import net.minecraft.tileentity.{TileEntity => MCTileEntity, TileEntityFurnace}
import li.cil.oc.{Settings, api}
import li.cil.oc.api.network.Visibility
import net.minecraft.world.World
import net.minecraft.world.biome.BiomeGenDesert
import java.util.{Date, GregorianCalendar}


class SolarGenerator(val owner: MCTileEntity) extends ManagedComponent {
  val node = api.Network.newNode(this, Visibility.Network).
    withConnector().
    create()
  override val canUpdate = true
  var timeToCheck = 0
  var isSunShining = false
  override def update() {
    super.update()

    if(timeToCheck==0){
      timeToCheck = 100
      val world = owner.getWorldObj
      val x = owner.xCoord
      val y = owner.yCoord
      val z = owner.zCoord
      isSunShining = isSunVisible(world,x,y+1,z)

    }
    timeToCheck-=1
    if (isSunShining) {
      node.changeBuffer(Settings.get.ratioBuildCraft * Settings.get.solarGeneratorEfficiency)

    }
  }

  def isSunVisible(world: World, x: Int, y: Int, z: Int): Boolean = {
  world.isDaytime && (!world.provider.hasNoSky) && world.canBlockSeeTheSky(x, y, z) && (world.getWorldChunkManager.getBiomeGenAt(x, z).isInstanceOf[BiomeGenDesert] || ((!world.isRaining) && (!world.isThundering)))

  }

}
