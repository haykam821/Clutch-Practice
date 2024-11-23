package io.github.haykam821.clutchpractice.clutch;

import java.util.Set;
import java.util.function.Consumer;

import io.github.haykam821.clutchpractice.TrackedBlockStateProvider;
import io.github.haykam821.clutchpractice.game.map.ClutchPracticeMap;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.random.Random;

public abstract class ClutchType {
	private final ItemStack icon;
	private Text name;

	public ClutchType(ItemStack icon) {
		this.icon = icon;
	}

	public final ItemStack getIcon() {
		return this.icon;
	}

	protected Text createName() {
		Identifier id = ClutchTypes.REGISTRY.getIdentifier(this);
		return Text.translatable(Util.createTranslationKey("clutchType", id));
	}

	public final Text getName() {
		if (this.name == null) {
			this.name = this.createName();
		}

		return this.name;
	}

	public ClutchType resolve(Random random) {
		return this;
	}

	public abstract void addItems(Consumer<ItemStack> adder, Set<BlockState> floor, Set<BlockState> base, RegistryWrapper.WrapperLookup registries);

	public abstract void clearArea(ServerWorld world, ClutchPracticeMap map, TrackedBlockStateProvider floor, TrackedBlockStateProvider base);
}
