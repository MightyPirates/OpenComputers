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
import net.minecraft.block.material.Material
import net.minecraft.init.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityNote
import net.minecraft.util.BlockPos
import net.minecraft.world.World

object DriverNoteBlock extends DriverTileEntity {
  override def getTileEntityClass: Class[_] = classOf[TileEntityNote]

  override def createEnvironment(world: World, pos: BlockPos): ManagedEnvironment =
    new Environment(world.getTileEntity(pos).asInstanceOf[TileEntityNote])

  final class Environment(tileEntity: TileEntityNote) extends ManagedTileEntityEnvironment[TileEntityNote](tileEntity, "note_block") with NamedBlock {
    override def preferredName = "note_block"

    override def priority = 0

    @Callback(direct = true, doc = "function():number -- Get the currently set pitch on this note block.")
    def getPitch(context: Context, args: Arguments): Array[AnyRef] = {
      result(tileEntity.note + 1)
    }

    @Callback(doc = "function(value:number) -- Set the pitch for this note block. Must be in the interval [1, 25].")
    def setPitch(context: Context, args: Arguments): Array[AnyRef] = {
      setPitch(args.checkInteger(0))
      result(true)
    }

    @Callback(doc = "function([pitch:number]):boolean -- Triggers the note block if possible. Allows setting the pitch for to save a tick.")
    def trigger(context: Context, args: Arguments): Array[AnyRef] = {
      if (args.count > 0 && args.checkAny(0) != null) {
        setPitch(args.checkInteger(0))
      }
      val world = tileEntity.getWorld
      val pos = tileEntity.getPos
      val material = world.getBlockState(pos.add(0, 1, 0)).getBlock.getMaterial
      val canTrigger = material eq Material.air
      tileEntity.triggerNote(world, pos)
      result(canTrigger)
    }

    private def setPitch(value: Int): Unit = {
      if (value < 1 || value > 25) {
        throw new IllegalArgumentException("invalid pitch")
      }
      tileEntity.note = (value - 1).toByte
      tileEntity.markDirty()
    }
  }

  object Provider extends EnvironmentProvider {
    override def getEnvironment(stack: ItemStack): Class[_] = {
      if (stack != null && Block.getBlockFromItem(stack.getItem) == Blocks.noteblock)
        classOf[Environment]
      else null
    }
  }

}
