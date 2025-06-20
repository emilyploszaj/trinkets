package dev.emi.trinkets.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import dev.emi.trinkets.TrinketModifiers;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.TrinketSaveData;
import dev.emi.trinkets.api.event.TrinketEquipCallback;
import dev.emi.trinkets.api.event.TrinketUnequipCallback;
import dev.emi.trinkets.payload.SyncInventoryPayload;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Pair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.api.SlotAttributes.SlotEntityAttribute;
import dev.emi.trinkets.api.TrinketEnums.DropRule;
import dev.emi.trinkets.api.event.TrinketDropCallback;
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
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;

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

	private LivingEntityMixin() {
		super(null, null);
	}

	@Inject(at = @At("HEAD"), method = "canFreeze", cancellable = true)
	private void canFreeze(CallbackInfoReturnable<Boolean> cir) {
        Optional<TrinketComponent> component = TrinketsApi.getTrinketComponent((LivingEntity) (Object) this);
		if (component.isPresent()) {
			for (Pair<SlotReference, ItemStack> equipped : component.get().getAllEquipped()) {
				if (equipped.getRight().isIn(ItemTags.FREEZE_IMMUNE_WEARABLES)) {
					cir.setReturnValue(false);
					break;
				}
			}
		}
	}

	@Inject(at = @At("TAIL"), method = "dropInventory")
	private void dropInventory(ServerWorld world, CallbackInfo info) {
		LivingEntity entity = (LivingEntity) (Object) this;

		boolean keepInv = world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY);
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
					if (EnchantmentHelper.hasAnyEnchantmentsWith(stack, EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP)) {
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

	@Unique
	private void dropFromEntity(ItemStack stack) {
		// Mimic player drop behavior for only players
		if (((Entity) this) instanceof PlayerEntity player) {
			ItemEntity entity = player.dropItem(stack, true, false);
		} else if (this.getWorld() instanceof ServerWorld serverWorld) {
			ItemEntity entity = dropStack(serverWorld, stack);
		}
	}

	@Inject(at = @At("TAIL"), method = "tick")
	private void tick(CallbackInfo info) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity.isRemoved()) {
			return;
		}
		TrinketsApi.getTrinketComponent(entity).ifPresent(trinkets -> {
			Map<String, ItemStack> newlyEquippedTrinkets = new HashMap<>();
			Map<String, ItemStack> contentUpdates = new HashMap<>();
			trinkets.forEach((ref, stack) -> {
				TrinketInventory inventory = ref.inventory();
				SlotType slotType = inventory.getSlotType();
				int index = ref.index();
				ItemStack oldStack = getOldStack(slotType, index);
				ItemStack newStack = inventory.getStack(index);
				ItemStack newStackCopy = newStack.copy();
				String newRef = slotType.getGroup() + "/" + slotType.getName() + "/" + index;

				if (!ItemStack.areEqual(newStack, oldStack)) {

					TrinketsApi.getTrinket(oldStack.getItem()).onUnequip(oldStack, ref, entity);
					TrinketUnequipCallback.EVENT.invoker().onUnequip(oldStack, ref, entity);
					TrinketsApi.getTrinket(newStack.getItem()).onEquip(newStack, ref, entity);
					TrinketEquipCallback.EVENT.invoker().onEquip(newStack, ref, entity);

					World world = this.getWorld();
					if (!world.isClient) {
						contentUpdates.put(newRef, newStackCopy);

						if (!oldStack.isEmpty()) {
							Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map = TrinketModifiers.get(oldStack, ref, entity);
							Multimap<String, EntityAttributeModifier> slotMap = HashMultimap.create();
							Set<RegistryEntry<EntityAttribute>> toRemove = Sets.newHashSet();
							for (RegistryEntry<EntityAttribute> attr : map.keySet()) {
								if (attr.hasKeyAndValue() && attr.value() instanceof SlotEntityAttribute slotAttr) {
									slotMap.putAll(slotAttr.slot, map.get(attr));
									toRemove.add(attr);
								}
							}
							for (RegistryEntry<EntityAttribute> attr : toRemove) {
								map.removeAll(attr);
							}
							//this.getAttributes().removeModifiers(map);
							map.asMap().forEach((attribute, modifiers) -> {
								EntityAttributeInstance entityAttributeInstance = this.getAttributes().getCustomInstance(attribute);
								if (entityAttributeInstance != null) {
									modifiers.forEach(modifier -> entityAttributeInstance.removeModifier(modifier.id()));
								}
							});

							trinkets.removeModifiers(slotMap);
						}

						if (!newStack.isEmpty()) {
							Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map = TrinketModifiers.get(newStack, ref, entity);
							Multimap<String, EntityAttributeModifier> slotMap = HashMultimap.create();
							Set<RegistryEntry<EntityAttribute>> toRemove = Sets.newHashSet();
							for (RegistryEntry<EntityAttribute> attr : map.keySet()) {
								if (attr.hasKeyAndValue() && attr.value() instanceof SlotEntityAttribute slotAttr) {
									slotMap.putAll(slotAttr.slot, map.get(attr));
									toRemove.add(attr);
								}
							}
							for (RegistryEntry<EntityAttribute> attr : toRemove) {
								map.removeAll(attr);
							}
							//this.getAttributes().addTemporaryModifiers(map);
							map.forEach((attribute, attributeModifier) -> {
								EntityAttributeInstance entityAttributeInstance = this.getAttributes().getCustomInstance(attribute);
								if (entityAttributeInstance != null) {
									entityAttributeInstance.removeModifier(attributeModifier.id());
									entityAttributeInstance.addTemporaryModifier(attributeModifier);
								}

							});
							trinkets.addTemporaryModifiers(slotMap);
						}
					}
				}
				TrinketsApi.getTrinket(newStack.getItem()).tick(newStack, ref, entity);
				ItemStack tickedStack = inventory.getStack(index);
				// Avoid calling equip/unequip on stacks that mutate themselves
				if (tickedStack.getItem() == newStackCopy.getItem()) {
					newlyEquippedTrinkets.put(newRef, tickedStack.copy());
				} else {
					newlyEquippedTrinkets.put(newRef, newStackCopy);
				}
			});

			World world = this.getWorld();
			if (!world.isClient) {
				Set<TrinketInventory> inventoriesToSend = trinkets.getTrackingUpdates();

				if (!contentUpdates.isEmpty() || !inventoriesToSend.isEmpty()) {
                    Map<String, TrinketSaveData.Metadata> map = new HashMap<>();

					for (TrinketInventory trinketInventory : inventoriesToSend) {
						map.put(trinketInventory.getSlotType().getId(), trinketInventory.getSyncMetadata());
					}
                    SyncInventoryPayload packet = new SyncInventoryPayload(this.getId(), contentUpdates, map);

					for (ServerPlayerEntity player : PlayerLookup.tracking(entity)) {
						ServerPlayNetworking.send(player, packet);
					}

					if (entity instanceof ServerPlayerEntity serverPlayer) {
						ServerPlayNetworking.send(serverPlayer, packet);

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