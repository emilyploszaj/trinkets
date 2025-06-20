package dev.emi.trinkets.api;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record TrinketSaveData(Map<String, Map<String, InventoryData>> data) {
    public static final Codec<TrinketSaveData> CODEC = Codec.unboundedMap(Codec.STRING, Codec.unboundedMap(Codec.STRING, InventoryData.CODEC)).xmap(TrinketSaveData::new, TrinketSaveData::data);
    public static final MapCodec<TrinketSaveData> MAP_CODEC = MapCodec.assumeMapUnsafe(CODEC);
    public record Metadata(List<EntityAttributeModifier> persistentModifiers, List<EntityAttributeModifier> cachedModifiers) {
        public static final Metadata EMPTY = new Metadata(List.of(), List.of());
        public static final Codec<Metadata> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                EntityAttributeModifier.CODEC.listOf().optionalFieldOf("PersistentModifiers", List.of()).forGetter(Metadata::persistentModifiers),
                EntityAttributeModifier.CODEC.listOf().optionalFieldOf("CachedModifiers", List.of()).forGetter(Metadata::cachedModifiers)
        ).apply(instance, Metadata::new));

        public static final PacketCodec<RegistryByteBuf, Metadata> PACKET_CODEC = PacketCodec.tuple(
                PacketCodecs.collection(ArrayList::new, EntityAttributeModifier.PACKET_CODEC), Metadata::persistentModifiers,
                PacketCodecs.collection(ArrayList::new, EntityAttributeModifier.PACKET_CODEC), Metadata::cachedModifiers,
                Metadata::new
        );

        public static final PacketCodec<RegistryByteBuf, Metadata> PACKET_CODEC_PERSISTENT_ONLY = PacketCodec.tuple(
                PacketCodecs.collection(ArrayList::new, EntityAttributeModifier.PACKET_CODEC), Metadata::persistentModifiers,
                list -> new Metadata(list, List.of())
        );
    }
    public record InventoryData(Metadata metadata, List<ItemStack> items) {
        public static final Codec<InventoryData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Metadata.CODEC.optionalFieldOf("Metadata", Metadata.EMPTY).forGetter(InventoryData::metadata),
                ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("Items", List.of()).forGetter(InventoryData::items)
        ).apply(instance, InventoryData::new));
    }
}