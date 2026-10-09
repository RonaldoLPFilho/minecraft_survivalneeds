package com.ronaldo.survivalneeds.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.ronaldo.survivalneeds.SurvivalNeeds;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Configuração do mod, lida de {@code config/survivalneeds.json}.
 * <p>
 * Todos os tempos ficam em <b>segundos</b>, distâncias em <b>blocos</b> e vida em <b>pontos</b> (2 pontos = 1 coração).
 * Se o arquivo não existir, é criado com os valores padrão. Campos que faltarem usam o padrão, e o arquivo é
 * reescrito a cada carga para mostrar as opções novas. Para aplicar mudanças sem reiniciar o jogo, use
 * {@code /survivalneeds reload}.
 * <p>
 * O código sempre lê os valores por {@link #get()} na hora de usar, nunca guarda cópias, para o reload valer na hora.
 */
public final class SurvivalNeedsConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static SurvivalNeedsConfig instance = new SurvivalNeedsConfig();

	public Thirst thirst = new Thirst();
	public Cold cold = new Cold();
	public Fear fear = new Fear();
	public Wet wet = new Wet();
	public Hunger hunger = new Hunger();
	public Fatigue fatigue = new Fatigue();
	public Purifier purifier = new Purifier();

	/** Sede. A barra tem sempre 20 pontos (10 gotas). */
	public static final class Thirst {
		public int noSprintAtOrBelow = 5;               // com este valor ou menos o jogador não corre
		public double secondsPerPointIdle = 125.0;      // parado, 1 ponto a cada tanto
		public double secondsPerPointSprinting = 40.0;  // correndo, 1 ponto a cada tanto
		public double sprintRampSeconds = 20.0;         // tempo correndo até o gasto chegar ao máximo
		public double sprintMaxBoost = 2.0;             // aceleração máxima: 2 = gasta 3x o gasto da corrida normal
		public double dryDamageIntervalSeconds = 4.0;   // com 0 pontos, de quanto em quanto tempo leva dano
		public double dryDamage = 1.0;                  // dano com 0 pontos de sede
		public double dryMinHealth = 2.0;               // a sede não tira vida abaixo disto (não mata)
		public double dirtyWaterDamage = 6.0;           // dano ao beber água suja (6 = 3 corações)
		public double dirtyWaterNauseaSeconds = 10.0;   // náusea ao beber água suja
		public int purifiedWaterRestores = 6;           // pontos de sede da água purificada
	}

	/** Frio de bioma de neve e frio da noite. */
	public static final class Cold {
		public double biomeDamage = 1.0;                // dano por segundo em bioma de neve, sem armadura
		public double nightExposureSeconds = 600.0;     // exposição à noite até o frio chegar
		public double nightShelterRecovery = 2.0;       // abrigado ou de armadura, a exposição some tantas vezes mais rápido
		public double nightDamage = 1.0;                // dano do frio da noite...
		public double nightDamageIntervalSeconds = 10.0; // ...a cada tantos segundos
		public double nightMinHealth = 2.0;             // o frio da noite não tira vida abaixo disto (não mata)
	}

	/** Medo. Os "níveis de arma" são: 1 madeira/ouro, 2 pedra/cobre, 3 ferro, 4 diamante/maça/tridente, 5 netherita. */
	public static final class Fear {
		public double damageReduction = 0.5;            // 0.5 = 50% menos dano
		public double minFearSeconds = 3.0;             // duração mínima depois de ativado

		public double crowdRadius = 8.0;                // "cercado"
		public double strongRadius = 16.0;              // monstro forte
		public double bossRadius = 24.0;                // chefes
		public double minThreatHealth = 8.0;            // inimigos com menos vida não contam como ameaça
		public double strongMinHealth = 40.0;           // vida a partir da qual o monstro é "forte"
		public int strongWeight = 3;                    // um monstro forte conta como tantos inimigos

		public int surroundedFew = 3;                   // cercado por poucos...
		public int fewMinWeaponTier = 3;                // ...com arma abaixo deste nível
		public int surroundedMany = 10;                 // cercado por horda...
		public int manyMinWeaponTier = 4;               // ...com arma abaixo deste nível

		public int braveXpLevel = 15;                   // monstro forte: sem medo com este nível de XP...
		public int braveArmorValue = 10;                // ...ou esta armadura (precisa dos dois para dispensar o medo)
		public int bossBraveXpLevel = 30;               // chefe: nível de XP...
		public int bossBraveArmorValue = 15;            // ...e armadura que dispensam o medo

		public double lastHeartHealth = 2.0;            // 1 coração: medo sempre
		public double severeHealth = 4.0;               // 2 corações: medo se houver qualquer inimigo na região
		public double severeRadius = 50.0;              // tamanho dessa região
		public double lowHealth = 6.0;                  // 3 corações: medo com inimigos por perto
		public int lowHealthEnemies = 2;                // quantos inimigos contam para a regra acima

		public int darkLightLevel = 4;                  // nível de luz considerado escuro
		public int darkEnemies = 2;                     // inimigos para a regra do escuro...
		public int darkMinWeaponTier = 2;               // ...com arma abaixo deste nível
	}

	/** Molhado. */
	public static final class Wet {
		public double coveredDrySeconds = 120.0;        // tempo para secar em lugar coberto
		public double openAirDrySeconds = 240.0;        // tempo para secar ao relento
		public double heatDryMultiplier = 2.5;          // perto de uma fonte de calor a contagem passa tantas vezes mais rápido
		public int heatRadius = 2;                      // distância para contar como "perto", nas 4 direções
		public int heatBelow = 1;                       // blocos abaixo dos pés
		public int heatAbove = 2;                       // blocos acima dos pés
		public double sprintLimitSeconds = 15.0;        // corre no máximo tanto tempo seguido...
		public double sprintCooldownSeconds = 30.0;     // ...e depois fica tanto tempo sem poder correr
		public double sprintGraceSeconds = 1.0;         // parar por menos que isso não zera a contagem
		public int placeLimit = 5;                      // blocos seguidos que pode colocar...
		public double placeResetSeconds = 5.0;          // ...e a espera depois disso
	}

	/** Fome. A comida do jogo vale 20 pontos (10 ícones de carne de 2 pontos cada). */
	public static final class Hunger {
		public double starveSeconds = 600.0;            // tempo, com a carne zerada, até morrer de fome
		public int recoverFoodLevel = 10;               // pontos de comida (de 20) para sair da fome: 10 = metade da barra
		public double healIntervalSeconds = 1.0;        // cura 1 "passo" a cada tantos segundos...
		public double healAmount = 1.0;                 // ...de tantos pontos de vida, gastando 1 ponto de comida
		public double slowdown = 0.5;                   // faminto anda tanto mais devagar: 0.5 = metade da velocidade
		public int placeLimit = 5;                      // blocos seguidos que pode colocar...
		public double placeResetSeconds = 5.0;          // ...e a espera depois disso
	}

	/** Cansaço: noites sem dormir. */
	public static final class Fatigue {
		public int nightsWithoutSleep = 2;             // noites seguidas sem dormir para ficar cansado
		public double counterSeconds = 300.0;          // tempo, depois de cansado, para dormir antes de perder vida
		public double damageReduction = 0.3;           // cansado causa tanto menos dano (0.3 = 30%)
		public boolean blockSprint = false;            // true: não corre de jeito nenhum; false: corre por um tempo e descansa
		public double sprintLimitSeconds = 10.0;       // (se blockSprint = false) corre no máximo tanto tempo seguido...
		public double sprintCooldownSeconds = 30.0;    // ...e depois fica tanto tempo sem poder correr
		public double sprintGraceSeconds = 1.0;        // parar de correr por menos que isso não zera a contagem
		public int placeLimit = 5;                     // blocos seguidos que pode colocar...
		public double placeResetSeconds = 5.0;         // ...e a espera depois disso
		public double damage = 1.0;                    // depois do contador, perde tanta vida...
		public double damageIntervalSeconds = 5.0;     // ...a cada tantos segundos, até dormir
		public double minHealth = 2.0;                 // o cansaço não tira vida abaixo disto (0 = pode matar)
	}

	/** Purificador de água. */
	public static final class Purifier {
		public int usesPerCoal = 5;                     // garrafas que 1 carvão purifica
		public double purifySeconds = 4.0;              // tempo para purificar 1 garrafa
	}

	public static SurvivalNeedsConfig get() {
		return instance;
	}

	/** Converte segundos em ticks do jogo (20 por segundo), com no mínimo 1. */
	public static int ticks(double seconds) {
		return Math.max(1, (int) Math.round(seconds * 20.0));
	}

	/** Lê o arquivo (criando-o se faltar) e o reescreve com todas as opções. Em caso de erro, mantém os padrões. */
	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("survivalneeds.json");
		SurvivalNeedsConfig loaded = null;

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				loaded = GSON.fromJson(reader, SurvivalNeedsConfig.class);
			} catch (IOException | JsonParseException e) {
				SurvivalNeeds.LOGGER.error("Não foi possível ler {}. Usando os valores padrão.", path, e);
			}
		}
		if (loaded == null) {
			loaded = new SurvivalNeedsConfig();
		}
		loaded.fillMissingSections();
		instance = loaded;

		try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
			GSON.toJson(loaded, writer);
		} catch (IOException e) {
			SurvivalNeeds.LOGGER.error("Não foi possível escrever {}.", path, e);
		}
	}

	/** Se o arquivo trouxer uma seção como null, volta ao padrão dela. */
	private void fillMissingSections() {
		if (thirst == null) thirst = new Thirst();
		if (cold == null) cold = new Cold();
		if (fear == null) fear = new Fear();
		if (wet == null) wet = new Wet();
		if (hunger == null) hunger = new Hunger();
		if (fatigue == null) fatigue = new Fatigue();
		if (purifier == null) purifier = new Purifier();
	}
}
