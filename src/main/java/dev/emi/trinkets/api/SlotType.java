package dev.emi.trinkets.api;

import dev.emi.trinkets.api.TrinketEnums.DropRule;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

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

	public TranslatableText getTranslation() {
		return new TranslatableText("trinkets.slot." + this.group + "." + this.name);
	}

	public void write(CompoundTag data) {
		CompoundTag tag = new CompoundTag();
		tag.putString("Group", group);
		tag.putString("Name", name);
		tag.putInt("Order", order);
		tag.putInt("Amount", amount);
		tag.putInt("Locked", locked);
		tag.putString("Icon", icon.toString());
		ListTag quickMoveList = new ListTag();

		for (Identifier id : quickMove) {
			quickMoveList.add(StringTag.of(id.toString()));
		}
		tag.put("QuickMove", quickMoveList);

		ListTag validatorList = new ListTag();

		for (Identifier id : validators) {
			validatorList.add(StringTag.of(id.toString()));
		}
		tag.put("Validators", validatorList);
		tag.putString("DropRule", dropRule.toString());
		data.put("SlotData", tag);
	}

	public static SlotType read(CompoundTag data) {
		CompoundTag slotData = data.getCompound("SlotData");
		String group = slotData.getString("Group");
		String name = slotData.getString("Name");
		int order = slotData.getInt("Order");
		int amount = slotData.getInt("Amount");
		int locked = slotData.getInt("Locked");
		Identifier icon = new Identifier(slotData.getString("Icon"));
		ListTag quickMoveList = slotData.getList("QuickMove", NbtType.STRING);
		Set<Identifier> quickMove = new HashSet<>();

		for (Tag tag : quickMoveList) {
			quickMove.add(new Identifier(tag.toString()));
		}
		ListTag validatorList = slotData.getList("Validators", NbtType.STRING);
		Set<Identifier> validators = new HashSet<>();

		for (Tag tag : validatorList) {
			validators.add(new Identifier(tag.toString()));
		}
		String dropRuleName = slotData.getString("DropRule");
		DropRule dropRule = DropRule.DEFAULT;

		if (TrinketEnums.DropRule.has(dropRuleName)) {
			dropRule = TrinketEnums.DropRule.valueOf(dropRuleName);
		}
		return new SlotType(group, name, order, amount, locked, icon, quickMove, validators, dropRule);
	}
}
