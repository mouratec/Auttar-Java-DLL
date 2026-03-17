package com.auttar;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.auttar.Enums.*;

/**
 * 🏗️ Camada de Serviço de Integração Auttar (Padrão Facade / Wrapper)
 * * Qual o objetivo desta classe?
 * Isolar toda a complexidade de comunicação de baixo nível com a DLL em C++ (JNA)
 * da lógica de negócios e interface do usuário (App.java).
 * * Ela atua como um "tradutor": recebe Objetos Java (TransactionRequest),
 * converte para os formatos primitivos exigidos pela DLL (String formatada, byte arrays),
 * executa as chamadas nativas e devolve os resultados de forma limpa.
 */
public class AuttarIntegrationService {

    // A data fiscal é o "dia útil" do caixa. Na Auttar, operações do mesmo dia
    // precisam compartilhar a mesma data fiscal para facilitar fechamentos e conciliações.
    private final String dataFiscal;

    public AuttarIntegrationService() {
        // Inicializa a data no momento em que o serviço é instanciado.
        this.dataFiscal = Helpers.getDataAtual();
    }

    /**
     * 🔌 Inicializa o cliente TEF (Handshake inicial).
     * * É obrigatório chamar este método UMA VEZ ao abrir o PDV, antes de qualquer transação.
     * Ele carrega configurações, sobe os serviços de comunicação e valida as chaves de segurança.
     * * @return true se a DLL inicializou corretamente e está pronta para uso; false em caso de falha.
     */
    public boolean inicializarCliente() {
        // No JNA (comunicação com C/C++), nós alocamos a memória no Java (array de 256 bytes)
        // e passamos o ponteiro para a DLL preencher a resposta dentro dele.
        byte[] resultado = new byte[256];
        java.util.Arrays.fill(resultado, (byte) ' ');

        // Enviamos "00" de antemão. Se a DLL não alterar isso e não estourar erro, assumimos sucesso.
        System.arraycopy("00".getBytes(), 0, resultado, 0, 2);

        // String de inicialização estendida.
        // "[suporte-https=1]" é crucial hoje em dia, pois obriga a comunicação encriptada via TLS com a Auttar.
        String strParametros = "[suporte-https=1]";

        try {
            System.out.println("INFO: Chamando iniciaClientCTF...");

            // Assinatura nativa: (buffer_resultado, cnpj, versao_automacao, nome_automacao, param_adicionais...)
            CtfClientLibrary.INSTANCE.iniciaClientCTF(resultado, "000000000000", "1.0.0", "PDV_JAVA",
                    "00", "HTTPS", "0", "1", "1", strParametros);

            // Transforma os bytes devolvidos pela DLL nativa de volta para String legível no Java
            String resposta = new String(resultado, StandardCharsets.UTF_8).trim();

            if ("00".equals(resposta)) {
                System.out.println("SUCESSO: CTFClient inicializado com sucesso.");
                return true;
            } else {
                System.out.println("ERRO: Falha ao inicializar o CTFClient. Código: " + resposta);
                return false;
            }
        } catch (Exception ex) {
            System.out.println("ERRO FATAL durante a inicialização: Verifique se a arquitetura (x86/x64) do Java bate com a DLL.");
            ex.printStackTrace();
            return false;
        }
    }

