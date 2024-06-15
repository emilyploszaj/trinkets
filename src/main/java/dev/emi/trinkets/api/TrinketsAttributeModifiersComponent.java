package dev.emi.trinkets.api;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

import net.minecraft.component.ComponentType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

public record TrinketsAttributeModifiersComponent(List<Entry> modifiers, boolean showInTooltip) {
	public static final TrinketsAttributeModifiersComponent DEFAULT = new TrinketsAttributeModifiersComponent(List.of(), true);
	private static final Codec<TrinketsAttributeModifiersComponent> BASE_CODEC = RecordCodecBuilder.create((instance) -> {
		return instance.group(
						Entry.CODEC.listOf().fieldOf("modifiers").forGetter(TrinketsAttributeModifiersComponent::modifiers),
						Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(TrinketsAttributeModifiersComponent::showInTooltip)
				).apply(instance, TrinketsAttributeModifiersComponent::new);
	});
	public static final Codec<TrinketsAttributeModifiersComponent> CODEC = Codec.withAlternative(BASE_CODEC, Entry.CODEC.listOf(), (attributeModifiers) -> {
		return new TrinketsAttributeModifiersComponent(attributeModifiers, true);
	});

	public static final PacketCodec<RegistryByteBuf, TrinketsAttributeModifiersComponent> PACKET_CODEC = PacketCodec.tuple(
			Entry.PACKET_CODEC.collect(PacketCodecs.toList()),
			TrinketsAttributeModifiersComponent::modifiers,
			PacketCodecs.BOOL,
			TrinketsAttributeModifiersComponent::showInTooltip,
			TrinketsAttributeModifiersComponent::new);

	public static final ComponentType<TrinketsAttributeModifiersComponent> TYPE = ComponentType.<TrinketsAttributeModifiersComponent>builder().codec(CODEC).packetCodec(PACKET_CODEC).build();

	public TrinketsAttributeModifiersComponent(List<Entry> modifiers, boolean showInTooltip) {
		this.modifiers = modifiers;
		this.showInTooltip = showInTooltip;
	}

	public TrinketsAttributeModifiersComponent withShowInTooltip(boolean showInTooltip) {
		return new TrinketsAttributeModifiersComponent(this.modifiers, showInTooltip);
	}

	public List<Entry> modifiers() {
		return this.modifiers;
	}

	public boolean showInTooltip() {
		return this.showInTooltip;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final ImmutableList.Builder<Entry> entries = ImmutableList.builder();

		Builder() {
		}

		public Builder add(RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier modifier) {
			return add(attribute, modifier, Optional.empty());
		}
		public Builder add(RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier modifier, String slot) {
			return add(attribute, modifier, Optional.of(slot));
		}

		public Builder add(RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier modifier, Optional<String> slot) {
			this.entries.add(new Entry (attribute, modifier, slot));
			return this;
		}

		public TrinketsAttributeModifiersComponent build() {
			return new TrinketsAttributeModifiersComponent(this.entries.build(), true);
		}
	}

	public record Entry(RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier modifier, Optional<String> slot) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create((instance) -> instance.group(
				Registries.ATTRIBUTE.getEntryCodec().fieldOf("type").forGetter(Entry::attribute),
				EntityAttributeModifier.MAP_CODEC.forGetter(Entry::modifier),
				Codec.STRING.optionalFieldOf("slot").forGetter(Entry::slot)
			).apply(instance, Entry::new));
		public static final PacketCodec<RegistryByteBuf, Entry> PACKET_CODEC = PacketCodec.tuple(
				PacketCodecs.registryEntry(RegistryKeys.ATTRIBUTE),
				Entry::attribute,
				EntityAttributeModifier.PACKET_CODEC,
				Entry::modifier,
				PacketCodecs.optional(PacketCodecs.STRING),
				Entry::slot,
				Entry::new);
	}
}
