package li.cil.oc.common.launch

import java.util

import cpw.mods.fml.relauncher.IFMLLoadingPlugin
import li.cil.oc.common.asm.ClassTransformer

class TransformerLoader extends IFMLLoadingPlugin {
  override def getAccessTransformerClass = null

  override def getASMTransformerClass = Array(classOf[ClassTransformer].getName)

  override def getModContainerClass = null

  override def getSetupClass = null

  override def injectData(data: util.Map[String, AnyRef]) {}
}
