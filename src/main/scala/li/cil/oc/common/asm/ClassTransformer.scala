package li.cil.oc.common.asm

import li.cil.oc.common.asm.template.SimpleComponentImpl
import li.cil.oc.integration.Mods
import net.minecraft.launchwrapper.IClassTransformer
import net.minecraft.launchwrapper.LaunchClassLoader
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper
import org.apache.logging.log4j.LogManager
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree._

import scala.annotation.tailrec
import scala.collection.convert.WrapAsJava._
import scala.collection.convert.WrapAsScala._

object ObfNames {
  final val Class_EntityHanging = Array("net/minecraft/entity/EntityHanging")
  final val Class_EntityLiving = Array("net/minecraft/entity/EntityLiving")
  final val Class_RenderLiving = Array("net/minecraft/client/renderer/entity/RenderLiving")
  final val Class_TileEntity = Array("net/minecraft/tileentity/TileEntity")
  final val Field_leashNBTTag = Array("leashNBTTag", "field_110170_bx")
  final val Field_leashedToEntity = Array("leashedToEntity", "field_110168_bw")
  final val Method_recreateLeash = Array("recreateLeash", "func_110165_bF")
  final val Method_recreateLeashDesc = Array("()V")
  final val Method_renderLeash = Array("renderLeash", "func_110827_b")
  final val Method_renderLeashDesc = Array("(Lnet/minecraft/entity/EntityLiving;DDDFF)V")
  final val Method_validate = Array("validate", "func_145829_t")
  final val Method_invalidate = Array("invalidate", "func_145843_s")
  final val Method_onChunkUnload = Array("onChunkUnload", "func_76623_d")
  final val Method_readFromNBT = Array("readFromNBT", "func_145839_a")
  final val Method_writeToNBT = Array("writeToNBT", "func_189515_b")
}

object ClassTransformer {
  var hadErrors = false
  var hadSimpleComponentErrors = false
}

class ClassTransformer extends IClassTransformer {
  private val loader = classOf[ClassTransformer].getClassLoader.asInstanceOf[LaunchClassLoader]
  private val log = LogManager.getLogger("OpenComputers")

