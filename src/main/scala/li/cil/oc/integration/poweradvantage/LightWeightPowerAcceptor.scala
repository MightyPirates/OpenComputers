package li.cil.oc.integration.poweradvantage

import cyano.poweradvantage.api.ConduitType
import cyano.poweradvantage.api.modsupport.ILightWeightPowerAcceptor
import cyano.poweradvantage.api.modsupport.LightWeightPowerRegistry
import li.cil.oc.Settings
import li.cil.oc.common.block
import li.cil.oc.common.tileentity.traits.PowerAcceptor
import net.minecraft.block.Block
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing

import scala.collection.convert.WrapAsScala._

object LightWeightPowerAcceptor extends ILightWeightPowerAcceptor {
  def init(): Unit = {
    for (b: Block <- Block.blockRegistry if b.isInstanceOf[block.traits.PowerAcceptor]) {
      LightWeightPowerRegistry.registerLightWeightPowerAcceptor(b, this)
    }
  }

  def canAcceptEnergyType(powerType: ConduitType) = ConduitType.areSameType(powerType, "electricity")

  def getEnergyDemand(tileEntity: TileEntity, powerType: ConduitType) = tileEntity match {
    case acceptor: PowerAcceptor if canAcceptEnergyType(powerType) =>
      EnumFacing.values().map(side => {
        val capacity = (acceptor.globalBufferSize(side) / Settings.get.ratioRedstoneFlux).toInt
        val stored = (acceptor.globalBuffer(side) / Settings.get.ratioRedstoneFlux).toInt
        capacity - stored
      }).max
    case _ => 0
  }

  def addEnergy(tileEntity: TileEntity, amountAdded: Float, powerType: ConduitType) = tileEntity match {
    case acceptor: PowerAcceptor if canAcceptEnergyType(powerType) =>
      var remainingEnergy = math.min(amountAdded, acceptor.energyThroughput.toFloat)
      // .exists() for early exit.
      EnumFacing.values().exists(side => {
        remainingEnergy -= acceptor.tryChangeBuffer(side, remainingEnergy)
        remainingEnergy <= 0
      })
      amountAdded - remainingEnergy
    case _ => 0
  }
}
