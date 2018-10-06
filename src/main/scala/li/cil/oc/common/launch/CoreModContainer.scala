package li.cil.oc.common.launch

import com.google.common.eventbus.EventBus
import cpw.mods.fml.common.DummyModContainer
import cpw.mods.fml.common.LoadController
import cpw.mods.fml.common.ModMetadata

class CoreModContainer extends DummyModContainer({
  val md = new ModMetadata()
  md.authorList.add("Sangar")
  md.modId = "OpenComputers|Core"
  md.version = "@VERSION@"
  md.name = "OpenComputers (Core)"
  md.url = "https://oc.cil.li/"
  md.description = "OC core mod used for class transformer and as API owner to avoid cyclic dependencies."
  md
}) {
  override def registerBus(bus: EventBus, controller: LoadController) = true
}
