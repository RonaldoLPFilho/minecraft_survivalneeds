package com.ronaldo.survivalneeds;

import java.util.function.BiConsumer;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.SharedSuggestionProvider;
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
		registerCurse(dispatcher);
	}

	/** /curse give <tipo>, /curse roll e /curse clear: para testar a maldição. */
	private static void registerCurse(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("curse")
			.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
			.then(Commands.literal("give")
				.then(Commands.argument("tipo", StringArgumentType.word())
					.suggests((context, builder) -> SharedSuggestionProvider.suggest(
						java.util.Arrays.stream(Curse.values()).map(curse -> curse.id), builder))
					.executes(context -> {
						Curse curse = Curse.byId(StringArgumentType.getString(context, "tipo"));
						if (curse == null) {
							context.getSource().sendFailure(Component.literal("Maldição desconhecida."));
							return 0;
						}
						Curses.give(context.getSource().getPlayerOrException(), curse);
						return 1;
					})))
			.then(Commands.literal("roll").executes(context -> {
				Curse curse = Curses.roll();
				if (curse == null) {
					context.getSource().sendFailure(Component.literal("Todos os pesos estão em zero."));
					return 0;
				}
				Curses.give(context.getSource().getPlayerOrException(), curse);
				return 1;
			}))
			.then(Commands.literal("clear").executes(context -> {
				Curses.clear(context.getSource().getPlayerOrException(), false);
				context.getSource().sendSuccess(() -> Component.literal("Maldição removida."), false);
				return 1;
			})));
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
