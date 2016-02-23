package li.cil.oc.server.component

/* TODO WRCBE
import codechicken.lib.vec.Vector3
import codechicken.wirelessredstone.core.WirelessReceivingDevice
import codechicken.wirelessredstone.core.WirelessTransmittingDevice
*/

import li.cil.oc.Settings
import li.cil.oc.api.network.EnvironmentHost
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network._
import li.cil.oc.common.EventHandler
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.fml.common.Optional

@Optional.InterfaceList(Array(
  new Optional.Interface(iface = "codechicken.wirelessredstone.core.WirelessReceivingDevice", modid = Mods.IDs.WirelessRedstoneCBE),
  new Optional.Interface(iface = "codechicken.wirelessredstone.core.WirelessTransmittingDevice", modid = Mods.IDs.WirelessRedstoneCBE)
))
trait RedstoneWireless extends RedstoneSignaller /* with WirelessReceivingDevice with WirelessTransmittingDevice TODO WRCBE */ {
  def redstone: EnvironmentHost

  var wirelessFrequency = 0

  var wirelessInput = false

  var wirelessOutput = false

  // ----------------------------------------------------------------------- //

  @Callback(doc = """function():number -- Get the wireless redstone input.""")
  def getWirelessInput(context: Context, args: Arguments): Array[AnyRef] = {
    wirelessInput = util.WirelessRedstone.getInput(this)
    result(wirelessInput)
  }

  @Callback(direct = true, doc = """function():boolean -- Get the wireless redstone output.""")
  def getWirelessOutput(context: Context, args: Arguments): Array[AnyRef] = result(wirelessOutput)

  @Callback(doc = """function(value:boolean):boolean -- Set the wireless redstone output.""")
  def setWirelessOutput(context: Context, args: Arguments): Array[AnyRef] = {
    val oldValue = wirelessOutput
    val newValue = args.checkBoolean(0)

    if (oldValue != newValue) {
      wirelessOutput = newValue

      util.WirelessRedstone.updateOutput(this)

      if (Settings.get.redstoneDelay > 0)
        context.pause(Settings.get.redstoneDelay)
    }

    result(oldValue)
  }

  @Callback(direct = true, doc = """function():number -- Get the currently set wireless redstone frequency.""")
  def getWirelessFrequency(context: Context, args: Arguments): Array[AnyRef] = result(wirelessFrequency)

  @Callback(doc = """function(frequency:number):number -- Set the wireless redstone frequency to use.""")
  def setWirelessFrequency(context: Context, args: Arguments): Array[AnyRef] = {
    val oldValue = wirelessFrequency
    val newValue = args.checkInteger(0)

    if (oldValue != newValue) {
      util.WirelessRedstone.removeReceiver(this)
      util.WirelessRedstone.removeTransmitter(this)

      wirelessFrequency = newValue
      wirelessInput = false
      wirelessOutput = false

      util.WirelessRedstone.addReceiver(this)

      context.pause(0.5)
    }

    result(oldValue)
  }

  // ----------------------------------------------------------------------- //
  /* TODO WRCBE
    @Optional.Method(modid = Mods.IDs.WirelessRedstoneCBE)
    override def updateDevice(frequency: Int, on: Boolean) {
      if (frequency == wirelessFrequency && on != wirelessInput) {
        wirelessInput = on
      onRedstoneChanged("wireless", if (on) 0 else 1, if (on) 1 else 0)
      }
    }

    @Optional.Method(modid = Mods.IDs.WirelessRedstoneCBE)
  override def getPosition = new Vector3(redstone.xPosition, redstone.yPosition, redstone.zPosition)

    @Optional.Method(modid = Mods.IDs.WirelessRedstoneCBE)
  override def getDimension = redstone.world.provider.getDimensionId

    @Optional.Method(modid = Mods.IDs.WirelessRedstoneCBE)
    override def getFreq = wirelessFrequency

    @Optional.Method(modid = Mods.IDs.WirelessRedstoneCBE)
    override def getAttachedEntity = null
  */
  // ----------------------------------------------------------------------- //

  override def onConnect(node: Node) {
    super.onConnect(node)
    if (node == this.node) {
      EventHandler.scheduleWirelessRedstone(this)
    }
  }

  override def onDisconnect(node: Node) {
    super.onDisconnect(node)
    if (node == this.node) {
      util.WirelessRedstone.removeReceiver(this)
      util.WirelessRedstone.removeTransmitter(this)
      wirelessOutput = false
      wirelessFrequency = 0
    }
  }

  // ----------------------------------------------------------------------- //

  override def load(nbt: NBTTagCompound) {
    super.load(nbt)
    wirelessFrequency = nbt.getInteger("wirelessFrequency")
    wirelessInput = nbt.getBoolean("wirelessInput")
    wirelessOutput = nbt.getBoolean("wirelessOutput")
  }

  override def save(nbt: NBTTagCompound) {
    super.save(nbt)
    nbt.setInteger("wirelessFrequency", wirelessFrequency)
    nbt.setBoolean("wirelessInput", wirelessInput)
    nbt.setBoolean("wirelessOutput", wirelessOutput)
  }
}
