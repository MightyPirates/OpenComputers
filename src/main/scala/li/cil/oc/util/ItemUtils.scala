package li.cil.oc.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Random

import com.google.common.base.Charsets
import com.google.common.base.Strings
import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api
import li.cil.oc.api.Persistable
import li.cil.oc.common.Tier
import li.cil.oc.integration.opencomputers.DriverScreen
import li.cil.oc.util.ExtendedNBT._
import net.minecraft.item.ItemBucket
import net.minecraft.item.ItemMap
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager
import net.minecraft.item.crafting.IRecipe
import net.minecraft.item.crafting.ShapedRecipes
import net.minecraft.item.crafting.ShapelessRecipes
import net.minecraft.nbt.CompressedStreamTools
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.oredict.ShapedOreRecipe
import net.minecraftforge.oredict.ShapelessOreRecipe

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import scala.io.Source

object ItemUtils {
  def caseTier(stack: ItemStack) = {
    val descriptor = api.Items.get(stack)
    if (descriptor == api.Items.get("case1")) Tier.One
    else if (descriptor == api.Items.get("case2")) Tier.Two
    else if (descriptor == api.Items.get("case3")) Tier.Three
    else if (descriptor == api.Items.get("caseCreative")) Tier.Four
    else if (descriptor == api.Items.get("microcontrollerCase1")) Tier.One
    else if (descriptor == api.Items.get("microcontrollerCase2")) Tier.Two
    else if (descriptor == api.Items.get("droneCase1")) Tier.One
    else if (descriptor == api.Items.get("droneCase2")) Tier.Two
    else if (descriptor == api.Items.get("server1")) Tier.One
    else if (descriptor == api.Items.get("server2")) Tier.Two
    else if (descriptor == api.Items.get("server3")) Tier.Three
    else Tier.None
  }

  def loadStack(nbt: NBTTagCompound) = ItemStack.loadItemStackFromNBT(nbt)

  def loadStack(data: Array[Byte]) = {
    ItemStack.loadItemStackFromNBT(loadTag(data))
  }

  def loadTag(data: Array[Byte]) = {
    val bais = new ByteArrayInputStream(data)
    CompressedStreamTools.readCompressed(bais)
  }

  def saveStack(stack: ItemStack) = {
    val tag = new NBTTagCompound()
    stack.writeToNBT(tag)
    saveTag(tag)
  }

  def saveTag(tag: NBTTagCompound) = {
    val baos = new ByteArrayOutputStream()
    CompressedStreamTools.writeCompressed(tag, baos)
    baos.toByteArray
  }

  def getIngredients(stack: ItemStack): Array[ItemStack] = try {
    val recipes = CraftingManager.getInstance.getRecipeList.map(_.asInstanceOf[IRecipe])
    val recipe = recipes.find(recipe => recipe.getRecipeOutput != null && recipe.getRecipeOutput.isItemEqual(stack))
    val count = recipe.fold(0)(_.getRecipeOutput.stackSize)
    val ingredients = (recipe match {
      case Some(recipe: ShapedRecipes) => recipe.recipeItems.toIterable
      case Some(recipe: ShapelessRecipes) => recipe.recipeItems.map(_.asInstanceOf[ItemStack])
      case Some(recipe: ShapedOreRecipe) => resolveOreDictEntries(recipe.getInput)
      case Some(recipe: ShapelessOreRecipe) => resolveOreDictEntries(recipe.getInput)
      case _ => Iterable.empty
    }).filter(ingredient => ingredient != null &&
      // Strip out buckets, because those are returned when crafting, and
      // we have no way of returning the fluid only (and I can't be arsed
      // to make it output fluids into fluiducts or such, sorry).
      !ingredient.getItem.isInstanceOf[ItemBucket]).toArray
    // Avoid positive feedback loops.
    if (ingredients.exists(ingredient => ingredient.isItemEqual(stack))) {
      return Array.empty
    }
    // Merge equal items for size division by output size.
    val merged = mutable.ArrayBuffer.empty[ItemStack]
    for (ingredient <- ingredients) {
      merged.find(_.isItemEqual(ingredient)) match {
        case Some(entry) => entry.stackSize += ingredient.stackSize
        case _ => merged += ingredient.copy()
      }
    }
    merged.foreach(_.stackSize /= count)
    // Split items up again to 'disassemble them individually'.
    val distinct = mutable.ArrayBuffer.empty[ItemStack]
    for (ingredient <- merged) {
      val size = ingredient.stackSize
      ingredient.stackSize = 1
      for (i <- 0 until size) {
        distinct += ingredient.copy()
      }
    }
    distinct.toArray
  }
  catch {
    case t: Throwable =>
      OpenComputers.log.warn("Whoops, something went wrong when trying to figure out an item's parts.", t)
      Array.empty
  }

