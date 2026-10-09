# Survival Needs

Mod Fabric para **Minecraft 26.3** que adiciona necessidades de sobrevivência: **sede**, **frio**, **medo**, **fome**, **cansaço** e o estado **molhado**. Cada efeito tem um ícone no canto superior direito da tela.

- **Versão:** 0.1.0
- **Id do mod:** `survivalneeds`
- **Dependências:** Fabric Loader 0.19.5+, Fabric API 0.162.0+26.3, Java 25
- Funciona em mundo de um jogador e em servidor. A lógica roda no servidor e o cliente só mostra o HUD.

---

## Resumo

| Efeito | Quando ativa | Consequência |
|---|---|---|
| **Sede** | Barra de sede com 5 pontos ou menos | Não corre. Com 0, perde vida aos poucos (sem morrer). |
| **Frio (bioma de neve)** | Em bioma de neve, sem nenhuma peça de armadura | Não corre e perde meio coração por segundo. |
| **Frio (noite)** | Depois de 10 min de exposição à noite, descoberto e sem armadura | Não corre e perde meio coração a cada 10 s (nunca mata). |
| **Medo** | Cercado, monstro forte, chefe, vida baixa ou escuro (ver regras) | Seus golpes causam 50% menos dano. |
| **Fome** | Barra de carne zerada | Anda na metade da velocidade, não corre, limite de blocos e 10 min para comer 5 ícones, senão morre. A carne cura 1 coração por ícone. |
| **Cansaço** | 2 noites seguidas sem dormir | Limites de construção e de corrida, 30% menos dano e 5 min para dormir, senão perde vida. Dorme de dia e com monstros por perto. |
| **Molhado** | Ficar sob chuva ou entrar na água | Corre só 15 s seguidos, não regenera vida, não dorme, limite de 5 blocos. Seca em 2 min (coberto) ou 4 min (ao relento). |

---

## Sede

### Barra e consumo
- Barra de **20 pontos**, mostrada em 10 gotas acima da barra de fome.
- O valor é **salvo no jogador** e volta a 20 quando ele morre.
- Parado, perde cerca de **1 ponto a cada 2 minutos**.
- **Correr acelera o gasto.** Quanto mais tempo você corre sem parar, mais rápido a sede cai, chegando a
  **3 vezes** o gasto da corrida normal depois de uns 20 segundos. Parar de correr zera a aceleração.
- Enquanto você corre, uma **seta azul** balança à esquerda da barra, avisando que a sede está sendo gasta.

### Consequências
- Com **5 pontos ou menos**: o jogador **não consegue correr** e o ícone de sede aparece no canto da tela.
- Com **0 pontos**: perde 1 ponto de vida a cada 4 segundos, **mas nunca morre de sede**
  (o dano para quando restam 1 coração).

### Água, purificador e garrafa
- **Água suja:** é a garrafa de água comum do jogo (a que se enche numa fonte). Ela **não recupera sede**.
  Se for bebida, **intoxica**: tira **3 corações** de vida e dá **náusea por 10 segundos**
  (sem matar: o dano para em 1 ponto de vida).
- **Purificador de Água** (bloco): receita com **8 tábuas de qualquer madeira em volta e 1 carvão
  (ou carvão vegetal) no centro**.
  ```
  P P P
  P C P      P = tábuas (qualquer madeira)   C = carvão ou carvão vegetal
  P P P
  ```
- **Como usar:** clique com o botão direito no purificador para abrir a tela, que tem a mesma disposição
  da fornalha:
  - **Slot de cima (esquerda):** a garrafa de água suja.
  - **Slot de baixo (esquerda):** o carvão (ou carvão vegetal). A chama mostra quanto resta.
  - **Slot da direita:** a garrafa de água purificada, que você retira.
  - Cada garrafa leva **4 segundos** (a seta mostra o progresso) e gasta **1 uso**.
- **Carvão e usos:** **1 carvão = 5 usos.** O carvão é consumido assim que entra e o purificador guarda os
  usos. A tela mostra "Usos: N/5" e a chama diminui conforme os usos acabam.
  - **Só cabe um carvão por vez:** enquanto ainda restam usos, o slot do carvão não aceita outro. Só depois
    que os usos chegam a 0 dá para colocar mais carvão.
  - **Sem usos, o purificador não funciona.** As garrafas ficam paradas até você colocar carvão.
