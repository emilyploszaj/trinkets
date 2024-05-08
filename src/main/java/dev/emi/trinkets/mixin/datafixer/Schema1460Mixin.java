package dev.emi.trinkets.mixin.datafixer;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import net.minecraft.datafixer.TypeReferences;
import net.minecraft.datafixer.schema.Schema1460;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Supplier;

@Mixin(Schema1460.class)
public class Schema1460Mixin {
    @Unique
    private static Schema schema;

    @Inject(method = "registerTypes", at = @At("HEAD"))
    private void captureSchema(Schema schemax, Map<String, Supplier<TypeTemplate>> entityTypes, Map<String, Supplier<TypeTemplate>> blockEntityTypes, CallbackInfo ci) {
        schema = schemax;
    }
    @ModifyReturnValue(method = {"method_5260", "method_5236"}, at = @At("RETURN"))
    private static TypeTemplate attachTrinketFixer(TypeTemplate original) {
        return DSL.allWithRemainder(original, DSL.optionalFields("cardinal_components",
                DSL.optionalFields("trinkets:trinkets", DSL.optional(DSL.compoundList(
                        DSL.optional(DSL.compoundList(DSL.optionalFields("Items", DSL.list(TypeReferences.ITEM_STACK.in(schema)))))
                )))));
    }
}