    /**
     * 🚀 Dá o "Start" em uma nova operação (Venda, Cancelamento, Reimpressão).
     * * @param request Objeto estruturado contendo operação, valor e identificadores.
     * @param numTrans Número sequencial da transação no dia (ex: "01", "02").
     * @return O código de status da inicialização. (0 = Sucesso, prosseguir para o loop).
     */
    public int iniciarTransacao(TransactionRequest request, String numTrans) {
        byte[] resultado = new byte[256];

        // A DLL exige formatações rígidas (ex: operação sempre com 3 dígitos,
        // e documento fiscal alinhado à esquerda com 20 posições).
        String operacaoStr = String.format("%03d", request.getOperacao().valor);
        String valorStr = Helpers.formataValor(request.getValor());
        String docFiscalStr = String.format("%-20s", request.getDocumentoFiscal() != null ? request.getDocumentoFiscal() : "");

        // Valida se a transação exige a passagem de um "pacote de dados extras" (Dados Estendidos)
        String dadosExt = construirDadosEstendidos(request);

        if (dadosExt.isEmpty()) {
            // Operações simples (Crédito comum, Débito comum) que não precisam de dados extras.
            CtfClientLibrary.INSTANCE.iniciaTransacaoCTF(resultado, operacaoStr, valorStr, docFiscalStr, dataFiscal, numTrans);
        } else {
            // Operações complexas (Parcelado, PIX, Cancelamento) usam a versão 'ext' (Extended) da função,
            // que aceita o parâmetro extra contendo a string de subcampos.
            CtfClientLibrary.INSTANCE.iniciaTransacaoCTFext(resultado, operacaoStr, valorStr, docFiscalStr, dataFiscal, numTrans, dadosExt);
        }

        return Integer.parseInt(new String(resultado).trim());
    }

    /**
     * O "Coração" do TEF (Loop de Continuação).
     * * A integração Auttar funciona como uma "Máquina de Estados". Após o 'iniciarTransacao',
     * o PDV deve chamar esta função repetidamente num loop (while) até que ela pare de retornar '99'.
     * * @param comando O que a DLL quer que o PDV faça (ex: "01" exibir mensagem, "05" exibir menu).
     * @param campo O ID da informação que está sendo trafegada (ex: "7000" para status, "7302" para cupom).
     * @param valor O conteúdo da informação (ex: "Transação Aprovada", ou os dados do cupom).
     * @param tamanho Quantidade de caracteres presentes no buffer 'valor'.
     * @param display Mensagens curtas enviadas paralelamente para exibir em visor de cliente (se houver).
     * @return 99 (Continue rodando o loop), 0 (Operação concluída com sucesso), ou -1 (Erro e fim de loop).
     */
    public int continuarTransacao(byte[] comando, byte[] campo, byte[] valor, byte[] tamanho, byte[] display) {
        byte[] resultado = new byte[256];
        CtfClientLibrary.INSTANCE.continuaTransacaoCTF(resultado, comando, campo, valor, tamanho, display);
        return Integer.parseInt(new String(resultado).trim());
    }

    /**
     *  Efetivação (Commit) ou Desfazimento (Rollback) da transação.
     * * Fundamental para garantir a integridade financeira. Só chamamos o Confirmar ("1")
     * DEPOIS que o PDV imprimiu o cupom ou registrou a venda no banco de dados local.
     * Caso o PDV caia, falte papel ou dê timeout no PIX, manda-se o Desfazer ("0").
     * * @param confirmar "1" = Transação validada e retida. "0" = Transação cancelada/estornada automaticamente.
     * @param numTrans O número sequencial da transação que está sendo finalizada.
     * @param camposRetornados Coleção do Java para guardarmos o log/resultado desta função.
     */
    public boolean finalizarTransacao(String confirmar, String numTrans, Map<String, String> camposRetornados) {
        // AUDITORIA DE SEGURANÇA: Mostra exatamente o que está indo para a DLL, enviado pela AC.
        System.out.println("[CTFClient] CHAMANDO: finalizaTransacaoCTF");
        System.out.println("NumTrans.....: " + numTrans);
        System.out.println("FinalizaTransação Confirma : " + confirmar + " (Onde 1 = CONFIRMA e 0 = DESFAZIMENTO)");
        System.out.println("=======================================================");

        // Mantido o array de bytes conforme solicitado
        byte[] resultado = new byte[256];
        CtfClientLibrary.INSTANCE.finalizaTransacaoCTF(resultado, confirmar, numTrans, dataFiscal);

        // Converte o array de bytes para String e remove espaços em branco
        String retorno = new String(resultado).trim();

        // Salva a resposta da dll (ex: "0", "00" ou "18") dentro do nosso dicionário
        // para auditoria ou prevenção de Erro 18 na camada de UI.
        camposRetornados.put("ResultadoFinalizacao", retorno);

        // Validação Profissional: Verifica se a DLL confirmou o sucesso
        if ("0".equals(retorno) || "00".equals(retorno)) {
            System.out.println("[CTFClient] Finalização concluída com SUCESSO. Retorno DLL: " + retorno);
            return true;
        } else {
            System.err.println("[ERRO CRÍTICO] Falha ao finalizar a transação no CTFClient!");
            System.err.println("Motivo/Código de Retorno: " + retorno);
            return false;
        }
    }


