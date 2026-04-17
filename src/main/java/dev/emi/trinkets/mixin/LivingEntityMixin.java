package dev.emi.trinkets.mixin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import dev.emi.trinkets.api.LivingEntityTrinketComponent;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.event.SlotCountModificationCallback;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketsNetwork;
import dev.emi.trinkets.api.SlotType;
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
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameRules;

/**
 * Trinket dropping on death, trinket EAMs, and trinket equip/unequip calls
 *
 * @author Emi
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity implements LivingEntityTrinketComponent.StackHistory {
	@Unique
	private final Map<String, ItemStack> lastEquippedTrinkets = new HashMap<>();

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

		boolean keepInv = entity.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY);
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
		if (entity.isRemoved()) {
			return;
		}
		TrinketsApi.getTrinketComponent(entity).ifPresent(trinkets -> {
			Map<String, ItemStack> newlyEquippedTrinkets = new HashMap<>();
			Map<String, ItemStack> contentUpdates = new HashMap<>();
			trinkets.forEach((ref, stack) -> {
				TrinketInventory inventory = ref.inventory();
				int index = ref.index();
				ItemStack oldStack = trinkets$getOldStack(ref);
				ItemStack newStack = inventory.getStack(index);
				ItemStack newStackCopy = newStack.copy();
				String newRef = ref.getId();

				if (!ItemStack.areEqual(newStack, oldStack)) {

					TrinketsApi.getTrinket(oldStack.getItem()).onUnequip(oldStack, ref, entity);
					TrinketsApi.getTrinket(newStack.getItem()).onEquip(newStack, ref, entity);

					if (!this.getWorld().isClient && trinkets instanceof LivingEntityTrinketComponent livingEntityTrinkets) {
						contentUpdates.put(newRef, newStackCopy);

						livingEntityTrinkets.processSlotModifiers(ref, oldStack, newStack);
					}
				}

				// Check that the inventory hasn't shrunk past the new stack
				if (index < inventory.size()) {
					TrinketsApi.getTrinket(newStack.getItem()).tick(newStack, ref, entity);
					ItemStack tickedStack = inventory.getStack(index);
					// Avoid calling equip/unequip on stacks that mutate themselves
					if (tickedStack.getItem() == newStackCopy.getItem()) {
						newlyEquippedTrinkets.put(newRef, tickedStack.copy());
					} else {
						newlyEquippedTrinkets.put(newRef, newStackCopy);
					}
				}
			});

			Set<TrinketInventory> inventoriesToSend = trinkets.getTrackingUpdates();

			if (!this.getWorld().isClient) {
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
				}
			}

			if (!inventoriesToSend.isEmpty()) {
				SlotCountModificationCallback.EVENT.invoker().onChange(trinkets, inventoriesToSend);
			}

			inventoriesToSend.clear();

			lastEquippedTrinkets.clear();
			lastEquippedTrinkets.putAll(newlyEquippedTrinkets);
		});
	}

	@Override
	public ItemStack trinkets$getOldStack(SlotReference ref) {
		return lastEquippedTrinkets.getOrDefault(ref.getId(), ItemStack.EMPTY);
	}
}
