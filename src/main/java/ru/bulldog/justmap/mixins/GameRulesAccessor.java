package ru.bulldog.justmap.mixins;

import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRules.class)
public interface GameRulesAccessor {
	@Invoker
	static <T extends GameRules.Rule<T>> GameRules.Key<T> callRegister(String name, GameRules.Category category, GameRules.Type<T> type) {
		throw new AssertionError("@Invoker dummy body called");
	}
}
