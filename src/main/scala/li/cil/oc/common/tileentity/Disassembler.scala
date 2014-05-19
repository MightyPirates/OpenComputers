package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{SideOnly, Side}
import li.cil.oc.{OpenComputers, api, Settings}
import li.cil.oc.api.network.Visibility
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.{ItemUtils, InventoryUtils}
import net.minecraft.item.{ItemBucket, Item, ItemStack}
import net.minecraft.item.crafting.{ShapelessRecipes, ShapedRecipes, IRecipe, CraftingManager}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.ForgeDirection
import net.minecraftforge.oredict.{ShapelessOreRecipe, ShapedOreRecipe}
import scala.collection.convert.WrapAsScala._
import scala.collection.mutable
import li.cil.oc.common.inventory.ServerInventory
import java.util.logging.Level

class Disassembler extends traits.Environment with traits.Inventory {
  val node = api.Network.newNode(this, Visibility.None).
    withConnector().
    create()

  var isActive = false

  val queue = mutable.ArrayBuffer.empty[ItemStack]

  var buffer = 0.0

  // ----------------------------------------------------------------------- //

  override def canUpdate = isServer

  override def updateEntity() {
    super.updateEntity()
    if (world.getWorldTime % Settings.get.tickFrequency == 0) {
      if (queue.isEmpty) {
        val stack = decrStackSize(0, 1)
        if (stack != null) {
          disassemble(stack)
          if (!isActive && !queue.isEmpty) {
            isActive = true
            ServerPacketSender.sendDisassemblerActive(this, isActive)
          }
        }
        else if (isActive) {
          isActive = false
          ServerPacketSender.sendDisassemblerActive(this, isActive)
        }
      }
      else {
        if (buffer < Settings.get.disassemblerItemCost) {
          val want = Settings.get.disassemblerTickAmount
          val have = want - node.changeBuffer(-want)
          buffer += have
        }
        if (buffer >= Settings.get.disassemblerItemCost) {
          buffer -= Settings.get.disassemblerItemCost
          val stack = queue.remove(0)
          if (world.rand.nextDouble > Settings.get.disassemblerBreakChance) {
            drop(stack)
          }
        }
      }
    }
  }

  def disassemble(stack: ItemStack) {
    // Validate the item, never trust Minecraft / other Mods on anything!
    if (isItemValidForSlot(0, stack)) {
      if (api.Items.get(stack) == api.Items.get("robot")) enqueueRobot(stack)
      else if (api.Items.get(stack) == api.Items.get("server1")) enqueueServer(stack, 0)
      else if (api.Items.get(stack) == api.Items.get("server2")) enqueueServer(stack, 1)
      else if (api.Items.get(stack) == api.Items.get("server3")) enqueueServer(stack, 2)
      else if (api.Items.get(stack) == api.Items.get("navigationUpgrade")) {
        enqueueNavigationUpgrade(stack)
      }
      else queue ++= getIngredients(stack)
    }
  }

  private def enqueueRobot(robot: ItemStack) {
    val info = new ItemUtils.RobotData(robot)
    queue += api.Items.get("case" + (info.tier + 1)).createItemStack(1)
    queue ++= info.containers
    queue ++= info.components
    node.changeBuffer(info.energy)
  }

  private def enqueueServer(server: ItemStack, serverTier: Int) {
    val info = new ServerInventory {
      override def tier = serverTier

      override def container = server
    }
    for (slot <- 0 until info.getSizeInventory) {
      val stack = info.getStackInSlot(slot)
      drop(stack)
    }
    queue ++= getIngredients(server)
  }

  private def enqueueNavigationUpgrade(stack: ItemStack) {
    val info = new ItemUtils.NavigationUpgradeData(stack)
    val parts = getIngredients(stack)
    queue ++= parts.map {
      case part if part.getItem == Item.map => info.map
      case part => part
    }
  }

  private def getIngredients(stack: ItemStack): Iterable[ItemStack] = try {
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
      return Iterable.empty
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
    distinct
  }
  catch {
    case t: Throwable =>
      OpenComputers.log.log(Level.WARNING, "Whoops, something went wrong when trying to figure out an item's parts.", t)
      Iterable.empty
  }

  private def resolveOreDictEntries[T](entries: Iterable[T]) = entries.collect {
    case stack: ItemStack => stack
    case list: java.util.ArrayList[ItemStack]@unchecked if !list.isEmpty => list.get(world.rand.nextInt(list.size))
  }

  private def drop(stack: ItemStack) {
    if (stack != null) {
      for (side <- ForgeDirection.VALID_DIRECTIONS if stack.stackSize > 0) {
        InventoryUtils.tryDropIntoInventoryAt(stack, world, x + side.offsetX, y + side.offsetY, z + side.offsetZ, side.getOpposite)
      }
      if (stack.stackSize > 0) {
        spawnStackInWorld(stack, ForgeDirection.UP)
      }
    }
  }

  // ----------------------------------------------------------------------- //

  override def readFromNBT(nbt: NBTTagCompound) {
    super.readFromNBT(nbt)
    queue.clear()
    queue ++= nbt.getTagList(Settings.namespace + "queue").map(ItemStack.loadItemStackFromNBT)
    buffer = nbt.getDouble(Settings.namespace + "buffer")
    isActive = !queue.isEmpty
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setNewTagList(Settings.namespace + "queue", queue)
    nbt.setDouble(Settings.namespace + "buffer", buffer)
  }

  @SideOnly(Side.CLIENT)
  override def readFromNBTForClient(nbt: NBTTagCompound) {
    super.readFromNBTForClient(nbt)
    isActive = nbt.getBoolean("isActive")
  }

  override def writeToNBTForClient(nbt: NBTTagCompound) {
    super.writeToNBTForClient(nbt)
    nbt.setBoolean("isActive", isActive)
  }

  // ----------------------------------------------------------------------- //

  override def getSizeInventory = 1

  override def getInventoryStackLimit = 64

  override def getInvName = Settings.namespace + "container.Disassembler"

  override def isItemValidForSlot(i: Int, stack: ItemStack) =
    api.Items.get(stack) == api.Items.get("robot") ||
      ((Settings.get.disassembleAllTheThings || api.Items.get(stack) != null) && !getIngredients(stack).isEmpty)
}