    /**
     * Serializador de Dados Estendidos.
     * * O padrão da Auttar para enviar múltiplas informações extras é uma string única
     * delimitada por colchetes e ponto-e-vírgula. Formato: "[ID=VALOR;ID2=VALOR2]".
     * Exemplo de Cancelamento: "[7012=123456;7161=251224]".
     * * @param request O objeto gerado pela interface com os dados informados pelo operador.
     * @return String formatada e pronta para injeção na DLL nativa.
     */
    private String construirDadosEstendidos(TransactionRequest request) {
        List<String> dados = new ArrayList<>();
        if (request == null) return ""; // Proteção contra NullPointer

        // REGRA GLOBAL: Se houver parcelamento, a DLL sempre exige o subcampo 7008
        if (request.getNumeroParcelas() != null && request.getNumeroParcelas() > 0) {
            // Garante que fique com duas posições (ex: "02" ou "12" parcelas)
            dados.add(String.valueOf(Subcampo.NumeroParcelas.valor) + "=" + String.format("%02d", request.getNumeroParcelas()));
        }

        if (request.getOperacao() == null) {
            return dados.isEmpty() ? "" : "[" + String.join(";", dados) + "]";
        }

        // 🔀 Análise de Requisitos Específicos por Tipo de Operação
        switch (request.getOperacao()) {
            case ConsultaPix:
            case CancelamentoGenerico:
            case DevolucaoPix:
                // Para reconsultar ou estornar algo, a Auttar precisa saber o "RG" (NSU) e a Data da operação original.
                if (request.getNsuOriginal() != null) dados.add(Subcampo.NsuCtfOriginal.valor + "=" + request.getNsuOriginal());
                if (request.getDataOriginal() != null) dados.add(Subcampo.DataTransacaoOriginal.valor + "=" + request.getDataOriginal());
                break;
            case DesfazimentoPorNsu: // 🛡️ ADICIONADO: Mapeamento para a Operação 281
                if (request.getNsuOriginal() != null) dados.add(Subcampo.NsuCtfOriginal.valor + "=" + request.getNsuOriginal());
                if (request.getDataOriginal() != null) dados.add(Subcampo.DataTransacaoOriginal.valor + "=" + request.getDataOriginal());
                break;
            case CancelamentoCreditoDigitado:
                // Estornos de crédito digitado exigem os dados da venda original + o número do cartão para bater a segurança.
                if (request.getNsuOriginal() != null) dados.add(Subcampo.NsuCtfOriginal.valor + "=" + request.getNsuOriginal());
                if (request.getDataOriginal() != null) dados.add(Subcampo.DataTransacaoOriginal.valor + "=" + request.getDataOriginal());
                if (request.getNumeroCartao() != null) dados.add(Subcampo.NumeroCartao.valor + "=" + request.getNumeroCartao());
                break;

            case CreditoDigitado:
                // Venda sem o cartão físico em mãos (venda por telefone/e-commerce manual).
                if (request.getNumeroCartao() != null) dados.add(Subcampo.NumeroCartao.valor + "=" + request.getNumeroCartao());
                if (request.getVencimentoCartao() != null) dados.add(Subcampo.VencimentoCartao.valor + "=" + request.getVencimentoCartao());
                break;

            case CreditoParceladoComJuros:
            case CreditoParceladoSemJuros:
                // Tratado na regra global de parcelamento acima.
                // Apenas deixamos o case aqui de forma explícita para clareza da regra de negócio.
                break;

            default:
                break;
        }

        // Empacota a lista na sintaxe exigida pela Auttar: "[chave=valor;chave2=valor2]"
        return dados.isEmpty() ? "" : "[" + String.join(";", dados) + "]";
    }
}