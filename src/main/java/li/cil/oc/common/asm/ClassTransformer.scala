package li.cil.oc.common.asm

import cpw.mods.fml.common.Loader
import cpw.mods.fml.common.versioning.{DefaultArtifactVersion, VersionParser}
import li.cil.oc.OpenComputers
import net.minecraft.launchwrapper.IClassTransformer
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.{ClassWriter, ClassReader}

class ClassTransformer extends IClassTransformer {
  def transform(name: String, transformedName: String, basicClass: Array[Byte]): Array[Byte] = {
    if (name == "li.cil.oc.common.tileentity.Computer" || name == "li.cil.oc.common.tileentity.Rack") {
      if (!Loader.isModLoaded("StargateTech2")) {
        // Handled by @Optional.
        return basicClass
      }

      val mod = Loader.instance.getIndexedModList.get("StargateTech2")
      val have = new DefaultArtifactVersion(mod.getVersion)
      val want = VersionParser.parseRange("[0.6.0,)")

      if (want.containsVersion(have)) {
        // All green, API version is new enough.
        return basicClass
      }

      // Version of SGT2 is too old, abstract bus API doesn't exist.
      OpenComputers.log.warning("Your version of StargateTech2 is out-dated, please upgrade for Abstract Bus support.")

      val classNode = new ClassNode()
      new ClassReader(basicClass).accept(classNode, 0)

      classNode.interfaces.remove("stargatetech2/api/bus/IBusDevice")

      val writer = new ClassWriter(ClassWriter.COMPUTE_MAXS)
      classNode.accept(writer)
      writer.toByteArray
    }
    else basicClass
  }
}
