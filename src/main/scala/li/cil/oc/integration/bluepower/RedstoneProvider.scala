package li.cil.oc.integration.bluepower

import com.bluepowermod.api.BPApi
import com.bluepowermod.api.wire.redstone.IBundledDevice
import com.bluepowermod.api.wire.redstone.IRedstoneDevice
import com.bluepowermod.api.wire.redstone.IRedstoneProvider
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware
import li.cil.oc.common.tileentity.traits.RedstoneAware
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.mutable

object RedstoneProvider extends IRedstoneProvider {
  val redstoneDevices = mutable.WeakHashMap.empty[RedstoneAware, RedstoneDevice]

  val bundledRedstoneDevices = mutable.WeakHashMap.empty[BundledRedstoneAware, BundledRedstoneDevice]

  def init(): Unit = {
    BPApi.getInstance.getRedstoneApi.registerRedstoneProvider(this)
  }

  override def getRedstoneDeviceAt(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, face: ForgeDirection): IRedstoneDevice = {
    world.getTileEntity(x, y, z) match {
      case tileEntity: RedstoneAware => redstoneDevices.getOrElseUpdate(tileEntity, new RedstoneDevice(tileEntity))
      case _ => null
    }
  }

  override def getBundledDeviceAt(world: World, x: Int, y: Int, z: Int, side: ForgeDirection, face: ForgeDirection): IBundledDevice = {
    world.getTileEntity(x, y, z) match {
      case tileEntity: BundledRedstoneAware => bundledRedstoneDevices.getOrElseUpdate(tileEntity, new BundledRedstoneDevice(tileEntity))
      case _ => null
    }
  }
}
