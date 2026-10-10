package com.ronaldo.survivalneeds.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	public Progression progression = new Progression();
	public Curse curse = new Curse();

	/** Maldição: debuff aleatório. Pesos maiores sorteiam mais; peso 0 desliga aquela maldição. */
	public static final class Curse {
		public boolean enabled = true;
		public double immunityMinutes = 10.0;           // depois de curado ou de morrer, ninguém é amaldiçoado por tanto tempo
		public List<Integer> warnMinutes = new ArrayList<>(List.of(10, 5, 1)); // avisos do contador (minutos que faltam)
		public double fragileHealthLoss = 0.3;          // Fragilidade: vida máxima perdida
		public double halfLifeHealthLoss = 0.7;         // Vida em ruínas: vida máxima perdida
		public double heavyBodySlowdown = 0.35;         // Corpo de chumbo: velocidade perdida
		public double paperSkinExtraDamage = 0.5;       // Pele de papel: dano recebido a mais (0.5 = +50%)
		public double blindHandsSlowdown = 0.7;         // Mãos cegas: velocidade de mineração perdida
		public double parchedThirstMultiplier = 2.0;    // Ressecado: a sede gasta tantas vezes mais rápido
		public double parchedHungerPerSecond = 0.08;    // Ressecado: cansaço de comida extra por segundo (4.0 = 1 ponto)
		public int buildLimit = 3;                      // Mãos trêmulas: blocos seguidos que pode colocar...
		public double buildResetSeconds = 8.0;          // ...e a espera depois disso
		public double drowningMinutes = 20.0;           // Afogamento: tempo até começar a se afogar
		public double drowningDamage = 2.0;             // dano de afogamento...
		public double drowningIntervalSeconds = 1.0;    // ...a cada tantos segundos
		public double doomMinutes = 30.0;               // Sentença: tempo até morrer
		public double explosionMinutes = 30.0;          // Explosão: tempo até explodir
		public float explosionPower = 10.0f;            // 10 dá dano num raio de uns 20 blocos
		public boolean explosionBreaksBlocks = false;   // false: só dano e susto, sem destruir blocos
		public Map<String, Integer> weights = defaultWeights();

		// Princess Brubin: preço da cura em pontos, por gravidade, e quanto vale cada coisa que se oferece a ela
		public Map<String, Integer> brubinPrice = defaultPrices();             // light, medium, grave
		public Map<String, Integer> brubinPriceOverrides = new LinkedHashMap<>(); // preço por id de maldição, ex.: "doom": 800
		public int brubinXpLevelValue = 20;             // pontos por nível de XP
		public int brubinFragmentValue = 80;            // pontos por Fragmento Amaldiçoado
		public double brubinAmuletFactor = 0.6;         // amuleto vale este fator x (8 minérios + coração)
		public Map<String, Integer> brubinOreValue = defaultOreValues();       // pontos por item de minério
		public Map<String, Integer> brubinHeartValue = defaultHeartValues();   // pontos por coração, por raridade
		public double brubinWildChancePerChunk = 0.002; // chance de uma Brubin solitária por chunk novo (fora de vilas)

		static Map<String, Integer> defaultPrices() {
			Map<String, Integer> map = new LinkedHashMap<>();
			map.put("light", 100);
			map.put("medium", 250);
			map.put("grave", 500);
			return map;
		}

		static Map<String, Integer> defaultOreValues() {
			Map<String, Integer> map = new LinkedHashMap<>();
			map.put("minecraft:coal", 2);
			map.put("minecraft:copper_ingot", 5);
			map.put("minecraft:redstone", 4);
			map.put("minecraft:lapis_lazuli", 4);
			map.put("minecraft:iron_ingot", 8);
			map.put("minecraft:gold_ingot", 20);
			map.put("minecraft:emerald", 70);
			map.put("minecraft:diamond", 100);
			map.put("minecraft:netherite_ingot", 400);
			return map;
		}

		static Map<String, Integer> defaultHeartValues() {
			Map<String, Integer> map = new LinkedHashMap<>();
			map.put("common", 12);
			map.put("uncommon", 25);
			map.put("rare", 60);
			map.put("epic", 150);
			map.put("legendary", 400);
			return map;
		}

		// Espectro da Maldição (vida e dano ficam no código; o ritmo do combate é aqui)
		public int specterTeleportCooldownTicks = 100;  // depois de se teletransportar, fica tantos ticks sem poder de novo (100 = 5 s): a abertura para bater
		public int specterAimTicks = 12;                // ao reaparecer, mira por tantos ticks antes de atirar
		public int specterVanishTicks = 8;              // tempo invisível entre sumir e reaparecer
		public double specterTeleportMinDistance = 6.0; // reaparece a esta distância do alvo, no mínimo...
		public double specterTeleportMaxDistance = 10.0; // ...e no máximo
		public double specterVolleyBelowHealth = 0.5;   // abaixo desta fração de vida, atira 2 dardos
		public double specterDartSpeed = 1.6;           // velocidade do dardo

		static Map<String, Integer> defaultWeights() {
			Map<String, Integer> map = new LinkedHashMap<>();
			for (com.ronaldo.survivalneeds.Curse curse : com.ronaldo.survivalneeds.Curse.values()) {
				map.put(curse.id, curse.defaultWeight);
			}
			return map;
		}
	}

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

	/** Progressão por XP: o nível do jogador libera equipamento forte. */
	public static final class Progression {
		public boolean enabled = true;                  // false desliga toda a progressão
		public Equipment equipment = new Equipment();
		public Death death = new Death();
		public DebuffXp debuffXp = new DebuffXp();
		public Hearts hearts = new Hearts();
		public Amulets amulets = new Amulets();
	}

	/** Amuletos: 8 minérios + 1 coração dão um bônus de XP. Os 2 espaços vêm de data/trinkets/slots/chest/amulet.json. */
	public static final class Amulets {
		public boolean enabled = true;
		public double maxTotalBonus = 2.0;              // soma máxima dos amuletos equipados (2.0 = +200%)
		public Map<String, Double> oreBonus = defaultOres(); // id do item do minério -> bônus (0.5 = +50%)

		static Map<String, Double> defaultOres() {
			Map<String, Double> map = new LinkedHashMap<>();
			map.put("minecraft:coal", 0.04);
			map.put("minecraft:copper_ingot", 0.08);
			map.put("minecraft:redstone", 0.12);
			map.put("minecraft:lapis_lazuli", 0.12);
			map.put("minecraft:iron_ingot", 0.15);
			map.put("minecraft:gold_ingot", 0.25);
			map.put("minecraft:emerald", 0.35);
			map.put("minecraft:diamond", 0.50);
			map.put("minecraft:netherite_ingot", 0.80);
			return map;
		}
	}

	/** Coração de monstro: chance de cair, XP ao consumir e bônus no amuleto, por raridade. */
	public static final class Hearts {
		public boolean enabled = true;
		public double lootingBonus = 0.02;              // chance extra por nível de Pilhagem (0.02 = +2%)
		public int pityKills = 30;                      // depois de tantas mortes sem coração, a próxima dá um (0 desliga)
		public Map<String, HeartTier> tiers = defaultTiers();
		public Map<String, String> monsters = defaultMonsters(); // id do monstro -> raridade

		static Map<String, HeartTier> defaultTiers() {
			Map<String, HeartTier> map = new LinkedHashMap<>();
			map.put("common", new HeartTier(0.06, 10, 0.03));
			map.put("uncommon", new HeartTier(0.08, 20, 0.06));
			map.put("rare", new HeartTier(0.12, 50, 0.15));
			map.put("epic", new HeartTier(1.0, 200, 0.60));
			map.put("legendary", new HeartTier(1.0, 500, 1.00));
			return map;
		}

		static Map<String, String> defaultMonsters() {
			Map<String, String> map = new LinkedHashMap<>();
			for (String id : new String[] {"zombie", "skeleton", "spider", "creeper", "slime"}) {
				map.put("minecraft:" + id, "common");
			}
			for (String id : new String[] {"cave_spider", "drowned", "husk", "stray", "witch", "pillager", "phantom"}) {
				map.put("minecraft:" + id, "uncommon");
			}
			for (String id : new String[] {"enderman", "blaze", "ghast", "guardian", "wither_skeleton", "piglin_brute",
					"vindicator", "evoker", "hoglin", "shulker", "ravager"}) {
				map.put("minecraft:" + id, "rare");
			}
			for (String id : new String[] {"warden", "wither", "elder_guardian"}) {
				map.put("minecraft:" + id, "epic");
			}
			map.put("minecraft:ender_dragon", "legendary");
			map.put("survivalneeds:curse_specter", "epic");
			return map;
		}
	}

	/** Uma raridade de coração. */
	public static final class HeartTier {
		public double chance;                           // chance de cair ao matar (1.0 = sempre)
		public int xp;                                  // XP ao consumir o coração
		public double bonus;                            // bônus de XP no amuleto (0.03 = +3%)

		public HeartTier(double chance, int xp, double bonus) {
			this.chance = chance;
			this.xp = xp;
			this.bonus = bonus;
		}
	}

	/** XP extra por superar um debuff, com proteção contra farm. */
	public static final class DebuffXp {
		public boolean enabled = true;
		public double tolerancePerType = 4.0;           // cada ganho do mesmo tipo reduz o próximo: xp x (1 - recente / tolerância)
		public double decayMinutes = 8.0;               // o "recente" de cada tipo cai 1 a cada tantos minutos
		public int hourlyCap = 200;                     // máximo de XP de debuffs por hora de jogo (72000 ticks)
		public Map<String, DebuffXpType> types = defaultTypes();

		static Map<String, DebuffXpType> defaultTypes() {
			Map<String, DebuffXpType> map = new LinkedHashMap<>();
			map.put("wet", new DebuffXpType(5, 60));
			map.put("cold", new DebuffXpType(12, 30));
			map.put("fear", new DebuffXpType(15, 15));
			map.put("thirst", new DebuffXpType(8, 60));
			map.put("fatigue", new DebuffXpType(40, 120));
			map.put("hunger", new DebuffXpType(60, 120));
			return map;
		}
	}

	/** XP base de um debuff e o tempo mínimo que o jogador precisa ter ficado nele para valer. */
	public static final class DebuffXpType {
		public int xp;
		public double minSeconds;

		public DebuffXpType(int xp, double minSeconds) {
			this.xp = xp;
			this.minSeconds = minSeconds;
		}
	}

	/** Perda de níveis ao morrer: quanto menor o nível, maior a fração perdida. */
	public static final class Death {
		public boolean enabled = true;                  // false: morrer não custa níveis extras (vale o jogo normal)
		public int fromLevel = 5;                       // neste nível ou menos perde a fração máxima
		public int toLevel = 30;                        // neste nível ou mais perde a fração mínima
		public double maxLoss = 1.0;                    // fração perdida em fromLevel (1.0 = 100%)
		public double minLoss = 0.3;                    // fração perdida em toLevel (0.3 = 30%)
	}

	/** Equipamento que exige nível. Sem o nível a peça rende menos e armadura não pode ser vestida. */
	public static final class Equipment {
		public double penaltyStrength = 1.2;            // penalidade = força x (nível pedido - seu nível) / nível pedido
		public double maxPenalty = 0.95;                // a peça nunca perde mais que isto (0.95 = 95%)
		public Map<String, Integer> requirements = defaultRequirements();  // nível pedido por material
		public Map<String, Integer> itemOverrides = new LinkedHashMap<>(); // nível pedido por id de item, ex.: "mod:espada": 12

		static Map<String, Integer> defaultRequirements() {
			Map<String, Integer> map = new LinkedHashMap<>();
			map.put("leather", 0);
			map.put("wood", 0);
			map.put("stone", 3);
			map.put("chainmail", 3);
			map.put("gold", 5);
			map.put("copper", 5);
			map.put("iron", 7);
			map.put("diamond", 15);
			map.put("netherite", 30);
			map.put("trident", 15);
			map.put("elytra", 20);
			map.put("mace", 20);
			return map;
		}
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
		if (progression == null) progression = new Progression();
		if (curse == null) curse = new Curse();
		if (curse.weights == null) curse.weights = Curse.defaultWeights();
		if (curse.brubinPrice == null) curse.brubinPrice = Curse.defaultPrices();
		if (curse.brubinPriceOverrides == null) curse.brubinPriceOverrides = new LinkedHashMap<>();
		if (curse.brubinOreValue == null) curse.brubinOreValue = Curse.defaultOreValues();
		if (curse.brubinHeartValue == null) curse.brubinHeartValue = Curse.defaultHeartValues();
		if (curse.warnMinutes == null) curse.warnMinutes = new ArrayList<>(List.of(10, 5, 1));
		if (progression.death == null) progression.death = new Death();
		if (progression.amulets == null) progression.amulets = new Amulets();
		if (progression.amulets.oreBonus == null) progression.amulets.oreBonus = Amulets.defaultOres();
		if (progression.hearts == null) progression.hearts = new Hearts();
		if (progression.hearts.tiers == null) progression.hearts.tiers = Hearts.defaultTiers();
		if (progression.hearts.monsters == null) progression.hearts.monsters = Hearts.defaultMonsters();
		progression.hearts.monsters.putIfAbsent("survivalneeds:curse_specter", "epic"); // configs antigas não têm o Espectro
		if (progression.debuffXp == null) progression.debuffXp = new DebuffXp();
		if (progression.debuffXp.types == null) progression.debuffXp.types = DebuffXp.defaultTypes();
		if (progression.equipment == null) progression.equipment = new Equipment();
		if (progression.equipment.requirements == null) progression.equipment.requirements = Equipment.defaultRequirements();
		if (progression.equipment.itemOverrides == null) progression.equipment.itemOverrides = new LinkedHashMap<>();
	}
}