- Shift+clique move os itens entre o inventário e o purificador. Ao quebrar o bloco, o que estiver dentro cai.
- **Garrafa de Água Purificada:** recupera **6 pontos de sede**, empilha até 16 e devolve a garrafa de
  vidro vazia depois de beber.
- O purificador é quebrado com machado e dropa o próprio bloco. Os dois itens aparecem no inventário criativo,
  em *Comidas e Bebidas* (garrafa) e *Blocos Funcionais* (purificador).

---

## Frio

Ativa em dois casos. Nos dois o jogador não corre e aparece o ícone de frio. O frio **só vale em modo
survival** e **só para quem não usa nenhuma peça de armadura** (couro conta como armadura).

### Bioma de neve
- Em qualquer bioma onde neva, sem armadura: perde **meio coração por segundo**.
- Ao colocar qualquer peça de armadura, o efeito para.

### Noite ao relento
Frio mais leve, para quem passa a noite desprotegido.

- Conta tempo de exposição quando as quatro condições valem juntas: é **noite** no Overworld, o **céu está
  totalmente aberto** sobre a cabeça (qualquer bloco por cima interrompe), o jogador **não usa armadura** e
  está em survival.
- Ao acumular **10 minutos** de exposição, o frio chega, com o aviso "O frio da noite te alcançou!".
- O dano é bem leve: **meio coração a cada 10 segundos**, e **nunca mata** (deixa 1 coração).
- **A contagem não zera de dia:** ela fica parada. Como a noite do jogo dura só 8 a 9 minutos reais, na prática o
  frio chega por volta da segunda noite. Abrigado (sob um teto) ou de armadura à noite, a contagem **diminui
  2 vezes mais rápido** do que subiu.

---

## Medo

O jogador com medo causa **50% menos dano** corpo a corpo (corte no atributo de ataque; flechas e tridente
arremessado não são afetados). O aviso na tela diz o motivo.

### Quem conta como ameaça
Inimigos hostis vivos com **8 ou mais de vida** (slimes pequenos e similares são ignorados). Um **monstro forte**
(40 ou mais de vida, como Ravager, Warden, Wither e Elder Guardian) conta como **3 inimigos** ao contar quem
cerca o jogador.

### Regras (qualquer uma ativa o medo)
| # | Situação | Condição |
|---|---|---|
| 1 | Cercado por poucos | 3 ou mais inimigos a até 8 blocos, com arma **inferior a ferro** |
| 2 | Cercado por horda | 10 ou mais inimigos a até 8 blocos, com arma **inferior a diamante** |
| 3 | Monstro forte | Monstro de 40+ de vida a até 16 blocos, com XP menor que 15 **e** armadura menor que 10 |
| 4 | Chefe | Warden, Wither ou Dragão a até 24 blocos, a menos que o jogador tenha XP 30+ **e** armadura 15+ |
| 5 | 1 coração | **1 coração de vida ou menos**: o medo vem sempre, sem olhar arma, XP, armadura nem inimigos (mesmo sem nenhum por perto) |
| 6 | 2 corações | **2 corações de vida ou menos** e **qualquer inimigo** a até **50 blocos** em todas as direções (sem olhar arma nem XP) |
| 7 | Ferido | 3 corações ou menos de vida e 2 ou mais inimigos por perto |
| 8 | Escuro | Luz muito baixa (nível 4 ou menos), 2 ou mais inimigos e arma inferior a pedra |

### Nível da arma
É sempre a arma da mão principal.

| Nível | Armas |
|---|---|
| 0 | Mão vazia ou qualquer outro item |
| 1 | Madeira, ouro |
| 2 | Pedra, cobre |
| 3 | Ferro |
| 4 | Diamante, maça, tridente |
| 5 | Netherita |

Valem espadas, machados e lanças de cada material.

### Duração
Depois de ativado, o medo dura **pelo menos 3 segundos**, para o ícone não ficar piscando quando um monstro
sai do raio.

---

## Fome

O jogo continua usando a barra de comida de 10 ícones, que aqui chamamos de **barra de carne**. Cada ícone vale
2 pontos internos de comida. O mod troca a cura natural e a inanição do jogo por regras próprias.

