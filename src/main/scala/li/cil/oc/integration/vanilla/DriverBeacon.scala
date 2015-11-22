package li.cil.oc.integration.vanilla

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.potion.Potion
import net.minecraft.tileentity.TileEntityBeacon
import net.minecraft.world.World

object DriverBeacon extends DriverTileEntity {
  override def getTileEntityClass: Class[_] = classOf[TileEntityBeacon]

  override def createEnvironment(world: World, x: Int, y: Int, z: Int): ManagedEnvironment =
    new Environment(world.getTileEntity(x, y, z).asInstanceOf[TileEntityBeacon])

  final class Environment(tileEntity: TileEntityBeacon) extends ManagedTileEntityEnvironment[TileEntityBeacon](tileEntity, "beacon") with NamedBlock {
    override def preferredName = "beacon"

    override def priority = 0

    @Callback(doc = "function():number -- Get the number of levels for this beacon.")
    def getLevels(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.getLevels)
    }

    @Callback(doc = "function():string -- Get the name of the active primary effect.")
    def getPrimaryEffect(context: Context, args: Arguments): Array[AnyRef] = {
      result(getEffectName(tileEntity.getPrimaryEffect))
    }

    @Callback(doc = "function():string -- Get the name of the active secondary effect.")
    def getSecondaryEffect(context: Context, args: Arguments): Array[AnyRef] = {
      result(getEffectName(tileEntity.getSecondaryEffect))
    }

    private def getEffectName(id: Int): String = {
      if (id >= 0 && id < Potion.potionTypes.length && Potion.potionTypes(id) != null)
        Potion.potionTypes(id).getName
      else null
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (stack != null && Block.getBlockFromItem(stack.getItem) == Blocks.beacon)
        classOf[Environment]
      else null
    }
  }

}
