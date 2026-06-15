package com.gamesprice.fonte;

import com.gamesprice.model.Loja;
import com.gamesprice.model.Oferta;
import java.util.List;

/**
 * Fonte de ofertas de uma loja. Cada loja implementa esta interface, de modo que o
 * comparador trabalhe com qualquer numero de lojas sem conhecer os detalhes de cada uma
 * (princípio aberto/fechado: adicionar loja = nova implementacao, nada mais muda).
 */
public interface FonteLoja {

    /** Loja que esta fonte representa (para rotulagem e diagnostico). */
    Loja loja();

    /**
     * Busca ofertas para o termo informado.
     *
     * <p>Implementacoes devem ser resilientes: em falha de rede ou mudanca de layout,
     * preferem retornar uma lista (possivelmente vazia) a propagar excecao, para que a
     * falha de uma loja nao derrube a comparacao das demais.
     *
     * @param termo texto digitado pelo usuario (ex.: "elden ring")
     * @return ofertas encontradas; nunca null
     */
    List<Oferta> buscar(String termo);
}