### Cura
- Enquanto houver carne e o jogador estiver ferido, ele **se cura sozinho**, até a carne acabar.
- **1 ícone de carne (2 pontos) cura 2 pontos de vida (1 coração).** A cura é de 1 ponto de vida por segundo, e
  cada ponto curado gasta 1 ponto de comida.
- A carne **continua caindo normalmente** com as ações (correr, pular, lutar), como no jogo original.
- Molhado, o jogador não se cura (ver Molhado), mesmo com carne.
- A cura natural e o gasto de comida que vinha com ela, no jogo original, foram desligados para não haver duas
  curas ao mesmo tempo.

### Faminto (carne zerada)
Quando a carne chega a **zero**, o jogador fica faminto:

| Efeito | Regra |
|---|---|
| **Velocidade** | Anda na **metade da velocidade** do andar normal. |
| **Corrida** | Não consegue correr. |
| **Blocos** | Vale o mesmo limite do molhado: 5 blocos seguidos e depois 5 segundos de espera. |
| **Contador** | Começa uma contagem de **10 minutos**. Ao chegar a zero, o jogador **morre de fome**. |

Para sair da fome, é preciso comer **até a metade da barra: 5 ícones de carne de um total de 10**. Comer um pouco
(menos que isso) **não** para a contagem nem remove os efeitos. Ao chegar à metade da barra, a fome passa e o
contador some.

- O ícone de fome e o contador (`10:00` até `0:00`, em vermelho) aparecem no canto superior direito.
- O contador é **salvo no jogador**, então sair e voltar ao mundo não o reinicia.
- A inanição do jogo original (dano contínuo de fome) foi desligada: quem decide a morte é a contagem.
- Em criativo e espectador a fome não se aplica.

---

## Cansaço

Passar noites seguidas sem dormir deixa o jogador **cansado**. Só vale em modo survival.

### Como fica cansado
- Uma **noite sem dormir** é uma noite que termina (amanhece) sem o jogador ter dormido nela.
- Depois de **2 noites seguidas sem dormir**, o jogador fica cansado, com o ícone no canto superior direito.
- Dormir (uma noite normal na cama) zera a contagem de noites.
- A contagem de noites é **salva no jogador**. A hora da noite segue a regra da cama do jogo (do fim da tarde ao amanhecer).

### O que o cansaço faz
Pelo sistema de debuffs, o jogador cansado recebe:

| Efeito | Regra |
|---|---|
| **Construção limitada** | O mesmo limite do molhado e da fome: 5 blocos seguidos e depois 5 segundos de espera. |
| **Corrida limitada** | Corre no máximo **10 segundos seguidos**, depois fica **30 segundos sem poder correr**. (Na configuração dá para trocar por bloqueio total.) |
| **Dano reduzido** | Causa **30% menos dano**. |
| **Contador de 5 minutos** | Ao ficar cansado começa uma contagem de **5 minutos** (`5:00` até `0:00`, em lilás, embaixo do ícone). |

Se o contador chegar a zero e o jogador **ainda não tiver dormido**, ele começa a **perder vida: 1 ponto
(meio coração) a cada 5 segundos**, até dormir. Essa perda de vida **não mata**: para quando resta 1 coração
(configurável).

### Dormir cansado
O jogador cansado **consegue dormir**:
- **de dia** (a cama normalmente só funciona à noite), e
- **mesmo com monstros por perto**.

Dormir de dia **não pula o tempo do mundo**: o dia não vira noite nem passa mais rápido. Basta ficar deitado
por cerca de 5 segundos para o cansaço passar. Dormir à noite funciona como sempre, inclusive pulando a noite
quando todos dormem.

O molhado continua impedindo de dormir: quem está molhado e cansado precisa secar antes.

Ao dormir o jogador recebe "Você descansou e o cansaço passou.", e os efeitos e o contador somem.

---

## Molhado

A **chuva** e **entrar na água** deixam o jogador molhado. O estado só vale em **modo survival** e aparece como ícone no canto da tela.

### Como molha
- Ficar **sob chuva** (com o céu aberto sobre a cabeça) deixa o jogador molhado. Em biomas de neve não molha,
  porque lá neva em vez de chover.
