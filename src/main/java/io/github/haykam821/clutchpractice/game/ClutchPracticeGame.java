package io.github.haykam821.clutchpractice.game;

import io.github.haykam821.clutchpractice.game.map.ClutchPracticeMap;
import io.github.haykam821.clutchpractice.game.map.ClutchPracticeMapBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.GameRules;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameLogic;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.PlaceBlockListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDamageListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.PlayerRemoveListener;
import xyz.nucleoid.plasmid.game.event.UseBlockListener;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public class ClutchPracticeGame implements GameTickListener, PlaceBlockListener, PlayerAddListener, PlayerDamageListener, PlayerDeathListener, PlayerRemoveListener, UseBlockListener {
	private final GameSpace gameSpace;
	private final ClutchPracticeMap map;
	private final ClutchPracticeConfig config;

	private ServerPlayerEntity mainPlayer;
	private int streak = 0;
	private int maxStreak = 0;

	public ClutchPracticeGame(GameSpace gameSpace, ClutchPracticeMap map, ClutchPracticeConfig config) {
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
	}

	public static void setRules(GameLogic game) {
		game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
		game.setRule(GameRule.BREAK_BLOCKS, RuleResult.DENY);
		game.setRule(GameRule.CRAFTING, RuleResult.DENY);
		game.setRule(GameRule.DISMOUNT_VEHICLE, RuleResult.DENY);
		game.setRule(GameRule.FALL_DAMAGE, RuleResult.ALLOW);
		game.setRule(GameRule.HUNGER, RuleResult.DENY);
		game.setRule(GameRule.INTERACTION, RuleResult.ALLOW);
		game.setRule(GameRule.PLACE_BLOCKS, RuleResult.ALLOW);
		game.setRule(GameRule.PLAYER_PROJECTILE_KNOCKBACK, RuleResult.DENY);
		game.setRule(GameRule.PORTALS, RuleResult.DENY);
		game.setRule(GameRule.PVP, RuleResult.DENY);
		game.setRule(GameRule.TEAM_CHAT, RuleResult.DENY);
		game.setRule(GameRule.THROW_ITEMS, RuleResult.DENY);
		game.setRule(GameRule.TRIDENTS_LOYAL_IN_VOID, RuleResult.DENY);
		game.setRule(GameRule.UNSTABLE_TNT, RuleResult.DENY);
	}

	public static GameOpenProcedure open(GameOpenContext<ClutchPracticeConfig> context) {
		ClutchPracticeConfig config = context.getConfig();
		ClutchPracticeMap map = new ClutchPracticeMapBuilder(config.getMapConfig()).build();

		BubbleWorldConfig worldConfig = new BubbleWorldConfig()
			.setGenerator(map.createGenerator(context.getServer()))
			.setDefaultGameMode(GameMode.ADVENTURE)
			.setGameRule(GameRules.DO_TILE_DROPS, false);

		return context.createOpenProcedure(worldConfig, game -> {
			ClutchPracticeGame phase = new ClutchPracticeGame(game.getSpace(), map, config);
			ClutchPracticeGame.setRules(game);

			// Listeners
			game.on(GameTickListener.EVENT, phase);
			game.on(PlaceBlockListener.EVENT, phase);
			game.on(PlayerAddListener.EVENT, phase);
			game.on(PlayerDamageListener.EVENT, phase);
			game.on(PlayerDeathListener.EVENT, phase);
			game.on(PlayerRemoveListener.EVENT, phase);
			game.on(UseBlockListener.EVENT, phase);
		});
	}

	// Listeners
	public void onTick() {
		if (this.mainPlayer == null) return;

		if (this.map.getExit().contains(this.mainPlayer.getPos())) {
			this.gameSpace.removePlayer(this.mainPlayer);
		}
		this.map.respawnIfOutOfBounds(this.mainPlayer);

		if (this.mainPlayer.isOnGround() && this.mainPlayer.getY() == this.map.getArea().getMin().getY() + 1) {
			this.resetAndUpdateStreak(ActionResult.SUCCESS);
		}
	}

	public void onAddPlayer(ServerPlayerEntity player) {
		if (this.mainPlayer == null) {
			this.mainPlayer = player;
			this.reset();
		} else {
			player.setGameMode(GameMode.SPECTATOR);
		}
	}

	public ActionResult onDamage(ServerPlayerEntity player, DamageSource source, float damage) {
		if (player == this.mainPlayer) {
			this.resetAndUpdateStreak(ActionResult.FAIL);
		}
		return ActionResult.FAIL;
	}

	public ActionResult onPlace(ServerPlayerEntity player, BlockPos pos, BlockState state, ItemUsageContext context) {
		return this.getAreaPosResult(pos);
	}

	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		if (player == this.mainPlayer) {
			this.resetAndUpdateStreak(ActionResult.FAIL);
		}
		return ActionResult.FAIL;
	}

	public void onRemovePlayer(ServerPlayerEntity player) {
		if (player == this.mainPlayer) {
			this.gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	public ActionResult onUseBlock(ServerPlayerEntity player, Hand hand, BlockHitResult hitResult) {
		return this.getAreaPosResult(hitResult.getBlockPos());
	}

	// Utilities
	private void resetAndUpdateStreak(ActionResult result) {
		map.spawn(this.mainPlayer);

		if (result == ActionResult.SUCCESS) {
			this.streak += 1;
			if (this.streak <= this.maxStreak) {
				this.sendSound(SoundEvents.ENTITY_VILLAGER_YES);
			} else {
				this.maxStreak = this.streak;
				this.sendSound(SoundEvents.ENTITY_PLAYER_LEVELUP);
			}
		} else if (result == ActionResult.FAIL) {
			this.sendSound(SoundEvents.ENTITY_VILLAGER_NO);
			this.streak = 0;
		}

		this.reset();
	}

	private void sendSound(SoundEvent sound) {
		this.gameSpace.getPlayers().sendSound(sound, SoundCategory.PLAYERS, 1, 1);
	}

	private void reset() {
		map.spawn(this.mainPlayer);
		BlockState baseState = map.clearArea(this.gameSpace.getWorld());

		ItemStack ladder = ItemStackBuilder.of(Items.LADDER)
			.addCanPlaceOn(baseState.getBlock())
			.build();

		this.mainPlayer.inventory.clear();
		this.mainPlayer.inventory.offerOrDrop(this.gameSpace.getWorld(), ladder);

		this.mainPlayer.setExperienceLevel(this.streak);
	}

	private ActionResult getAreaPosResult(BlockPos pos) {
		if (this.map.getArea().contains(pos)) {
			return ActionResult.PASS;
		}
		return ActionResult.FAIL;
	}
}
