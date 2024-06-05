package dev.emi.trinkets.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.emi.trinkets.TrinketSlotTarget;
import net.minecraft.enchantment.Enchantment;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Mixin(Enchantment.Definition.class)
public class EnchantmentDefinitionMixin implements TrinketSlotTarget {
	@Unique
	private Set<String> trinketSlots = Set.of();


	@Override
	public Set<String> trinkets$slots() {
		return this.trinketSlots;
	}

	@Override
	public void trinkets$slots(Set<String> slots) {
		this.trinketSlots = slots;
	}

	@ModifyExpressionValue(method = "<clinit>", at = @At(value = "INVOKE", target = "Lcom/mojang/serialization/codecs/RecordCodecBuilder;mapCodec(Ljava/util/function/Function;)Lcom/mojang/serialization/MapCodec;"))
	private static MapCodec<Enchantment.Definition> extendCodec(MapCodec<Enchantment.Definition> codec) {
		return RecordCodecBuilder.mapCodec(instance -> instance.group(
				codec.forGetter(Function.identity()),
				Codec.list(Codec.STRING).xmap(Set::copyOf, List::copyOf)
						.optionalFieldOf("trinkets:slots", Set.of())
						.forGetter(x -> ((TrinketSlotTarget) (Object) x).trinkets$slots())
			).apply(instance, (Enchantment.Definition def, Set<String> slots) -> {
				((TrinketSlotTarget) (Object) def).trinkets$slots(slots);
				return def;
			}));
	}
}
