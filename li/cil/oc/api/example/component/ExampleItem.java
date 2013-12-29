package li.cil.oc.api.example.component;


import net.minecraft.item.Item;

public class ExampleItem extends Item{
    //for convenience reasons
    public static ExampleItem thisItem;
    public ExampleItem(int id){
        super(id);
        thisItem = this;
        setTextureName("opencomputers:placeholder");
    }
    //add all the important methods for your item like images and logic (not lua stuff)

}
