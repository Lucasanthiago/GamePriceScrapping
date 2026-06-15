"use strict";

// highlightCard — conteúdo do destaque "Vale a pena" (melhor custo-benefício).

import { h } from "../dom.js";

/** Retorna os nós para preencher o contêiner #destaque. */
export function highlightCard(jogo) {
  const metaNota = jogo.notaReviews != null
    ? h("span", null, " · ", h("b", { text: jogo.notaReviews + "%" }), " de avaliações positivas")
    : null;

  return [
    h("span", { class: "selo", text: "Vale a pena" }),
    h("div", { class: "d-jogo" },
      h("p", { class: "d-titulo", text: jogo.titulo }),
      h("p", { class: "d-meta" },
        "Mais barato na ", h("b", { text: jogo.lojaMaisBarata }), metaNota, ` · score ${jogo.valeAPena}`,
      ),
    ),
    h("div", { class: "d-preco" },
      h("span", { class: "pv", text: jogo.precoMaisBaratoFormatado }),
      h("span", { class: "pl", text: "melhor custo-benefício da busca" }),
    ),
  ];
}
