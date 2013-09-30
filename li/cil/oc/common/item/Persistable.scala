package li.cil.oc.common.item

import java.io._
import java.util.logging.Level
import li.cil.oc.api
import li.cil.oc.{OpenComputers, Config}
import net.minecraft.item.ItemStack
import net.minecraft.nbt.{NBTBase, NBTTagCompound, NBTTagString}
import net.minecraftforge.common.DimensionManager
import net.minecraftforge.event.ForgeSubscribe
import net.minecraftforge.event.world.WorldEvent
import scala.collection.mutable
import scala.reflect.runtime.{universe => ru}

object Persistable {
  private val mirror = ru.runtimeMirror(getClass.getClassLoader)

  private val types = mutable.Map.empty[ru.Type, (String, () => api.Persistable)]

  private val cache = mutable.Map.empty[String, api.Persistable]

  private val deleted = mutable.Set.empty[String]

  def add[T <: api.Persistable : ru.TypeTag](name: String, constructor: () => T): Unit =
    types += ru.typeOf[T] ->(name, constructor)

  def get[T <: api.Persistable : ru.TypeTag](stack: ItemStack): Option[T] = {
    val uuid = if (stack.hasTagCompound && stack.getTagCompound.hasKey("component")) {
      stack.getTagCompound.getString("component")
    } else {
      val uuid = java.util.UUID.randomUUID().toString
      stack.setTagInfo("component", new NBTTagString(null, uuid))
      uuid
    }
    Some(cache.getOrElseUpdate(uuid, load(uuid).getOrElse({
      val (_, constructor) = types(ru.typeOf[T])
      constructor()
    })).asInstanceOf[T])
  }

  def delete(stack: ItemStack): Unit = if (stack.hasTagCompound && stack.getTagCompound.hasKey("component")) {
    val uuid = stack.getTagCompound.getString("component")
    cache -= uuid
    deleted += uuid
  }

  private def saveDirectory = {
    val directory = new File(DimensionManager.getCurrentSaveRootDirectory, Config.resourceDomain + "/components")
    if (!directory.exists())
      directory.mkdirs()
    directory
  }

  @ForgeSubscribe
  def onWorldUnload(e: WorldEvent.Unload) = {
    cache.clear()
    deleted.clear()
  }

  @ForgeSubscribe
  def onWorldSave(e: WorldEvent.Save) {
    val directory = saveDirectory
    for ((uuid, instance) <- cache) try {
      val (name, _) = types(mirror.classSymbol(instance.getClass).toType)
      val nbt = new NBTTagCompound(name)
      instance.save(nbt)
      val file = new File(directory, uuid)
      val stream = new DataOutputStream(new FileOutputStream(file))
      NBTBase.writeNamedTag(nbt, stream)
      stream.close()
    } catch {
      case e: Throwable => OpenComputers.log.log(Level.WARNING, "Error saving component.", e)
    }
    for (uuid <- deleted) {
      try {
        new File(saveDirectory, uuid).delete()
      } catch {
        case e: Throwable => OpenComputers.log.log(Level.WARNING, "Error deleting component.", e)
      }
    }
    deleted.clear()
  }

  def load(uuid: String) = {
    val file = new File(saveDirectory, uuid)
    if (file.exists()) try {
      val stream = new DataInputStream(new FileInputStream(file))
      val nbt = NBTBase.readNamedTag(stream).asInstanceOf[NBTTagCompound]
      stream.close()
      val (_, constructor) = types.values.find {
        case (name, _) => name == nbt.getName
      }.get
      val instance = constructor()
      instance.load(nbt)
      Some(instance)
    } catch {
      case e: Throwable => OpenComputers.log.log(Level.WARNING, "Error loading component.", e); None
    } else None
  }
}
