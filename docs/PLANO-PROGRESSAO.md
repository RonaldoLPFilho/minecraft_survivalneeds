# Plano: Progressão por XP, coração de monstro e amuletos

> **Status:** especificação **fechada**. **Fase A (equipamento) implementada**, aguardando teste no jogo. Fases B a E ainda não.
> Os números abaixo são os valores padrão e ficam todos no JSON de configuração.
> O que já está pronto: os ícones do coração, do amuleto e da mesa (em `src/main/resources/assets/survivalneeds/textures/item/`)
> e a verificação da dependência do Trinkets.

## Decisões fechadas

| Tema | Decisão |
|---|---|
| Nível que conta | **Nível atual** (o da barra de XP). |
| Peça fraca já vestida | A peça **fica**, mas protege menos (penalidade proporcional). |
| Armas e ferramentas | Dá para segurar, mas **rendem menos** (dano e mineração). |
| Requisito do ouro | **5** (reduzido do rascunho de 10). |
| XP original do jogo | **Continua como está.** Orbes de monstro, mineração, fundição e o XP solto ao morrer não mudam. O XP por superar debuffs e o de corações são **fontes extras**. |
| Teto de XP dos debuffs | **Sim:** 200 pontos por hora real. |
| Espaço de amuleto | **Mod Trinkets Updated**, como dependência declarada. |

---

## Visão geral

O XP vira a **progressão do personagem**:

1. **Equipamento forte exige nível.** Sem o nível, a peça rende menos, de forma proporcional à diferença.
2. **Morrer custa níveis**, mais para quem tem pouco.
3. **Sobreviver aos debuffs dá XP** (frio, medo, sede, fome, cansaço, molhado), com proteção contra farm.
4. **Corações de monstro** dropam dos inimigos, servem de XP e são o coração do amuleto.
5. **Amuletos** (minério + coração) aumentam o XP ganho, num espaço de acessório novo.

---

## 1. Equipamento exige nível

### Requisitos por material (por peça, não por conjunto)

| Material | Nível |
|---|---|
| Couro, madeira | 0 |
| Pedra, malha | 3 |
| **Ouro** | **5** |
| Cobre | 5 |
| **Ferro** | **7** |
| **Diamante** | **15** |
| Tridente, elitra | 15 a 20 |
| Maça | 20 |
| Netherita | 30 |

Vale para **armadura** e para **armas e ferramentas** (espada, machado, lança, maça, picareta, pá, enxada).
Escudo, arco e besta ficam sem requisito na primeira versão.

### Penalidade: linear e previsível

```
déficit    = nível_requerido − nível_do_jogador          (se ≤ 0, sem penalidade)
penalidade = mínimo( 95%,  1,2 × déficit / nível_requerido )
```

`1,2` (força) e `95%` (teto) são configuráveis. O teto garante que a peça nunca fica 100% inútil.

| Requisito \ Nível do jogador | 0 | 3 | 5 | 10 | 15 | 20 | 25 |
|---|---|---|---|---|---|---|---|
| Ouro (5) | 95% | 48% | 0% | 0% | 0% | 0% | 0% |
| Ferro (7) | 95% | 69% | 34% | 0% | 0% | 0% | 0% |
| Diamante (15) | 95% | 95% | 80% | 40% | 0% | 0% | 0% |
| Netherita (30) | 95% | 95% | 95% | 80% | 60% | 40% | 20% |

### O que a penalidade faz

| Tipo | Efeito |
|---|---|
| **Arma** (mão principal) | Dano causado cai a penalidade. |
| **Ferramenta** (mão principal) | Velocidade de mineração cai a penalidade. |
| **Armadura** | A proteção de **cada peça** cai a penalidade dela. |

### Equipar armadura

Sem o nível, a armadura **não pode ser equipada**, por nenhum caminho: clique, shift+clique, botão direito com a peça
na mão e dispensador. Aparece aviso na tela e som de recusa. Se o nível **cair depois** de vestida (encantar,
bigorna, morte), a peça fica e passa a render menos pela fórmula acima.

