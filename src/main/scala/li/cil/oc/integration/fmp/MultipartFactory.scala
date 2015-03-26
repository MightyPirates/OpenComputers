package li.cil.oc.integration.fmp

import codechicken.multipart.MultiPartRegistry
import codechicken.multipart.MultiPartRegistry.IPartFactory
import codechicken.multipart.TMultiPart
import li.cil.oc.Settings

object MultipartFactory extends IPartFactory {
  def init() {
    MultiPartRegistry.registerParts(MultipartFactory, Array(
      Settings.namespace + "cable",
      Settings.namespace + "print"
    ))
  }

  override def createPart(name: String, client: Boolean): TMultiPart = {
    if (name.equals(Settings.namespace + "cable"))
      return new CablePart()
    if (name.equals(Settings.namespace + "print"))
      return new PrintPart()
    null
  }
}
