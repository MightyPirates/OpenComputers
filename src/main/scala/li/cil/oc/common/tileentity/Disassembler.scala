package li.cil.oc.common.tileentity

import cpw.mods.fml.relauncher.{Side, SideOnly}
import li.cil.oc.api.network.Visibility
import li.cil.oc.common.Tier
import li.cil.oc.common.inventory.ServerInventory
import li.cil.oc.server.{PacketSender => ServerPacketSender}
import li.cil.oc.util.ExtendedNBT._
import li.cil.oc.util.{InventoryUtils, ItemUtils}
import li.cil.oc.{OpenComputers, Settings, api}
import net.minecraft.item.crafting.{CraftingManager, IRecipe, ShapedRecipes, ShapelessRecipes}
import net.minecraft.item.{ItemBucket, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraftforge.common.util.Constants.NBT
import net.minecraftforge.common.util.ForgeDirection
import net.minecraftforge.oredict.{ShapedOreRecipe, ShapelessOreRecipe}

import scala.collection.convert.WrapAsScala._
import scala.collection.mutable

class Disassembler extends traits.Environment with traits.PowerAcceptor with traits.Inventory {
  val node = api.Network.newNode(this, Visibility.None).
    withConnector(Settings.get.bufferConverter).
    create()

  var isActive = false

  val queue = mutable.ArrayBuffer.empty[ItemStack]

  var totalRequiredEnergy = 0.0

  var buffer = 0.0

  def progress = if (queue.isEmpty) 0 else (1 - (queue.size * Settings.get.disassemblerItemCost - buffer) / totalRequiredEnergy) * 100

  private def setActive(value: Boolean) = if (value != isActive) {
    isActive = value
    ServerPacketSender.sendDisassemblerActive(this, isActive)
  }

  // ----------------------------------------------------------------------- //

  @SideOnly(Side.CLIENT)
  override protected def hasConnector(side: ForgeDirection) = side != ForgeDirection.UP

  override protected def connector(side: ForgeDirection) = Option(if (side != ForgeDirection.UP) node else null)

  // ----------------------------------------------------------------------- //

  override def canUpdate = isServer

  override def updateEntity() {
    super.updateEntity()
    if (world.getTotalWorldTime % Settings.get.tickFrequency == 0) {
      if (queue.isEmpty) {
        disassemble(decrStackSize(0, 1))
        setActive(queue.nonEmpty)
      }
      else {
        if (buffer < Settings.get.disassemblerItemCost) {
          val want = Settings.get.disassemblerTickAmount
          val success = node.tryChangeBuffer(-want)
          setActive(success) // If energy is insufficient indicate it visually.
          if (success) {
            buffer += want
          }
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
    if (stack != null && isItemValidForSlot(0, stack)) {
      if (api.Items.get(stack) == api.Items.get("robot")) enqueueRobot(stack)
      else if (api.Items.get(stack) == api.Items.get("server1")) enqueueServer(stack, 0)
      else if (api.Items.get(stack) == api.Items.get("server2")) enqueueServer(stack, 1)
      else if (api.Items.get(stack) == api.Items.get("server3")) enqueueServer(stack, 2)
      else if (api.Items.get(stack) == api.Items.get("navigationUpgrade")) {
        enqueueNavigationUpgrade(stack)
      }
      else queue ++= getIngredients(stack)
      totalRequiredEnergy = queue.size * Settings.get.disassemblerItemCost
    }
  }

  private def enqueueRobot(robot: ItemStack) {
    val info = new ItemUtils.RobotData(robot)
    val itemName =
      if (info.tier == Tier.Four) "caseCreative"
      else "case" + (info.tier + 1)
    queue += api.Items.get(itemName).createItemStack(1)
    queue ++= info.containers
    queue ++= info.components
    node.changeBuffer(info.robotEnergy)
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
      case part if part.getItem == net.minecraft.init.Items.filled_map => info.map
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
      OpenComputers.log.warn("Whoops, something went wrong when trying to figure out an item's parts.", t)
      Iterable.empty
  }

  private def resolveOreDictEntries[T](entries: Iterable[T]) = entries.collect {
    case stack: ItemStack => stack
    case list: java.util.ArrayList[ItemStack]@unchecked if !list.isEmpty => list.get(world.rand.nextInt(list.size))
  }

  private def drop(stack: ItemStack) {
    if (stack != null) {
      for (side <- ForgeDirection.VALID_DIRECTIONS if stack.stackSize > 0) {
        InventoryUtils.insertIntoInventoryAt(stack, world, x + side.offsetX, y + side.offsetY, z + side.offsetZ, side.getOpposite)
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
    queue ++= nbt.getTagList(Settings.namespace + "queue", NBT.TAG_COMPOUND).map((list, index) => {
      ItemStack.loadItemStackFromNBT(list.getCompoundTagAt(index))
    })
    buffer = nbt.getDouble(Settings.namespace + "buffer")
    totalRequiredEnergy = nbt.getDouble(Settings.namespace + "total")
    isActive = queue.nonEmpty
  }

  override def writeToNBT(nbt: NBTTagCompound) {
    super.writeToNBT(nbt)
    nbt.setNewTagList(Settings.namespace + "queue", queue)
    nbt.setDouble(Settings.namespace + "buffer", buffer)
    nbt.setDouble(Settings.namespace + "total", totalRequiredEnergy)
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

  override def isItemValidForSlot(i: Int, stack: ItemStack) =
    api.Items.get(stack) == api.Items.get("robot") ||
      ((Settings.get.disassembleAllTheThings || api.Items.get(stack) != null) && !getIngredients(stack).isEmpty)
}
