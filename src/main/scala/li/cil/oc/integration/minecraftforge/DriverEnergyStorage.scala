package li.cil.oc.integration.minecraftforge

import li.cil.oc.api.Network
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.driver.SidedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.network.Visibility
import li.cil.oc.api.prefab
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.energy.IEnergyStorage

/**
  * @author Vexatos
  */
object DriverEnergyStorage extends SidedBlock {

  override def worksWith(world: World, pos: BlockPos, side: EnumFacing): Boolean = world.getTileEntity(pos) match {
    case tile: TileEntity if tile.hasCapability(CapabilityEnergy.ENERGY, side) => true
    case _ => false
  }

  override def createEnvironment(world: World, pos: BlockPos, side: EnumFacing): ManagedEnvironment = world.getTileEntity(pos) match {
    case tile: TileEntity if tile.hasCapability(CapabilityEnergy.ENERGY, side) => new Environment(tile.getCapability(CapabilityEnergy.ENERGY, side))
    case _ => null
  }

  final class Environment(val storage: IEnergyStorage) extends prefab.ManagedEnvironment with NamedBlock {

    setNode(Network.newNode(this, Visibility.Network).withComponent("energy_device").create)

    @Callback(doc = "function():number -- Returns the amount of stored energy on the connected side.")
    def getEnergyStored(context: Context, args: Arguments): Array[AnyRef] = result(storage.getEnergyStored)

    @Callback(doc = "function():number -- Returns the maximum amount of stored energy on the connected side.")
    def getMaxEnergyStored(context: Context, args: Arguments): Array[AnyRef] = result(storage.getMaxEnergyStored)

    @Callback(doc = "function():number -- Returns whether this component can have energy extracted from the connected side.")
    def canExtract(context: Context, args: Arguments): Array[AnyRef] = result(storage.canExtract)

    @Callback(doc = "function():number -- Returns whether this component can receive energy on the connected side.")
    def canReceive(context: Context, args: Arguments): Array[AnyRef] = result(storage.canReceive)

    override def preferredName(): String = "energy_device"

    override def priority(): Int = 0
  }

}
