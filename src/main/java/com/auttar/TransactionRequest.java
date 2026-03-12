package com.auttar;

import java.math.BigDecimal;
import com.auttar.Enums.CodigoOperacao;

/**
 * Classe modelo (POJO) que representa os dados a serem ENVIADOS para iniciar uma transação.
 * Agrupa todas as informações necessárias para uma operação em um único objeto.
 */
public class TransactionRequest {
    private CodigoOperacao operacao;       // Tipo da transação (Crédito, Débito, etc.)
    private BigDecimal valor;              // Valor da transação
    private String documentoFiscal = "";   // Número do documento fiscal associado
    private String nsuOriginal;            // NSU da transação original (para cancelamentos)
    private String dataOriginal;           // Data da transação original (para cancelamentos)
    private String numeroCartao;           // Número do cartão (para operações digitadas)
    private String vencimentoCartao;       // Vencimento do cartão (para operações digitadas)

    // ADICIONE ESTE CAMPO
    private Integer numeroParcelas; // Número de parcelas (para crédito parcelado)


    // Getters e Setters para todos os campos...
    public CodigoOperacao getOperacao() { return operacao; }
    public void setOperacao(CodigoOperacao operacao) { this.operacao = operacao; }
    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
    public String getDocumentoFiscal() { return documentoFiscal; }
    public void setDocumentoFiscal(String documentoFiscal) { this.documentoFiscal = documentoFiscal; }
    public String getNsuOriginal() { return nsuOriginal; }
    public void setNsuOriginal(String nsuOriginal) { this.nsuOriginal = nsuOriginal; }
    public String getDataOriginal() { return dataOriginal; }
    public void setDataOriginal(String dataOriginal) { this.dataOriginal = dataOriginal; }
    public String getNumeroCartao() { return numeroCartao; }
    public void setNumeroCartao(String numeroCartao) { this.numeroCartao = numeroCartao; }
    public String getVencimentoCartao() { return vencimentoCartao; }
    public void setVencimentoCartao(String vencimentoCartao) { this.vencimentoCartao = vencimentoCartao; }

    // ADICIONE ESTE GETTER E SETTER
    public Integer getNumeroParcelas() { return numeroParcelas; }
    public void setNumeroParcelas(Integer numeroParcelas) { this.numeroParcelas = numeroParcelas; }

}