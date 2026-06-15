"use strict";

// offerRow — uma linha de oferta (loja, preço, desconto, nota, link).

import { h, sanitizarUrl } from "../dom.js";
import { infoLoja } from "../format.js";
import { ICONES } from "../config.js";

/**
 * @param of             oferta vinda da API
 * @param destacarMelhor se true e for a mais barata, ganha a faixa "menor preço"
 */
export function offerRow(of, destacarMelhor) {
  const melhor = destacarMelhor && of.maisBarata;
  const loja = infoLoja(of.loja);

  const colLoja = h("div", { class: "loja" },
    h("span", { class: "loja-badge", style: { background: loja.cor }, text: loja.sigla }),
    h("div", { class: "loja-nome" },
      h("b", { text: of.loja }),
      melhor ? h("span", { class: "melhor-tag", text: "Menor preço" }) : null,
    ),
  );

  const colPreco = h("div", { class: "preco-col" },
    h("div", { class: "preco-atual", text: of.precoFormatado }),
    of.temDesconto ? h("span", { class: "preco-cheio", text: of.precoOriginalFormatado }) : null,
  );

  const colAcoes = h("div", { class: "acoes" },
    of.temDesconto ? h("span", { class: "desconto", text: "-" + of.desconto + "%" }) : null,
    of.nota != null ? h("span", { class: "nota", title: "Avaliações positivas", text: of.nota + "%" }) : null,
    h("a", {
      class: "link-ext",
      href: sanitizarUrl(of.url),
      target: "_blank",
      rel: "noopener noreferrer",
      title: "Abrir na loja",
      "aria-label": "Abrir " + of.loja + " em nova aba",
      html: ICONES.linkExterno,
    }),
  );

  return h("li", { class: "oferta" + (melhor ? " melhor" : "") }, colLoja, colPreco, colAcoes);
}
