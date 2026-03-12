package com.auttar;

import java.util.HashMap;
import java.util.Map;
import com.auttar.Enums.CodigoOperacao;
import com.auttar.Enums.Subcampo;

/**
 * Classe modelo (POJO) que representa a resposta RECEBIDA após o processamento de uma transação.
 * Estrutura os dados retornados pela DLL de forma organizada.
 */
public class TransactionResponse {
    private TransactionRequest operacaoRequest;     // Guarda a requisição original que gerou esta resposta.
    private boolean aprovada;                       // Indica se a transação foi aprovada (true) ou não (false).
    private String codigoRetorno = "";              // Código de retorno principal da transação (ex: "00" para sucesso).
    private String codigoErro = "";                 // Código de erro específico, se houver.
    private String mensagemRetorno = "";            // Mensagem descritiva do resultado.
    private Map<String, String> camposRetornados = new HashMap<>(); // Mapa que armazena todos os subcampos retornados pela DLL.

    // Métodos "get" inteligentes que extraem e formatam dados específicos do mapa 'camposRetornados'.

    public String getNsuCtf() {
        return camposRetornados.get(String.valueOf(Subcampo.NsuCtf.valor));
    }
    public String getComprovanteCliente() {
        String comp = camposRetornados.get(String.valueOf(Subcampo.CupomCliente.valor));
        // A DLL retorna quebras de linha como '\\', este método já as converte para '\n'.
        return (comp != null) ? comp.replace('\\', '\n') : null;
    }
    public String getComprovanteLojista() {
        String comp = camposRetornados.get(String.valueOf(Subcampo.CupomLojista.valor));
        return (comp != null) ? comp.replace('\\', '\n') : null;
    }

    public CodigoOperacao getOperacao() {
        String codOperacaoStr = camposRetornados.get(String.valueOf(Subcampo.CodigoTransacao.valor));
        if (codOperacaoStr != null) {
            try {
                int codOperacao = Integer.parseInt(codOperacaoStr);
                for (CodigoOperacao op : CodigoOperacao.values()) {
                    if (op.valor == codOperacao) {
                        return op;
                    }
                }
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // Getters e Setters para todos os campos...
    public TransactionRequest getOperacaoRequest() { return operacaoRequest; }
    public void setOperacaoRequest(TransactionRequest operacaoRequest) { this.operacaoRequest = operacaoRequest; }
    public boolean isAprovada() { return aprovada; }
    public void setAprovada(boolean aprovada) { this.aprovada = aprovada; }
    public String getCodigoRetorno() { return codigoRetorno; }
    public void setCodigoRetorno(String codigoRetorno) { this.codigoRetorno = codigoRetorno; }
    public String getCodigoErro() { return codigoErro; }
    public void setCodigoErro(String codigoErro) { this.codigoErro = codigoErro; }
    public String getMensagemRetorno() { return mensagemRetorno; }
    public void setMensagemRetorno(String mensagemRetorno) { this.mensagemRetorno = mensagemRetorno; }
    public Map<String, String> getCamposRetornados() { return camposRetornados; }
    public void setCamposRetornados(Map<String, String> camposRetornados) { this.camposRetornados = camposRetornados; }
}