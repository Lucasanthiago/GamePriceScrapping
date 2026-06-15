# Comparador de Preços de Jogos

Web scraping em **Java 17 + Jsoup** que raspa lojas de jogos, cruza os títulos e mostra
**onde cada jogo está mais barato**. Trabalho de Desenvolvimento Web — a técnica central é
**parsing de HTML com seletores CSS** (`doc.select(...)`).

Tem **duas interfaces**: uma **CLI** (tabela no terminal) e uma **UI web** glanceável
(`--web`), servida por um servidor HTTP embutido (sem dependência nova além do Jsoup).

> Este arquivo é a documentação viva do projeto. É mantido atualizado a cada evolução para
> servir de contexto rápido tanto a um desenvolvedor humano quanto ao Claude Code.

---

## Como rodar

Pré-requisitos: **JDK 17+** e **Maven** (há um Maven local em `~/.local/share/apache-maven-3.9.9`
nesta máquina; o comando `mvn` está em `~/.local/bin/mvn`).

```bash
# rodar os testes
mvn test

# gerar o JAR executável (fat jar com o Jsoup embutido)
mvn package            # -> target/comparador-precos-jogos.jar

# executar (CLI)
java -jar target/comparador-precos-jogos.jar                 # modo interativo (digita o jogo)
java -jar target/comparador-precos-jogos.jar elden ring      # busca direta
java -jar target/comparador-precos-jogos.jar --verbose witcher   # logs de scraping no stderr
java -jar target/comparador-precos-jogos.jar --no-cache zelda    # ignora o cache local
java -jar target/comparador-precos-jogos.jar --html witcher      # exporta comparacao-*.html
java -jar target/comparador-precos-jogos.jar --csv witcher       # exporta comparacao-*.csv

# executar (UI web) -> abre http://localhost:8080
java -jar target/comparador-precos-jogos.jar --web
java -jar target/comparador-precos-jogos.jar --web --port 9000

# ou, sem empacotar:
mvn exec:java -Dexec.args="elden ring"
mvn exec:java -Dexec.args="--web"
```

Flags: `--verbose`/`-v` (logs), `--no-cache` (não usa o cache em disco), `--web` (UI web),
`--port N`/`--porta N` (porta do servidor), `--exato` (desliga o match aproximado de títulos),
`--cambio-fixo` (usa taxa fixa em vez da cotação USD/BRL ao vivo), `--csv`/`--html` (exporta
o resultado da busca para arquivo).

---

## O que ele faz (fluxo)

1. O usuário digita um termo (ex.: "elden ring").
2. Cada **fonte de loja** raspa sua busca e devolve uma lista de **ofertas** (já em BRL).
3. O **comparador** cruza as ofertas pelo **título normalizado** e agrupa em **jogos**.
4. A **CLI** imprime, por jogo, uma tabela `loja | preço | desconto | nota | link`,
   destacando com `>>` a loja mais barata. Jogos disponíveis em mais de uma loja
   (comparáveis) aparecem primeiro.

---

## Lojas suportadas

Só usamos lojas que entregam o preço **no HTML renderizado pelo servidor** (Jsoup faz um GET
simples e não executa JavaScript). Validação rápida de uma loja nova: abrir a página, `Ctrl+U`
e `Ctrl+F` no preço de um jogo — se o preço está no código-fonte, dá pra raspar.

| Loja | Busca | Como o preço vem | Seletores-chave |
|------|-------|------------------|-----------------|
| **Steam** | `store.steampowered.com/search/?term=` | SSR, em **R$** (região Brasil) | `a.search_result_row` → `span.title`, `div.discount_block[data-price-final]` (centavos), `data-discount`, `span.search_review_summary[data-tooltip-html]` |
| **GamersGate** | `gamersgate.com/games/?query=` | React pré-renderizado; `data-*` limpos | `div.catalog-item.product--item` → `data-name`, `data-price`, `data-currency`, `data-url`, `div.product--label-discount`, `div.catalog-item--full-price` |
| **GamesPlanet** | `us.gamesplanet.com/search?query=` | SSR, em **USD** (domínio US) | `div.game_list_small` → `h4 a` (título/URL relativa), `span.price_current`, `span.price_base strike` (preço cheio), `span.price_saving` (desconto) |

**Descartadas (SPA, preço só via JS):** Nuuvem, Epic, GOG, Humble, Green Man Gaming, Fanatical,
IndieGala. GameBillet bloqueia o GET com 403 (Cloudflare).

**Nota sobre moeda:** as lojas entregam moedas diferentes — Steam em **BRL**, GamesPlanet em
**USD**, GamersGate em BRL ou USD conforme geolocalização (`data-currency`). O código não assume
nada: passa todo preço pelo `ConversorMoeda`, que converte apenas quando a moeda for USD. A
conversão usa a **cotação USD/BRL ao vivo** (`CotacaoAoVivoConversor`), com a taxa fixa
`Config.USD_BRL` como fallback se a fonte estiver fora do ar.

