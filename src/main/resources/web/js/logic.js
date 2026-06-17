"use strict";

// logic — transformações puras sobre os jogos (sem DOM). Fáceis de testar.

/** Filtra os jogos conforme os controles. Retorna um novo array. */
export function filtrarJogos(jogos, { soComparaveis }) {
  return soComparaveis ? jogos.filter((j) => j.comparavel) : jogos.slice();
}

/** Ordena por critério: "barato" | "vale" | "desconto". Retorna um novo array. */
export function ordenarJogos(jogos, criterio) {
  const arr = jogos.slice();
  if (criterio === "vale") {
    arr.sort((a, b) => (b.valeAPena ?? -1) - (a.valeAPena ?? -1));
  } else if (criterio === "desconto") {
    arr.sort((a, b) => maxDesconto(b) - maxDesconto(a));
  } else { // "barato"
    arr.sort((a, b) => a.precoMaisBarato - b.precoMaisBarato);
  }
  return arr;
}

/** Maior desconto entre as ofertas de um jogo. */
export function maxDesconto(jogo) {
  return jogo.ofertas.reduce((m, of) => Math.max(m, of.desconto || 0), 0);
}

/** Melhor custo-benefício (maior "vale a pena") entre os jogos exibidos, ou null. */
export function melhorVale(jogos) {
  return jogos
    .filter((j) => j.valeAPena != null)
    .sort((a, b) => b.valeAPena - a.valeAPena)[0] || null;
}
