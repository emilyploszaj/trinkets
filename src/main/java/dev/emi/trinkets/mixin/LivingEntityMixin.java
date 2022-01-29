package dev.emi.trinkets.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketsNetwork;
import dev.emi.trinkets.api.SlotAttributes;
import dev.emi.trinkets.api.SlotAttributes.SlotEntityAttribute;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketEnums.DropRule;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.event.TrinketDropCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.tag.ItemTags;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameRules;

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
	protected abstract AttributeContainer getAttributes();

	private LivingEntityMixin() {
		super(null, null);
	}

	@Inject(at = @At("HEAD"), method = "canFreeze", cancellable = true)
	private void canFreeze(CallbackInfoReturnable<Boolean> cir) {
		var component = TrinketsApi.getTrinketComponent((LivingEntity) (Object) this);
		if (component.isPresent()) {
			for (var equipped : component.get().getAllEquipped()) {
				if (equipped.getRight().isIn(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
					cir.setReturnValue(false);
					break;
				}
			}
		}
	}

	@Inject(at = @At("TAIL"), method = "dropInventory")
	private void dropInventory(CallbackInfo info) {
		LivingEntity entity = (LivingEntity) (Object) this;

		boolean keepInv = entity.world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY);
		TrinketsApi.getTrinketComponent(entity).ifPresent(trinkets -> trinkets.forEach((ref, stack) -> {
			if (stack.isEmpty()) {
				return;
			}

			DropRule dropRule = TrinketsApi.getTrinket(stack.getItem()).getDropRule(stack, ref, entity);

			dropRule = TrinketDropCallback.EVENT.invoker().drop(dropRule, stack, ref, entity);
			
			TrinketInventory inventory = ref.inventory();

			if (dropRule == DropRule.DEFAULT) {
				dropRule = inventory.getSlotType().getDropRule();
			}

			if (dropRule == DropRule.DEFAULT) {
				if (keepInv && entity.getType() == EntityType.PLAYER) {
					dropRule = DropRule.KEEP;
				} else {
					if (EnchantmentHelper.hasVanishingCurse(stack)) {
						dropRule = DropRule.DESTROY;
					} else {
						dropRule = DropRule.DROP;
					}
				}
			}

			switch (dropRule) {
				case DROP:
					dropFromEntity(stack);
					// Fallthrough
				case DESTROY:
					inventory.setStack(ref.index(), ItemStack.EMPTY);
					break;
				default:
					break;
			}
		}));
	}

	private void dropFromEntity(ItemStack stack) {
		ItemEntity entity = dropStack(stack);
		// Mimic player drop behavior for only players
		if (entity != null && ((Entity) this) instanceof PlayerEntity) {
			entity.setPos(entity.getX(), this.getEyeY() - 0.3, entity.getZ());
			entity.setPickupDelay(40);
			float magnitude = this.random.nextFloat() * 0.5f;
			float angle = this.random.nextFloat() * ((float)Math.PI * 2);
			entity.setVelocity(-MathHelper.sin(angle) * magnitude, 0.2f, MathHelper.cos(angle) * magnitude);
		}
	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		LivingEntity entity = (LivingEntity) (Object) this;
		TrinketsApi.getTrinketComponent(entity).ifPresent(trinkets -> {
			Map<String, ItemStack> newlyEquippedTrinkets = new HashMap<>();
			Map<String, ItemStack> contentUpdates = new HashMap<>();
			trinkets.clearCachedModifiers();
			trinkets.forEach((ref, stack) -> {
				TrinketInventory inventory = ref.inventory();
				SlotType slotType = inventory.getSlotType();
				int index = ref.index();
				ItemStack oldStack = getOldStack(slotType, index);
				ItemStack newStack = inventory.getStack(index);
				ItemStack copy = newStack.copy();
				String newRef = slotType.getGroup() + "/" + slotType.getName() + "/" + index;
				newlyEquippedTrinkets.put(newRef, copy);

				if (!ItemStack.areEqual(newStack, oldStack)) {

					if (!this.world.isClient) {
						contentUpdates.put(newRef, copy);
						UUID uuid = SlotAttributes.getUuid(ref);

						if (!oldStack.isEmpty()) {
							Trinket trinket = TrinketsApi.getTrinket(oldStack.getItem());
							Multimap<EntityAttribute, EntityAttributeModifier> map = trinket.getModifiers(oldStack, ref, entity, uuid);
							Multimap<String, EntityAttributeModifier> slotMap = HashMultimap.create();
							Set<SlotEntityAttribute> toRemove = Sets.newHashSet();
							for (EntityAttribute attr : map.keySet()) {
								if (attr instanceof SlotEntityAttribute slotAttr) {
									slotMap.putAll(slotAttr.slot, map.get(attr));
									toRemove.add(slotAttr);
								}
							}
							for (SlotEntityAttribute attr : toRemove) {
								map.removeAll(attr);
							}
							this.getAttributes().removeModifiers(map);
							trinkets.removeModifiers(slotMap);
						}

						if (!newStack.isEmpty()) {
							Trinket trinket = TrinketsApi.getTrinket(newStack.getItem());
							Multimap<EntityAttribute, EntityAttributeModifier> map = trinket.getModifiers(newStack, ref, entity, uuid);
							Multimap<String, EntityAttributeModifier> slotMap = HashMultimap.create();
							Set<SlotEntityAttribute> toRemove = Sets.newHashSet();
							for (EntityAttribute attr : map.keySet()) {
								if (attr instanceof SlotEntityAttribute slotAttr) {
									slotMap.putAll(slotAttr.slot, map.get(attr));
									toRemove.add(slotAttr);
								}
							}
							for (SlotEntityAttribute attr : toRemove) {
								map.removeAll(attr);
							}
							this.getAttributes().addTemporaryModifiers(map);
							trinkets.addTemporaryModifiers(slotMap);
						}
					}

					if (!newStack.isItemEqual(oldStack)) {
						TrinketsApi.getTrinket(oldStack.getItem()).onUnequip(oldStack, ref, entity);
						TrinketsApi.getTrinket(newStack.getItem()).onEquip(newStack, ref, entity);
					}
				}
			});

			if (!this.world.isClient) {
				Set<TrinketInventory> inventoriesToSend = trinkets.getTrackingUpdates();

				if (!contentUpdates.isEmpty() || !inventoriesToSend.isEmpty()) {
					PacketByteBuf buf = PacketByteBufs.create();
					buf.writeInt(entity.getId());
					NbtCompound tag = new NbtCompound();

					for (TrinketInventory trinketInventory : inventoriesToSend) {
						tag.put(trinketInventory.getSlotType().getGroup() + "/" + trinketInventory.getSlotType().getName(), trinketInventory.getSyncTag());
					}

					buf.writeNbt(tag);
					tag = new NbtCompound();

					for (Map.Entry<String, ItemStack> entry : contentUpdates.entrySet()) {
						tag.put(entry.getKey(), entry.getValue().writeNbt(new NbtCompound()));
					}

					buf.writeNbt(tag);

					for (ServerPlayerEntity player : PlayerLookup.tracking(entity)) {
						ServerPlayNetworking.send(player, TrinketsNetwork.SYNC_INVENTORY, buf);
					}

					if (entity instanceof ServerPlayerEntity serverPlayer) {
						ServerPlayNetworking.send(serverPlayer, TrinketsNetwork.SYNC_INVENTORY, buf);

						if (!inventoriesToSend.isEmpty()) {
							((TrinketPlayerScreenHandler) serverPlayer.playerScreenHandler).trinkets$updateTrinketSlots(false);
						}
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
		return lastEquippedTrinkets.getOrDefault(type.getGroup() + "/" + type.getName() + "/" + index, ItemStack.EMPTY);
	}
}
