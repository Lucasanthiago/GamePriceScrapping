"use strict";

// format — formatação de apresentação (lojas, moeda).

import { LOJAS, LOJA_PADRAO } from "./config.js";

/** Identidade visual (cor/sigla) de uma loja, com fallback seguro. */
export const infoLoja = (loja) => LOJAS[loja] || LOJA_PADRAO;

/** Número da cotação -> "R$ 5,09". */
export function formatarCambio(valor) {
  const n = Number(valor).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  return "R$ " + n;
}
