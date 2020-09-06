package dev.emi.trinkets.api;

import dev.onyxstudios.cca.api.v3.component.AutoSyncedComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.CompoundTag;

public class LivingEntityTrinketComponent implements TrinketComponent, AutoSyncedComponent {

	public LivingEntity entity;

	public LivingEntityTrinketComponent(LivingEntity entity) {
		this.entity = entity;
	}

	@Override
	public void readFromNbt(CompoundTag tag) {

	}

	@Override
	public void writeToNbt(CompoundTag tag) {

	}
}