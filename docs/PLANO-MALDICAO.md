# Plano: Maldição

**Status:** plano em revisão, nada implementado.

Três partes: a maldição (debuff aleatório), o Espectro da Maldição (inimigo que a causa) e a Princess Brubin (NPC que cura).
Tudo configurável em `config/survivalneeds.json`, seção nova `curse`, recarregável com `/survivalneeds reload`.

---

## 1. A maldição

### Regras
- Uma maldição por vez. Dardo em quem já está amaldiçoado não troca a maldição.
- Imunidade de 10 min depois de curado ou de morrer (config `immunityMinutes`).
- A morte remove a maldição (inclusive as de contador).
- Estado salvo no jogador (persistente): maldição, tempo restante e imunidade. Sobrevive a sair e voltar.
- Contador corre só com o jogador online. Avisos aos 10, 5 e 1 min, e o tempo aparece na tela.
- Escudo erguido bloqueia o dardo.
- **Amuleto como proteção:** se o jogador tem amuleto equipado (Trinkets), o primeiro dardo que acerta **quebra o amuleto** e não amaldiçoa. Sem amuleto, o dardo amaldiçoa. Com dois equipados, quebra um por dardo (o de menor bônus).

### Catálogo (pesos ajustáveis, a config normaliza)
| Gravidade | Maldição | Peso | Implementação |
|---|---|---|---|
| Leve | Pernas pesadas: não corre | 14 | `SprintBlock` (existe) |
| Leve | Mãos de pedra: não ataca | 10 | cancelar dano causado pelo jogador (novo) |
| Leve | Insônia: não dorme | 10 | `EntitySleepEvents.ALLOW_SLEEPING` |
| Leve | Mãos trêmulas: construção limitada | 10 | `BuildLimit` (existe) |
| Leve | Fragilidade: vida máx. −30% (cura só até 70%) | 12 | modificador de `MAX_HEALTH` (novo) |
| Leve | Estômago selado: não come (pode beber) | 8 | `FoodDataMixin` / início do uso (novo) |
| Média | Vida pela metade: vida máx. −70% | 6 | `MAX_HEALTH` |
| Média | Corpo de chumbo: lentidão | 6 | `Slowdown` (existe) |
| Média | Pele de papel: +50% de dano recebido | 6 | efeito novo no `DebuffSystem` |
| Média | Mãos cegas: mineração lenta | 5 | `MiningSlowdown` (existe) |
| Média | Ressecado: sede e fome 2× | 5 | multiplicador nos sistemas de necessidades |
| Média | Mente turva: sem ganho de XP | 2 | `PlayerMixin.giveExperiencePoints` |
| Grave | Afogamento em 20 min | 5 | contador + dano de afogamento fora da água |
| Grave | Sentença: morre em 30 min | 5 | contador + morte |
| Grave | Explosão em 30 min (raio 20) | 3 | contador + explosão |

- Explosão: opção `explosionBreaksBlocks`, **desligada por padrão** (só dano e susto). Quem quiser, liga.
- Cada maldição tem gravidade (leve, média, grave) que define o preço da cura.
- Novos efeitos entram no `DebuffSystem` como os atuais, com fonte `"curse"`.
- Ícone do HUD: arte do rosto roxo enviada pelo usuário, reduzida para o tamanho dos ícones do `StatusFlags`.

---

## 2. Espectro da Maldição

- **Muito agressivo, mas justo.** Um dardo a cada ~5 s. O ciclo é:
  1. **Recua** sempre antes de atirar (abre distância, ~2 s).
  2. **Atira** o dardo.
  3. **No meio-termo, pula ofensivo** contra o jogador e ataca com as garras.
  4. **Recua ao tomar dano forte** (limite configurável, ex.: 20% da vida num golpe) e reinicia o ciclo.
  Com menos de 50% de vida pode disparar rajada de 2 dardos, mantendo os 5 s entre disparos.
  Valores na config: `dartCooldownTicks` (100), `retreatBeforeShotTicks`, `leapCooldownTicks`, `retreatDamageFraction`.
- Vida ~100, tamanho grande (próximo de um Ravager), resistência a flechas (corpo semi-etéreo). Valores na config.
- **Dardo:** entidade projétil própria, velocidade alta, rastro roxo/verde. Ao acertar jogador: dano pequeno, e aí vale a regra do amuleto (quebra) ou da maldição. Escudo bloqueia.
- **Aparição:** raro, à noite e em escuridão (cavernas, pântanos). Cap de população baixo.
- **Drops:** coração de monstro de raridade alta (entra na tabela de `hearts`) e Fragmento Amaldiçoado (material, valor na cura).
- **Visual:** modelo em blocos em código (garras grandes, ombreiras de osso/pedra com espiral, correntes, faixas vermelhas, fumaça roxa, olhos verdes brilhantes) e textura pixel art gerada por script a partir da arte de referência. Olhos e detalhes emissivos. É uma interpretação em blocos do conceito, não cópia fiel.

