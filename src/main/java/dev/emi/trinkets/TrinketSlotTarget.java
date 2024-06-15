package dev.emi.trinkets;

import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public interface TrinketSlotTarget {
	Set<String> trinkets$slots();
	void trinkets$slots(Set<String> slots);
}
