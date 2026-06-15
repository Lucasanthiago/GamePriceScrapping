"use strict";

// gameCard — cartão de um jogo: título, tags e a lista de ofertas.

import { h } from "../dom.js";
import { offerRow } from "./offerRow.js";

export function gameCard(jogo) {
  // Só faz sentido marcar "menor preço" quando há mais de uma loja para comparar.
  const destacarMelhor = jogo.comparavel;

  const tags = h("div", { class: "tags" },
    jogo.comparavel ? h("span", { class: "tag comp", text: `${jogo.quantidadeLojas} lojas` }) : null,
    jogo.valeAPena != null ? h("span", { class: "tag vale", text: `vale ${jogo.valeAPena}` }) : null,
  );

  const topo = h("div", { class: "card-topo" },
    h("h3", { class: "card-titulo", text: jogo.titulo }),
    tags,
  );

  const ofertas = h("ul", { class: "ofertas" },
    jogo.ofertas.map((of) => offerRow(of, destacarMelhor)),
  );

  return h("article", { class: "card" }, topo, ofertas);
}