- **Entrar na água** (rio, lago, mar ou até uma poça de um bloco) também molha, na hora.
- Enquanto continuar chovendo sobre ele, a contagem para secar é reiniciada.
- Enquanto estiver na água, a contagem para secar fica zerada. Ela só corre depois que o jogador sai.

### O que o molhado faz
| Efeito | Regra |
|---|---|
| **Corrida limitada** | Corre no máximo **15 segundos seguidos**. Depois fica **30 segundos sem poder correr**. Parar por menos de 1 segundo não zera a contagem. |
| **Sem regenerar vida** | A cura natural por estar alimentado **não funciona**. Poções e outros tipos de cura continuam valendo. A fome continua gastando normalmente. |
| **Sem dormir** | A cama recusa o jogador, com o aviso "Você está molhado demais para dormir!". |
| **Blocos limitados** | Coloca no máximo **5 blocos seguidos**. Depois precisa esperar **5 segundos**. Se passar 5 segundos sem colocar nada, a contagem volta a 0 (colocar 2 blocos e esperar 5 s limpa o contador). |

### Como secar
A contagem para secar corre sempre que **não está chovendo sobre o jogador nem ele está na água**:

| Onde o jogador está | Tempo para secar |
|---|---|
| **Em lugar coberto** (qualquer bloco acima da cabeça) | **2 minutos** |
| **Ao relento** (céu aberto) | **4 minutos** |

Isso vale qualquer que seja a origem da umidade (chuva ou água). Perto de uma fonte de calor (até 2 blocos de distância), a
contagem passa **2,5 vezes mais rápido** (coberto: cerca de 48 segundos; ao relento: cerca de 1 minuto e 36 segundos).

Se o jogador volta para a chuva ou para a água, a contagem é reiniciada. Trocar entre abrigo e céu aberto não a zera:
o tempo restante apenas passa a descer na velocidade do novo lugar.

Perto significa **até 2 blocos nas quatro direções** (norte, sul, leste e oeste), 1 bloco abaixo dos pés e até a
altura da cabeça. Uma tocha a 3 blocos de distância não conta.

Contam como fonte de calor: fogo e fogo de alma, lava, tochas (inclusive de alma e de cobre), lanternas,
fogueiras acesas, velas acesas, fornalhas, alto-fornos e defumadores acesos, e blocos de magma.

### Indicadores na tela

No canto superior direito, junto do ícone de molhado:

- **Fogueira:** aparece ao lado do ícone de molhado quando há uma **fonte de calor bem perto** (até 2 blocos),
  avisando que a secagem está acelerada.
- **Contador:** aparece embaixo do ícone de molhado **enquanto a contagem para secar está correndo**. Mostra o
  tempo que falta nas condições de agora: começa em `2:00` em lugar coberto ou em `4:00` ao relento, e desce até
  `0:00`. Perto do fogo ele desce **2,5 vezes mais rápido**. Na chuva ou na água a contagem fica parada e o contador
  some.

Ao secar, o jogador recebe o aviso "Você secou." e todos os limites acima são desfeitos.

---

## Ícones e HUD

- **Barra de sede:** acima da barra de fome, à direita. Usa os sprites de **bolha de ar do próprio jogo**,
  então herda o visual de qualquer pacote de texturas ativo (como o PureBDcraft).
- **Ícones de efeito (frio, medo, sede, fome, cansaço, molhado):** no **canto superior direito**, abaixo das linhas de poção do jogo
  para não sobrepor. Se mais de um efeito está ativo, ficam lado a lado, da direita para a esquerda.
- **Seta de gasto de sede:** à esquerda da barra, só enquanto o jogador corre.
- Os ícones dos efeitos ficam em
  `src/main/resources/assets/survivalneeds/textures/gui/sprites/hud/` (`status_cold.png`,
  `status_fear.png`, `status_thirst.png`, `status_hunger.png`, `status_fatigue.png`, `status_wet.png`, `status_heat.png`, 25×25 px) e a seta em `thirst_draining.png` (11×11 px).
  Para trocá-los, basta substituir os arquivos e recompilar.

---

## Comandos

Exigem permissão de operador (trapaças ativadas).

