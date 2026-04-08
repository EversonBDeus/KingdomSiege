package com.eversonbdeus.kingdomsiege.entity;

import com.eversonbdeus.kingdomsiege.soldier.SoldierIdentityData;
import com.eversonbdeus.kingdomsiege.soldier.SoldierRank;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * [REFATORAÇÃO] Helper de progressão militar do soldado.
 *
 * Mantém fora da entidade base a lógica de XP, promoção de rank e
 * notificação ao dono, sem alterar o comportamento já validado.
 */
final class CastleSoldierProgression {
	private static final int NORMAL_KILL_XP = 5;
	private static final int ELITE_KILL_XP = 15;
	private static final float ELITE_HEALTH_THRESHOLD = 40.0F;
	private static final int ELITE_ARMOR_THRESHOLD = 10;

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
		}
	}

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
