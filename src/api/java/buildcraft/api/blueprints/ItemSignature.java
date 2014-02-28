/**
 * Copyright (c) 2011-2014, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.api.blueprints;

@Deprecated
public class ItemSignature {

	public String itemClassName;
	public String itemName;

	public ItemSignature(String str) {
		String[] values = str.split("/");

		itemClassName = values[1];
		itemName = values[2];

		replaceNullWithStar();

	}

	public ItemSignature() {
		replaceNullWithStar();
	}

	@Override
	public String toString() {
		replaceNullWithStar();

		return "#I/" + itemClassName + "/" + itemName;
	}

	public void replaceNullWithStar() {
		if (itemClassName == null) {
			itemClassName = "*";
		}

		if (itemName == null) {
			itemName = "*";
		}
	}
}
