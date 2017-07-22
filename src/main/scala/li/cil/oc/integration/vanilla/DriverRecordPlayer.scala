package li.cil.oc.integration.vanilla

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
import net.minecraft.block.BlockJukebox
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemRecord
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.util.ForgeDirection

object DriverRecordPlayer extends DriverSidedTileEntity {
  override def getTileEntityClass: Class[_] = classOf[BlockJukebox.TileEntityJukebox]

  override def createEnvironment(world: World, x: Int, y: Int, z: Int, side: ForgeDirection): ManagedEnvironment = new Environment(world.getTileEntity(x, y, z).asInstanceOf[BlockJukebox.TileEntityJukebox])

  final class Environment(tileEntity: BlockJukebox.TileEntityJukebox) extends ManagedTileEntityEnvironment[BlockJukebox.TileEntityJukebox](tileEntity, "jukebox") with NamedBlock {
    override def preferredName = "jukebox"

    override def priority = 0

    @Callback(doc = "function():string -- Get the title of the record currently in the jukebox.")
    def getRecord(context: Context, args: Arguments): Array[AnyRef] = {
      val record = tileEntity.func_145856_a()
      if (record != null && record.getItem.isInstanceOf[ItemRecord]) {
        result(record.getItem.asInstanceOf[ItemRecord].getRecordNameLocal)
      }
      else null
    }

    @Callback(doc = "function() -- Start playing the record currently in the jukebox.")
    def play(context: Context, args: Arguments): Array[AnyRef] = {
      val record = tileEntity.func_145856_a()
      if (record != null && record.getItem.isInstanceOf[ItemRecord]) {
        tileEntity.getWorldObj.playAuxSFXAtEntity(null, 1005, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, Item.getIdFromItem(record.getItem))
        result(true)
      }
      else null
    }

    @Callback(doc = "function() -- Stop playing the record currently in the jukebox.")
    def stop(context: Context, args: Arguments): Array[AnyRef] = {
      tileEntity.getWorldObj.playAuxSFX(1005, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord, 0)
      tileEntity.getWorldObj.playRecord(null, tileEntity.xCoord, tileEntity.yCoord, tileEntity.zCoord)
      null
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (stack != null && Block.getBlockFromItem(stack.getItem) == Blocks.jukebox)
        classOf[Environment]
      else null
    }
  }

}
