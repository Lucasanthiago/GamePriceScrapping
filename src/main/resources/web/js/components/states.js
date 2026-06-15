"use strict";

// states — telas de carregando / vazio / erro (preenchem o contêiner #estado).

import { h, fromHTML } from "../dom.js";
import { ICONES } from "../config.js";

export function loadingState(termo) {
  return [
    h("div", { class: "spinner" }),
    h("h2", { text: `Buscando "${termo}"…` }),
    h("p", { text: "Raspando as lojas e cruzando os títulos." }),
  ];
}

export function emptyState(termo) {
  return [
    fromHTML(ICONES.vazio),
    h("h2", { text: "Nada encontrado" }),
    h("p", { text: `Não achamos "${termo}" nas lojas suportadas. Tente outro termo.` }),
  ];
}

export function errorState(err) {
  const msg = err && err.message ? err.message : String(err);
  return [
    fromHTML(ICONES.erro),
    h("h2", { text: "Algo deu errado" }),
    h("p", { text: msg }),
  ];
}