### Indicação ao jogador

Uma linha no texto de cada peça: **"Requer nível 15"**, vermelha quando o jogador não tem o nível. Criativo e
espectador ignoram tudo, e a seção tem um `enabled` para desligar.

---

## 2. Morrer custa níveis

Quanto **menor** o nível, **maior** a fração perdida. A fração cai linearmente de 100% (nível 5 ou menos) até 30%
(nível 30 ou mais):

```
perda% = 100% − 70% × (nível − 5) / 25         (limitada entre 30% e 100%)
perdidos = arredondar(nível × perda%)
```

| Nível ao morrer | Perda | Níveis perdidos | Volta com |
|---|---|---|---|
| 5 | 100% | 5 | 0 |
| 10 | 86% | 9 | 1 |
| 15 | 72% | 11 | 4 |
| 20 | 58% | 12 | 8 |
| 25 | 44% | 11 | 14 |
| 30 | 30% | 9 | 21 |
| 40 | 30% | 12 | 28 |

A barra de progresso dentro do nível zera. **O XP que o jogo original solta ao morrer** (até 100 pontos em orbes
no local da morte) **continua existindo**, para permitir recuperar um pouco numa corrida ao corpo.

---

## 3. Sobreviver aos debuffs dá XP

O XP é dado quando o jogador **se livra** do debuff por recuperação. Morrer, trocar de modo de jogo, sair do jogo ou
usar comando **não** dá XP. É uma fonte **extra**, que não substitui nenhuma do jogo.

| Debuff superado | Quando conta | Duração mínima | XP base |
|---|---|---|---|
| Molhado | Secou | 60 s molhado | 5 |
| Frio | Saiu do frio | 30 s com frio | 12 |
| Medo | O medo passou | 15 s com medo | 15 |
| Sede | Voltou acima do limite | 60 s com sede baixa | 8 |
| Cansaço | Dormiu | 120 s cansado | 40 |
| Fome | Comeu até a metade da barra | 120 s faminto | 60 |

Referência: no jogo original, subir do nível 0 ao 1 custa 7 pontos, do 10 ao 11 custa 27, e do 20 ao 21 custa 62.

### Proteção contra farm

1. **Duração mínima** (coluna acima): uma exposição curta não vale nada.
2. **Tolerância por tipo:** cada ganho de um tipo sobe um contador `recente` daquele tipo. O XP vira
   `base × (1 − recente / 4)`, então a 4ª repetição seguida já não dá nada. O contador **cai 1 a cada 8 minutos**.
3. **Teto por hora:** no máximo **200 pontos** de XP por hora real vindos de debuffs.

Cada tipo tem o seu contador: molhado repetido não atrapalha o XP de fome.

---

## 4. Coração de monstro

Um item novo, `Coração de Monstro`, **um só item** que guarda de qual monstro veio (um componente de dados, como o ovo
de spawn). Isso evita dezenas de itens e de texturas. A raridade colore o nome do item.

### Quem dropa e com que chance

Só com o **jogador** como autor da morte. Looting soma **+2% por nível**.

| Faixa | Monstros | Chance |
|---|---|---|
| **Comum** | Zumbi, esqueleto, aranha, creeper, slime | 6% |
| **Incomum** | Aranha de caverna, afogado, husk, stray, bruxa, saqueador, phantom | 8% |
| **Raro** | Enderman, blaze, ghast, guardian, esqueleto wither, piglin brutal, vindicador, evocador, hoglin, shulker, ravager | 12% |
| **Épico** | Warden, Wither, Elder Guardian | **100%** |
| **Lendário** | Dragão do Fim | **100%** |

**Proteção contra azar:** depois de 30 mortes de monstros comuns, incomuns ou raros sem nenhum coração, a
próxima dá um garantido. Isso também garante que o primeiro coração, necessário para a Mesa de Amuletos, chega em tempo.

