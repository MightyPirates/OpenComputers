package li.cil.oc.common.tileentity.traits

import cpw.mods.fml.common.Optional
import li.cil.oc.api
import li.cil.oc.common.asm.Injectable
import li.cil.oc.integration.Mods

@Injectable.Interface(value = "buildcraft.api.tiles.IHasWork", modid = Mods.IDs.BuildCraft)
trait StateAware extends api.util.StateAware {
  @Optional.Method(modid = Mods.IDs.BuildCraft)
  def hasWork: Boolean = getCurrentState.contains(api.util.StateAware.State.CanWork) || getCurrentState.contains(api.util.StateAware.State.IsWorking)
}