---

## Arquitetura

Cada loja fica atrás de uma interface (`FonteLoja`), então adicionar loja = nova classe, sem
mexer no resto (aberto/fechado). A busca HTTP é abstraída (`Buscador`) para centralizar as boas
práticas e permitir cache e testes sem rede.

```
App (CLI + Web / composition root)
 ├─ ComparadorPrecos
 │   ├─ List<FonteLoja>
 │   │   ├─ FonteSteam       ─┐
 │   │   ├─ FonteGamersGate  ─┤── usam ──> Buscador (HTTP)
 │   │   └─ FonteGamesPlanet ─┤            ├─ JsoupBuscador (rede: userAgent, timeout, pausa)
 │   │                        │            └─ CacheBuscador (decorator: HTML em disco c/ TTL)
 │   │            GamersGate/GamesPlanet usam ──> ConversorMoeda
 │   │                                            ├─ CotacaoAoVivoConversor (USD/BRL ao vivo)
 │   │                                            └─ TaxaFixaConversor (fallback)
 │   ├─ NormalizadorTitulo  (chave de cruzamento exato)
 │   └─ SimilaridadeTitulo  (match aproximado: funde títulos quase iguais)
 └─ apresentação dos Jogo[]:
     ├─ TabelaPrecos          ── terminal
     ├─ ExportadorComparacao  ── CSV / HTML autossuficiente
     ├─ ValeAPena             ── ranking custo-benefício (preço + nota)
     └─ ServidorWeb           ── UI web (HTTP embutido) + API JSON (RespostaBusca, Json)
```

### Pacotes (`src/main/java/com/gamesprice`)

| Pacote | Responsabilidade |
|--------|------------------|
| `model` | `Loja` (enum), `Oferta` (record imutável, preço em BRL), `Jogo` (agrupa ofertas) |
| `normalizacao` | `NormalizadorTitulo` (forma canônica do título), `SimilaridadeTitulo` (Levenshtein) |
| `http` | `Buscador` (interface), `JsoupBuscador` (rede), `CacheBuscador` (decorator de cache) |
| `cambio` | `ConversorMoeda` (interface), `TaxaFixaConversor` (fallback), `CotacaoAoVivoConversor` (cotação ao vivo) |
| `fonte` | `FonteLoja` (interface), `FonteSteam`, `FonteGamersGate`, `FonteGamesPlanet` |
| `comparador` | `ComparadorPrecos` (roda fontes + cruza por título; opcionalmente match aproximado) |
| `ranking` | `ValeAPena` (score de custo-benefício: preço + nota das reviews) |
| `cli` | `TabelaPrecos` (saída no terminal), `ExportadorComparacao` (CSV/HTML) |
| `web` | `ServidorWeb` (HTTP embutido), `RespostaBusca` (mapeia domínio→JSON), `Json` (serializador) |
| `config` | `Config` (constantes: userAgent, timeout, pausa, cache, taxa, pesos, porta) |
| `util` | `Precos` (parsing/format de moeda), `Log` (log simples no stderr) |

A UI web é estática em `src/main/resources/web/` (`index.html`, `styles.css`, `app.js`),
embutida no jar e servida pelo `ServidorWeb` a partir do classpath.

---

## Cruzamento de títulos (o desafio do projeto)

O mesmo jogo aparece com nomes diferentes em cada loja. `NormalizadorTitulo` reduz tudo a uma
chave canônica antes de comparar:

- minúsculas → remove acentos (NFD) → remove apóstrofos (juntam letras: `assassin's` → `assassins`)
- demais símbolos (®, ™, ©, `:`, `-`) viram separador → remove sufixos de edição
  ("Deluxe Edition", "GOTY", "Game of the Year", "Ultimate Edition"...) → colapsa espaços

O `ComparadorPrecos` agrupa as ofertas por essa chave. Em seguida, um passo opcional de
**match aproximado** (`SimilaridadeTitulo`, distância de Levenshtein) funde grupos cujas chaves
ficaram quase iguais (ex.: "The Witcher 3" × "Witcher 3"), acima do limiar `Config.LIMIAR_SIMILARIDADE`.
Há uma **guarda de sequência**: títulos com números diferentes nunca se fundem ("Portal" ≠ "Portal 2",
"FIFA 22" ≠ "FIFA 23"). O match aproximado é ligado por padrão na CLI/UI e desligável com `--exato`.

---

## Boas práticas de scraping aplicadas

- **User-Agent** identificando o projeto e **timeout** em toda requisição (`Config`).
- **Pausa de cortesia** entre requisições (`JsoupBuscador`).
- **Cache local** em disco com TTL (`CacheBuscador`) — não bate no servidor a cada teste.
- Cookie **`birthtime`** na Steam — evita a tela de verificação de idade em jogos adultos.
- **Resiliência**: cada fonte trata seus próprios erros e devolve lista (possivelmente vazia);
  a falha de uma loja não derruba a comparação das demais.