---

## 3. Princess Brubin

### Aparência
Modelo humanoide com vestido em camadas (vermelho, creme e dourado), cabelo longo ondulado escuro, coroa com rubi, manto e braceletes, como no turnaround enviado. Feita em código sobre o modelo humanoide + peças extras, textura 64×64 pintada por script. O rosto será simplificado ao estilo do jogo (olhos e sorriso), sem fidelidade de retrato.

### Comportamento
- NPC invulnerável, parada, olha para o jogador próximo, não some, não é empurrada. Clicar abre a tela de cura.
- Se o jogador não tem maldição, ela diz uma fala e não cobra nada.

### Onde aparece
1. **Em toda vila:** na geração/carregamento do chunk que contém uma vila, se ainda não houver Brubin ali, uma é colocada perto do centro (marcador salvo no mundo para não repetir). Funciona também em vilas já exploradas, na próxima vez que o chunk carregar.
2. **Rara pelo mapa:** pequena chance por chunk gerado de aparecer uma Brubin solitária na superfície (config `wildChance`).
3. **Invocada:** ver abaixo.
- Brubins encontradas (vila ou mapa) **ficam no lugar** e cobram sempre, quantas vezes for preciso.

### Invocação com amuleto (consumível)
- **Gesto proposto:** Shift + clique direito num bloco com o amuleto na mão (clique normal continua equipando).
- Só funciona se o jogador está amaldiçoado; senão avisa e **não gasta o amuleto**.
- Consome o amuleto, a Brubin aparece ao lado, limpa a maldição (sem cobrar mais nada) e some com efeito de partículas. Some também após 5 min sem interação.
- Qualquer amuleto serve, o valor dele não importa para a invocação.

### Cobrança por pontos
Cada maldição tem um preço em pontos conforme a gravidade (ajustável por maldição):
| Gravidade | Preço |
|---|---|
| Leve | 100 |
| Média | 250 |
| Grave | 500 |

O jogador combina o que quiser até chegar no total:
| Pagamento | Valor (padrão) |
|---|---|
| Minérios (os da lista de `amulets.oreBonus`) | diamante 100, ouro 20, ferro 8, cobre 5, etc. |
| XP | 20 pontos por nível |
| Coração de monstro | por raridade: comum 12, incomum 25, raro 60, épico 150, lendário 400 |
| Amuleto | valor do minério + valor do coração dele |
| Fragmento Amaldiçoado | 80 |

Exemplo do usuário: 3 diamantes (300) + amuleto de prata com coração de aranha (50) + 5 níveis (100) + 4 corações de zumbi (50) = 500.
Todos os valores ficam na config para calibrar depois, em especial o amuleto, para o exemplo bater.

**Tela (menu):**
- Fileira de espaços onde se colocam itens, mais botão "Pagar 1 nível" para o XP.
- Mostra: maldição atual, preço, quanto já foi oferecido e uma barra de progresso.
- Botão "Confirmar" só liga ao atingir o preço. Passar do preço não dá troco (aviso na tela).
- Fechar a tela devolve os itens que não foram confirmados. XP só é gasto ao confirmar.

---

## Fases
1. **Núcleo:** estado persistente, catálogo e sorteio ponderado, efeitos novos, contadores e avisos, ícone do HUD, `/curse give <tipo>` e `/curse clear` para teste.
2. **Espectro e dardo:** entidade, modelo, textura, IA agressiva, projétil, regra do amuleto, spawn e drops.
3. **Brubin:** entidade, modelo, textura, geração em vilas e no mapa, tela de pagamento e cura.
4. **Invocação:** gesto com amuleto, aparecer e sumir.
5. **Balanceamento e docs:** pesos, preços, README.

## Riscos e limitações
- Modelo e textura feitos por script e código, sem Blockbench: ficam estilizados, ajustáveis depois por quem tiver o arquivo.
- Colocar Brubin em vilas é feito por código (checagem de estrutura ao carregar chunk), não por peça de vila. Pode raramente cair em lugar estranho; trato fugindo de blocos sólidos e água.
- Explosão e afogamento mexem com o jogador à força: precisam de avisos claros (feitos nos contadores).
- Dicas e preços lidos da config do cliente podem divergir da do servidor.
