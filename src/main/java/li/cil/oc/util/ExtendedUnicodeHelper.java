package li.cil.oc.util;

/**
 * Helper functions for handling strings with characters outside of the Unicode BMP.
 */
public final class ExtendedUnicodeHelper {
	private ExtendedUnicodeHelper() {

	}

	public static int length(String s) {
		return s.codePointCount(0, s.length());
	}

	public static String reverse(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = s.length() - 1; i >= 0; i--) {
			char c = s.charAt(i);
			if (Character.isLowSurrogate(c) && i > 0) {
				i--;
				char c2 = s.charAt(i);
				if (Character.isHighSurrogate(c2)) {
					sb.append(c2).append(c);
				} else {
					// Invalid surrogate pair?
					sb.append(c).append(c2);
				}
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public static String substring(String s, int start, int end) {
		return s.substring(
				s.offsetByCodePoints(0, start),
				s.offsetByCodePoints(0, end)
		);
	}
}
