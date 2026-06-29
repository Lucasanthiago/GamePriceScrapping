# Deploy — Comparador de Preços de Jogos

O projeto roda em **dois hosts separados**, porque a Vercel não executa Java/JVM:

| Camada   | Host   | URL                                       |
| -------- | ------ | ----------------------------------------- |
| Frontend | Vercel | https://web-green-five-83.vercel.app      |
| Backend  | Render | https://gamepricescrapping.onrender.com   |

O frontend (HTML/CSS/JS estático) é servido pela Vercel e chama a API do
backend Java hospedado no Render.

---

## 1. Backend (Render) — sobe sozinho via Git

O Render faz **auto-deploy** a cada push no `main`. Depois de qualquer
mudança no código Java:

```powershell
git add -A
git commit -m "sua mensagem"
git push origin main
```

O Render detecta o push e rebuilda o Docker automaticamente (~3-5 min).
Não precisa rodar mais nada além do `git push`.

## 2. Frontend (Vercel) — comando manual

Da pasta do frontend:

```powershell
cd "c:\Users\santo\OneDrive\Área de Trabalho\GamePriceScrapping\src\main\resources\web"
vercel --prod --yes
```

## Resumo prático

| O que mudou                | Comando                                            |
| -------------------------- | -------------------------------------------------- |
| Só HTML/CSS/JS (frontend)  | `vercel --prod --yes` (na pasta `web`)             |
| Código Java (backend)      | `git push origin main` (Render rebuilda sozinho)   |
| Mudou os dois              | `git push origin main` **e** `vercel --prod --yes` |

---

## ⚠️ Importante antes de apresentar

O backend no plano free do Render **dorme após ~15 min sem uso**. A primeira
requisição depois disso leva **30-60s** para acordar — vai parecer que travou.

**Aqueça o backend 1-2 minutos antes de apresentar.** Abra a URL no navegador
ou rode:

```powershell
curl https://gamepricescrapping.onrender.com/api/destaques
```

Assim, quando você mostrar o site, ele já responde na hora.

---

## Como funciona o wiring (referência)

- [src/main/resources/web/index.html](src/main/resources/web/index.html) define
  `window.__API_BASE__` com a URL do backend no Render.
- [src/main/resources/web/js/config.js](src/main/resources/web/js/config.js)
  exporta `API_BASE` a partir disso.
- [src/main/resources/web/js/api.js](src/main/resources/web/js/api.js) prefixa
  todas as chamadas `fetch` com `API_BASE`.
- `API_BASE` vazio (`""`) = mesma origem → volta ao modo local tudo-em-um
  (`java -jar comparador-precos-jogos.jar --web`).

## Rodar local (tudo-em-um)

```powershell
mvn -DskipTests package
java -jar target/comparador-precos-jogos.jar --web
# abre em http://localhost:8080
```

> Nesse modo, para o front local chamar o backend local, `window.__API_BASE__`
> em `index.html` precisa estar vazio (`""`). Hoje está apontado para o Render.
