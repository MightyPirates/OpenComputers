/** 
 * Copyright (c) SpaceToad, 2011
 * http://www.mod-buildcraft.com
 * 
 * BuildCraft is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package buildcraft.api.blueprints;

public class BlockSignature {

	public String blockClassName;
	public String tileClassName;
	public String blockName;
	public String mod;
	public String modVersion;
	public String customField;

	public BlockSignature(String str) {
		String[] values = str.split("/");

		int i = 0;

		if (values[0].equals("#B")) {
			i++;
		}

		blockClassName = values[i];
		tileClassName = values[i + 1];
		blockName = values[i + 2];
		mod = values[i + 3];
		modVersion = values[i + 4];
		customField = values[i + 5];

		replaceNullWithStar();

	}

	public BlockSignature() {
		replaceNullWithStar();
	}

	@Override
	public String toString() {
		replaceNullWithStar();

		return "#B/" + blockClassName + "/" + tileClassName + "/" + blockName + "/" + mod + "/" + modVersion + "/" + customField;
	}

	public void replaceNullWithStar() {
		if (blockClassName == null) {
			blockClassName = "*";
		}

		if (tileClassName == null) {
			tileClassName = "*";
		}

		if (blockName == null) {
			blockName = "*";
		}

		if (mod == null) {
			mod = "*";
		}

		if (modVersion == null) {
			modVersion = "*";
		}

		if (customField == null) {
			customField = "*";
		}
	}
}
