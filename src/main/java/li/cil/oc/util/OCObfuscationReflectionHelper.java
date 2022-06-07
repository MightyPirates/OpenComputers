package li.cil.oc.util;

import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;

public final class OCObfuscationReflectionHelper {
	private OCObfuscationReflectionHelper() {

	}

	public static <T, E> T getPrivateValue(Class<? super E> classToAccess, @Nullable E instance, String srgName) {
		// HACK: Don't break compatibility with older Forge versions.
		// This also works around a Scala compiler crash: "trying to do lub/glb of typevar ?E".
		return ObfuscationReflectionHelper.getPrivateValue(classToAccess, instance, new String[]{srgName});
	}

	public static <T, E> void setPrivateValue(Class<? super T> classToAccess, T instance, E value, String srgName) {
		// HACK: Don't break compatibility with older Forge versions.
		// This also works around a Scala compiler crash: "trying to do lub/glb of typevar ?E".
		ObfuscationReflectionHelper.setPrivateValue(classToAccess, instance, new String[]{srgName});
	}
}
