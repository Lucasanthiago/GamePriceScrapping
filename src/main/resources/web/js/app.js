"use strict";

// app — controlador: dono do estado, liga eventos e orquestra
// api (dados) -> logic (transformações puras) -> components (visual).

import { $, h } from "./dom.js";
import { buscar, urlsExportacao } from "./api.js";
import { filtrarJogos, ordenarJogos, melhorVale } from "./logic.js";
import { gameCard } from "./components/gameCard.js";
import { highlightCard } from "./components/highlightCard.js";
import { renderCambio } from "./components/cambioBadge.js";
import { loadingState, emptyState, errorState } from "./components/states.js";

const els = {
  form: $("#formBusca"),
  termo: $("#termo"),
  btn: $("#btnBuscar"),
  painel: $("#painel"),
  estado: $("#estado"),
  grade: $("#grade"),
  destaque: $("#destaque"),
  resumo: $("#resumoTexto"),
  soComparaveis: $("#soComparaveis"),
  ordem: $("#ordem"),
  exportCsv: $("#exportCsv"),
  exportHtml: $("#exportHtml"),
  cambio: $("#cambio"),
  cambioValor: $("#cambioValor"),
};

let ultimaResposta = null; // resposta crua da API, para re-renderizar com filtros/ordem

// -------------------------------------------------------------- eventos
els.form.addEventListener("submit", (e) => {
  e.preventDefault();
  iniciarBusca(els.termo.value.trim());
});

document.querySelectorAll(".chip").forEach((chip) => {
  chip.addEventListener("click", () => {
    els.termo.value = chip.dataset.termo;
    iniciarBusca(chip.dataset.termo);
  });
});

els.soComparaveis.addEventListener("change", render);
els.ordem.addEventListener("change", render);

// -------------------------------------------------------------- busca
async function iniciarBusca(termo) {
  if (!termo) return;
  mostrarEstado(loadingState(termo));
  els.painel.hidden = true;
  els.btn.disabled = true;
  try {
    ultimaResposta = await buscar(termo);
    renderCambio(els.cambio, els.cambioValor, ultimaResposta.cotacaoUsdBrl);
    aplicarExportacao(termo);
    render();
  } catch (err) {
    ultimaResposta = null;                              // não re-renderiza dados antigos
    renderCambio(els.cambio, els.cambioValor, null);    // não manter cotação da busca anterior
    mostrarEstado(errorState(err));
    els.painel.hidden = true;
  } finally {
    els.btn.disabled = false;
  }
}

// -------------------------------------------------------------- render
function render() {
  if (!ultimaResposta) return;
  const { termo, jogos: todos, comparaveis } = ultimaResposta;

  // nada veio do servidor: estado de "nada encontrado" em tela cheia
  if (todos.length === 0) {
    els.painel.hidden = true;
    mostrarEstado(emptyState(termo));
    return;
  }

  const jogos = ordenarJogos(
    filtrarJogos(todos, { soComparaveis: els.soComparaveis.checked }),
    els.ordem.value,
  );

  els.estado.hidden = true;
  els.painel.hidden = false;

  // havia resultados, mas o filtro removeu todos: mantém os controles para desfazer
  if (jogos.length === 0) {
    els.destaque.hidden = true;
    setResumo(h("span", null,
      "Nenhum jogo ", h("strong", { text: "comparável" }), " para ", h("strong", { text: termo }),
      ". Desmarque o filtro para ver todas as ofertas.",
    ));
    els.grade.replaceChildren();
    return;
  }

  setResumo(h("span", null,
    h("strong", { text: String(jogos.length) }), " jogo(s) para ", h("strong", { text: termo }), " · ",
    h("strong", { text: String(comparaveis) }), " comparáveis entre lojas",
  ));

  renderDestaque(jogos);
  renderLista(jogos);
}

function renderDestaque(jogos) {
  const top = melhorVale(jogos);
  if (!top) {
    els.destaque.hidden = true;
    return;
  }
  els.destaque.hidden = false;
  els.destaque.replaceChildren(...highlightCard(top));
}

function renderLista(jogos) {
  els.grade.replaceChildren(...jogos.map((jogo, i) => {
    const card = gameCard(jogo);
    card.style.animationDelay = Math.min(i * 35, 350) + "ms";  // entrada escalonada
    return card;
  }));
}

// -------------------------------------------------------------- helpers de tela
function setResumo(node) {
  els.resumo.replaceChildren(node);
}

function mostrarEstado(nodes) {
  els.estado.hidden = false;
  els.estado.replaceChildren(...nodes);
}

function aplicarExportacao(termo) {
  const { csv, html } = urlsExportacao(termo);
  els.exportCsv.href = csv;
  els.exportHtml.href = html;
}
