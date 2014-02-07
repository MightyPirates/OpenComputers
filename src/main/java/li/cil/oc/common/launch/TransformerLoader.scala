package li.cil.oc.common.launch

import cpw.mods.fml.relauncher.IFMLLoadingPlugin
import java.util
import li.cil.oc.common.asm.ClassTransformer

class TransformerLoader extends IFMLLoadingPlugin {
  override def getLibraryRequestClass = null

  override def getASMTransformerClass = Array(classOf[ClassTransformer].getName)

  override def getModContainerClass = null

  override def getSetupClass = null

  override def injectData(data: util.Map[String, AnyRef]) {}
}