---

## UI web e API

`--web` sobe um servidor HTTP embutido (`com.sun.net.httpserver`, sem dependência nova) que
serve a página estática e uma pequena API JSON:

| Endpoint | O que faz |
|----------|-----------|
| `GET /` (+ `/styles.css`, `/app.js`) | a UI glanceável (busca, cards por jogo, destaque "Vale a pena", loja mais barata em verde, badges de desconto/nota, câmbio ao vivo no topo) |
| `GET /api/buscar?termo=...` | roda o comparador e devolve os jogos em JSON, já com o ranking e a cotação |
| `GET /api/exportar?termo=...&formato=csv\|html` | baixa a comparação como arquivo |

O front-end (vanilla JS) ainda filtra (só comparáveis) e ordena (mais barato / vale a pena /
maior desconto) no cliente, sem novo request. O JSON é montado por `RespostaBusca` e serializado
pelo `Json` (escritor mínimo, sem Jackson/Gson).

## Ranking "Vale a pena" e exportação

- **`ValeAPena`** combina **menor preço** (normalizado dentro do conjunto buscado) e **nota das
  reviews** num score 0..100 (pesos em `Config`). Jogos sem avaliação usam uma nota neutra.
- **`ExportadorComparacao`** gera **CSV** (uma linha por oferta, com escape RFC 4180) e um **HTML**
  autossuficiente e estilizado — ambos com o score embutido.

---

## Testes

`mvn test` — 34 testes, sem rede (fontes e câmbio são dublados; HTML vem de fixtures):

- `NormalizadorTituloTest` — variantes do mesmo jogo colapsam na mesma chave.
- `PrecosTest` — parsing (centavos, padrão BR, ponto decimal) e formatação BRL.
- `TaxaFixaConversorTest` — BRL passa direto, USD converte, moeda desconhecida não quebra.
- `CotacaoAoVivoConversorTest` — extrai a taxa do corpo da fonte; cai no fallback sem rede.
- `ComparadorPrecosTest` — cruzamento exato entre lojas e ordenação.
- `MatchAproximadoTest` — funde títulos quase iguais e respeita a guarda de sequência (números).
- `ValeAPenaTest` — ranking: mais barato/bem avaliado primeiro; nota neutra sem reviews.
- `ExportadorComparacaoTest` — cabeçalho/escape do CSV e documento HTML escapado.
- `FonteGamesPlanetTest` — seletores da GamesPlanet sobre HTML fixo, com conversão USD→BRL.

---

## Estado atual

- [x] Projeto Maven + Jsoup + JUnit, com fat jar executável.
- [x] Modelos, normalização e cruzamento.
- [x] `FonteSteam`, `FonteGamersGate` e `FonteGamesPlanet` raspando dados reais ponta a ponta.
- [x] Conversão de moeda (genérica por `data-currency`), com **cotação USD/BRL ao vivo** e fallback.
- [x] CLI com tabela e destaque da loja mais barata; notas de review da Steam exibidas.
- [x] Cache local, pausa, user-agent, cookie de idade.
- [x] **Match aproximado** de títulos (`SimilaridadeTitulo`) ligado ao comparador, com guarda de sequência.
- [x] **Ranking "Vale a pena"** (menor preço + nota das reviews).
- [x] **Exportação** da comparação para **CSV** e **HTML** (`ExportadorComparacao`).
- [x] **UI web** glanceável + API JSON (`--web`), servidor HTTP embutido.
- [x] Testes unitários (34, sem rede).

### Próximos passos / extras possíveis
- Quarta loja HTML validada pelo "teste dos 10 segundos" (nova `FonteLoja`).
- Cache da cotação compartilhado em disco (hoje é em memória, por execução).
- Histórico de preços (persistir buscas para mostrar variação ao longo do tempo).
- Paginação/“ver mais resultados” na UI quando a busca retorna muitos jogos.

---

## Notas para quem for evoluir (humano ou Claude Code)

- **Adicionar loja:** criar `FonteXyz implements FonteLoja` no pacote `fonte`, registrar na
  lista de fontes em `App.montar(...)` e adicionar o valor em `Loja`. Nada mais muda (a UI, o
  ranking e a exportação trabalham sobre `Jogo[]`, então herdam a loja nova de graça).
- **Seletores quebram quando o site muda de layout** — eles estão isolados dentro de cada
  `Fonte*`. A tabela "Lojas suportadas" acima lista os seletores atuais; atualize-a junto.
- **Preço é sempre BigDecimal em BRL** dentro do domínio. Conversão e parsing ficam em
  `cambio` e `util/Precos` — não espalhe lógica de moeda pelas fontes.
- Durante o desenvolvimento o **cache** (`<tmp>/comparador-precos-cache`) serve respostas por 6h;
  use `--no-cache` para forçar rede ao depurar seletores.
