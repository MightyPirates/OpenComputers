package li.cil.oc.api

import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import scala.reflect.runtime.{universe => ru}

/**
 * An object that can be persisted to an NBT tag and restored back from it.
 */
trait Persistable {
  /**
   * Restores a previous state of the object from the specified NBT tag.
   *
   * @param nbt the tag to read the state from.
   */
  def load(nbt: NBTTagCompound)

  /**
   * Saves the current state of the object into the specified NBT tag.
   * <p/>
   * This should write the state in such a way that it can be restored when
   * `load` is called with that tag.
   *
   * @param nbt the tag to save the state to.
   */
  def save(nbt: NBTTagCompound)
}

/**
 * Allows linking persistable instances to an item stack.
 * <p/>
 * This is used to track item components. It creates an entry in the item
 * stack's tag compound name "component", with a UUID that is used to link
 * it to the component instance.
 * Each type this is used with has to be registered first. For components, I
 * recommend registering them in the constructor of the item the component
 * belongs to.
 * <p/>
 * It is important to note that since there is no way for the persistent
 * object registry to know when a persistent object met its ultimate doom,
 * entries will live forever, unless they are explicitly `delete`d.
 */
object Persistable {
  /**
   * Registers a new type valid for lookups.
   * <p/>
   * The name has to be unique among all registered types. It is similar to
   * how TileEntities names have to be unique, since this is used to look
   * up the constructor when unpersisting instances.
   *
   * @param name the name of the type
   * @param constructor a function that can be used to create a new instance.
   * @tparam T the type of the object.
   */
  def add[T <: Persistable : ru.TypeTag](name: String, constructor: () => T): Unit =
    instance.foreach(_.add(name, constructor))

  /**
   * Retrieve the object associated to the specified item stack.
   * <p/>
   * If there is no instance yet, one will be created.
   *
   * @param stack the item stack to get the persistent object for.
   * @tparam T the type of the object.
   * @return the persistent object associated with that item stack.
   */
  def get[T <: Persistable : ru.TypeTag](stack: ItemStack): Option[T] =
    instance.fold(None: Option[T])(_.get(stack))

  /**
   * Marks the object associated to the specified item stack for deletion.
   * <p/>
   * Note that the persisted data is only deleted upon the next world save, to
   * avoid discrepancies between the persistent storage and the savegame.
   *
   * @param stack the item stack to delete the persistent object for.
   */
  def delete(stack: ItemStack): Unit =
    instance.foreach(_.delete(stack))

  // ----------------------------------------------------------------------- //

  /** Initialized in pre-init. */
  private[oc] var instance: Option[ {
    def add[T <: Persistable : ru.TypeTag](name: String, constructor: () => T): Unit

    def get[T <: Persistable : ru.TypeTag](stack: ItemStack): Option[T]

    def delete(stack: ItemStack): Unit
  }] = None
}