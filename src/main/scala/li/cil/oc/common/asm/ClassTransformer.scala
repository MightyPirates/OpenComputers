package li.cil.oc.common.asm

import cpw.mods.fml.common.Loader
import li.cil.oc.util.mods.StargateTech2
import net.minecraft.launchwrapper.IClassTransformer
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.{ClassWriter, ClassReader}

class ClassTransformer extends IClassTransformer {
  override def transform(name: String, transformedName: String, basicClass: Array[Byte]): Array[Byte] = {
    if (name == "li.cil.oc.common.tileentity.Computer" || name == "li.cil.oc.common.tileentity.Rack") {
      if (!Loader.isModLoaded("StargateTech2")) {
        // Handled by @Optional.
        return basicClass
      }

      if (StargateTech2.isAvailable) {
        // All green, API version is new enough.
        return basicClass
      }

      // Version of SGT2 is too old, abstract bus API doesn't exist.
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
