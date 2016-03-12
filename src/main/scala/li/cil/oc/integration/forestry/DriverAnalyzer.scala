package li.cil.oc.integration.forestry

import forestry.api.genetics.AlleleManager
import forestry.core.tiles.TileAnalyzer
import li.cil.oc.api.driver.NamedBlock
import li.cil.oc.api.machine.Arguments
import li.cil.oc.api.machine.Callback
import li.cil.oc.api.machine.Context
import li.cil.oc.api.prefab.DriverTileEntity
import li.cil.oc.integration.ManagedTileEntityEnvironment
import li.cil.oc.util.ResultWrapper._
import net.minecraft.world.World

class DriverAnalyzer extends DriverTileEntity {
  override def getTileEntityClass = classOf[TileAnalyzer]

  override def createEnvironment(world: World, x: Int, y: Int, z: Int) = new Environment(world.getTileEntity(x, y, z).asInstanceOf[TileAnalyzer])

  final class Environment(tileEntity: TileAnalyzer) extends ManagedTileEntityEnvironment[TileAnalyzer](tileEntity, "forestry_analyzer") with NamedBlock {
    override def preferredName = "forestry_analyzer"

    override def priority = 0

    @Callback(doc = "function():boolean -- Get whether the analyzer can work.")
    def isWorking(context: Context, args: Arguments): Array[AnyRef] = result(tileEntity.hasWork)

    @Callback(doc = "function():boolean -- Get the progress of the current operation.")
    def getProgress(context: Context, args: Arguments): Array[AnyRef] = result(1.0 - tileEntity.getProgressScaled(100) / 100.0)

    @Callback(doc = "function():boolean -- Get info on the currently present bee.")
    def getIndividualOnDisplay(context: Context, args: Arguments): Array[AnyRef] = result(AlleleManager.alleleRegistry.getIndividual(tileEntity.getIndividualOnDisplay))
  }

}
