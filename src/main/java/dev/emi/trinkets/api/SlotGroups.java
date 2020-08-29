package dev.emi.trinkets.api;

public final class SlotGroups {
	/**
	 * Slot group over the vanilla helmet slot, for any trinkets placed on the head or face
	 */
	public static final String HEAD = "head";
	/**
	 * Slot group over the vanilla chestplate slot, for any trinkets placed on the torso or neck
	 */
	public static final String CHEST = "chest";
	/**
	 * Slot group over the vanilla leggings slot, for any trinkets placed on the legs or hips
	 */
	public static final String LEGS = "legs";
	/**
	 * Slot group over the vanilla boots slot, for any trinkets placed on the feet or ankles
	 */
	public static final String FEET = "feet";
	/**
	 * Slot group over the vanilla offhand slot, for any trinkets placed on only the offhand
	 */
	public static final String OFFHAND = "offhand";
	/**
	 * Slot group representing either the main hand or both hands, for any trinkets placed on only the main hand or both hands at once
	 */
	public static final String HAND = "hand";

	private SlotGroups() {
	}
}
