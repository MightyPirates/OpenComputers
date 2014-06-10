package thaumcraft.api.research;

import net.minecraft.entity.Entity;

public class ScanResult {
	public byte type = 0;   //1=blocks,2=entities,3=phenomena
	public int id;
	public int meta;
	public Entity entity;
	public String phenomena;

	public ScanResult(byte type, int blockId, int blockMeta, Entity entity,
			String phenomena) {
		super();
		this.type = type;
		this.id = blockId;
		this.meta = blockMeta;		
		this.entity = entity;
		this.phenomena = phenomena;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ScanResult) {
			ScanResult sr = (ScanResult) obj;
			if (type != sr.type)
				return false;
			if (type == 1
					&& (id != sr.id || meta != sr.meta))
				return false;
			if (type == 2 && entity.getEntityId() != sr.entity.getEntityId())
				return false;
			if (type == 3 && !phenomena.equals(sr.phenomena))
				return false;
		}
		return true;
	}

}
