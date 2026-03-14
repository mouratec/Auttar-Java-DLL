package com.auttar;

import java.math.BigDecimal;
import com.auttar.Enums.CodigoOperacao;

/**
 * 📦 Modelo de Requisição de Transação (DTO - Data Transfer Object)
 * * Qual o objetivo desta classe?
 * Em vez de passar 8 ou 9 parâmetros separados para o método iniciarTransacao()
 * (o que deixaria o código confuso), nós "empacotamos" todas as informações que o
 * caixa (PDV) coletou do usuário dentro deste único objeto.
 * * É o "formulário de pedido" que a interface preenche e entrega para a camada de serviço.
 */
public class TransactionRequest {

    // -------------------------------------------------------------------------
    // 📌 DADOS OBRIGATÓRIOS (Base de qualquer transação)
    // -------------------------------------------------------------------------

    /** * O "O Quê". Define a intenção da chamada na DLL.
     * Ex: É uma venda de Débito (101)? Uma configuração (800)? Um Cancelamento (128)?
     */
    private CodigoOperacao operacao;

    /** * O "Quanto". Representa o valor financeiro da operação.
     * Importante: Usamos BigDecimal para evitar erros de arredondamento de centavos
     * que ocorrem comumente ao usar Double ou Float nativos do Java.
     */
    private BigDecimal valor;

    /** * O "Vínculo Físico". É o número do Cupom Fiscal (NFC-e / SAT) emitido pelo seu sistema.
     * A Auttar usa isso para "amarrar" a transação do cartão ao recibo das compras do cliente.
     */
    private String documentoFiscal = "";


    // -------------------------------------------------------------------------
    // 🔙 DADOS PARA CANCELAMENTO E ESTORNO (A "Máquina do Tempo")
    // -------------------------------------------------------------------------

    /** * O "RG" da transação antiga. NSU (Número Sequencial Único).
     * Se o cliente quiser devolver uma compra, a DLL precisa saber exatamente
     * qual foi o NSU gerado no dia daquela compra para conseguir anular o pagamento.
     */
    private String nsuOriginal;

    /** * A data em que a compra original ocorreu (formato DDMMAA).
     * Trabalha em conjunto com o nsuOriginal para localizar a transação no servidor CTF.
     */
    private String dataOriginal;


    // -------------------------------------------------------------------------
    // 💳 DADOS PARA TRANSAÇÕES MANUAIS (Sem passar o cartão na maquininha)
    // -------------------------------------------------------------------------

    /** * O PAN (Primary Account Number). Usado exclusivamente em operações de
     * Crédito Digitado (120), como vendas por telefone ou e-commerce manual.
     */
    private String numeroCartao;

    /** * Validade do cartão físico (formato MMAA) exigida para aprovar Crédito Digitado.
     */
    private String vencimentoCartao;


    // -------------------------------------------------------------------------
    // 🛒 DADOS PARA PARCELAMENTO
    // -------------------------------------------------------------------------

    /** * Quantidade de cotas em que o valor será dividido.
     * Injetado no subcampo estendido 7008. Se for à vista, costuma ser nulo ou 1.
     */
    private Integer numeroParcelas;


    // =========================================================================
    // ⚙️ GETTERS E SETTERS (Métodos de Acesso Padrão)
    // =========================================================================

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

    public Integer getNumeroParcelas() { return numeroParcelas; }
    public void setNumeroParcelas(Integer numeroParcelas) { this.numeroParcelas = numeroParcelas; }
}