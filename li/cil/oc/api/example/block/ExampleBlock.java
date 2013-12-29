package li.cil.oc.api.example.block;


import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class ExampleBlock extends Block {

    public ExampleBlock(int id,Material material){
        super(id,material);
        setTextureName("opencomputers:placeholder");
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, int metadata) {
        return new ExampleTileEntity();
    }
}
