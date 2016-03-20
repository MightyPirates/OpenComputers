package li.cil.oc.common.tileentity.traits.power

import cpw.mods.fml.common.Optional
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods
import li.cil.oc.integration.util.Power
import net.minecraftforge.common.util.ForgeDirection

@Injectable.Interface(value = "Reika.RotaryCraft.API.Power.ShaftPowerReceiver", modid = Mods.IDs.RotaryCraft)
trait RotaryCraft extends Common {
  private lazy val useRotaryCraftPower = isServer && Mods.RotaryCraft.isAvailable

  private var omega = 0
  private var torque = 0
  private var power = 0L
  private var alpha = 0

  // ----------------------------------------------------------------------- //

  override def updateEntity() {
    if (useRotaryCraftPower) updateEnergy()
    super.updateEntity()
  }

  @Optional.Method(modid = Mods.IDs.RotaryCraft)
  private def updateEnergy() {
    if (world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      tryAllSides((demand, _) => {
        val consumed = demand.toLong min power
        power -= consumed
        consumed
      }, Power.fromWA, Power.toWA)
    }
  }

  // ----------------------------------------------------------------------- //
  // ShaftMachine

  @Optional.Method(modid = Mods.IDs.RotaryCraft)
  def getOmega: Int = omega

  @Optional.Method(modid = Mods.IDs.RotaryCraft)
  def getTorque: Int = torque

  @Optional.Method(modid = Mods.IDs.RotaryCraft)
  def getPower: Long = power

  @Optional.Method(modid = Mods.IDs.RotaryCraft)
  def getName: String = OpenComputers.Name

  @Optional.Method(modid = Mods.IDs.RotaryCraft)
  def getIORenderAlpha: Int = alpha

  @Optional.Method(modid = Mods.IDs.RotaryCraft)
  def setIORenderAlpha(value: Int): Unit = alpha = value

  // ----------------------------------------------------------------------- //
  // ShaftPowerReceiver

  @Optional.Method(modid = Mods.IDs.RotaryCraft)
  def setOmega(value: Int): Unit = omega = value

  @Optional.Method(modid = Mods.IDs.RotaryCraft)
  def setTorque(value: Int): Unit = torque = value

  @Optional.Method(modid = Mods.IDs.RotaryCraft)
  def setPower(value: Long): Unit = power = value

  @Optional.Method(modid = Mods.IDs.RotaryCraft)
  def noInputMachine(): Unit = {
    omega = 0
    torque = 0
    power = 0
  }

  // ----------------------------------------------------------------------- //
  // PowerAcceptor

  @Optional.Method(modid = Mods.IDs.RotaryCraft)
  def canReadFrom(forgeDirection: ForgeDirection): Boolean = true

  @Optional.Method(modid = Mods.IDs.RotaryCraft)
  def isReceiving: Boolean = true

  @Optional.Method(modid = Mods.IDs.RotaryCraft)
  def getMinTorque(available: Int): Int = 0
}
