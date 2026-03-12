package com.auttar;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Classe final utilitária (não pode ser herdada) com métodos estáticos para tarefas comuns.
 * Centraliza a lógica de formatação de dados para manter o código principal mais limpo.
 */
public final class Helpers {

    // Construtor privado para impedir que esta classe seja instanciada.
    private Helpers() {}

    /**
     * Formata um valor BigDecimal para uma string de 12 dígitos representando o valor em centavos.
     * Ex: R$ 10.50 -> "000000001050"
     * @param valor O valor monetário.
     * @return A string formatada.
     */
    public static String formataValor(BigDecimal valor) {
        if (valor == null) valor = BigDecimal.ZERO;
        long valorEmCentavos = valor.multiply(new BigDecimal("100")).longValue();
        return String.format("%012d", valorEmCentavos);
    }

    /**
     * Retorna a data atual no formato "yyyyMMdd".
     * @return A data formatada.
     */
    public static String getDataAtual() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * Retorna a data atual no formato "ddMMyy".
     * @return A data formatada.
     */
    public static String getDataAtualDdmmaa() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMMyy"));
    }

    /**
     * Obtém um valor de um mapa de forma segura, retornando null se a chave não existir.
     * @param dictionary O mapa de onde o valor será extraído.
     * @param key A chave a ser procurada.
     * @return O valor correspondente ou null.
     */
    public static String getValueOrDefault(Map<String, String> dictionary, String key) {
        return dictionary.getOrDefault(key, null);
    }
}