  override def transform(obfName: String, name: String, basicClass: Array[Byte]): Array[Byte] = {
    if (basicClass == null || name.startsWith("scala.")) return basicClass
    var transformedClass = basicClass
    try {
      if (!name.startsWith("net.minecraft.")
        && !name.startsWith("net.minecraftforge.")
        && !name.startsWith("li.cil.oc.common.asm.")
        && !name.startsWith("li.cil.oc.integration.")) {
        if (name.startsWith("li.cil.oc.")) {
          // Strip foreign interfaces from scala generated classes. This is
          // primarily intended to clean up mix-ins / synthetic classes
          // generated by Scala.
          val classNode = newClassNode(transformedClass)
          val missingInterfaces = classNode.interfaces.filter(!classExists(_))
          for (interfaceName <- missingInterfaces) {
            log.trace(s"Stripping interface $interfaceName from class $name because it is missing.")
          }
          classNode.interfaces.removeAll(missingInterfaces)

          val missingClasses = classNode.innerClasses.filter(clazz => clazz.outerName != null && !classExists(clazz.outerName))
          for (innerClass <- missingClasses) {
            log.trace(s"Stripping inner class ${innerClass.name} from class $name because its type ${innerClass.outerName} is missing.")
          }
          classNode.innerClasses.removeAll(missingClasses)

          val incompleteMethods = classNode.methods.filter(method => missingFromSignature(method.desc).nonEmpty)
          for (method <- incompleteMethods) {
            val missing = missingFromSignature(method.desc).mkString(", ")
            log.trace(s"Stripping method ${method.name} from class $name because the following types in its signature are missing: $missing")
          }
          classNode.methods.removeAll(incompleteMethods)

          // Inject available interfaces where requested.
          if (classNode.visibleAnnotations != null) {
            def injectInterface(annotation: AnnotationNode): Unit = {
              val values = annotation.values.grouped(2).map(buffer => buffer.head -> buffer.last).toMap
              (values.get("value"), values.get("modid")) match {
                case (Some(interfaceName: String), Some(modid: String)) =>
                  Mods.All.find(_.id == modid) match {
                    case Some(mod) =>
                      if (mod.isModAvailable) {
                        val interfaceDesc = interfaceName.replaceAllLiterally(".", "/")
                        val node = classNodeFor(interfaceDesc)
                        if (node == null) {
                          log.warn(s"Interface $interfaceName not found, skipping injection.")
                        }
                        else {
                          val missing = node.methods.filterNot(im => classNode.methods.exists(cm => im.name == cm.name && im.desc == cm.desc)).map(method => s"Missing implementation of ${method.name + method.desc}")
                          if (missing.isEmpty) {
                            log.info(s"Injecting interface $interfaceName into $name.")
                            classNode.interfaces.add(interfaceDesc)
                          }
                          else {
                            log.warn(s"Missing implementations for interface $interfaceName, skipping injection.")
                            missing.foreach(log.warn)
                            ClassTransformer.hadErrors = true
                          }
                        }
                      }
                      else {
                        log.info(s"Skipping interface $interfaceName from missing mod $modid.")
                      }
                    case _ =>
                      log.warn(s"Skipping interface $interfaceName from unknown mod $modid.")
                      ClassTransformer.hadErrors = true
                  }
                case _ =>
              }
            }
            classNode.visibleAnnotations.find(_.desc == "Lli/cil/oc/common/asm/Injectable$Interface;") match {
              case Some(annotation) =>
                injectInterface(annotation)
              case _ =>
            }
            classNode.visibleAnnotations.find(_.desc == "Lli/cil/oc/common/asm/Injectable$InterfaceList;") match {
              case Some(annotation) =>
                val values = annotation.values.grouped(2).map(buffer => buffer.head -> buffer.last).toMap
                values.get("value") match {
                  case Some(interfaceList: java.lang.Iterable[AnnotationNode]@unchecked) =>
                    interfaceList.foreach(injectInterface)
                  case _ =>
                }
              case _ =>
            }
          }

          transformedClass = writeClass(classNode)
        }
        {
          val classNode = newClassNode(transformedClass)
          if (classNode.interfaces.contains("li/cil/oc/api/network/SimpleComponent") &&
            (classNode.visibleAnnotations == null || !classNode.visibleAnnotations.
              exists(annotation => annotation != null && annotation.desc == "Lli/cil/oc/api/network/SimpleComponent$SkipInjection;"))) {
            try {
              transformedClass = injectEnvironmentImplementation(classNode)
              log.info(s"Successfully injected component logic into class $name.")
            }
            catch {
              case e: Throwable =>
                log.warn(s"Failed injecting component logic into class $name.", e)
                ClassTransformer.hadSimpleComponentErrors = true
            }
          }
        }
      }

      // Inject some code into the EntityLiving classes recreateLeash method to allow
      // proper loading of leashes tied to entities using the leash upgrade. This is
      // necessary because entities only save the entity they are leashed to if that
      // entity is an EntityLivingBase - which drones, for example, are not, for good
      // reason. We work around this by re-leashing them in the load method of the
      // leash upgrade. The leashed entity would then still unleash itself and, more
      // problematically drop a leash item. To avoid this, we extend the
      //    if (this.isLeashed && this.field_110170_bx != null)
      // check to read
      //    if (this.isLeashed && this.field_110170_bx != null && this.leashedToEntity == null)
      // which should not interfere with any existing logic, but avoid leashing
      // restored manually in the load phase to not be broken again.
      if (ObfNames.Class_EntityLiving.contains(name.replace('.', '/'))) {
        val classNode = newClassNode(transformedClass)
        insertInto(classNode, ObfNames.Method_recreateLeash, ObfNames.Method_recreateLeashDesc, instructions => instructions.toArray.sliding(3, 1).exists {
          case Array(varNode: VarInsnNode, fieldNode: FieldInsnNode, jumpNode: JumpInsnNode)
            if varNode.getOpcode == Opcodes.ALOAD && varNode.`var` == 0 &&
              fieldNode.getOpcode == Opcodes.GETFIELD && ObfNames.Field_leashNBTTag.contains(fieldNode.name) &&
              jumpNode.getOpcode == Opcodes.IFNULL =>
            classNode.fields.find(field => ObfNames.Field_leashedToEntity.contains(field.name)) match {
              case Some(field) =>
                val toInject = new InsnList()
                toInject.add(new VarInsnNode(Opcodes.ALOAD, 0))
                toInject.add(new FieldInsnNode(Opcodes.GETFIELD, classNode.name, field.name, field.desc))
                toInject.add(new JumpInsnNode(Opcodes.IFNONNULL, jumpNode.label))
                instructions.insert(jumpNode, toInject)
                true
              case _ =>
                false
            }
          case _ =>
            false
        }) match {
          case Some(data) => transformedClass = data
          case _ =>
        }
      }

      // Little change to the renderer used to render leashes to center it on drones.
      // This injects the code
      //   if (entity instanceof Drone) {
      //     d5 = 0.0;
      //     d6 = 0.0;
      //     d7 = -0.25;
      //   }
      // before the `instanceof EntityHanging` check in func_110827_b.
      if (ObfNames.Class_RenderLiving.contains(name.replace('.', '/'))) {
        val classNode = newClassNode(transformedClass)
        insertInto(classNode, ObfNames.Method_renderLeash, ObfNames.Method_renderLeashDesc, instructions => instructions.toArray.sliding(3, 1).exists {
          case Array(varNode: VarInsnNode, typeNode: TypeInsnNode, jumpNode: JumpInsnNode)
            if varNode.getOpcode == Opcodes.ALOAD && varNode.`var` == 10 &&
              typeNode.getOpcode == Opcodes.INSTANCEOF && ObfNames.Class_EntityHanging.contains(typeNode.desc) &&
              jumpNode.getOpcode == Opcodes.IFEQ =>
            val toInject = new InsnList()
            toInject.add(new VarInsnNode(Opcodes.ALOAD, 10))
            toInject.add(new TypeInsnNode(Opcodes.INSTANCEOF, "li/cil/oc/common/entity/Drone"))
            val skip = new LabelNode()
            toInject.add(new JumpInsnNode(Opcodes.IFEQ, skip))
            toInject.add(new LdcInsnNode(Double.box(0.0)))
            toInject.add(new VarInsnNode(Opcodes.DSTORE, 17))
            toInject.add(new LdcInsnNode(Double.box(0.0)))
            toInject.add(new VarInsnNode(Opcodes.DSTORE, 19))
            toInject.add(new LdcInsnNode(Double.box(-0.25)))
            toInject.add(new VarInsnNode(Opcodes.DSTORE, 21))
            toInject.add(skip)
            instructions.insertBefore(varNode, toInject)
            true
          case _ =>
            false
        }) match {
          case Some(data) => transformedClass = data
          case _ =>
        }
      }

      transformedClass
    }
    catch {
      case t: Throwable =>
        log.warn("Something went wrong!", t)
        ClassTransformer.hadErrors = true
        basicClass
    }
  }

