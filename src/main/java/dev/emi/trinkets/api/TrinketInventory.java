package dev.emi.trinkets.api;

import dev.emi.trinkets.TrinketsMain;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.fabricmc.fabric.api.network.PacketConsumer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.BasicInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.PacketByteBuf;

/**
 * Inventory that marks its parent PlayerTrinketComponent dirty and syncs with the server when needed
 */
public class TrinketInventory extends BasicInventory {
	private PlayerTrinketComponent component;

	public TrinketInventory(PlayerTrinketComponent component, int size) {
		super(size);
		this.component = component;
	}

	public PlayerTrinketComponent getComponent(){
		return component;
	}

	@Override
	public void setInvStack(int i, ItemStack stack) {
		if (getInvStack(i).getItem() instanceof ITrinket) {
			onUnEquipItemStack(getInvStack(i), (PlayerEntity) component.getEntity());
		}
		super.setInvStack(i, stack);
		if(getInvStack(i).getItem() instanceof ITrinket) {
			onEquipItemStack(getInvStack(i), (PlayerEntity) component.getEntity());
		}
	}

	@Override
	public ItemStack removeInvStack(int i) {
		if(getInvStack(i).getItem() instanceof ITrinket){
			onUnEquipItemStack(getInvStack(i), (PlayerEntity) component.getEntity());
		}
		return super.removeInvStack(i);
	}

	@Override
	public ItemStack takeInvStack(int i, int count) {
		ItemStack stack = super.takeInvStack(i, count);
		if (!stack.isEmpty() && getInvStack(i).isEmpty() && stack.getItem() instanceof ITrinket) {
			onUnEquipItemStack(stack, (PlayerEntity) component.getEntity());
		}
		return stack;
	}

	@Override
	public void markDirty() {
		component.markDirty();
	}

	private void onEquipItemStack(ItemStack stack, PlayerEntity player) {
		((ITrinket) stack.getItem()).onEquip(player, stack);
		PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
		passedData.writeBoolean(true); // Equipping, not unequipping
		passedData.writeItemStack(stack);
		ClientSidePacketRegistry.INSTANCE.sendToServer(TrinketsMain.ITEM_EQUIPPED_CHANGE_PACKET, passedData);
	}

	private void onUnEquipItemStack(ItemStack stack, PlayerEntity player) {
		((ITrinket) stack.getItem()).onUnequip(player, stack);
		PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
		passedData.writeBoolean(false); // Unequipping, not equipping
		passedData.writeItemStack(stack);
		ClientSidePacketRegistry.INSTANCE.sendToServer(TrinketsMain.ITEM_EQUIPPED_CHANGE_PACKET, passedData);
	}

	public static PacketConsumer EQUIP_STACK_HANDLER = ((packetContext, data) -> {
		boolean isEquipping = data.readBoolean();
		ItemStack stack = data.readItemStack();
		PlayerEntity player = packetContext.getPlayer();
		if (!(stack.getItem() instanceof ITrinket)) return;
		ITrinket trinket = (ITrinket) stack.getItem();
		packetContext.getTaskQueue().execute(() -> {
			if (isEquipping) {
				trinket.onEquipServer(player, stack);
			} else {
				trinket.onUnequipServer(player, stack);
			}
		});
	});
}