  private lazy val rng = new Random()

  private def resolveOreDictEntries[T](entries: Iterable[T]) = entries.collect {
    case stack: ItemStack => stack
    case list: java.util.ArrayList[ItemStack]@unchecked if !list.isEmpty => list.get(rng.nextInt(list.size))
  }

  abstract class ItemData extends Persistable {
    def load(stack: ItemStack) {
      if (stack.hasTagCompound) {
        // Because ItemStack's load function doesn't copy the compound tag,
        // but keeps it as is, leading to oh so fun bugs!
        load(stack.getTagCompound.copy().asInstanceOf[NBTTagCompound])
      }
    }

    def save(stack: ItemStack) {
      if (!stack.hasTagCompound) {
        stack.setTagCompound(new NBTTagCompound())
      }
      save(stack.getTagCompound)
    }
  }

  class MicrocontrollerData extends ItemData {
    def this(stack: ItemStack) {
      this()
      load(stack)
    }

    var tier = Tier.One

    var components = Array.empty[ItemStack]

    var storedEnergy = 0

    override def load(nbt: NBTTagCompound) {
      tier = nbt.getByte(Settings.namespace + "tier")
      components = nbt.getTagList(Settings.namespace + "components", NBT.TAG_COMPOUND).
        toArray[NBTTagCompound].map(loadStack)
      storedEnergy = nbt.getInteger(Settings.namespace + "storedEnergy")
    }

    override def save(nbt: NBTTagCompound) {
      nbt.setByte(Settings.namespace + "tier", tier.toByte)
      nbt.setNewTagList(Settings.namespace + "components", components.toIterable)
      nbt.setInteger(Settings.namespace + "storedEnergy", storedEnergy)
    }

    def createItemStack() = {
      val stack = api.Items.get("microcontroller").createItemStack(1)
      save(stack)
      stack
    }

    def copyItemStack() = {
      val stack = createItemStack()
      // Forget all node addresses and so on. This is used when 'picking' a
      // microcontroller in creative mode.
      val newInfo = new MicrocontrollerData(stack)
      newInfo.components.foreach(cs => Option(api.Driver.driverFor(cs)) match {
        case Some(driver) if driver == DriverScreen =>
          val nbt = driver.dataTag(cs)
          for (tagName <- nbt.getKeySet.toArray) {
            nbt.removeTag(tagName.asInstanceOf[String])
          }
        case _ =>
      })
      newInfo.save(stack)
      stack
    }
  }

  class NavigationUpgradeData extends ItemData {
    def this(stack: ItemStack) {
      this()
      load(stack)
    }

    var map = new ItemStack(net.minecraft.init.Items.filled_map)

    def mapData(world: World) = try map.getItem.asInstanceOf[ItemMap].getMapData(map, world) catch {
      case _: Throwable => throw new Exception("invalid map")
    }

    override def load(stack: ItemStack) {
      if (stack.hasTagCompound) {
        load(stack.getTagCompound.getCompoundTag(Settings.namespace + "data"))
      }
    }

    override def save(stack: ItemStack) {
      if (!stack.hasTagCompound) {
        stack.setTagCompound(new NBTTagCompound())
      }
      save(stack.getCompoundTag(Settings.namespace + "data"))
    }

    override def load(nbt: NBTTagCompound) {
      if (nbt.hasKey(Settings.namespace + "map")) {
        map = loadStack(nbt.getCompoundTag(Settings.namespace + "map"))
      }
    }

    override def save(nbt: NBTTagCompound) {
      if (map != null) {
        nbt.setNewCompoundTag(Settings.namespace + "map", map.writeToNBT)
      }
    }
  }

  class RobotData extends ItemData {
    def this(stack: ItemStack) {
      this()
      load(stack)
    }

    var name = ""

    // Overall energy including components.
    var totalEnergy = 0

    // Energy purely stored in robot component - this is what we have to restore manually.
    var robotEnergy = 0

    var tier = 0

    var components = Array.empty[ItemStack]

    var containers = Array.empty[ItemStack]

