"use strict";

// Identidade visual de cada loja (cor + sigla no badge).
const LOJAS = {
  "Steam":       { sigla: "S",  cor: "#1b9be0" },
  "GamersGate":  { sigla: "GG", cor: "#f0892a" },
  "GamesPlanet": { sigla: "GP", cor: "#2bc4a8" },
};
const LOJA_PADRAO = { sigla: "?", cor: "#5a6485" };

const $ = (sel) => document.querySelector(sel);

const form        = $("#formBusca");
const inputTermo  = $("#termo");
const btnBuscar   = $("#btnBuscar");
const painel      = $("#painel");
const estado      = $("#estado");
const grade       = $("#grade");
const destaque    = $("#destaque");
const resumoTexto = $("#resumoTexto");
const soComparaveis = $("#soComparaveis");
const ordem       = $("#ordem");
const exportCsv   = $("#exportCsv");
const exportHtml  = $("#exportHtml");
const cambioBox   = $("#cambio");
const cambioValor = $("#cambioValor");

let ultimaResposta = null; // resposta crua da API, para re-renderizar com filtros/ordem

// -------------------------------------------------------------- eventos
form.addEventListener("submit", (e) => {
  e.preventDefault();
  buscar(inputTermo.value.trim());
});

document.querySelectorAll(".chip").forEach((chip) => {
  chip.addEventListener("click", () => {
    inputTermo.value = chip.dataset.termo;
    buscar(chip.dataset.termo);
  });
});

soComparaveis.addEventListener("change", renderResultados);
ordem.addEventListener("change", renderResultados);

// -------------------------------------------------------------- busca
async function buscar(termo) {
  if (!termo) return;
  mostrarCarregando(termo);
  btnBuscar.disabled = true;
  try {
    const resp = await fetch("/api/buscar?termo=" + encodeURIComponent(termo));
    if (!resp.ok) throw new Error("HTTP " + resp.status);
    ultimaResposta = await resp.json();
    atualizarCambio(ultimaResposta.cotacaoUsdBrl);
    atualizarExportacao(termo);
    renderResultados();
  } catch (err) {
    ultimaResposta = null;       // evita que filtro/ordem re-renderizem dados antigos
    atualizarCambio(null);       // nao manter a cotacao da busca anterior como "ao vivo"
    mostrarErro(err);
  } finally {
    btnBuscar.disabled = false;
  }
}

// -------------------------------------------------------------- render
function renderResultados() {
  if (!ultimaResposta) return;
  const termo = ultimaResposta.termo;
  let jogos = ultimaResposta.jogos.slice();

  if (soComparaveis.checked) {
    jogos = jogos.filter((j) => j.comparavel);
  }
  ordenar(jogos, ordem.value);

  // nada veio do servidor: estado de "nada encontrado" em tela cheia (sem painel/controles)
  if (ultimaResposta.jogos.length === 0) {
    painel.hidden = true;
    mostrarVazio(termo);
    return;
  }

  // havia resultados, mas o filtro removeu todos: mantem os controles para o usuario desfazer
  estado.hidden = true;
  painel.hidden = false;
  if (jogos.length === 0) {
    destaque.hidden = true;
    resumoTexto.innerHTML =
      `Nenhum jogo <strong>comparável</strong> para <strong>${escapar(termo)}</strong>. ` +
      `Desmarque o filtro para ver todas as ofertas.`;
    grade.innerHTML = "";
    return;
  }

  resumoTexto.innerHTML =
    `<strong>${jogos.length}</strong> jogo(s) para <strong>${escapar(termo)}</strong> · ` +
    `<strong>${ultimaResposta.comparaveis}</strong> comparáveis entre lojas`;

  renderDestaque(jogos);

  grade.innerHTML = "";
  jogos.forEach((jogo, i) => {
    const card = renderCard(jogo);
    card.style.animationDelay = Math.min(i * 35, 350) + "ms";
    grade.appendChild(card);
  });
}

function renderDestaque(jogos) {
  // Melhor custo-benefício entre os exibidos (maior "vale a pena").
  const top = jogos
    .filter((j) => j.valeAPena != null)
    .sort((a, b) => b.valeAPena - a.valeAPena)[0];
  if (!top) { destaque.hidden = true; return; }

  const nota = top.notaReviews != null ? ` · <b>${top.notaReviews}%</b> de avaliações positivas` : "";
  destaque.hidden = false;
  destaque.innerHTML = `
    <span class="selo">★ Vale a pena</span>
    <div class="d-jogo">
      <p class="d-titulo">${escapar(top.titulo)}</p>
      <p class="d-meta">Mais barato na <b>${escapar(top.lojaMaisBarata)}</b>${nota} · score ${top.valeAPena}</p>
    </div>
    <div class="d-preco">
      <span class="pv">${escapar(top.precoMaisBaratoFormatado)}</span>
      <span class="pl">melhor custo-benefício da busca</span>
    </div>`;
}

