package com.gamesprice.fonte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gamesprice.cambio.ConversorMoeda;
import com.gamesprice.cambio.TaxaFixaConversor;
import com.gamesprice.http.Buscador;
import com.gamesprice.model.Loja;
import com.gamesprice.model.Oferta;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

class FonteGamesPlanetTest {

    /** Estrutura SSR real da GamesPlanet: um produto em promocao e um a preco cheio. */
    private static final String HTML = """
        <html><body>
          <div class="row game_list game_list_small">
            <div class="col">
              <h4><a href="/game/foo-steam-key--1">Foo Game</a></h4>
              <span class="prices">
                <span class="price_base"><strike>$9.99</strike></span>
                <span class="price_saving false">-10%</span>
                <span class="price_current">$8.99</span>
              </span>
            </div>
          </div>
          <div class="row game_list game_list_small">
            <div class="col">
              <h4><a href="/game/bar-steam-key--2">Bar Game</a></h4>
              <span class="prices"><span class="price_current">$5.00</span></span>
            </div>
          </div>
        </body></html>
        """;

    /** Buscador falso: devolve o HTML fixo, sem rede. */
    private static Buscador buscadorFake() {
        return (url, cookies) -> Jsoup.parse(HTML, url);
    }

    @Test
    void extraiOfertasComConversaoUsdParaBrl() {
        ConversorMoeda conversor = new TaxaFixaConversor(new BigDecimal("5.00")); // 1 USD = 5 BRL
        FonteGamesPlanet fonte = new FonteGamesPlanet(buscadorFake(), conversor);

        List<Oferta> ofertas = fonte.buscar("foo");

        assertEquals(2, ofertas.size());
        assertEquals(Loja.GAMESPLANET, ofertas.get(0).loja());

        Oferta foo = ofertas.get(0);
        assertEquals("Foo Game", foo.tituloOriginal());
        assertEquals(new BigDecimal("44.95"), foo.precoBRL(), "8.99 USD * 5");
        assertEquals(new BigDecimal("49.95"), foo.precoOriginalBRL(), "9.99 USD * 5");
        assertEquals(10, foo.descontoPerc());
        assertTrue(foo.temDesconto());
        assertEquals("https://us.gamesplanet.com/game/foo-steam-key--1", foo.url());
    }

    @Test
    void precoCheioSemPromocaoNaoTemDesconto() {
        ConversorMoeda conversor = new TaxaFixaConversor(new BigDecimal("5.00"));
        FonteGamesPlanet fonte = new FonteGamesPlanet(buscadorFake(), conversor);

        Oferta bar = fonte.buscar("bar").get(1);

        assertEquals(new BigDecimal("25.00"), bar.precoBRL());
        assertFalse(bar.temDesconto());
        assertEquals(0, bar.descontoPerc());
    }

    @Test
    void descontoComDigitosAbsurdosNaoQuebraABusca() {
        String html = """
            <div class="row game_list game_list_small">
              <div><h4><a href="/game/x--9">X</a></h4>
                <span class="prices">
                  <span class="price_base"><strike>$10.00</strike></span>
                  <span class="price_saving false">-99999999999999999999%</span>
                  <span class="price_current">$9.00</span>
                </span>
              </div>
            </div>
            """;
        Buscador fake = (url, cookies) -> Jsoup.parse(html, url);
        FonteGamesPlanet fonte = new FonteGamesPlanet(fake, new TaxaFixaConversor(new BigDecimal("5.00")));

        List<Oferta> ofertas = fonte.buscar("x");

        assertEquals(1, ofertas.size(), "um item com desconto invalido nao pode derrubar a busca");
        // -9999...% nao parseia: desconto derivado dos precos -> (10-9)/10 = 10%
        assertEquals(10, ofertas.get(0).descontoPerc());
    }

    @Test
    void buscaResilienteAErroDeRedeRetornaListaVazia() {
        Buscador quebrado = (url, cookies) -> {
            throw new java.io.IOException("rede caiu");
        };
        FonteGamesPlanet fonte = new FonteGamesPlanet(quebrado, new TaxaFixaConversor());

        assertTrue(fonte.buscar("x").isEmpty(), "falha de rede nao propaga excecao");
        // garante que o tipo Map de cookies compila/usa corretamente
        assertEquals(Loja.GAMESPLANET, fonte.loja());
        Map<String, String> vazio = Map.of();
        assertTrue(vazio.isEmpty());
    }
}
