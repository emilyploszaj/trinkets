package dev.emi.trinkets.mixin;

import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketsNetwork;
import dev.emi.trinkets.api.*;
import dev.emi.trinkets.api.Trinket.SlotReference;
import dev.emi.trinkets.api.TrinketEnums.DropRule;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Pair;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

/**
 * Trinket dropping on death, trinket EAMs, and trinket equip/unequip calls
 *
 * @author Emi
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
	@Unique
	private final Map<String, ItemStack> lastEquippedTrinkets = new HashMap<>();
	
	@Shadow
	public abstract AttributeContainer getAttributes();

	protected LivingEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
		super(entityType, world);
	}

	@Inject(at = @At("TAIL"), method = "dropInventory")
	public void dropInventory(CallbackInfo info) {
		boolean keepInv = this.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY);
		LivingEntity entity = (LivingEntity) (Object) this;
		TrinketsApi.getTrinketComponent(entity).ifPresent(trinkets -> trinkets.forEach((slotReference, stack) -> {
			if (stack.isEmpty()) {
				return;
			}

			DropRule dropRule = TrinketsApi.getTrinket(stack.getItem())
					.map(trinket -> trinket.getDropRule(stack, slotReference, entity)).orElse(DropRule.DEFAULT);
			TrinketInventory inventory = slotReference.inventory;

			if (dropRule == DropRule.DEFAULT) {
				dropRule = inventory.getSlotType().getDropRule();
			}

			if (dropRule == DropRule.DEFAULT) {
				if (keepInv && this.getType() == EntityType.PLAYER) {
					dropRule = DropRule.ALWAYS_KEEP;
				} else {
					if (EnchantmentHelper.hasVanishingCurse(stack)) {
						dropRule = DropRule.DESTROY;
					} else {
						dropRule = DropRule.ALWAYS_DROP;
					}
				}
			}

			switch (dropRule) {
				case ALWAYS_DROP:
					dropStack(stack);
					// Fallthrough
				case DESTROY:
					inventory.setStack(slotReference.index, ItemStack.EMPTY);
					break;
				default:
					break;
			}
		}));
	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		LivingEntity entity = (LivingEntity) (Object) this;
		TrinketsApi.getTrinketComponent(entity).ifPresent(trinkets -> {
			Map<String, ItemStack> newlyEquippedTrinkets = new HashMap<>();
			trinkets.clearCachedModifiers();
			trinkets.forEach((ref, stack) -> {
				TrinketInventory inventory = ref.inventory;
				SlotType slotType = inventory.getSlotType();
				int index = ref.index;
				ItemStack oldStack = getOldStack(slotType, index);
				ItemStack newStack = inventory.getStack(index);
				newlyEquippedTrinkets.put(slotType.getGroup() + ":" + slotType.getName() + ":" + index, newStack.copy());

				if (!ItemStack.areEqual(newStack, oldStack)) {

					if (!this.world.isClient) {
						UUID uuid = UUID.nameUUIDFromBytes((ref.index + slotType.getName() + slotType.getGroup()).getBytes());

						if (!oldStack.isEmpty()) {
							Optional<Trinket> trinket = TrinketsApi.getTrinket(oldStack.getItem());
							trinket.ifPresent(value -> {
								this.getAttributes().removeModifiers(value.getModifiers(oldStack, ref, entity, uuid));
								trinkets.removeModifiers(value.getSlotModifiers(oldStack, ref, entity, uuid));
							});
						}

						if (!newStack.isEmpty()) {
							Optional<Trinket> trinket = TrinketsApi.getTrinket(newStack.getItem());
							trinket.ifPresent(value -> {
								this.getAttributes().addTemporaryModifiers(value.getModifiers(newStack, ref, entity, uuid));
								trinkets.addTemporaryModifiers(value.getSlotModifiers(newStack, ref, entity, uuid));
							});
						}
					}

					if (!newStack.isItemEqual(oldStack)) {
						TrinketsApi.getTrinket(oldStack.getItem()).ifPresent(trinket -> trinket.onUnequip(oldStack, ref, entity));
						TrinketsApi.getTrinket(newStack.getItem()).ifPresent(trinket -> trinket.onEquip(newStack, ref, entity));
					}
				}
			});

			if (!this.world.isClient) {
				Set<TrinketInventory> inventoriesToSend = trinkets.getTrackingUpdates();

				if (!inventoriesToSend.isEmpty()) {
					PacketByteBuf buf = PacketByteBufs.create();
					CompoundTag tag = new CompoundTag();
					buf.writeInt(entity.getId());
					for (TrinketInventory trinketInventory : inventoriesToSend) {
						tag.put(trinketInventory.getSlotType().getGroup() + ":" + trinketInventory.getSlotType().getName(), trinketInventory.getSyncTag());
					}
					buf.writeCompoundTag(tag);

					for (ServerPlayerEntity player : PlayerLookup.tracking(entity)) {
						ServerPlayNetworking.send(player, TrinketsNetwork.SYNC_MODIFIERS, buf);
					}
					if (entity instanceof ServerPlayerEntity) {
						ServerPlayNetworking.send((ServerPlayerEntity) entity, TrinketsNetwork.SYNC_MODIFIERS, buf);
						((TrinketPlayerScreenHandler) ((ServerPlayerEntity) entity).playerScreenHandler).updateTrinketSlots(false);
					}
					inventoriesToSend.clear();
				}
			}

			lastEquippedTrinkets.clear();
			lastEquippedTrinkets.putAll(newlyEquippedTrinkets);
		});
	}

	@Unique
	private ItemStack getOldStack(SlotType type, int index) {
		return lastEquippedTrinkets.getOrDefault(type.getGroup() + ":" + type.getName() + ":" + index, ItemStack.EMPTY);
	}
}
