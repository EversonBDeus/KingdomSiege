package com.eversonbdeus.kingdomsiege.entity;

import com.eversonbdeus.kingdomsiege.soldier.SoldierIdentityData;
import com.eversonbdeus.kingdomsiege.soldier.SoldierRank;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * [REFATORAÇÃO] Helper de progressão militar do soldado.
 *
 * Mantém fora da entidade base a lógica de XP, promoção de rank e
 * notificação ao dono, sem alterar o comportamento já validado.
 */
final class CastleSoldierProgression {
	private static final int NORMAL_KILL_XP = 10;
	private static final int ELITE_KILL_XP = 25;
	private static final float ELITE_HEALTH_THRESHOLD = 40.0F;
	private static final int ELITE_ARMOR_THRESHOLD = 10;

	// ─── Rank-up: cura + invulnerabilidade ───────────────────────────────────

	/**
	 * Duração em ticks do efeito de rank-up: 10 segundos × 20 t/s = 200 ticks.
	 */
	private static final int RANK_UP_EFFECT_DURATION_TICKS = 200;

	/**
	 * Marcador visual exclusivo do rank-up.
	 *
	 * O client deve usar este amplificador junto com MobEffects.RESISTANCE
	 * para decidir quando renderizar a animação estilo creeper charged.
	 */
	static final int RANK_UP_VISUAL_MARKER_AMPLIFIER = 39;

	private final CastleSoldierEntity soldier;

	CastleSoldierProgression(CastleSoldierEntity soldier) {
		this.soldier = soldier;
	}

	void handleKill(ServerLevel level, LivingEntity killedEntity) {
		int xpGain = resolveXpGainFor(killedEntity);
		if (xpGain <= 0) {
			return;
		}

		SoldierRank rankBefore = soldier.getSoldierRank();
		SoldierIdentityData updatedIdentity = soldier.getSoldierIdentity()
				.earnXp(xpGain)
				.withKill();

		soldier.setSoldierIdentity(updatedIdentity);
		soldier.clearBattleRegistrationForNextCombat();

		SoldierRank rankAfter = soldier.getSoldierRank();
		if (rankAfter.ordinal() > rankBefore.ordinal()) {
			notifyOwnerOfPromotion(level, rankAfter);
			applyRankUpEffect();
		}
	}

	// ─── Efeito de rank-up ────────────────────────────────────────────────────

	/**
	 * Ao subir de rank, o soldado:
	 * <ol>
	 *   <li>Tem a vida restaurada a 100 %.</li>
	 *   <li>Recebe {@link MobEffects#RESISTANCE} com amplificador especial
	 *       de marcador visual por {@value #RANK_UP_EFFECT_DURATION_TICKS}
	 *       ticks (10 s).</li>
	 * </ol>
	 *
	 * Executado somente no servidor; o efeito é sincronizado automaticamente
	 * com o cliente pelo Minecraft.
	 */
	private void applyRankUpEffect() {
		// 1. Cura completa.
		soldier.setHealth(soldier.getMaxHealth());

		// 2. Marca visual sincronizada para o client.
		soldier.triggerVisualRankUpAnimation(RANK_UP_EFFECT_DURATION_TICKS);

		// 3. Invulnerabilidade de 10 s com marcador de compatibilidade.
		soldier.addEffect(new MobEffectInstance(
				MobEffects.RESISTANCE,
				RANK_UP_EFFECT_DURATION_TICKS,
				RANK_UP_VISUAL_MARKER_AMPLIFIER,
				/* ambient   */ false,
				/* particles */ false,
				/* icon      */ true
		));
	}

	// ─── Internos ─────────────────────────────────────────────────────────────

	private int resolveXpGainFor(LivingEntity killed) {
		if (killed == null) {
			return 0;
		}
		if (killed instanceof CastleSoldierEntity) {
			return 0;
		}
		if (killed instanceof Player) {
			return 0;
		}

		boolean isElite = killed.getMaxHealth() >= ELITE_HEALTH_THRESHOLD
				|| killed.getArmorValue() >= ELITE_ARMOR_THRESHOLD;
		return isElite ? ELITE_KILL_XP : NORMAL_KILL_XP;
	}

	private void notifyOwnerOfPromotion(ServerLevel level, SoldierRank newRank) {
		Player owner = soldier.getValidOwnerPlayer();
		if (owner == null) {
			return;
		}

		owner.sendSystemMessage(Component.translatable(
				"message.kingdomsiege.soldier_rank_up",
				soldier.getSoldierDisplayName(),
				Component.translatable(newRank.getTranslationKey())
		));
	}
}