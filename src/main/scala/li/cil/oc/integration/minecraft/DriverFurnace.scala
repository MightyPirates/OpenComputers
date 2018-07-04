package li.cil.oc.integration.minecraft

import li.cil.oc.api.driver.EnvironmentProvider
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.network.ManagedEnvironment
import li.cil.oc.api.prefab.DriverSidedTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ResultWrapper.result
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityFurnace
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object DriverFurnace extends DriverSidedTileEntity {
  override def getTileEntityClass: Class[_] = classOf[TileEntityFurnace]

  override def createEnvironment(world: World, pos: BlockPos, side: EnumFacing): ManagedEnvironment =
    new Environment(world.getTileEntity(pos).asInstanceOf[TileEntityFurnace])

  final class Environment(tileEntity: TileEntityFurnace) extends ManagedTileEntityEnvironment[TileEntityFurnace](tileEntity, "furnace") with NamedBlock {
    override def preferredName = "furnace"

    override def priority = 0

    @Callback(doc = "function():number -- The number of ticks that the furnace will keep burning from the last consumed fuel.")
    def getBurnTime(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.getField(0))
    }

    @Callback(doc = "function():number -- The number of ticks that the currently burning fuel lasts in total.")
    def getCurrentItemBurnTime(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.getField(1))
    }

    @Callback(doc = "function():number -- The number of ticks that the current item has been cooking for.")
    def getCookTime(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.getField(2))
    }

    @Callback(doc = "function():number -- The number of ticks that the current item needs to cook.")
    def getTotalCookTime(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.getField(3))
    }

    @Callback(doc = "function():boolean -- Get whether the furnace is currently active.")
    def isBurning(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.isBurning)
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (!stack.isEmpty && Block.getBlockFromItem(stack.getItem) == Blocks.FURNACE)
        classOf[Environment]
      else null
    }
  }

}
