package dev.emi.trinkets.api;

public class TrinketEnums {

	public enum DropRule {
		KEEP, DROP, DESTROY, DEFAULT;

		static public boolean has(String name) {
			DropRule[] rules = DropRule.values();

			for (DropRule rule : rules) {

				if (rule.toString().equals(name)) {
					return true;
				}
			}
			return false;
		}
	}
}
