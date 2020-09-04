package dev.emi.trinkets.api;

public class TrinketEnums {

  public enum DropRule {
    ALWAYS_KEEP, ALWAYS_DROP, DEFAULT, DESTROY;

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
