package io.github.haykam821.clutchpractice.clutch;

import java.util.ArrayList;
import java.util.List;

import io.github.haykam821.clutchpractice.Main;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.registry.TinyRegistry;

public final class ClutchTypes {
	public static final TinyRegistry<ClutchType> REGISTRY = TinyRegistry.create();

	public static final ClutchType RANDOM = register("random", new RandomClutchType());
	public static final ClutchType LADDER = register("ladder", new LadderClutchType());

	private ClutchTypes() {
		return;
	}

	public static ClutchType getNext(ClutchType type) {
		List<ClutchType> types = new ArrayList<>(REGISTRY.values());

		int index = types.indexOf(type);
		return types.get((index + 1) % types.size());
	}

	private static ClutchType register(String path, ClutchType type) {
		Identifier id = new Identifier(Main.MOD_ID, path);
		REGISTRY.register(id, type);

		return type;
	}
}