| Comando | Efeito |
|---|---|
| `/cold true` | Liga o frio à força, em qualquer lugar e mesmo com armadura. |
| `/cold false` | Desliga o frio forçado. Se o bioma ou a noite ainda causam frio, ele continua. |
| `/fear true` | Liga o medo à força, sem precisar de monstros. |
| `/fear false` | Desliga o medo forçado. |
| `/thirst true` | Deixa a sede em 5 pontos (limite do efeito). |
| `/thirst false` | Enche a barra de sede para 20. |
| `/wet true` | Deixa o jogador molhado, sem secar sozinho. |
| `/wet false` | Seca o jogador na hora e desfaz os limites. |
| `/hunger true` | Zera a carne e inicia a fome e a contagem de morte. |
| `/hunger false` | Enche a barra de carne e encerra a fome. |
| `/fatigue true` | Deixa o jogador cansado na hora, com o contador de 5 minutos. |
| `/fatigue false` | Tira o cansaço e zera a contagem de noites. |
| `/survivalneeds reload` | Relê `config/survivalneeds.json` sem reiniciar o jogo. |

Funcionam também em criativo, o que ajuda a ver os ícones sem se preocupar com dano.

---

## Configuração

Todos os valores ajustáveis ficam em **`config/survivalneeds.json`** (na pasta da instância do jogo). Não é preciso
recompilar: edite o arquivo e use `/survivalneeds reload` (ou reabra o jogo).

- Se o arquivo **não existir**, ele é criado com os valores padrão na primeira vez que o jogo abre.
- Se faltar algum campo (por exemplo, numa versão nova do mod), vale o **padrão** daquele campo. O arquivo é
  reescrito a cada carga, com todas as opções.
- Se o arquivo tiver um erro de sintaxe, o mod registra o erro no log e usa os padrões, sem travar.
- **Unidades:** tempos em **segundos**, distâncias em **blocos**, vida em **pontos** (2 pontos = 1 coração).
  Frações como `0.5` valem 50%.

### `thirst` (sede)
| Campo | Padrão | O que faz |
|---|---|---|
| `noSprintAtOrBelow` | 5 | Com este valor de sede ou menos, o jogador não corre. |
| `secondsPerPointIdle` | 125 | Parado, perde 1 ponto de sede a cada tanto tempo. |
| `secondsPerPointSprinting` | 40 | Correndo, perde 1 ponto a cada tanto tempo. |
| `sprintRampSeconds` | 20 | Tempo correndo até o gasto chegar ao máximo. |
| `sprintMaxBoost` | 2.0 | Aceleração máxima do gasto (2 = 3 vezes o gasto da corrida normal). |
| `dryDamageIntervalSeconds` | 4 | Com sede 0, de quanto em quanto tempo leva dano. |
| `dryDamage` | 1.0 | Dano com sede 0. |
| `dryMinHealth` | 2.0 | A sede não tira vida abaixo deste valor (não mata). |
| `dirtyWaterDamage` | 6.0 | Dano ao beber água suja (6 = 3 corações). |
| `dirtyWaterNauseaSeconds` | 10 | Duração da náusea ao beber água suja. |
| `purifiedWaterRestores` | 6 | Pontos de sede que a água purificada devolve. |

### `cold` (frio)
| Campo | Padrão | O que faz |
|---|---|---|
| `biomeDamage` | 1.0 | Dano por segundo em bioma de neve, sem armadura. |
| `nightExposureSeconds` | 600 | Exposição à noite, ao relento e sem armadura, até o frio chegar. |
| `nightShelterRecovery` | 2.0 | Abrigado ou de armadura, a exposição some tantas vezes mais rápido. |
| `nightDamage` | 1.0 | Dano do frio da noite... |
| `nightDamageIntervalSeconds` | 10 | ...a cada tantos segundos. |
| `nightMinHealth` | 2.0 | O frio da noite não tira vida abaixo deste valor (não mata). |

