# Comparador de Preços de Jogos

Web scraping em **Java 17 + Jsoup** que raspa lojas de jogos, cruza os títulos e mostra
**onde cada jogo está mais barato**. Trabalho de Desenvolvimento Web — a técnica central é
**parsing de HTML com seletores CSS** (`doc.select(...)`).

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

# executar
java -jar target/comparador-precos-jogos.jar                 # modo interativo (digita o jogo)
java -jar target/comparador-precos-jogos.jar elden ring      # busca direta
java -jar target/comparador-precos-jogos.jar --verbose witcher   # logs de scraping no stderr
java -jar target/comparador-precos-jogos.jar --no-cache zelda    # ignora o cache local

# ou, sem empacotar:
mvn exec:java -Dexec.args="elden ring"
```

Flags: `--verbose`/`-v` (logs), `--no-cache` (não usa o cache em disco).

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

**Descartadas (SPA, preço só via JS):** Nuuvem, Epic, GOG, Humble, Green Man Gaming.

**Nota sobre moeda:** a GamersGate atualmente entrega preços em **BRL** por geolocalização
(`data-currency="BRL"`). O código não assume isso: passa todo preço pelo `ConversorMoeda`, que
converte apenas quando a moeda for USD (taxa fixa `Config.USD_BRL`). Se a loja voltar a entregar
USD, a comparação continua correta sem mudança de código.

---

## Arquitetura

Cada loja fica atrás de uma interface (`FonteLoja`), então adicionar loja = nova classe, sem
mexer no resto (aberto/fechado). A busca HTTP é abstraída (`Buscador`) para centralizar as boas
práticas e permitir cache e testes sem rede.

```
App (CLI / composition root)
 └─ ComparadorPrecos
     ├─ List<FonteLoja>
     │   ├─ FonteSteam      ─┐
     │   └─ FonteGamersGate ─┤── usam ──> Buscador (HTTP)
     │                       │            ├─ JsoupBuscador (rede: userAgent, timeout, pausa)
     │                       │            └─ CacheBuscador (decorator: HTML em disco c/ TTL)
     │                       └─ FonteGamersGate usa ──> ConversorMoeda
     │                                                  └─ TaxaFixaConversor (USD->BRL)
     └─ NormalizadorTitulo (chave de cruzamento)

TabelaPrecos  ── renderiza os Jogo[] para o terminal
```

### Pacotes (`src/main/java/com/gamesprice`)

| Pacote | Responsabilidade |
|--------|------------------|
| `model` | `Loja` (enum), `Oferta` (record imutável, preço em BRL), `Jogo` (agrupa ofertas) |
| `normalizacao` | `NormalizadorTitulo` (forma canônica do título), `SimilaridadeTitulo` (Levenshtein) |
| `http` | `Buscador` (interface), `JsoupBuscador` (rede), `CacheBuscador` (decorator de cache) |
| `cambio` | `ConversorMoeda` (interface), `TaxaFixaConversor` (taxa fixa do MVP) |
| `fonte` | `FonteLoja` (interface), `FonteSteam`, `FonteGamersGate` |
| `comparador` | `ComparadorPrecos` (roda fontes + cruza por título normalizado) |
| `cli` | `TabelaPrecos` (formatação da saída) |
| `config` | `Config` (constantes: userAgent, timeout, pausa, cache, taxa) |
| `util` | `Precos` (parsing/format de moeda), `Log` (log simples no stderr) |

---

## Cruzamento de títulos (o desafio do projeto)

O mesmo jogo aparece com nomes diferentes em cada loja. `NormalizadorTitulo` reduz tudo a uma
chave canônica antes de comparar:

- minúsculas → remove acentos (NFD) → remove apóstrofos (juntam letras: `assassin's` → `assassins`)
- demais símbolos (®, ™, ©, `:`, `-`) viram separador → remove sufixos de edição
  ("Deluxe Edition", "GOTY", "Game of the Year", "Ultimate Edition"...) → colapsa espaços

O `ComparadorPrecos` agrupa as ofertas por essa chave. `SimilaridadeTitulo` (distância de
Levenshtein) está pronto como reforço opcional para matches aproximados (extra).

---

## Boas práticas de scraping aplicadas

- **User-Agent** identificando o projeto e **timeout** em toda requisição (`Config`).
- **Pausa de cortesia** entre requisições (`JsoupBuscador`).
- **Cache local** em disco com TTL (`CacheBuscador`) — não bate no servidor a cada teste.
- Cookie **`birthtime`** na Steam — evita a tela de verificação de idade em jogos adultos.
- **Resiliência**: cada fonte trata seus próprios erros e devolve lista (possivelmente vazia);
  a falha de uma loja não derruba a comparação das demais.

---

## Testes

`mvn test` — 14 testes, sem rede:

- `NormalizadorTituloTest` — variantes do mesmo jogo colapsam na mesma chave.
- `PrecosTest` — parsing (centavos, padrão BR, ponto decimal) e formatação BRL.
- `TaxaFixaConversorTest` — BRL passa direto, USD converte, moeda desconhecida não quebra.
- `ComparadorPrecosTest` — cruzamento entre lojas (usa `FonteLoja` fake) e ordenação.

---

## Estado atual

- [x] Projeto Maven + Jsoup + JUnit, com fat jar executável.
- [x] Modelos, normalização e cruzamento.
- [x] `FonteSteam` e `FonteGamersGate` raspando dados reais ponta a ponta.
- [x] Conversão de moeda (genérica por `data-currency`).
- [x] CLI com tabela e destaque da loja mais barata; notas de review da Steam exibidas.
- [x] Cache local, pausa, user-agent, cookie de idade.
- [x] Testes unitários.

### Próximos passos / extras possíveis
- "Vale a pena": ranking combinando menor preço + nota das reviews.
- Cotação USD/BRL raspada ao vivo (nova implementação de `ConversorMoeda`).
- Exportar a comparação para CSV/HTML (nova classe ao lado de `TabelaPrecos`).
- Terceira loja HTML validada pelo "teste dos 10 segundos" (nova `FonteLoja`).
- Match aproximado ligando `SimilaridadeTitulo` ao agrupamento do comparador.

---

## Notas para quem for evoluir (humano ou Claude Code)

- **Adicionar loja:** criar `FonteXyz implements FonteLoja` no pacote `fonte`, registrar em
  `App.montarComparador(...)` e adicionar o valor em `Loja`. Nada mais muda.
- **Seletores quebram quando o site muda de layout** — eles estão isolados dentro de cada
  `Fonte*`. A tabela "Lojas suportadas" acima lista os seletores atuais; atualize-a junto.
- **Preço é sempre BigDecimal em BRL** dentro do domínio. Conversão e parsing ficam em
  `cambio` e `util/Precos` — não espalhe lógica de moeda pelas fontes.
- Durante o desenvolvimento o **cache** (`<tmp>/comparador-precos-cache`) serve respostas por 6h;
  use `--no-cache` para forçar rede ao depurar seletores.