### Consumir dá XP

O coração pode ser **consumido** (segurar o botão direito) para ganhar XP na hora: consumir ou guardar para o amuleto.
O XP do coração consumido **não** recebe bônus de amuleto.

| Faixa | XP ao consumir | Bônus no amuleto |
|---|---|---|
| Comum | 10 | +3% |
| Incomum | 20 | +6% |
| Raro | 50 | +15% |
| Épico | 200 | +60% |
| Lendário (Dragão) | 500 | +100% |

---

## 5. Amuletos

### Receita

**8 minérios em volta e 1 coração de monstro no centro**, na Mesa de Amuletos. O bônus é a **soma** do bônus do
minério com o do coração.

| Minério (8 unidades) | Bônus de XP |
|---|---|
| Carvão | +4% |
| Cobre | +8% |
| Redstone | +12% |
| Lápis-lazúli | +12% |
| Ferro | +15% |
| Ouro | +25% |
| Esmeralda | +35% |
| **Diamante** | **+50%** |
| Netherita (lingote) | +80% |

Exemplos: 8 diamantes + coração de zumbi = 50% + 3% = **+53%**. 8 diamantes + coração do Dragão = 50% + 100% =
**+150%**. O máximo teórico de um amuleto é 8 netherita + Dragão = **+180%**.

### Como funciona

- Um único item de amuleto guarda de qual minério e de qual monstro veio, e calcula o bônus. O nome e a dica mostram
  por exemplo "Amuleto de Diamante (Zumbi)" e "+53% de XP".
- O bônus vale para **todo XP ganho**: o XP original do jogo (orbes de monstro, mineração, fundição) e o XP dos
  debuffs. **Não** vale para o XP de coração consumido.
- **2 espaços** de amuleto, com a soma limitada a **+200%** no total (`maxTotalBonus`).

---

## 6. Mesa de Amuletos

Um bloco novo, com tela de **3x3 + resultado** (como a bancada de criação), onde **só** a receita do amuleto funciona.
A mesa não guarda itens: ao fechar a tela, o que sobrou volta ao jogador (igual à bancada de criação).

### Receita da mesa (possível por volta de 3 horas de jogo)

Só usa materiais que já se têm nessa altura e **um coração de monstro**, o primeiro contato com o sistema:

```
  Ferro   Livro    Ferro
  Lápis   Coração  Lápis        Coração = qualquer coração de monstro
  Pedra   Pedra    Pedra        Pedra = pedra lisa
```

(2 ferros, 2 lápis-lazúli, 1 livro, 1 coração, 3 pedras lisas.) Nada de diamante nem ouro.

---

## 7. Plano técnico por fase

### Peças compartilhadas (criadas na Fase A e reaproveitadas)

- **Config `progression`** em `SurvivalNeedsConfig` (nova seção, com `enabled`).
- **`Progression`** (classe central de XP): `giveXp(player, pontos, origem)` aplica, no ponto certo, o bônus de amuleto
  uma única vez. Evita aplicar o bônus duas vezes ou no XP do coração consumido.
- **`EquipmentGate`**: dada uma peça, devolve a categoria (armadura, arma, ferramenta), o nível requerido e a penalidade.

### Fase A: equipamento

| Item | Detalhe |
|---|---|
| Novos debuffs | `MiningSlowdown` (atributo de velocidade de mineração) e `ArmorReduction` (atributo de armadura), ambos sobre `AttributeDebuff`. `DamageReduction` já existe. |
| Fonte nova | `GearSystem` pede os efeitos ao `DebuffSystem` (fonte `"gear"`), checando a mão principal e as 4 peças a cada poucos ticks. |
| Material de cada peça | Pelo id do item (`iron_*`, `diamond_*`...) e por ids especiais (`trident`, `mace`, `elytra`). A config aceita sobrescrever **por id de item**, o que cobre itens de outros mods. |
| Bloquear equipar | Mixins nos caminhos que colocam armadura no jogador: botão direito com a peça, clique e shift+clique nos espaços do inventário e dispensador. **Cada caminho será conferido no código do jogo** e, se algum ficar de fora, será registrado. |
| Dica no item | Evento de dica do Fabric (cliente) com a linha "Requer nível X". |
| Aviso | Mensagem na tela e som de recusa. |

