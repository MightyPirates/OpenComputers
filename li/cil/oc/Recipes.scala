package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.block.Block
import net.minecraft.item.{Item, ItemStack}

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

    GameRegistry.addRecipe(Items.card.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Items.disk.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Items.gpu1.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Items.gpu2.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Items.gpu3.createItemStack(),
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

    GameRegistry.addShapelessRecipe(Items.lan.createItemStack(), Items.card.createItemStack(), Blocks.cable.createItemStack())

    GameRegistry.addRecipe(Items.ram1.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Items.ram2.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addRecipe(Items.ram3.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addShapelessRecipe(Items.rs.createItemStack(), Items.card.createItemStack(), new ItemStack(Item.redstone, 1))

    GameRegistry.addRecipe(Items.wlan.createItemStack(),
      "x  ",
      "   ",
      "xxx", 'x': Character, dirt)

    GameRegistry.addShapelessRecipe(Items.ironCutter.createItemStack(16), new ItemStack(Item.shears), new ItemStack(Item.ingotIron))
    GameRegistry.addShapelessRecipe(Items.circuitBoardBody.createItemStack(), Items.ironCutter.createItemStack(), new ItemStack(Item.clay))
    GameRegistry.addShapelessRecipe(Items.circuitBoard.createItemStack(), new ItemStack(Item.potion, 1, 8196), Item.goldNugget, Items.circuitBoardBody.createItemStack())
    GameRegistry.addShapelessRecipe(Items.circuitBoard.createItemStack(), new ItemStack(Item.potion, 1, 8228), Item.goldNugget, Items.circuitBoardBody.createItemStack())
    GameRegistry.addShapelessRecipe(Items.circuitBoard.createItemStack(), new ItemStack(Item.potion, 1, 8260), Item.goldNugget, Items.circuitBoardBody.createItemStack())
    GameRegistry.addShapelessRecipe(Items.circuitBoard.createItemStack(), new ItemStack(Item.potion, 1, 16388), Item.goldNugget, Items.circuitBoardBody.createItemStack())
    GameRegistry.addShapelessRecipe(Items.circuitBoard.createItemStack(), new ItemStack(Item.potion, 1, 16420), Item.goldNugget, Items.circuitBoardBody.createItemStack())
    GameRegistry.addShapelessRecipe(Items.circuitBoard.createItemStack(), new ItemStack(Item.potion, 1, 16452), Item.goldNugget, Items.circuitBoardBody.createItemStack())

  }
}
