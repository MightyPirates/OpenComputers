package li.cil.oc.common.asm

import net.minecraft.launchwrapper.IClassTransformer
import net.minecraft.launchwrapper.LaunchClassLoader
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper
import org.apache.logging.log4j.LogManager
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree._

import scala.annotation.tailrec
import scala.collection.convert.WrapAsScala._

object ObfNames {
  final val Class_EntityHanging = Array("net/minecraft/entity/EntityHanging", "xy")
  final val Class_EntityLiving = Array("net/minecraft/entity/EntityLiving", "sg")
  final val Class_RenderLiving = Array("net/minecraft/client/renderer/entity/RenderLiving", "btb")
  final val Field_leashNBTTag = Array("leashNBTTag", "field_110170_bx", "bG")
  final val Field_leashedToEntity = Array("leashedToEntity", "field_110168_bw", "bF")
  final val Method_recreateLeash = Array("recreateLeash", "func_110165_bF", "cY")
  final val Method_recreateLeashDesc = Array("()V")
  final val Method_renderLeash = Array("renderLeash", "func_110827_b", "b")
  final val Method_renderLeashDesc = Array("(Lsg;DDDFF)V", "(Lnet/minecraft/entity/EntityLiving;DDDFF)V")
}

object ClassTransformer {
  var hadErrors = false
  var hadSimpleComponentErrors = false
}

class ClassTransformer extends IClassTransformer {
  private val loader = classOf[ClassTransformer].getClassLoader.asInstanceOf[LaunchClassLoader]
  private val log = LogManager.getLogger("OpenComputers")

  override def transform(name: String, transformedName: String, basicClass: Array[Byte]): Array[Byte] = {
    if (basicClass == null || name.startsWith("scala.")) return basicClass
    var transformedClass = basicClass
    try {
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

  @tailrec final def isAssignable(parent: ClassNode, child: ClassNode): Boolean = parent != null && child != null && !isFinal(parent) && {
    parent.name == "java/lang/Object" ||
      parent.name == child.name ||
      parent.name == child.superName ||
      child.interfaces.contains(parent.name) ||
      (child.superName != null && isAssignable(parent, classNodeFor(child.superName)))
  }

  def isFinal(node: ClassNode): Boolean = (node.access & Opcodes.ACC_FINAL) != 0

  def isInterface(node: ClassNode): Boolean = node != null && (node.access & Opcodes.ACC_INTERFACE) != 0

  def classNodeFor(name: String): ClassNode = {
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

  def newClassNode(data: Array[Byte]): ClassNode = {
    val classNode = new ClassNode()
    new ClassReader(data).accept(classNode, 0)
    classNode
  }

  def writeClass(classNode: ClassNode, flags: Int = ClassWriter.COMPUTE_MAXS): Array[Byte] = {
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
