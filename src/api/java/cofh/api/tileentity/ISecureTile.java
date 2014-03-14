package cofh.api.tileentity;

public interface ISecureTile {

	public static enum AccessMode {
		PUBLIC, RESTRICTED, PRIVATE;

		public boolean isPublic() {

			return this == PUBLIC;
		}

		public boolean isRestricted() {

			return this == RESTRICTED;
		}

		public boolean isPrivate() {

			return this == PRIVATE;
		}

		public static AccessMode stepForward(AccessMode curAccess) {

			return curAccess == AccessMode.PUBLIC ? AccessMode.RESTRICTED : curAccess == AccessMode.PRIVATE ? AccessMode.PUBLIC : AccessMode.PRIVATE;
		}

		public static AccessMode stepBackward(AccessMode curAccess) {

			return curAccess == AccessMode.PUBLIC ? AccessMode.PRIVATE : curAccess == AccessMode.PRIVATE ? AccessMode.RESTRICTED : AccessMode.PUBLIC;
		}
	}

	public boolean setAccess(AccessMode access);

	public AccessMode getAccess();

	public boolean setOwnerName(String name);

	public String getOwnerName();

	public boolean canPlayerAccess(String name);

}
