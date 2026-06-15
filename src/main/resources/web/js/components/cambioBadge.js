"use strict";

// cambioBadge — mostra/oculta a cotação USD/BRL ao vivo no cabeçalho.

import { formatarCambio } from "../format.js";

/**
 * @param caixa  contêiner #cambio (mostrado só quando há cotação)
 * @param valorEl  span #cambioValor
 * @param valor  cotação numérica, ou null para ocultar
 */
export function renderCambio(caixa, valorEl, valor) {
  if (valor) {
    valorEl.textContent = formatarCambio(valor);
    caixa.hidden = false;
  } else {
    caixa.hidden = true;
  }
}
