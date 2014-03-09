package li.cil.oc.common.asm

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper
import cpw.mods.fml.common.Loader
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions
import java.util.logging.{Level, Logger}
import li.cil.oc.util.mods.StargateTech2
import net.minecraft.launchwrapper.{LaunchClassLoader, IClassTransformer}
import net.minecraft.tileentity.TileEntity
import org.objectweb.asm.tree._
import org.objectweb.asm.{ClassWriter, ClassReader}
import scala.collection.convert.WrapAsScala._
import li.cil.oc.common.asm.template.SimpleComponentImpl

@TransformerExclusions(Array("li.cil.oc.common.asm"))
class ClassTransformer extends IClassTransformer {
  val loader = classOf[ClassTransformer].getClassLoader.asInstanceOf[LaunchClassLoader]

  val log = Logger.getLogger("OpenComputers")

  override def transform(name: String, transformedName: String, basicClass: Array[Byte]): Array[Byte] = try {
    if (name == "li.cil.oc.common.tileentity.Computer" || name == "li.cil.oc.common.tileentity.Rack") {
      return ensureStargateTechCompatibility(basicClass)
    }
    else if (basicClass != null
      && !name.startsWith("""net.minecraft.""")
      && !name.startsWith("""net.minecraftforge.""")
      && !name.startsWith("""li.cil.oc.common.asm.""")
      && !name.startsWith("""li.cil.oc.api.""")) {
      val classNode = newClassNode(basicClass)
      if (classNode.interfaces.contains("li/cil/oc/api/network/SimpleComponent")) {
        try {
          val transformedClass = injectEnvironmentImplementation(classNode, basicClass)
          log.info(s"Successfully injected component logic into class $name.")
          return transformedClass
        } catch {
          case e: Throwable =>
            log.log(Level.WARNING, s"Failed injecting component logic into class $name.", e)
        }
      }
    }
    basicClass
  }
  catch {
    case t: Throwable =>
      log.log(Level.WARNING, "Something went wrong!", t)
      basicClass
  }

  def ensureStargateTechCompatibility(basicClass: Array[Byte]): Array[Byte] = {
    if (!Loader.isModLoaded("StargateTech2")) {
      // Handled by @Optional.
      return basicClass
    }

    if (StargateTech2.isAvailable) {
      // All green, API version is new enough.
      return basicClass
    }

    // Version of SGT2 is too old, abstract bus API doesn't exist.
    val classNode = newClassNode(basicClass)
    classNode.interfaces.remove("stargatetech2/api/bus/IBusDevice")
    writeClass(classNode)
  }

  def injectEnvironmentImplementation(classNode: ClassNode, basicClass: Array[Byte]): Array[Byte] = {
    // TODO find actual implementations, i.e. descend into sub-classes until in a leaf, and transform those?
    if (!isTileEntity(classNode)) {
      throw new InjectionFailedException("Found SimpleComponent on something that isn't a tile entity, ignoring.")
    }

    val template = classNodeFor("li/cil/oc/common/asm/template/SimpleEnvironment")

    log.fine("Injecting methods from Environment interface.")
    def inject(methodName: String, signature: String, required: Boolean = false) {
      def filter(method: MethodNode) = method.name == methodName && method.desc == signature
      if (classNode.methods.exists(filter)) {
        if (required) {
          throw new InjectionFailedException(s"Could not inject method $methodName$signature because it was already present!")
        }
      }
      else template.methods.find(filter) match {
        case Some(method) => classNode.methods.add(method)
        case _ => throw new AssertionError()
      }
    }
    inject("node", "()Lli/cil/oc/api/network/Node;", required = true)
    inject("onConnect", "(Lli/cil/oc/api/network/Node;)V")
    inject("onDisconnect", "(Lli/cil/oc/api/network/Node;)V")
    inject("onMessage", "(Lli/cil/oc/api/network/Message;)V")

    log.fine("Injecting / wrapping overrides for required tile entity methods.")
    def replace(methodName: String, methodNameSrg: String, desc: String) {
      val mapper = FMLDeobfuscatingRemapper.INSTANCE
      def filter(method: MethodNode) = {
        val descDeObf = mapper.mapMethodDesc(method.desc)
        val methodNameDeObf = mapper.mapMethodName(tileEntityName, method.name, method.desc)
        val areSamePlain = method.name + descDeObf == methodName + desc
        val areSameDeObf = methodNameDeObf + descDeObf == methodNameSrg + desc
        areSamePlain || areSameDeObf
      }
      if (classNode.methods.exists(method => method.name == methodName + SimpleComponentImpl.PostFix && mapper.mapMethodDesc(method.desc) == desc)) {
        throw new InjectionFailedException(s"Delegator method name ${methodName + SimpleComponentImpl.PostFix} is already in use.")
      }
      classNode.methods.find(filter) match {
        case Some(method) =>
          log.fine(s"Found original implementation of $methodName, wrapping.")
          method.name = methodName + SimpleComponentImpl.PostFix
        case _ =>
          log.fine(s"No original implementation of $methodName, will inject override.")
          template.methods.find(_.name == methodName + SimpleComponentImpl.PostFix) match {
            case Some(method) => classNode.methods.add(method)
            case _ => throw new AssertionError(s"Couldn't find ${methodName + SimpleComponentImpl.PostFix} in template implementation.")
          }
      }
      template.methods.find(filter) match {
        case Some(method) => classNode.methods.add(method)
        case _ => throw new AssertionError(s"Couldn't find $methodName in template implementation.")
      }
    }
    replace("validate", "func_145829_t", "()V")
    replace("invalidate", "func_145843_s", "()V")
    replace("onChunkUnload", "func_76623_d", "()V")
    replace("readFromNBT", "func_145839_a", "(Lnet/minecraft/nbt/NBTTagCompound;)V")
    replace("writeToNBT", "func_145841_b", "(Lnet/minecraft/nbt/NBTTagCompound;)V")

    log.fine("Injecting interface.")
    classNode.interfaces.add("li/cil/oc/common/asm/template/SimpleComponentImpl")

    writeClass(classNode, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
  }

  val tileEntityName = FMLDeobfuscatingRemapper.INSTANCE.map("net.minecraft.tileentity.TileEntity").replace('.', '/')

  def isTileEntity(classNode: ClassNode): Boolean = {
    classNode != null && classNode.name != "java/lang/Object" &&
      (classNode.name == tileEntityName || classNode.superName == tileEntityName ||
        isTileEntity(classNodeFor(classNode.superName)))
  }

  def classNodeFor(name: String) = newClassNode(loader.getClassBytes(name.replace('/', '.')))

  def newClassNode(data: Array[Byte]) = {
    val classNode = new ClassNode()
    new ClassReader(data).accept(classNode, 0)
    classNode
  }

  def writeClass(classNode: ClassNode, flags: Int = ClassWriter.COMPUTE_MAXS) = {
    val writer = new ClassWriter(flags)
    classNode.accept(writer)
    writer.toByteArray
  }

  class InjectionFailedException(message: String) extends Exception(message)

}