### `fear` (medo)
| Campo | Padrão | O que faz |
|---|---|---|
| `damageReduction` | 0.5 | Corte de dano com medo (0.5 = 50%). |
| `minFearSeconds` | 3 | Duração mínima do medo depois de ativado. |
| `crowdRadius` / `strongRadius` / `bossRadius` | 8 / 16 / 24 | Raios de "cercado", de monstro forte e de chefe. |
| `minThreatHealth` | 8 | Inimigos com menos vida não contam como ameaça. |
| `strongMinHealth` | 40 | Vida a partir da qual um monstro é "forte". |
| `strongWeight` | 3 | Um monstro forte conta como tantos inimigos. |
| `surroundedFew` / `fewMinWeaponTier` | 3 / 3 | Cercado por tantos inimigos, com arma abaixo deste nível. |
| `surroundedMany` / `manyMinWeaponTier` | 10 / 4 | Cercado por horda, com arma abaixo deste nível. |
| `braveXpLevel` / `braveArmorValue` | 15 / 10 | Contra monstro forte, só dispensa o medo quem tem XP e armadura acima disto. |
| `bossBraveXpLevel` / `bossBraveArmorValue` | 30 / 15 | O mesmo, contra chefes. |
| `lastHeartHealth` | 2.0 | Vida (1 coração) abaixo da qual o medo vem sempre. |
| `severeHealth` / `severeRadius` | 4.0 / 50 | Vida (2 corações) com que qualquer inimigo na região causa medo, e o tamanho dela. |
| `lowHealth` / `lowHealthEnemies` | 6.0 / 2 | Vida (3 corações) e inimigos por perto que causam medo. |
| `darkLightLevel` / `darkEnemies` / `darkMinWeaponTier` | 4 / 2 / 2 | Regra do escuro. |

Níveis de arma: 1 madeira/ouro, 2 pedra/cobre, 3 ferro, 4 diamante/maça/tridente, 5 netherita.

### `wet` (molhado)
| Campo | Padrão | O que faz |
|---|---|---|
| `coveredDrySeconds` | 120 | Tempo para secar em lugar coberto. |
| `openAirDrySeconds` | 240 | Tempo para secar ao relento. |
| `heatDryMultiplier` | 2.5 | Perto do calor, a contagem passa tantas vezes mais rápido. |
| `heatRadius` / `heatBelow` / `heatAbove` | 2 / 1 / 2 | O que conta como "perto" do calor (blocos). |
| `sprintLimitSeconds` | 15 | Corre no máximo tanto tempo seguido... |
| `sprintCooldownSeconds` | 30 | ...e depois fica tanto tempo sem poder correr. |
| `sprintGraceSeconds` | 1 | Parar de correr por menos que isso não zera a contagem. |
| `placeLimit` / `placeResetSeconds` | 5 / 5 | Blocos seguidos que pode colocar, e a espera depois. |

### `hunger` (fome)
| Campo | Padrão | O que faz |
|---|---|---|
| `starveSeconds` | 600 | Tempo, com a carne zerada, até morrer de fome. |
| `recoverFoodLevel` | 10 | Pontos de comida (de 20) para a fome passar: 10 = metade da barra. |
| `healIntervalSeconds` | 1 | A cada tanto tempo, cura um passo... |
| `healAmount` | 1.0 | ...de tantos pontos de vida, gastando 1 ponto de comida. |
| `slowdown` | 0.5 | Faminto anda tanto mais devagar (0.5 = metade da velocidade). |
| `placeLimit` / `placeResetSeconds` | 5 / 5 | Limite de blocos seguidos do faminto, e a espera. |

### `fatigue` (cansaço)
| Campo | Padrão | O que faz |
|---|---|---|
| `nightsWithoutSleep` | 2 | Noites seguidas sem dormir para ficar cansado. |
| `counterSeconds` | 300 | Tempo, depois de cansado, para dormir antes de começar a perder vida. |
| `damageReduction` | 0.3 | Cansado causa tanto menos dano (0.3 = 30%). |
| `blockSprint` | false | `true`: não corre de jeito nenhum. `false`: corre por um tempo e descansa. |
| `sprintLimitSeconds` | 10 | (com `blockSprint` false) corre no máximo tanto tempo seguido... |
| `sprintCooldownSeconds` | 30 | ...e depois fica tanto tempo sem poder correr. |
| `sprintGraceSeconds` | 1 | Parar de correr por menos que isso não zera a contagem. |
| `placeLimit` / `placeResetSeconds` | 5 / 5 | Blocos seguidos que pode colocar, e a espera depois. |
| `damage` / `damageIntervalSeconds` | 1.0 / 5 | Depois do contador, perde tanta vida a cada tantos segundos, até dormir. |
| `minHealth` | 2.0 | O cansaço não tira vida abaixo disto (use 0 para poder matar). |

