package li.cil.oc

import cpw.mods.fml.common.registry.GameRegistry
import net.minecraft.item.{Item, ItemStack}
import li.cil.oc.common.{block,item}
import net.minecraft.block.Block
import net.minecraft.potion.Potion

object Recipes {
  def init(){
    val ironStack = new ItemStack(Item.ingotIron)
    val dirt = new ItemStack(Block.dirt)

    GameRegistry.addRecipe(Blocks.adapter.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Blocks.capacitor.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Blocks.diskDrive.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Blocks.powerDistributor.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Blocks.powerSupply.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Blocks.screen1.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Blocks.screen2.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Blocks.screen3.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Blocks.router.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Blocks.computerCase.itemStack,"xxx","x x","xxx",'x':Character,ironStack)
    GameRegistry.addRecipe(Blocks.cable.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Blocks.keyboard.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Blocks.robotProxy.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Items.analyzer.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Items.card.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Items.disk.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Items.gpu1.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Items.gpu2.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Items.gpu3.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Items.hdd1.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Items.hdd2.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Items.hdd3.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addShapelessRecipe(Items.lan.itemStack,Items.card.itemStack,Blocks.cable.itemStack)
    GameRegistry.addRecipe(Items.ram1.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Items.ram2.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addRecipe(Items.ram3.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addShapelessRecipe(Items.rs.itemStack,Items.card.itemStack,new ItemStack(Item.redstone,1))
    GameRegistry.addRecipe(Items.wlan.itemStack,"x  ","   ","xxx",'x':Character,dirt)
    GameRegistry.addShapelessRecipe(Items.ironCutter.itemStack(16),new ItemStack(Item.shears),new ItemStack(Item.ingotIron))
    GameRegistry.addShapelessRecipe(Items.platineBody.itemStack,Items.ironCutter.itemStack,new ItemStack(Item.clay))
    GameRegistry.addShapelessRecipe(Items.platine.itemStack,new ItemStack(Item.potion,1,8196),Item.goldNugget,Items.platineBody.itemStack)
    GameRegistry.addShapelessRecipe(Items.platine.itemStack,new ItemStack(Item.potion,1,8228),Item.goldNugget,Items.platineBody.itemStack)
    GameRegistry.addShapelessRecipe(Items.platine.itemStack,new ItemStack(Item.potion,1,8260),Item.goldNugget,Items.platineBody.itemStack)
    GameRegistry.addShapelessRecipe(Items.platine.itemStack,new ItemStack(Item.potion,1,16388),Item.goldNugget,Items.platineBody.itemStack)
    GameRegistry.addShapelessRecipe(Items.platine.itemStack,new ItemStack(Item.potion,1,16420),Item.goldNugget,Items.platineBody.itemStack)
    GameRegistry.addShapelessRecipe(Items.platine.itemStack,new ItemStack(Item.potion,1,16452),Item.goldNugget,Items.platineBody.itemStack)


  }
}
