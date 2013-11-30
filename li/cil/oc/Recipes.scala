package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.block.Block
import net.minecraft.item.{Item, ItemStack}
import net.minecraftforge.oredict.OreDictionary

object Recipes {
  def init() {
    val ironStack = new ItemStack(Item.ingotIron)
    val dirt = new ItemStack(Block.dirt)

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

    GameRegistry.addRecipe(Blocks.case1.createItemStack(),
      "xxx",
      "x x",
      "xxx", 'x': Character, ironStack)

    GameRegistry.addRecipe(Blocks.cable.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Blocks.keyboard.createItemStack(),
      "xxx",
      "xan", 'x': Character, Items.buttonGroup.createItemStack(),'a':Character,Items.arrowKeys.createItemStack(),'n':Character,Items.numPad.createItemStack())

    GameRegistry.addRecipe(Blocks.robotProxy.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Items.chip1.createItemStack(),
      "xxx",
      "xyx",
      "xxx", 'x': Character, new ItemStack(Block.fenceIron), 'y': Character, new ItemStack(Item.redstone))

    GameRegistry.addRecipe(Items.chip2.createItemStack(),
      "xxx",
      "xyx",
      "xxx", 'x': Character, Items.chip1.createItemStack(), 'y': Character, new ItemStack(Item.ingotGold))

    GameRegistry.addRecipe(Items.chip3.createItemStack(),
      "xxx",
      "xyx",
      "xxx", 'x': Character, Items.chip2.createItemStack(), 'y': Character, new ItemStack(Item.diamond))

    GameRegistry.addRecipe(Items.analyzer.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Items.card1.createItemStack(),
      "xyy",
      "xzz",
      "x  ", 'x': Character, ironStack, 'z': Character, Items.circuitBoard.createItemStack(), 'y': Character, Items.chip1.createItemStack())

    GameRegistry.addRecipe(Items.card2.createItemStack(),
      "xyy",
      "xzz",
      "x  ", 'x': Character, ironStack, 'z': Character, Items.circuitBoard.createItemStack(), 'y': Character, Items.chip2.createItemStack())

    GameRegistry.addRecipe(Items.card3.createItemStack(),
      "xyy",
      "xzz",
      "x  ", 'x': Character, ironStack, 'z': Character, Items.circuitBoard.createItemStack(), 'y': Character, Items.chip3.createItemStack())

    GameRegistry.addRecipe(Items.disk.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Items.gpu1.createItemStack(),
      "x",
      "y",
      "x", 'x': Character, Items.chip1.createItemStack(), 'y': Character, Items.card1.createItemStack())

    GameRegistry.addRecipe(Items.gpu2.createItemStack(),
      "x",
      "y",
      "x", 'x': Character, Items.chip2.createItemStack(), 'y': Character, Items.card2.createItemStack())

    GameRegistry.addRecipe(Items.gpu3.createItemStack(),
      "x",
      "y",
      "x", 'x': Character, Items.chip3.createItemStack(), 'y': Character, Items.card3.createItemStack())


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

    GameRegistry.addShapelessRecipe(Items.lan.createItemStack(), Items.card1.createItemStack(), Blocks.cable.createItemStack())

    GameRegistry.addRecipe(Items.ram1.createItemStack(),
      "xxx",
      "yyy", 'x': Character, Items.chip1.createItemStack(), 'y': Character, Items.circuitBoard.createItemStack())

    GameRegistry.addRecipe(Items.ram2.createItemStack(),
      "xxx",
      "yyy", 'x': Character, Items.chip2.createItemStack(), 'y': Character, Items.circuitBoard.createItemStack())

    GameRegistry.addRecipe(Items.ram3.createItemStack(),
      "xxx",
      "yyy", 'x': Character, Items.chip3.createItemStack(), 'y': Character, Items.circuitBoard.createItemStack())

    GameRegistry.addShapelessRecipe(Items.rs.createItemStack(), Items.card1.createItemStack(), new ItemStack(Item.redstone, 1))

    GameRegistry.addRecipe(Items.wlan.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)
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

    addRecipe(Items.transistor.createItemStack(),
    " r ",
    "dcd",
    " t ", 'r',new ItemStack(Item.redstoneRepeater),'d',new ItemStack(Item.redstone),'c',new ItemStack(Item.comparator),'t',new ItemStack(Block.torchRedstoneIdle))

    addRecipe(Items.cpu.createItemStack(),
      "brb",
      "rcr",
      "brb", 'b',ironStack,'r',new ItemStack(Item.redstone),'c',new ItemStack(Item.pocketSundial))


    GameRegistry.addShapelessRecipe(Items.ironCutter.createItemStack(16), new ItemStack(Item.shears, 1, OreDictionary.WILDCARD_VALUE), new ItemStack(Item.ingotIron))
    GameRegistry.addShapelessRecipe(Items.circuitBoardBody.createItemStack(), Items.ironCutter.createItemStack(), new ItemStack(Block.hardenedClay))
    GameRegistry.addShapelessRecipe(Items.circuitBoard.createItemStack(), new ItemStack(Item.potion, 1, 8196), Item.goldNugget, Items.circuitBoardBody.createItemStack())
    GameRegistry.addShapelessRecipe(Items.circuitBoard.createItemStack(), new ItemStack(Item.potion, 1, 8228), Item.goldNugget, Items.circuitBoardBody.createItemStack())
    GameRegistry.addShapelessRecipe(Items.circuitBoard.createItemStack(), new ItemStack(Item.potion, 1, 8260), Item.goldNugget, Items.circuitBoardBody.createItemStack())
    GameRegistry.addShapelessRecipe(Items.circuitBoard.createItemStack(), new ItemStack(Item.potion, 1, 16388), Item.goldNugget, Items.circuitBoardBody.createItemStack())
    GameRegistry.addShapelessRecipe(Items.circuitBoard.createItemStack(), new ItemStack(Item.potion, 1, 16420), Item.goldNugget, Items.circuitBoardBody.createItemStack())
    GameRegistry.addShapelessRecipe(Items.circuitBoard.createItemStack(), new ItemStack(Item.potion, 1, 16452), Item.goldNugget, Items.circuitBoardBody.createItemStack())
    GameRegistry.addShapelessRecipe(new ItemStack(Item.potion), Item.bucketWater, Item.glassBottle)

  }

  def addRecipe(output:ItemStack,args:Any*)={
    GameRegistry.addRecipe(output,args.map(_.asInstanceOf[AnyRef]):_*)
  }
}