### `purifier` (purificador de água)
| Campo | Padrão | O que faz |
|---|---|---|
| `usesPerCoal` | 5 | Garrafas que 1 carvão purifica. |
| `purifySeconds` | 4 | Tempo para purificar 1 garrafa. |

---

## Sistema de debuffs

Os efeitos negativos (não correr, andar devagar, construir pouco, causar menos dano) **não ficam dentro** de cada
sistema. Existe um módulo genérico, `debuff/`, e os sistemas (frio, sede, fome, molhado, medo) só **pedem** os efeitos
que querem. Assim nada é duplicado, e um sistema novo reaproveita tudo.

```
ColdSystem ──┐                                         ┌─ SprintDebuff  (bloqueio e limite de corrida)
ThirstSystem ┤                                         ├─ BuildDebuff   (limite de blocos seguidos)
WetSystem ───┼─ DebuffSystem.set(jogador, fonte, ...) ─┼─ DamageDebuff  (menos dano)
HungerSystem ┤        (junta os pedidos)               └─ SpeedDebuff   (andar mais devagar)
FearSystem ──┘
```

**Efeitos disponíveis** (`DebuffEffect`):

| Efeito | O que faz |
|---|---|
| `SprintBlock` | O jogador não consegue correr. |
| `SprintLimit(limite, descanso, tolerância, mensagens)` | Corre por um tempo e depois descansa sem poder correr. |
| `BuildLimit(máximo, espera, motivo)` | Só coloca alguns blocos seguidos; depois espera. |
| `DamageReduction(fração)` | O dano causado cai essa fração. |
| `Slowdown(fração)` | A velocidade de andar cai essa fração. |

**Como um sistema usa**, a cada tick, com uma "fonte" (um texto único para ele):

```java
// enquanto molhado, pede dois efeitos; quando seca, os retira
DebuffSystem.set(player, "wet", isWet, () -> List.of(
    new DebuffEffect.SprintLimit(...),
    new DebuffEffect.BuildLimit(...)));
```

**Regras de combinação** (quando duas fontes pedem o mesmo tipo de efeito ao mesmo tempo):
- **Bloqueio de corrida:** vale se qualquer fonte pedir. **Limite de corrida e de construção:** vale o mais restrito.
- **Dano e velocidade:** as reduções se combinam multiplicando o que sobra (50% e 50% dão 75% de redução).

**Para criar um debuff novo:** adicione um `record` em `DebuffEffect` e um `DebuffHandler` que o aplique (os de dano e
velocidade são só duas linhas, sobre `AttributeDebuff`). **Para um sistema novo usar os existentes:** é só chamar
`DebuffSystem.set(...)` com os efeitos que quiser.

A ordem de cada tick no servidor é: os sistemas (`Cold`, `Fear`, `Wet`, `Hunger`, `Fatigue`, `Thirst`) calculam o estado e
pedem os efeitos, depois `DebuffSystem.tick` aplica tudo, e por fim `StatusFlags.update` envia os ícones ao cliente.

---

## Estrutura do código

Pacote `com.ronaldo.survivalneeds`:

| Arquivo | Função |
|---|---|
| `SurvivalNeeds.java` | Ponto de entrada. Carrega a configuração, registra blocos, itens, comandos e o laço do servidor. |
| `config/SurvivalNeedsConfig.java` | Lê, cria e recarrega `config/survivalneeds.json`. Todo valor ajustável vem daqui. |
| `debuff/DebuffSystem.java` | Central dos debuffs: recebe os pedidos dos sistemas e aplica os efeitos. |
| `debuff/DebuffEffect.java` | Os tipos de efeito (corrida, construção, dano, velocidade). |
| `debuff/DebuffHandler.java`, `AttributeDebuff.java` | Base dos aplicadores de efeito. |
| `debuff/SprintDebuff.java`, `BuildDebuff.java`, `DamageDebuff.java`, `SpeedDebuff.java` | Aplicadores de cada efeito. |
| `ThirstSystem.java` | Barra de sede: consumo, aceleração ao correr, dano com 0, água suja. Salva e sincroniza o valor. |
| `ColdSystem.java` | Frio de bioma de neve e frio da noite. |
| `FearSystem.java` | Todas as regras do medo. |
| `WetSystem.java` | Estado molhado: chuva, água, secagem, calor por perto. |
| `HungerSystem.java` | Cura pela carne, estado faminto e contagem de morte. |
| `FatigueSystem.java` | Noites sem dormir, estado cansado, contador e perda de vida. |
| `mixin/FoodDataMixin.java` | Mixin: desliga a cura natural e a inanição do jogo (o HungerSystem as substitui). |
| `mixin/BlockItemMixin.java` | Mixin: pergunta ao `DebuffSystem` se o jogador pode colocar o bloco. |
| `mixin/PlayerMixin.java`, `mixin/ServerPlayerMixin.java` | Mixins: o cansado pode deitar e continuar dormindo de dia. |
| `StatusFlags.java` | Resume os efeitos ativos, o tempo para secar e o calor por perto, e sincroniza com o cliente. |
| `StatusCommands.java` | Comandos `/cold`, `/fear`, `/thirst`, `/wet`, `/hunger`, `/fatigue` e `/survivalneeds reload`. |
| `WaterPurifierBlock.java` | Bloco do purificador: abre a tela e liga o tick do servidor. Também reconhece a água suja. |
| `WaterPurifierBlockEntity.java` | Inventário (3 slots), usos de carvão, progresso e a lógica de purificar. Salva e carrega. |
| `PurifierMenu.java` | Menu do purificador (slots, shift+clique, sincronização de usos e progresso). |
| `ModBlockEntities.java`, `ModMenus.java` | Registro do bloco com inventário e do menu. |
| `PurifiedWaterItem.java` | Garrafa de água purificada. |
| `ModBlocks.java`, `ModItems.java` | Registro de blocos e itens. |
| `client/SurvivalNeedsClient.java` | Entrada do cliente: registra os elementos do HUD. |
| `client/ThirstHud.java` | Barra de sede e seta de gasto. |
| `client/StatusIconsHud.java` | Ícones dos efeitos, fogueira e contadores no canto superior direito. |
| `client/PurifierScreen.java` | Tela do purificador, usando a textura da fornalha do jogo. |

Recursos: modelos, blockstate, receita, loot table e traduções (`en_us`, `pt_br`) em
`src/main/resources/assets/survivalneeds` e `src/main/resources/data/survivalneeds`.

---

## Compilar e instalar

É preciso o **JDK 25**.

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot"
.\gradlew.bat build
```

O arquivo gerado é `build/libs/survivalneeds-0.1.0.jar`. Copie para a pasta `mods` da instância junto com o
Fabric API e reinicie o jogo.

## Limitações conhecidas

- O bloqueio de corrida é feito só pelo servidor. O cliente decide sozinho se está correndo, então pode haver
  um pequeno "puxão" ou alguns instantes de corrida antes do bloqueio. A correção definitiva é um mixin
  do lado do cliente, ainda não implementado.
- A tela do purificador reaproveita a textura da fornalha, então os slots de entrada e saída têm o mesmo
  desenho dela. Não há automação com funis testada.
- A água suja só causa dano se for bebida diretamente; não há outras fontes de intoxicação.
- Os estados (exposição ao frio da noite, aceleração de sede, molhado) ficam em memória e reiniciam quando o
  servidor fecha. Ficam salvos: a barra de sede e o contador de fome.
- O medo reduz apenas dano corpo a corpo.
- A contagem de noites sem dormir considera a noite como terminada quando o dia amanhece. Se o servidor for reiniciado no
  meio de uma noite em que o jogador já tinha dormido, essa noite pode ser contada como sem dormir.
- Dormir de dia (cansado) não pula o tempo, de propósito, para não virar um jeito de passar a noite.
- O limite de blocos é aplicado no servidor, então o jogador pode ver o bloco aparecer por um instante antes de
  ele sumir.
- Em servidor dedicado cada máquina lê o seu próprio `survivalneeds.json`. As regras valem pelo arquivo do
  servidor; a tela do purificador usa os números do servidor, mas o HUD usa valores fixos (sede de 20 pontos).
- O mod foi compilado, mas ainda não passou por testes completos dentro do jogo.