function renderCard(jogo) {
  const card = document.createElement("article");
  card.className = "card";

  const topo = document.createElement("div");
  topo.className = "card-topo";

  const h = document.createElement("h3");
  h.className = "card-titulo";
  h.textContent = jogo.titulo;
  topo.appendChild(h);

  const tags = document.createElement("div");
  tags.className = "tags";
  if (jogo.comparavel) {
    tags.appendChild(tag("comp", `${jogo.quantidadeLojas} lojas`));
  }
  if (jogo.valeAPena != null) {
    tags.appendChild(tag("vale", `vale ${jogo.valeAPena}`));
  }
  topo.appendChild(tags);
  card.appendChild(topo);

  const lista = document.createElement("ul");
  lista.className = "ofertas";
  jogo.ofertas.forEach((of) => lista.appendChild(renderOferta(of)));
  card.appendChild(lista);

  return card;
}

function renderOferta(of) {
  const li = document.createElement("li");
  li.className = "oferta" + (of.maisBarata ? " melhor" : "");

  // loja
  const loja = document.createElement("div");
  loja.className = "loja";
  const info = LOJAS[of.loja] || LOJA_PADRAO;
  const badge = document.createElement("span");
  badge.className = "loja-badge";
  badge.style.background = info.cor;
  badge.textContent = info.sigla;
  loja.appendChild(badge);

  const nome = document.createElement("div");
  nome.className = "loja-nome";
  const b = document.createElement("b");
  b.textContent = of.loja;
  nome.appendChild(b);
  if (of.maisBarata) {
    const tagMelhor = document.createElement("span");
    tagMelhor.className = "melhor-tag";
    tagMelhor.textContent = "» mais barata";
    nome.appendChild(tagMelhor);
  }
  loja.appendChild(nome);
  li.appendChild(loja);

  // preço
  const precoCol = document.createElement("div");
  precoCol.className = "preco-col";
  const atual = document.createElement("div");
  atual.className = "preco-atual";
  atual.textContent = of.precoFormatado;
  precoCol.appendChild(atual);
  if (of.temDesconto) {
    const cheio = document.createElement("span");
    cheio.className = "preco-cheio";
    cheio.textContent = of.precoOriginalFormatado;
    precoCol.appendChild(cheio);
  }
  li.appendChild(precoCol);

  // ações (desconto, nota, link)
  const acoes = document.createElement("div");
  acoes.className = "acoes";
  if (of.temDesconto) {
    const desc = document.createElement("span");
    desc.className = "desconto";
    desc.textContent = "-" + of.desconto + "%";
    acoes.appendChild(desc);
  }
  if (of.nota != null) {
    const nota = document.createElement("span");
    nota.className = "nota";
    nota.textContent = of.nota + "%";
    nota.title = "Avaliações positivas";
    acoes.appendChild(nota);
  }
  acoes.appendChild(linkExterno(of.url));
  li.appendChild(acoes);

  return li;
}

// -------------------------------------------------------------- ordenação
function ordenar(jogos, criterio) {
  if (criterio === "vale") {
    jogos.sort((a, b) => (b.valeAPena ?? -1) - (a.valeAPena ?? -1));
  } else if (criterio === "desconto") {
    jogos.sort((a, b) => maxDesconto(b) - maxDesconto(a));
  } else { // barato
    jogos.sort((a, b) => a.precoMaisBarato - b.precoMaisBarato);
  }
}

function maxDesconto(jogo) {
  return jogo.ofertas.reduce((m, of) => Math.max(m, of.desconto || 0), 0);
}

// -------------------------------------------------------------- auxiliares UI
function tag(classe, texto) {
  const s = document.createElement("span");
  s.className = "tag " + classe;
  s.textContent = texto;
  return s;
}

function linkExterno(url) {
  const a = document.createElement("a");
  a.className = "link-ext";
  a.href = sanitizarUrl(url);
  a.target = "_blank";
  a.rel = "noopener noreferrer";
  a.title = "Abrir na loja";
  a.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7 17L17 7M17 7H8M17 7v9"/></svg>';
  return a;
}

function atualizarCambio(valor) {
  if (valor) {
    const fmt = Number(valor).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    cambioValor.textContent = "R$ " + fmt;
    cambioBox.hidden = false;
  } else {
    cambioBox.hidden = true;
  }
}

function atualizarExportacao(termo) {
  const q = encodeURIComponent(termo);
  exportCsv.href = "/api/exportar?formato=csv&termo=" + q;
  exportHtml.href = "/api/exportar?formato=html&termo=" + q;
}

function mostrarCarregando(termo) {
  painel.hidden = true;
  estado.hidden = false;
  estado.innerHTML = `<div class="spinner"></div><h2>Buscando "${escapar(termo)}"…</h2><p>Raspando as lojas e cruzando os títulos.</p>`;
}

function mostrarVazio(termo) {
  estado.hidden = false;
  estado.innerHTML = `<span class="emoji">🕹️</span><h2>Nada encontrado</h2><p>Não achamos "${escapar(termo)}" nas lojas suportadas. Tente outro termo.</p>`;
  painel.hidden = true;
}

function mostrarErro(err) {
  painel.hidden = true;
  estado.hidden = false;
  estado.innerHTML = `<span class="emoji">⚠️</span><h2>Algo deu errado</h2><p>${escapar(String(err.message || err))}</p>`;
}

// -------------------------------------------------------------- segurança
function escapar(texto) {
  const div = document.createElement("div");
  div.textContent = texto == null ? "" : String(texto);
  return div.innerHTML;
}

function sanitizarUrl(url) {
  try {
    const u = new URL(url, window.location.origin);
    return (u.protocol === "http:" || u.protocol === "https:") ? u.href : "#";
  } catch {
    return "#";
  }
}