**Como testar:** `/xp set @s 0 levels`, tentar vestir diamante (deve recusar), `/give` uma espada de diamante e
bater (dano reduzido), subir o nível e ver a penalidade cair.

### Fase B: morte

- Ao morrer, o nível é guardado num dado que **acompanha o jogador na morte** (`copyOnDeath`). No reaparecimento,
  o nível calculado pela fórmula é aplicado.
- Config `death` com os quatro números (`fromLevel`, `toLevel`, `maxLoss`, `minLoss`).
- Decisão a tomar na implementação: com `keepInventory` ligado a perda também vale (padrão: vale).

**Como testar:** `/xp set @s 20 levels`, `/kill`, e conferir que volta com 8.

### Fase C: XP por superar debuffs

- Cada sistema (`Cold`, `Fear`, `Thirst`, `Hunger`, `Fatigue`, `Wet`) avisa a classe `DebuffXp` ao **começar** e ao
  **terminar por recuperação** (pontos já existentes no código: secar, sair do frio, o medo passar, comer até a
  metade da barra, dormir, a sede voltar).
- `DebuffXp` guarda quando cada debuff começou, aplica duração mínima, tolerância por tipo e teto por hora, e entrega
  o XP por `Progression.giveXp`. Aviso na tela: "+12 XP (superou o frio)".
- Estados forçados por comando **não** contam.

**Como testar:** cada debuff por tempo suficiente (use o `reload` para encurtar tempos), e repetir várias vezes
para ver o XP cair a zero e voltar com a espera.

### Fase D: coração de monstro

- **Item** `monster_heart` com um componente de dados `heart_source` (monstro de origem). Textura pronta:
  `textures/item/monster_heart.png`.
- **Drops:** evento de loot da Fabric API para entidades (já usado no `fastharvest`), com chance por faixa, looting
  e o contador de azar por jogador.
- **Consumir:** componente de consumível com animação, e o XP entregue por `Progression.giveXp` sem bônus de amuleto.
- Tabela monstro → faixa na config, e dica no item com o monstro, a faixa e o bônus de amuleto.

**Como testar:** `/summon` zumbis e vê-los dropar, forçar a chance para 100% na config e testar o consumo.

### Fase E: amuletos, espaço e mesa

**Dependência do Trinkets Updated (verificada):**

| | |
|---|---|
| Mod | Trinkets Updated, **versão 4.2.1+26.3**, licença MIT |
| Id do mod | `trinkets_updated` |
| Maven | repositório `https://maven.nucleoid.xyz/releases`, coordenada `eu.pb4:trinkets:4.2.1+26.3` |
| `build.gradle` | adicionar o repositório e `implementation "eu.pb4:trinkets:4.2.1+26.3"` (variável no `gradle.properties`) |
| `fabric.mod.json` | `"depends": { "trinkets_updated": ">=4.2.1" }`: sem ele o jogo **não abre** e avisa o jogador |
| Dependências dele | Fabric API e a biblioteca `yumi-mc-foundation`, que já vem **embutida** no jar. Nada extra para o jogador instalar. |
| Instalação no jogo | O jar `trinkets-4.2.1+26.3.jar` (648 KB, hash conferido) vai na pasta `mods`, junto com o nosso. |

- **Espaço:** o Trinkets já traz um espaço de colar (`chest/necklace`). Vamos definir **um espaço próprio de amuleto**
  (`data/trinkets/slots/chest/amulet.json`, com 2 unidades) e um grupo de itens
  (`data/trinkets/tags/item/chest/amulet.json`) com o nosso amuleto. O formato exato dessas chaves será conferido
  contra a documentação do Trinkets na hora de implementar.
