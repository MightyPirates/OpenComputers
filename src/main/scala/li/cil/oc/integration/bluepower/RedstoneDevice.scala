package li.cil.oc.integration.bluepower

import com.bluepowermod.api.BPApi
import com.bluepowermod.api.connect.ConnectionType
import com.bluepowermod.api.connect.IConnectionCache
import com.bluepowermod.api.wire.redstone.IRedstoneDevice
import li.cil.oc.common.tileentity.traits.RedstoneAware
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

class RedstoneDevice(val tileEntity: RedstoneAware) extends IRedstoneDevice {
  lazy val cache = BPApi.getInstance.getRedstoneApi.createRedstoneConnectionCache(this)

  override def getX: Int = tileEntity.x

  override def getY: Int = tileEntity.y

  override def getZ: Int = tileEntity.z

  override def getWorld: World = tileEntity.world

  override def canConnect(side: ForgeDirection, dev: IRedstoneDevice, connectionType: ConnectionType): Boolean = tileEntity.isOutputEnabled

  override def isNormalFace(side: ForgeDirection): Boolean = true

  override def getRedstoneConnectionCache: IConnectionCache[_ <: IRedstoneDevice] = cache

  override def getRedstonePower(side: ForgeDirection): Byte = tileEntity.getOutput(side).toByte

  override def setRedstonePower(side: ForgeDirection, power: Byte): Unit = tileEntity.setInput(side, power & 0xFF)

  override def onRedstoneUpdate(): Unit = tileEntity.checkRedstoneInputChanged()
}
