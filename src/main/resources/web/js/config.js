"use strict";

// config — constantes de apresentação (identidade das lojas e ícones SVG).

/** Cor + sigla do badge de cada loja. Loja nova: basta um item aqui. */
export const LOJAS = {
  "Steam":       { sigla: "S",  cor: "#1b9be0" },
  "GamersGate":  { sigla: "GG", cor: "#f0892a" },
  "GamesPlanet": { sigla: "GP", cor: "#2bc4a8" },
};
export const LOJA_PADRAO = { sigla: "?", cor: "#5a6485" };

/** Ícones em SVG (sem emojis), usados nos componentes via h(..., { html }). */
export const ICONES = {
  linkExterno:
    '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M7 17L17 7M17 7H8M17 7v9"/></svg>',
  vazio:
    '<svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4.3-4.3"/></svg>',
  erro:
    '<svg class="ico" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"><path d="M12 9v4M12 17h.01M10.3 3.9L1.8 18a2 2 0 0 0 1.7 3h17a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0z"/></svg>',
};
