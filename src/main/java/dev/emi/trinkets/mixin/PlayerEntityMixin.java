package dev.emi.trinkets.mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Multimap;

import net.minecraft.entity.attribute.EntityAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.SlotGroups;
import dev.emi.trinkets.api.Slots;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketSlots;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Drops trinkets on death if other items are dropping, elytra check redirect, and handling attributes
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
	float addHealth = 0f;
	public List<ItemStack> oldStacks = new ArrayList<ItemStack>();

	protected PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
		super(type, world);
	}

	@Shadow
	public abstract ItemEntity dropItem(ItemStack stack, boolean b1, boolean b2);

	@Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getEquippedStack(Lnet/minecraft/entity/EquipmentSlot;)Lnet/minecraft/item/ItemStack;"), method = "checkFallFlying")
	public ItemStack getEquippedStackProxy(PlayerEntity entity, EquipmentSlot slot) {
		if (entity instanceof PlayerEntity) {
			PlayerEntity player = (PlayerEntity) entity;
			TrinketComponent comp = TrinketsApi.getTrinketComponent(player);
			return comp.getStack(SlotGroups.CHEST, Slots.CAPE);
		} else {
			return entity.getEquippedStack(slot);
		}
	}

	@Inject(at = @At("TAIL"), method = "dropInventory")
	protected void dropInventory(CallbackInfo info) {
		if (!this.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
			Inventory inv = TrinketsApi.TRINKETS.get(this).getInventory();
			for (int i = 0; i < inv.size(); i++) {
				ItemStack stack = inv.getStack(i);
				if (!stack.isEmpty()) {
					this.dropItem(stack, true, false);
				}
			}
			inv.clear();
		}
	}

	@Inject(at = @At("TAIL"), method = "readCustomDataFromTag")
	public void readCustomDataFromTag(CompoundTag tag, CallbackInfo info) {
		if (tag.contains("Health", 99)) { // Health is applied before attributes are calculated, mojbug, we store the difference and add it once they are
			addHealth = tag.getFloat("Health") - this.getHealth();
		}
	}

	@Inject(at = @At("TAIL"), method = "tick")
	public void tick(CallbackInfo info) {
		if(world.isClient) return;
		Inventory inv = TrinketsApi.getTrinketsInventory((PlayerEntity) (LivingEntity) this);
		if (oldStacks.size() < inv.size()){
			for (int i = oldStacks.size(); i < inv.size(); i++) {
				oldStacks.add(ItemStack.EMPTY);
			}
		}
		for (int i = 0; i < inv.size(); i++) {
			ItemStack old = oldStacks.get(i);
			ItemStack current = inv.getStack(i);
			if (!old.isItemEqualIgnoreDamage(current) || old.getCount() != current.getCount()) {
				if (old.getItem() instanceof Trinket){
					removeAttributes((PlayerEntity) (LivingEntity) this, old, i);
				}
				if (current.getItem() instanceof Trinket) {
					addAttributes((PlayerEntity) (LivingEntity) this, current, i);
				}
				oldStacks.set(i, current.copy());
			}
		}
		if (addHealth > 0f) {
			this.setHealth(this.getHealth() + addHealth);
			addHealth = 0f;
		}
	}

	@Inject(at = @At("HEAD"), method = "canPickUp(Lnet/minecraft/item/ItemStack;)Z", cancellable = true)
	public void canPickUp(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (stack.getItem() instanceof Trinket) {
			TrinketComponent comp = TrinketsApi.getTrinketComponent((PlayerEntity) (LivingEntity) this);
			for (TrinketSlots.Slot slot : TrinketSlots.getAllSlots()) {
				if (slot.canEquip.apply(slot, stack)) {
					cir.setReturnValue(comp.getStack(slot.getName()).isEmpty());
					cir.cancel();
				}
			}
		}
	}
	
	public void addAttributes(PlayerEntity player, ItemStack stack, int i) {
		if (stack.getItem() instanceof Trinket) {
			TrinketSlots.Slot slot = TrinketSlots.getAllSlots().get(i);
			TrinketSlots.SlotGroup group = slot.getSlotGroup();
			Trinket trinket = (Trinket) stack.getItem();
			Multimap<EntityAttribute, EntityAttributeModifier> eams;
			eams = trinket.getTrinketModifiers(group.getName(), slot.getName(), UUID.nameUUIDFromBytes((slot.getName() + ":" + group.getName()).getBytes()), stack);
			player.getAttributes().addTemporaryModifiers(eams);
		}
	}
	
	public void removeAttributes(PlayerEntity player, ItemStack stack, int i) {
		if (stack.getItem() instanceof Trinket) {
			TrinketSlots.Slot slot = TrinketSlots.getAllSlots().get(i);
			TrinketSlots.SlotGroup group = slot.getSlotGroup();
			Trinket trinket = (Trinket) stack.getItem();
			Multimap<EntityAttribute, EntityAttributeModifier> eams;
			eams = trinket.getTrinketModifiers(group.getName(), slot.getName(), UUID.nameUUIDFromBytes((slot.getName() + ":" + group.getName()).getBytes()), stack);
			player.getAttributes().removeModifiers(eams);
		}
	}
}