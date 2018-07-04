package li.cil.oc.common.launch

import java.util

import li.cil.oc.common.asm.ClassTransformer
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin.{SortingIndex, TransformerExclusions}

@SortingIndex(1001)
@TransformerExclusions(Array("li.cil.oc.common.asm"))
class TransformerLoader extends IFMLLoadingPlugin {
  val instance = this

  override def getModContainerClass = "li.cil.oc.common.launch.CoreModContainer"

  override def getASMTransformerClass = Array(classOf[ClassTransformer].getName)

  override def getAccessTransformerClass = null

  override def getSetupClass = null

  override def injectData(data: util.Map[String, AnyRef]) {}
}
