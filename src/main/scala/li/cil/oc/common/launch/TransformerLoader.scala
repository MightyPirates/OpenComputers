package li.cil.oc.common.launch

import java.util

import com.google.common.eventbus.EventBus
import cpw.mods.fml.common.DummyModContainer
import cpw.mods.fml.common.LoadController
import cpw.mods.fml.common.ModMetadata
import cpw.mods.fml.relauncher.IFMLLoadingPlugin
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.MCVersion
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions
import li.cil.oc.common.asm.ClassTransformer

@TransformerExclusions(Array("li.cil.oc.common.asm"))
@MCVersion("1.7.10")
class TransformerLoader extends DummyModContainer({
  val md = new ModMetadata()
  md.authorList.add("Sangar")
  md.modId = "OpenComputers|Core"
  md.version = "1.0.0"
  md.name = "OpenComputers (Core)"
  md.url = "http://oc.cil.li/"
  md.description = "OC core mod used for class transformer and as API owner to avoid cyclic dependencies."
  md
}) with IFMLLoadingPlugin {
  val instance = this

  override def getModContainerClass = getClass.getName

  override def registerBus(bus: EventBus, controller: LoadController) = true

  override def getASMTransformerClass = Array(classOf[ClassTransformer].getName)

  override def getAccessTransformerClass = null

  override def getSetupClass = null

  override def injectData(data: util.Map[String, AnyRef]) {}
}
