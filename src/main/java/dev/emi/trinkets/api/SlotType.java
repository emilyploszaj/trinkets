package dev.emi.trinkets.api;

import dev.emi.trinkets.api.TrinketEnums.DropRule;
import java.util.Set;
import net.minecraft.util.Identifier;

public class SlotType {

	private final String group;
	private final String name;
	private final int order;
	private final int amount;
	private final int locked;
	private final Identifier icon;
	private final Set<Identifier> quickMove;
	private final Set<Identifier> validators;
	private final DropRule dropRule;

	public SlotType(String group, String name, int order, int amount, int locked, Identifier icon,
			Set<Identifier> quickMove, Set<Identifier> validators, DropRule dropRule) {
		this.group = group;
		this.name = name;
		this.order = order;
		this.amount = amount;
		this.locked = locked;
		this.icon = icon;
		this.quickMove = quickMove;
		this.validators = validators;
		this.dropRule = dropRule;
	}

	public String getGroup() {
		return group;
	}

	public String getName() {
		return name;
	}

	public int getOrder() {
		return order;
	}

	public int getAmount() {
		return amount;
	}

	public int getLocked() {
		return locked;
	}

	public Identifier getIcon() {
		return icon;
	}

	public Set<Identifier> getQuickMove() {
		return quickMove;
	}

	public Set<Identifier> getValidators() {
		return validators;
	}

	public DropRule getDropRule() {
		return dropRule;
	}
}
