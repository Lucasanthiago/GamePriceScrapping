"use strict";

// api — única camada que fala com o backend. Componentes não fazem fetch.

/** Busca a comparação de um termo. Lança em erro de rede/HTTP. */
export async function buscar(termo) {
  const resp = await fetch("/api/buscar?termo=" + encodeURIComponent(termo));
  if (!resp.ok) throw new Error("HTTP " + resp.status);
  return resp.json();
}

/** URLs de download da exportação (todos os resultados da busca, sem filtro de tela). */
export function urlsExportacao(termo) {
  const q = encodeURIComponent(termo);
  return {
    csv: "/api/exportar?formato=csv&termo=" + q,
    html: "/api/exportar?formato=html&termo=" + q,
  };
}
