package dev.emi.trinkets.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.emi.trinkets.TrinketSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.collection.DefaultedList;

/***
 * Patches insertItem to support slots that limit the number of items inserted.
 * This mixin is set up to run after most normal mods and to fail-soft
 * if injection points are relocated or removed (unlikely).
 *
 * Since failer to inject this mixin results in very minor behavioural differences, errors here are considered 'tolerable'.
 *
 * @author Sollace
 */
@Mixin(
    value = ScreenHandler.class,
    priority = 2000 // run after other mods
)
abstract class ScreenHandlerMixin {
	@Nullable
	private Slot currentSlot;

	@Redirect(method = "insertItem",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/util/collection/DefaultedList;get(I)Ljava/lang/Object;"
			),
			require = 0
	)
	// manual capture of the current slot since @Redirect doesn't support local captures
	private Object onGetSlot(DefaultedList<Slot> sender, int index) {
		currentSlot = sender.get(index);
		return currentSlot;
	}

	@Redirect(method = "insertItem",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/screen/slot/Slot;getMaxItemCount()I"
			),
            require = 0
	)
	// redirect slot.getMaxItemCount() to stack aware version
	private int onGetMaxItemCount(Slot sender, ItemStack stack) {
		return mustChangeBehaviour(sender) ? sender.getMaxItemCount(stack) : sender.getMaxItemCount();
	}

	@Redirect(method = "insertItem",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/item/ItemStack;isEmpty()Z",
					ordinal = 1
			),
            require = 0
	)
	// redirect "if (!itemStack.isEmpty() && ItemStack.canCombine(stack, itemStack))" -> "if (!canNotInsert(itemStack, slot) && ItemStack.canCombine(stack, itemStack))"
	private boolean canNotInsert(ItemStack sender) {
		return sender.isEmpty() || (mustChangeBehaviour(currentSlot) && (sender.getCount() + currentSlot.getStack().getCount()) >= currentSlot.getMaxItemCount(sender));
	}

	private static boolean mustChangeBehaviour(Slot slot) {
	    // avoid changing vanilla behaviour
	    // TODO: consider adding a config so people can choose whether they want this globally enabled or not
	    //       as it would very likely fix a number of vanilla bugs
	    return slot instanceof TrinketSlot;
	}
}