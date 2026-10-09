# Survival Needs

Mod Fabric para **Minecraft 26.3** que adiciona três necessidades de sobrevivência: **sede**, **frio**
e **medo**. Cada efeito tem um ícone no canto superior direito da tela.

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
| **Medo** | Cercado, monstro forte, chefe, pouca vida ou escuro (ver regras) | Seus golpes causam 50% menos dano. |

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
| 5 | Ferido | 3 corações ou menos de vida e 2 ou mais inimigos por perto |
| 6 | Escuro | Luz muito baixa (nível 4 ou menos), 2 ou mais inimigos e arma inferior a pedra |

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

## Ícones e HUD

- **Barra de sede:** acima da barra de fome, à direita. Usa os sprites de **bolha de ar do próprio jogo**,
  então herda o visual de qualquer pacote de texturas ativo (como o PureBDcraft).
- **Ícones de efeito (frio, medo, sede):** no **canto superior direito**, abaixo das linhas de poção do jogo
  para não sobrepor. Se mais de um efeito está ativo, ficam lado a lado, da direita para a esquerda.
- **Seta de gasto de sede:** à esquerda da barra, só enquanto o jogador corre.
- Os ícones dos efeitos ficam em
  `src/main/resources/assets/survivalneeds/textures/gui/sprites/hud/` (`status_cold.png`,
  `status_fear.png`, `status_thirst.png`, 25×25 px) e a seta em `thirst_draining.png` (11×11 px).
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

Funcionam também em criativo, o que ajuda a ver os ícones sem se preocupar com dano.

---

## Estrutura do código

Pacote `com.ronaldo.survivalneeds`:

| Arquivo | Função |
|---|---|
| `SurvivalNeeds.java` | Ponto de entrada. Registra blocos, itens, comandos e o laço de verificação a cada tick do servidor. |
| `ThirstSystem.java` | Barra de sede: consumo, aceleração ao correr, dano com 0, água suja. Salva e sincroniza o valor. |
| `ColdSystem.java` | Frio de bioma de neve e frio da noite. |
| `FearSystem.java` | Todas as regras do medo e o corte de dano. |
| `SprintLock.java` | Bloqueia a corrida enquanto há frio ou sede baixa. |
| `StatusFlags.java` | Resume quais efeitos estão ativos e sincroniza com o cliente para os ícones. |
| `StatusCommands.java` | Comandos `/cold`, `/fear` e `/thirst`. |
| `WaterPurifierBlock.java` | Bloco do purificador: abre a tela e liga o tick do servidor. Também reconhece a água suja. |
| `WaterPurifierBlockEntity.java` | Inventário (3 slots), usos de carvão, progresso e a lógica de purificar. Salva e carrega. |
| `PurifierMenu.java` | Menu do purificador (slots, shift+clique, sincronização de usos e progresso). |
| `ModBlockEntities.java`, `ModMenus.java` | Registro do bloco com inventário e do menu. |
| `PurifiedWaterItem.java` | Garrafa de água purificada. |
| `ModBlocks.java`, `ModItems.java` | Registro de blocos e itens. |
| `client/SurvivalNeedsClient.java` | Entrada do cliente: registra os elementos do HUD. |
| `client/ThirstHud.java` | Barra de sede e seta de gasto. |
| `client/StatusIconsHud.java` | Ícones de frio, medo e sede no canto superior direito. |
| `client/PurifierScreen.java` | Tela do purificador, usando a textura da fornalha do jogo. |

Recursos: modelos, blockstate, receita, loot table e traduções (`en_us`, `pt_br`) em
`src/main/resources/assets/survivalneeds` e `src/main/resources/data/survivalneeds`.

### Como o bloqueio de corrida funciona
O servidor aplica um modificador de velocidade que cancela o bônus da corrida (+30%) enquanto houver frio ou
sede baixa, e força o estado de corrida para desligado.

---

## Ajustando os valores

Todos os números ficam como constantes no começo de cada classe:

- `ThirstSystem.java`: consumo, aceleração, limite de corrida, dano da água suja.
- `ColdSystem.java`: dano do bioma, tempo de exposição da noite (`NIGHT_EXPOSURE_TICKS`), dano da noite.
- `FearSystem.java`: raios, quantidade de inimigos, limites de XP e armadura, corte de dano, duração.
- `PurifiedWaterItem.java`: quanto a água purificada recupera (`THIRST_RESTORED`).

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
- Os estados (exposição ao frio da noite, aceleração de sede) ficam em memória e reiniciam quando o
  servidor fecha. O valor da barra de sede é salvo.
- O medo reduz apenas dano corpo a corpo.
- O mod foi compilado, mas ainda não passou por testes completos dentro do jogo.
