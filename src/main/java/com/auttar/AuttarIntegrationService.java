package com.auttar;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.auttar.Enums.*;

/**
 * Camada de Serviço que encapsula a lógica de comunicação com a CtfClientLibrary.
 * Ela serve como um intermediário, simplificando as chamadas que a camada de apresentação (App.java) precisa fazer.
 */
public class AuttarIntegrationService {
    private final String dataFiscal; // Data fiscal da operação, definida uma vez na inicialização do serviço.

    public AuttarIntegrationService() {
        this.dataFiscal = Helpers.getDataAtual();
    }

    /**
     * Inicializa o cliente TEF (CTFClient). Esta função deve ser chamada uma vez no início da aplicação.
     * @return true se a inicialização for bem-sucedida, false caso contrário.
     */
    public boolean inicializarCliente() {
        byte[] resultado = new byte[256];
        java.util.Arrays.fill(resultado, (byte) ' ');
        // Preenche o buffer de resultado com "00" para indicar sucesso inicial, a DLL pode sobrescrever.
        System.arraycopy("00".getBytes(), 0, resultado, 0, 2);

        // Parâmetros de configuração para a DLL. "[suporte-https=1]" habilita comunicação segura.
        String strParametros = "[suporte-https=1]";

        try {
            System.out.println("INFO: Chamando iniciaClientCTF...");
            // Chama a função nativa através da interface JNA.
            CtfClientLibrary.INSTANCE.iniciaClientCTF(resultado, "000000000000", "1.0.0", "PDV_JAVA",
                    "00", "HTTPS", "0", "1", "1", strParametros);

            // Converte a resposta do buffer de bytes para String e verifica o resultado.
            String resposta = new String(resultado, StandardCharsets.UTF_8).trim();
            if ("00".equals(resposta)) {
                System.out.println("SUCESSO: CTFClient inicializado com sucesso.");
                return true;
            } else {
                System.out.println("ERRO: Falha ao inicializar o CTFClient. Código: " + resposta);
                return false;
            }
        } catch (Exception ex) {
            System.out.println("ERRO FATAL durante a inicialização: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * Inicia uma transação TEF, seja ela uma venda, cancelamento, etc.
     * @param request Objeto contendo todos os dados da transação a ser iniciada.
     * @param numTrans Número sequencial da transação no dia.
     * @return O código de retorno da função da DLL.
     */
    public int iniciarTransacao(TransactionRequest request, String numTrans) {
        byte[] resultado = new byte[256];
        // Formata os dados do objeto Request para as strings esperadas pela DLL.
        String operacaoStr = String.format("%03d", request.getOperacao().valor);
        String valorStr = Helpers.formataValor(request.getValor());
        String docFiscalStr = String.format("%-20s", request.getDocumentoFiscal() != null ? request.getDocumentoFiscal() : "");

        // Verifica se a operação requer dados estendidos (ex: cancelamento, crédito digitado, parcelamento).
        String dadosExt = construirDadosEstendidos(request);

        if (dadosExt.isEmpty()) {
            // Chama a função padrão se não houver dados estendidos.
            CtfClientLibrary.INSTANCE.iniciaTransacaoCTF(resultado, operacaoStr, valorStr, docFiscalStr, dataFiscal, numTrans);
        } else {
            // Chama a função estendida ('ext') se houver dados adicionais.
            CtfClientLibrary.INSTANCE.iniciaTransacaoCTFext(resultado, operacaoStr, valorStr, docFiscalStr, dataFiscal, numTrans, dadosExt);
        }
        // Retorna o código de status da chamada.
        return Integer.parseInt(new String(resultado).trim());
    }

    /**
     * Continua uma transação interativa. Esta função é chamada em um loop até que a transação termine.
     * @param comando Buffer que envia/recebe o comando a ser executado.
     * @param campo Buffer que envia/recebe o campo relacionado ao comando.
     * @param valor Buffer que envia/recebe o valor relacionado ao comando.
     * @param tamanho Buffer que envia/recebe o tamanho do valor.
     * @param display Buffer que recebe mensagens a serem exibidas no PDV.
     * @return O código de retorno da função, tipicamente 99 (AguardandoContinuação) ou 0 (Sucesso).
     */
    public int continuarTransacao(byte[] comando, byte[] campo, byte[] valor, byte[] tamanho, byte[] display) {
        byte[] resultado = new byte[256];
        CtfClientLibrary.INSTANCE.continuaTransacaoCTF(resultado, comando, campo, valor, tamanho, display);
        return Integer.parseInt(new String(resultado).trim());
    }

    /**
     * Finaliza uma transação, confirmando-a (commit) ou desfazendo-a (rollback).
     * @param confirmar "1" para confirmar, "0" para desfazer.
     * @param numTrans O número da transação a ser finalizada.
     * @param camposRetornados Mapa para armazenar o resultado da finalização.
     */
    public void finalizarTransacao(String confirmar, String numTrans, Map<String, String> camposRetornados) {
        byte[] resultado = new byte[256];
        CtfClientLibrary.INSTANCE.finalizaTransacaoCTF(resultado, confirmar, numTrans, dataFiscal);
        camposRetornados.put("ResultadoFinalizacao", new String(resultado).trim());
    }

    /**
     * Constrói a string de dados estendidos no formato "[chave1=valor1;chave2=valor2]".
     * @param request O objeto de requisição da transação.
     * @return A string formatada ou uma string vazia se não houver dados estendidos.
     */
    private String construirDadosEstendidos(TransactionRequest request) {
        List<String> dados = new ArrayList<>();
        if (request == null) return ""; // Proteção

        // Adiciona o número de parcelas (7008) se ele foi definido
        if (request.getNumeroParcelas() != null && request.getNumeroParcelas() > 0) {
            // Formata o número de parcelas com 2 dígitos (ex: "03", "12")
            dados.add(String.valueOf(Subcampo.NumeroParcelas.valor) + "=" + String.format("%02d", request.getNumeroParcelas()));
        }

        if (request.getOperacao() == null) {
            // Retorna apenas os dados de parcela se a operação for nula
            return dados.isEmpty() ? "" : "[" + String.join(";", dados) + "]";
        }

        // Monta a lista de pares chave=valor de acordo com o tipo de operação.
        switch (request.getOperacao()) {
            case ConsultaPix: case CancelamentoGenerico: case DevolucaoPix:
                if (request.getNsuOriginal() != null) dados.add(Subcampo.NsuCtfOriginal.valor + "=" + request.getNsuOriginal());
                if (request.getDataOriginal() != null) dados.add(Subcampo.DataTransacaoOriginal.valor + "=" + request.getDataOriginal());
                break;
            case CancelamentoCreditoDigitado:
                if (request.getNsuOriginal() != null) dados.add(Subcampo.NsuCtfOriginal.valor + "=" + request.getNsuOriginal());
                if (request.getDataOriginal() != null) dados.add(Subcampo.DataTransacaoOriginal.valor + "=" + request.getDataOriginal());
                if (request.getNumeroCartao() != null) dados.add(Subcampo.NumeroCartao.valor + "=" + request.getNumeroCartao());
                break;
            case CreditoDigitado:
                if (request.getNumeroCartao() != null) dados.add(Subcampo.NumeroCartao.valor + "=" + request.getNumeroCartao());
                if (request.getVencimentoCartao() != null) dados.add(Subcampo.VencimentoCartao.valor + "=" + request.getVencimentoCartao());
                break;

            case CreditoParceladoComJuros:
            case CreditoParceladoSemJuros:
                // O número de parcelas já foi adicionado no bloco acima.
                // Nenhum outro dado estendido é necessário para esta operação.
                break;

            default: break;
        }
        // Junta a lista em uma única string, se não estiver vazia.
        return dados.isEmpty() ? "" : "[" + String.join(";", dados) + "]";
    }
}