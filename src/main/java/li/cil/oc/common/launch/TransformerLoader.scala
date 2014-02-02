package li.cil.oc.common.launch

import cpw.mods.fml.relauncher.IFMLLoadingPlugin
import java.util
import li.cil.oc.common.asm.ClassTransformer

class TransformerLoader extends IFMLLoadingPlugin {
  def getLibraryRequestClass = null

  def getASMTransformerClass = Array(classOf[ClassTransformer].getName)

  def getModContainerClass = null

  def getSetupClass = null

  def injectData(data: util.Map[String, AnyRef]) {}
}
