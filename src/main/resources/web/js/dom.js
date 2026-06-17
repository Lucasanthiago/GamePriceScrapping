"use strict";

// dom — utilitários mínimos de DOM compartilhados por todos os componentes.

/** querySelector curto. */
export const $ = (sel, raiz = document) => raiz.querySelector(sel);

/**
 * Mini "hyperscript": cria um elemento de forma declarativa e segura.
 *
 *   h("a", { class: "x", href: url, onClick: fn }, "texto", outroNo)
 *
 * Props especiais: `class`, `text` (textContent), `html` (innerHTML — só para
 * SVG/markup confiável), `dataset`, `style` (objeto) e `onEvento` (listener).
 * Strings passadas como filhos viram nós de texto (escapadas pelo DOM), então
 * não há risco de injeção ao usar dados das lojas.
 */
export function h(tag, props, ...filhos) {
  const el = document.createElement(tag);
  if (props) {
    for (const [k, v] of Object.entries(props)) {
      if (v == null || v === false) continue;
      if (k === "class") el.className = v;
      else if (k === "text") el.textContent = v;
      else if (k === "html") el.innerHTML = v;
      else if (k === "dataset") Object.assign(el.dataset, v);
      else if (k === "style") Object.assign(el.style, v);
      else if (k.startsWith("on") && typeof v === "function") el.addEventListener(k.slice(2).toLowerCase(), v);
      else el.setAttribute(k, v);
    }
  }
  for (const c of filhos.flat()) {
    if (c == null || c === false) continue;
    el.append(c.nodeType ? c : document.createTextNode(String(c)));
  }
  return el;
}

/** Converte uma string de markup confiável (ex.: um SVG) num nó de elemento. */
export function fromHTML(markup) {
  const t = document.createElement("template");
  t.innerHTML = markup.trim();
  return t.content.firstElementChild;
}

/** Só deixa passar http/https; protege os href= vindos das lojas. */
export function sanitizarUrl(url) {
  try {
    const u = new URL(url, window.location.origin);
    return (u.protocol === "http:" || u.protocol === "https:") ? u.href : "#";
  } catch {
    return "#";
  }
}
