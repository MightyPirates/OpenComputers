package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.block.Block
import net.minecraft.item.{Item, ItemStack}
import net.minecraftforge.oredict.{ShapelessOreRecipe, ShapedOreRecipe, OreDictionary}
import net.minecraft.item.crafting.FurnaceRecipes

object Recipes {
  def init() {
    val ironIngot = new ItemStack(Item.ingotIron)
    val ironBars = new ItemStack(Block.fenceIron)
    val dirt = new ItemStack(Block.dirt)
    val lapis = new ItemStack(Item.dyePowder, 1, 4)
    val cactusGreen = new ItemStack(Item.dyePowder, 1, 4)
    val diamond = new ItemStack(Item.diamond)
    val glowstoneDust = new ItemStack(Item.glowstone)
    val redstoneDust = new ItemStack(Item.redstone)
    val redstoneTorch = new ItemStack(Block.torchRedstoneActive)
    val comparator = new ItemStack(Item.comparator)
    val repeater = new ItemStack(Item.redstoneRepeater)
    val emerald = new ItemStack(Item.emerald)
    val goldNugget = new ItemStack(Item.goldNugget)
    val clock = new ItemStack(Item.pocketSundial)

    val chip1 = Items.chip1.createItemStack()
    val chip2 = Items.chip2.createItemStack()
    val chip3 = Items.chip3.createItemStack()
    val transistor = Items.transistor.createItemStack()
    val circuitBoard =Items.circuitBoardBody.createItemStack()
    val rawCircuitBoard =Items.rawCircuitBoard.createItemStack()
    val card = Items.card.createItemStack()
    val cpu = Items.cpu.createItemStack()
    val alu = Items.alu.createItemStack()
    val cu = Items.cu.createItemStack()
    val ironNugget = Items.ironNugget.createItemStack()


    GameRegistry.addRecipe(Blocks.adapter.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Blocks.capacitor.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Blocks.diskDrive.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Blocks.powerDistributor.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Blocks.powerSupply.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Blocks.screen1.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Blocks.screen2.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Blocks.screen3.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Blocks.router.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Blocks.cable.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)


    GameRegistry.addRecipe(Blocks.robotProxy.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)
    GameRegistry.addRecipe(Items.analyzer.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)
    GameRegistry.addRecipe(Items.disk.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Items.hdd1.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Items.hdd2.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Items.hdd3.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Items.wlan.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)





    GameRegistry.addRecipe(Blocks.case1.createItemStack(),
      "xxx",
      "x x",
      "xxx", 'x': Character, ironIngot)
    GameRegistry.addRecipe(Blocks.keyboard.createItemStack(),
      "xxx",
      "xan", 'x': Character, Items.buttonGroup.createItemStack(), 'a': Character, Items.arrowKeys.createItemStack(), 'n': Character, Items.numPad.createItemStack())






    GameRegistry.addRecipe(card,
      "xyy",
      "xzz",
      "x  ", 'x': Character, ironIngot, 'z': Character, Items.circuitBoard.createItemStack(), 'y': Character, Items.chip1.createItemStack())


    GameRegistry.addRecipe(Items.gpu1.createItemStack(),
      "x",
      "y",
      "x", 'x': Character, Items.chip1.createItemStack(), 'y': Character, card)

    GameRegistry.addRecipe(Items.gpu2.createItemStack(),
      "x",
      "y",
      "x", 'x': Character, Items.chip2.createItemStack(), 'y': Character, card)

    GameRegistry.addRecipe(Items.gpu3.createItemStack(),
      "x",
      "y",
      "x", 'x': Character, Items.chip3.createItemStack(), 'y': Character, card)


    GameRegistry.addShapelessRecipe(Items.lan.createItemStack(),card, Blocks.cable.createItemStack())

    GameRegistry.addRecipe(Items.ram1.createItemStack(),
      "xxx",
      "yyy", 'x': Character, Items.chip1.createItemStack(), 'y': Character, Items.circuitBoard.createItemStack())

    GameRegistry.addRecipe(Items.ram2.createItemStack(),
      "xxx",
      "yyy", 'x': Character, Items.chip2.createItemStack(), 'y': Character, Items.circuitBoard.createItemStack())

    GameRegistry.addRecipe(Items.ram3.createItemStack(),
      "xxx",
      "yyy", 'x': Character, Items.chip3.createItemStack(), 'y': Character, Items.circuitBoard.createItemStack())

    GameRegistry.addShapelessRecipe(Items.rs.createItemStack(),card, new ItemStack(Item.redstone, 1))

    GameRegistry.addRecipe(Items.numPad.createItemStack(),
      "xxx",
      "xxx",
      "xxx", 'x': Character, new ItemStack(Block.stoneButton))

    GameRegistry.addRecipe(Items.arrowKeys.createItemStack(),
      " x ",
      "xxx", 'x': Character, new ItemStack(Block.stoneButton))

    GameRegistry.addRecipe(Items.buttonGroup.createItemStack(),
      "xxx",
      "xxx", 'x': Character, new ItemStack(Block.stoneButton))

    addRecipe(transistor,
      "ttt",
      "drd",
      " d ", 'r',repeater , 'd', redstoneDust, 't', redstoneTorch)

    addRecipe(cpu,
      "crc",
      "bub",
      "cac", 'b', ironBars, 'r', redstoneDust, 'c',chip1,'a',alu ,'u',cu)

    addRecipe(alu,
      "rtr",
      "sss",
      "bdb", 'r', repeater, 's', transistor, 't', redstoneTorch,
      'b', ironNugget, 'd',redstoneDust)

    addRecipe(cu,
      "gtg",
      "scs",
      "grg", 'r', redstoneDust, 's', transistor, 't', redstoneTorch,
      'c', clock, 'g', goldNugget)

    addRecipe(chip1,
      "ibi",
      "rtr",
      "ibi", 'i', "nuggetIron", 'b', ironIngot, 'r', redstoneDust, 't', transistor)

    addRecipe(chip2,
      "glg",
      "cdc",
      "glg", 'g', goldNugget, 'l', lapis, 'c', chip1, 'd', diamond)

    addRecipe(chip3,
      "dmd",
      "cec",
      "dmd", 'd', glowstoneDust, 'm', comparator, 'c', chip2, 'e', emerald)





    GameRegistry.addShapelessRecipe(Items.ironCutter.createItemStack(16), new ItemStack(Item.shears, 1, OreDictionary.WILDCARD_VALUE), new ItemStack(Item.ingotIron),new ItemStack(Item.stick))
    GameRegistry.addShapelessRecipe(rawCircuitBoard, Items.ironCutter.createItemStack(), new ItemStack(Block.blockClay),cactusGreen)
  FurnaceRecipes.smelting().addSmelting(rawCircuitBoard.itemID,rawCircuitBoard.getItemDamage,circuitBoard,1)
    GameRegistry.addRecipe(new ShapelessOreRecipe(Items.circuitBoard.createItemStack(), "potionPoison", Item.goldNugget, circuitBoard))
    GameRegistry.addShapelessRecipe(new ItemStack(Item.potion), Item.bucketWater, Item.glassBottle)
    GameRegistry.addRecipe(new ShapelessOreRecipe(Items.ironNugget.createItemStack(9), ironIngot))
  }

  def addRecipe(output: ItemStack, args: Any*) = {
    GameRegistry.addRecipe(new ShapedOreRecipe(output, args.map(_.asInstanceOf[AnyRef]): _*))
  }
}
