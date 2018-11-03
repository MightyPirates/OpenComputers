package li.cil.oc.integration.bluepower

import com.bluepowermod.api.BPApi
import com.bluepowermod.api.connect.ConnectionType
import com.bluepowermod.api.connect.IConnectionCache
import com.bluepowermod.api.misc.MinecraftColor
import com.bluepowermod.api.wire.redstone.IBundledDevice
import li.cil.oc.common.tileentity.traits.BundledRedstoneAware
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class BundledRedstoneDevice(val tileEntity: BundledRedstoneAware) extends IBundledDevice {
  lazy val cache = BPApi.getInstance.getRedstoneApi.createBundledConnectionCache(this)

  override def getX: Int = tileEntity.x

  override def getY: Int = tileEntity.y

  override def getZ: Int = tileEntity.z

  override def getWorld: World = tileEntity.world

  override def canConnect(side: ForgeDirection, dev: IBundledDevice, connectionType: ConnectionType): Boolean = tileEntity.isOutputEnabled

  override def isNormalFace(side: ForgeDirection): Boolean = true

  override def getBundledConnectionCache: IConnectionCache[_ <: IBundledDevice] = cache

  override def getBundledColor(side: ForgeDirection): MinecraftColor = MinecraftColor.ANY

  override def getBundledOutput(side: ForgeDirection): Array[Byte] = tileEntity.getBundledOutput(side).map(_.toByte)

  override def getBundledPower(side: ForgeDirection): Array[Byte] = tileEntity.getBundledInput(side).map(_.toByte)

  override def setBundledPower(side: ForgeDirection, power: Array[Byte]): Unit = tileEntity.setBundledInput(side, power.map(_ & 0xFF))

  override def onBundledUpdate(): Unit = tileEntity.checkRedstoneInputChanged()
}
