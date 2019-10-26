package dev.emi.trinkets.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Multimap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.trinkets.api.ITrinket;
import dev.emi.trinkets.api.TrinketSlots;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

/**
 * Drops trinkets on death if other items are dropping and handling attributes
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
	public List<ItemStack> oldStacks = new ArrayList<ItemStack>();

	protected PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
		super(type, world);
	}

	@Shadow
	public abstract ItemEntity dropItem(ItemStack stack, boolean b1, boolean b2);

	@Inject(at = @At("TAIL"), method = "dropInventory")
	protected void dropInventory(CallbackInfo info) {
		if (!this.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
			Inventory inv = TrinketsApi.TRINKETS.get(this).getInventory();
			for (int i = 0; i < inv.getInvSize(); i++) {
				ItemStack stack = inv.getInvStack(i);
				if (!stack.isEmpty()) {
					this.dropItem(stack, true, false);
				}
			}
			inv.clear();
		}
	}

	@Inject(at = @At("TAIL"), method = "tick")
	public void tick(CallbackInfo info) {
		if(world.isClient) return;
		Inventory inv = TrinketsApi.getTrinketsInventory((PlayerEntity) (LivingEntity) this);
		if (oldStacks.size() < inv.getInvSize()){
			for (int i = oldStacks.size(); i < inv.getInvSize(); i++) {
				oldStacks.add(ItemStack.EMPTY);
			}
		}
		for (int i = 0; i < inv.getInvSize(); i++) {
			ItemStack old = oldStacks.get(i);
			ItemStack current = inv.getInvStack(i);
			if (!old.isItemEqualIgnoreDamage(current)) {
				if (old.getItem() instanceof ITrinket){
					removeAttributes((PlayerEntity) (LivingEntity) this, old, i);
				}
				if (current.getItem() instanceof ITrinket) {
					addAttributes((PlayerEntity) (LivingEntity) this, current, i);
				}
				oldStacks.set(i, current.copy());
			}
		}
	}
	
	public void addAttributes(PlayerEntity player, ItemStack stack, int i) {
		if (stack.getItem() instanceof ITrinket) {
			TrinketSlots.Slot slot = TrinketSlots.getAllSlots().get(i);
			TrinketSlots.SlotGroup group = slot.getSlotGroup();
			ITrinket trinket = (ITrinket) stack.getItem();
			Multimap<String, EntityAttributeModifier> eams;
			eams = trinket.getTrinketModifiers(group.getName(), slot.getName(), UUID.nameUUIDFromBytes((slot.getName() + ":" + group.getName()).getBytes()), stack);
			player.getAttributeContainer().replaceAll(eams);
		}
	}
	
	public void removeAttributes(PlayerEntity player, ItemStack stack, int i) {
		if (stack.getItem() instanceof ITrinket) {
			TrinketSlots.Slot slot = TrinketSlots.getAllSlots().get(i);
			TrinketSlots.SlotGroup group = slot.getSlotGroup();
			ITrinket trinket = (ITrinket) stack.getItem();
			Multimap<String, EntityAttributeModifier> eams;
			eams = trinket.getTrinketModifiers(group.getName(), slot.getName(), UUID.nameUUIDFromBytes((slot.getName() + ":" + group.getName()).getBytes()), stack);
			player.getAttributeContainer().removeAll(eams);
		}
	}
}