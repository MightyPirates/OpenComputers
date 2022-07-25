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
import net.minecraft.block.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.MusicDiscItem
import net.minecraft.tileentity.JukeboxTileEntity
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraft.util.text.LanguageMap
import net.minecraft.world.World

object DriverRecordPlayer extends DriverSidedTileEntity {
  override def getTileEntityClass: Class[_] = classOf[JukeboxTileEntity]

  override def createEnvironment(world: World, pos: BlockPos, side: Direction): ManagedEnvironment =
    new Environment(world.getBlockEntity(pos).asInstanceOf[JukeboxTileEntity])

  final class Environment(tileEntity: JukeboxTileEntity) extends ManagedTileEntityEnvironment[JukeboxTileEntity](tileEntity, "jukebox") with NamedBlock {
    override def preferredName = "jukebox"

    override def priority = 0

    @Callback(doc = "function():string -- Get the title of the record currently in the jukebox.")
    def getRecord(context: Context, args: Arguments): Array[AnyRef] = {
      val record = tileEntity.getRecord
      if (!record.isEmpty && record.getItem.isInstanceOf[MusicDiscItem]) {
        result(LanguageMap.getInstance.getOrDefault(record.getItem.asInstanceOf[MusicDiscItem].getDescriptionId))
      }
      else null
    }

    @Callback(doc = "function() -- Start playing the record currently in the jukebox.")
    def play(context: Context, args: Arguments): Array[AnyRef] = {
      val record = tileEntity.getRecord
      if (!record.isEmpty && record.getItem.isInstanceOf[MusicDiscItem]) {
        tileEntity.getLevel.levelEvent(null, 1010, tileEntity.getBlockPos, Item.getId(record.getItem))
        result(true)
      }
      else null
    }

    @Callback(doc = "function() -- Stop playing the record currently in the jukebox.")
    def stop(context: Context, args: Arguments): Array[AnyRef] = {
      tileEntity.getLevel.levelEvent(1010, tileEntity.getBlockPos, 0)
      null
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (stack.getItem == Blocks.JUKEBOX.asItem)
        classOf[Environment]
      else null
    }
  }

}
