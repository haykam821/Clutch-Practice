package io.github.haykam821.clutchpractice;

import io.github.haykam821.clutchpractice.game.ClutchPracticeConfig;
import io.github.haykam821.clutchpractice.game.ClutchPracticeGame;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.api.game.GameType;

public class Main implements ModInitializer {
	private static final String MOD_ID = "clutchpractice";

	private static final Identifier CLUTCH_PRACTICE_ID = Main.identifier("clutch_practice");
	public static final GameType<ClutchPracticeConfig> CLUTCH_PRACTICE_TYPE = GameType.register(CLUTCH_PRACTICE_ID, ClutchPracticeConfig.CODEC, ClutchPracticeGame::open);

	@Override
	public void onInitialize() {
		return;
	}

	public static Identifier identifier(String path) {
		return Identifier.of(MOD_ID, path);
	}
}
