package com.ronaldo.survivalneeds;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.server.level.ServerPlayer;

/**
 * Quais efeitos (frio, medo, sede) estão ativos no jogador. O servidor calcula e sincroniza
 * com o cliente, que usa isso para mostrar os ícones no canto superior direito.
 */
public final class StatusFlags {
	public static final int COLD = 1;
	public static final int FEAR = 2;
	public static final int THIRST = 4;

	public static final AttachmentType<Integer> FLAGS = AttachmentRegistry.<Integer>create(
		SurvivalNeeds.id("status_flags"),
		builder -> builder
			.initializer(() -> 0)
			.syncWith(ByteBufCodecs.VAR_INT, AttachmentSyncPredicate.targetOnly()));

	private StatusFlags() {}

	public static void init() {
		// força o carregamento da classe (registra o attachment)
	}

	/** Recalcula as flags do jogador e só sincroniza quando algo muda. */
	public static void update(ServerPlayer player) {
		int flags = (ColdSystem.isFreezing(player) ? COLD : 0)
			| (FearSystem.isAfraid(player) ? FEAR : 0)
			| (ThirstSystem.isTooThirstyToSprint(player) ? THIRST : 0);
		if (player.getAttachedOrElse(FLAGS, 0) != flags) {
			player.setAttached(FLAGS, flags);
		}
	}
}
