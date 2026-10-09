package com.ronaldo.survivalneeds;

import java.util.function.BiConsumer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.ronaldo.survivalneeds.config.SurvivalNeedsConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Comandos para ligar ou desligar cada efeito à mão: /cold, /fear, /thirst, /wet e /hunger, seguidos de true ou false. */
public final class StatusCommands {
	private StatusCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		registerReload(dispatcher);
		registerToggle(dispatcher, "cold", "frio", ColdSystem::setForced);
		registerToggle(dispatcher, "fear", "medo", FearSystem::setForced);
		registerToggle(dispatcher, "thirst", "sede", ThirstSystem::setActive);
		registerToggle(dispatcher, "wet", "molhado", WetSystem::setForced);
		registerToggle(dispatcher, "hunger", "fome", HungerSystem::setActive);
		registerToggle(dispatcher, "fatigue", "cansaço", FatigueSystem::setActive);
	}

	/** /survivalneeds reload: relê config/survivalneeds.json sem reiniciar o jogo. */
	private static void registerReload(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("survivalneeds")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("reload").executes(context -> {
				SurvivalNeedsConfig.load();
				context.getSource().sendSuccess(() -> Component.literal("Configuração recarregada."), true);
				return 1;
			})));
	}

	private static void registerToggle(CommandDispatcher<CommandSourceStack> dispatcher, String command, String label,
			BiConsumer<ServerPlayer, Boolean> action) {
		dispatcher.register(Commands.literal(command)
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.argument("ativo", BoolArgumentType.bool())
				.executes(context -> {
					ServerPlayer player = context.getSource().getPlayerOrException();
					boolean active = BoolArgumentType.getBool(context, "ativo");
					action.accept(player, active);
					context.getSource().sendSuccess(
						() -> Component.literal(label + (active ? " ativado" : " desativado")), false);
					return 1;
				})));
	}
}