    override def load(nbt: NBTTagCompound) {
      if (nbt.hasKey("display") && nbt.getCompoundTag("display").hasKey("Name")) {
        name = nbt.getCompoundTag("display").getString("Name")
      }
      if (Strings.isNullOrEmpty(name)) {
        name = RobotData.randomName
      }
      totalEnergy = nbt.getInteger(Settings.namespace + "storedEnergy")
      robotEnergy = nbt.getInteger(Settings.namespace + "robotEnergy")
      tier = nbt.getInteger(Settings.namespace + "tier")
      components = nbt.getTagList(Settings.namespace + "components", NBT.TAG_COMPOUND).
        toArray[NBTTagCompound].map(loadStack)
      containers = nbt.getTagList(Settings.namespace + "containers", NBT.TAG_COMPOUND).
        toArray[NBTTagCompound].map(loadStack)
    }

    override def save(nbt: NBTTagCompound) {
      if (name != null) {
        if (!nbt.hasKey("display")) {
          nbt.setTag("display", new NBTTagCompound())
        }
        nbt.getCompoundTag("display").setString("Name", name)
      }
      nbt.setInteger(Settings.namespace + "storedEnergy", totalEnergy)
      nbt.setInteger(Settings.namespace + "robotEnergy", robotEnergy)
      nbt.setInteger(Settings.namespace + "tier", tier)
      nbt.setNewTagList(Settings.namespace + "components", components.toIterable)
      nbt.setNewTagList(Settings.namespace + "containers", containers.toIterable)
    }

    def createItemStack() = {
      val stack = api.Items.get("robot").createItemStack(1)
      save(stack)
      stack
    }

    def copyItemStack() = {
      val stack = createItemStack()
      // Forget all node addresses and so on. This is used when 'picking' a
      // robot in creative mode.
      val newInfo = new RobotData(stack)
      newInfo.components.foreach(cs => Option(api.Driver.driverFor(cs)) match {
        case Some(driver) if driver == DriverScreen =>
          val nbt = driver.dataTag(cs)
          for (tagName <- nbt.getKeySet.toArray) {
            nbt.removeTag(tagName.asInstanceOf[String])
          }
        case _ =>
      })
      // Don't show energy info (because it's unreliable) but fill up the
      // internal buffer. This is for creative use only, anyway.
      newInfo.totalEnergy = 0
      newInfo.robotEnergy = 50000
      newInfo.save(stack)
      stack
    }
  }

  object RobotData {
    val names = try {
      Source.fromInputStream(getClass.getResourceAsStream(
        "/assets/" + Settings.resourceDomain + "/robot.names"))(Charsets.UTF_8).
        getLines().map(_.takeWhile(_ != '#').trim()).filter(_ != "").toArray
    }
    catch {
      case t: Throwable =>
        OpenComputers.log.warn("Failed loading robot name list.", t)
        Array.empty[String]
    }

    def randomName = if (names.length > 0) names((math.random * names.length).toInt) else "Robot"
  }

  class TabletData extends ItemData {
    def this(stack: ItemStack) {
      this()
      load(stack)
    }

    var items = Array.fill[Option[ItemStack]](32)(None)
    var isRunning = false
    var energy = 0.0
    var maxEnergy = 0.0

    override def load(nbt: NBTTagCompound) {
      nbt.getTagList(Settings.namespace + "items", NBT.TAG_COMPOUND).foreach((slotNbt: NBTTagCompound) => {
        val slot = slotNbt.getByte("slot")
        if (slot >= 0 && slot < items.length) {
          items(slot) = Option(loadStack(slotNbt.getCompoundTag("item")))
        }
      })
      isRunning = nbt.getBoolean(Settings.namespace + "isRunning")
      energy = nbt.getDouble(Settings.namespace + "energy")
      maxEnergy = nbt.getDouble(Settings.namespace + "maxEnergy")
    }

    override def save(nbt: NBTTagCompound) {
      nbt.setNewTagList(Settings.namespace + "items",
        items.zipWithIndex collect {
          case (Some(stack), slot) => (stack, slot)
        } map {
          case (stack, slot) =>
            val slotNbt = new NBTTagCompound()
            slotNbt.setByte("slot", slot.toByte)
            slotNbt.setNewCompoundTag("item", stack.writeToNBT)
        })
      nbt.setBoolean(Settings.namespace + "isRunning", isRunning)
      nbt.setDouble(Settings.namespace + "energy", energy)
      nbt.setDouble(Settings.namespace + "maxEnergy", maxEnergy)
    }
  }

}
