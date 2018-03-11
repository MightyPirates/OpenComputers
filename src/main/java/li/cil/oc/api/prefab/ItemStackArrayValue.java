package li.cil.oc.api.prefab;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.util.HashMap;
import java.util.TreeMap;

public class ItemStackArrayValue extends AbstractValue {

	private ItemStack[] array = null;
	private int iteratorIndex;

	private static final byte TAGLIST_ID = (new NBTTagList()).getId();
	private static final byte COMPOUND_ID = (new NBTTagCompound()).getId();
	private static final String ARRAY_KEY = "Array";
	private static final String INDEX_KEY = "Index";

	private static final HashMap<Object,Object> emptyMap = new HashMap<Object,Object>();

	public ItemStackArrayValue(ItemStack[] arr){
		if (arr != null){
			this.array = new ItemStack[arr.length];
			for (int i=0; i< arr.length; i++){
				this.array[i] = arr[i] != null ? arr[i].copy() : null;
			}
		}
		this.iteratorIndex = 0;
	}

	public ItemStackArrayValue(){
		this(null);
	}

	@Override
	public Object[] call(Context context, Arguments arguments) {
		if (this.array == null)
			return null;
		if (this.iteratorIndex >= this.array.length)
			return null;
		int index = this.iteratorIndex++;
		if (this.array[index] == null)//TODO 1.11 change to ItemStack.EMPTY?
			return new Object[]{ emptyMap };
		return new Object[]{ this.array[index] != null ? this.array[index] : emptyMap };
	}

	@Override
	public Object apply(Context context, Arguments arguments) {
		if (arguments.count() == 0 || this.array == null)
			return null;
		if (arguments.isInteger(0)){//index access
			int luaIndex = arguments.checkInteger(0);
			if (luaIndex > this.array.length || luaIndex < 1){
				return null;
			}
			return this.array[luaIndex-1];
		}
		if (arguments.isString(0)){
			String arg = arguments.checkString(0);
			if (arg.equals("n")){
				return this.array.length;
			}
		}
		return null;
	}

	@Override
	public void load(NBTTagCompound nbt) {
		if (nbt.hasKey(ARRAY_KEY, TAGLIST_ID)){
			NBTTagList tagList = nbt.getTagList(ARRAY_KEY,COMPOUND_ID);
			this.array = new ItemStack[tagList.tagCount()];
			for (int i = 0; i < tagList.tagCount(); ++i){
				NBTTagCompound el = tagList.getCompoundTagAt(i);
				if (el.hasNoTags())
					this.array[i] = null;//TODO 1.11 change to ItemStack.EMPTY?
				else
					this.array[i] = ItemStack.loadItemStackFromNBT(el);
			}
		} else {
			this.array = null;
		}
		this.iteratorIndex = nbt.getInteger(INDEX_KEY);
	}

	@Override
	public void save(NBTTagCompound nbt) {

		NBTTagCompound nullnbt = new NBTTagCompound();

		if (this.array != null) {
			NBTTagList nbttaglist = new NBTTagList();
			for (ItemStack stack : this.array) {
				if (stack != null) {
					NBTTagCompound nbttagcompound = new NBTTagCompound();
					stack.writeToNBT(nbttagcompound);
					nbttaglist.appendTag(nbttagcompound);
				} else {
					nbttaglist.appendTag(nullnbt);
				}
			}

			nbt.setTag(ARRAY_KEY, nbttaglist);
		}

		nbt.setInteger(INDEX_KEY, iteratorIndex);
	}

	@Callback(doc="function():nil -- Reset the iterator index so that the next call will return the first element.")
	public Object[] reset(Context context, Arguments arguments) throws Exception {
		this.iteratorIndex = 0;
		return null;
	}

	@Callback(doc="function():number -- Returns the number of elements in the this.array.")
	public Object[] count(Context context, Arguments arguments) throws Exception {
		return new Object[] { this.array != null ? this.array.length : 0 };
	}

	@Callback(doc="function():table -- Returns ALL the stack in the this.array. Memory intensive.")
	public Object[] getAll(Context context, Arguments arguments) throws Exception {
		TreeMap<Integer,Object> map = new TreeMap<Integer,Object>();
		for (int i=0; i<this.array.length; i++){
			map.put(i, this.array[i] != null ? this.array[i] : emptyMap);
		}
		return new Object[] { map };
	}

	public String toString(){
		return "{ItemStack Array}";
	}
}