  private def insertInto(classNode: ClassNode, methodNames: Array[String], methodDescs: Array[String], inserter: (InsnList) => Boolean): Option[Array[Byte]] = {
    classNode.methods.find(method => methodNames.contains(method.name) && methodDescs.contains(method.desc)) match {
      case Some(methodNode) =>
        if (inserter(methodNode.instructions)) {
          log.info(s"Successfully patched ${classNode.name}.${methodNames(0)}.")
          Option(writeClass(classNode, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES))
        }
        else {
          log.warn(s"Failed patching ${classNode.name}.${methodNames(0)}, injection point not found.")
          ClassTransformer.hadErrors = true
          None
        }
      case _ =>
        log.warn(s"Failed patching ${classNode.name}.${methodNames(0)}, method not found.")
        ClassTransformer.hadErrors = true
        None
    }
  }

  private def classExists(name: String) = {
    loader.getClassBytes(name) != null ||
      loader.getClassBytes(FMLDeobfuscatingRemapper.INSTANCE.unmap(name)) != null ||
      (try loader.findClass(name.replace('/', '.')) != null catch {
        case _: ClassNotFoundException => false
      })
  }

  private def missingFromSignature(desc: String) = {
    """L([^;]+);""".r.findAllMatchIn(desc).map(_.group(1)).filter(!classExists(_))
  }