- **Item amuleto:** componentes `ore` e `heart`, com o bônus calculado pela config. Textura pronta:
  `textures/item/amulet.png`.
- **Bônus de XP:** um mixin em `Player.giveExperiencePoints` multiplica o XP (soma dos amuletos equipados, limitada
  pelo teto). Passa por `Progression` para o XP do coração consumido ficar de fora.
- **Mesa de Amuletos:** bloco com menu próprio (3x3 + resultado), no estilo da bancada de criação: a conta é feita
  em código (8 minérios iguais + 1 coração no centro), não por receita em JSON, para aceitar qualquer minério e qualquer
  coração. A **receita de fazer a mesa** é um JSON normal. Visual do bloco com texturas do próprio jogo (herda o
  PureBDcraft); ícone do inventário: `textures/item/amulet_table.png` (64x64, pronto).

**Como testar:** crafting da mesa; amuleto com 8 diamantes + coração de zumbi dá "+53%"; equipar nos dois espaços;
conferir que o teto de +200% é respeitado; matar monstros e ver o XP maior.

---

## 8. Configuração (esboço do JSON)

```json
"progression": {
  "enabled": true,
  "equipment": {
    "penaltyStrength": 1.2,
    "maxPenalty": 0.95,
    "requirements": { "leather": 0, "wood": 0, "stone": 3, "chainmail": 3, "gold": 5, "copper": 5,
                      "iron": 7, "diamond": 15, "netherite": 30, "trident": 15, "elytra": 20, "mace": 20 },
    "itemOverrides": { }
  },
  "death":    { "fromLevel": 5, "toLevel": 30, "maxLoss": 1.0, "minLoss": 0.3 },
  "debuffXp": { "tolerancePerType": 4, "decayMinutes": 8, "hourlyCap": 200,
                "types": { "wet": {"xp": 5, "minSeconds": 60}, "cold": {"xp": 12, "minSeconds": 30} } },
  "hearts":   { "lootingBonus": 0.02, "pityKills": 30,
                "tiers": { "common": {"chance": 0.06, "xp": 10, "bonus": 0.03} } },
  "amulets":  { "slots": 2, "maxTotalBonus": 2.0, "oreBonus": { "diamond": 0.50 } }
}
```

---

## 9. Riscos e cuidados

- **Bloqueio de armadura** passa por vários caminhos do jogo: exige mixins e teste de cada um.
- **Inflação de XP:** com amuletos de até +180%, o teto de +200% e os números dos debuffs são o controle.
- **Gastar nível enfraquece o equipamento** (encantar, bigorna, reparo). É intencional na regra de "nível atual".
- **Equipamento de outros mods:** sem requisito por padrão; `itemOverrides` na config permite incluir.
- **Multijogador:** cada jogador tem seu nível e seus contadores; tudo roda no servidor.
- **Trinkets:** o mod passa a ser obrigatório. Se uma versão nova do Minecraft não tiver Trinkets, a Fase E bloqueia.
- **Escudo, arco, besta:** fora da primeira versão.

---

## 10. Ordem de implementação

| Fase | O que entrega | Depende de |
|---|---|---|
| **A** | Config `progression`, `Progression`, `EquipmentGate`, `GearSystem`, `MiningSlowdown`, `ArmorReduction`, bloqueio de armadura e a dica "Requer nível X". | Sistema de debuffs (pronto) |
| **B** | Perda de níveis ao morrer. | A (config) |
| **C** | XP por superar debuffs, com duração mínima, tolerância e teto por hora. | A (`Progression`) |
| **D** | Coração de Monstro: item, drops, azar protegido e consumo. | A (`Progression`) |
| **E** | Trinkets (dependência), amuleto, espaços, bônus de XP e a Mesa de Amuletos. | D, e C/B para o XP |

As fases B, C e D são independentes entre si e podem ser feitas em qualquer ordem depois da A.