  def injectEnvironmentImplementation(classNode: ClassNode): Array[Byte] = {
    log.trace(s"Injecting methods from Environment interface into ${classNode.name}.")
    if (!isTileEntity(classNode)) {
      throw new InjectionFailedException("Found SimpleComponent on something that isn't a tile entity, ignoring.")
    }

    val template = classNodeFor("li/cil/oc/common/asm/template/SimpleEnvironment")
    if (template == null) {
      throw new InjectionFailedException("Could not find SimpleComponent template!")
    }

    def inject(methodName: String, signature: String, required: Boolean = false) {
      def filter(method: MethodNode) = method.name == methodName && method.desc == signature
      if (classNode.methods.exists(filter)) {
        if (required) {
          throw new InjectionFailedException(s"Could not inject method '$methodName$signature' because it was already present!")
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

    log.trace("Injecting / wrapping overrides for required tile entity methods.")
    def replace(methodName: String, methodNameSrg: String, desc: String) {
      val mapper = FMLDeobfuscatingRemapper.INSTANCE
      def filter(method: MethodNode) = {
        val descDeObf = mapper.mapMethodDesc(method.desc)
        val methodNameDeObf = mapper.mapMethodName(mapper.unmap(ObfNames.Class_TileEntity(0)), method.name, method.desc)
        val areSamePlain = method.name + descDeObf == methodName + desc
        val areSameDeObf = methodNameDeObf + descDeObf == methodNameSrg + desc
        areSamePlain || areSameDeObf
      }
      if (classNode.methods.exists(method => method.name == methodName + SimpleComponentImpl.PostFix && mapper.mapMethodDesc(method.desc) == desc)) {
        throw new InjectionFailedException(s"Delegator method name '${methodName + SimpleComponentImpl.PostFix}' is already in use.")
      }
      classNode.methods.find(filter) match {
        case Some(method) =>
          log.trace(s"Found original implementation of '$methodName', wrapping.")
          method.name = methodName + SimpleComponentImpl.PostFix
        case _ =>
          log.trace(s"No original implementation of '$methodName', will inject override.")
          @tailrec def ensureNonFinalIn(name: String) {
            if (name != null) {
              val node = classNodeFor(name)
              if (node != null) {
                node.methods.find(filter) match {
                  case Some(method) =>
                    if ((method.access & Opcodes.ACC_FINAL) != 0) {
                      throw new InjectionFailedException(s"Method '$methodName' is final in superclass ${node.name.replace('/', '.')}.")
                    }
                  case _ =>
                }
                ensureNonFinalIn(node.superName)
              }
            }
          }
          ensureNonFinalIn(classNode.superName)
          template.methods.find(_.name == methodName + SimpleComponentImpl.PostFix) match {
            case Some(method) => classNode.methods.add(method)
            case _ => throw new AssertionError(s"Couldn't find '${methodName + SimpleComponentImpl.PostFix}' in template implementation.")
          }
      }
      template.methods.find(filter) match {
        case Some(method) => classNode.methods.add(method)
        case _ => throw new AssertionError(s"Couldn't find '$methodName' in template implementation.")
      }
    }
    replace(ObfNames.Method_validate(0), ObfNames.Method_validate(1), "()V")
    replace(ObfNames.Method_invalidate(0), ObfNames.Method_invalidate(1), "()V")
    replace(ObfNames.Method_onChunkUnload(0), ObfNames.Method_onChunkUnload(1), "()V")
    replace(ObfNames.Method_readFromNBT(0), ObfNames.Method_readFromNBT(1), "(Lnet/minecraft/nbt/NBTTagCompound;)V")
    replace(ObfNames.Method_writeToNBT(0), ObfNames.Method_writeToNBT(1), "(Lnet/minecraft/nbt/NBTTagCompound;)Lnet/minecraft/nbt/NBTTagCompound;")

    log.trace("Injecting interface.")
    classNode.interfaces.add("li/cil/oc/common/asm/template/SimpleComponentImpl")

    writeClass(classNode, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES)
  }

  @tailrec final def isTileEntity(classNode: ClassNode): Boolean = {
    if (classNode == null) false
    else {
      log.trace(s"Checking if class ${classNode.name} is a TileEntity...")
      ObfNames.Class_TileEntity.contains(FMLDeobfuscatingRemapper.INSTANCE.map(classNode.name)) ||
        (classNode.superName != null && isTileEntity(classNodeFor(classNode.superName)))
    }
  }

  @tailrec final def isAssignable(parent: ClassNode, child: ClassNode): Boolean = parent != null && child != null && !isFinal(parent) && {
    parent.name == "java/lang/Object" ||
      parent.name == child.name ||
      parent.name == child.superName ||
      child.interfaces.contains(parent.name) ||
      (child.superName != null && isAssignable(parent, classNodeFor(child.superName)))
  }

  def isFinal(node: ClassNode): Boolean = (node.access & Opcodes.ACC_FINAL) != 0

  def isInterface(node: ClassNode): Boolean = node != null && (node.access & Opcodes.ACC_INTERFACE) != 0

  def classNodeFor(name: String) = {
    val namePlain = name.replace('/', '.')
    val bytes = loader.getClassBytes(namePlain)
    if (bytes != null) newClassNode(bytes)
    else {
      val nameObfed = FMLDeobfuscatingRemapper.INSTANCE.unmap(name).replace('/', '.')
      val bytes = loader.getClassBytes(nameObfed)
      if (bytes == null) null
      else newClassNode(bytes)
    }
  }

  def newClassNode(data: Array[Byte]) = {
    val classNode = new ClassNode()
    new ClassReader(data).accept(classNode, 0)
    classNode
  }

  def writeClass(classNode: ClassNode, flags: Int = ClassWriter.COMPUTE_MAXS) = {
    val writer = new ClassWriter(flags) {
      // Implementation without class loads, avoids https://github.com/MinecraftForge/FML/issues/655
      override def getCommonSuperClass(type1: String, type2: String): String = {
        val node1 = classNodeFor(type1)
        val node2 = classNodeFor(type2)
        if (isAssignable(node1, node2)) node1.name
        else if (isAssignable(node2, node1)) node2.name
        else if (isInterface(node1) || isInterface(node2)) "java/lang/Object"
        else {
          var parent = Option(node1).map(_.superName).map(classNodeFor).orNull
          while (parent != null && parent.superName != null && !isAssignable(parent, node2)) {
            parent = classNodeFor(parent.superName)
          }
          if (parent == null) "java/lang/Object" else parent.name
        }
      }
    }
    classNode.accept(writer)
    writer.toByteArray
  }

  class InjectionFailedException(message: String) extends Exception(message)

}
