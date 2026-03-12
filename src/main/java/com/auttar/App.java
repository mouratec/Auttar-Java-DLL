package com.auttar;

import com.auttar.Enums.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * 🚀 **Classe Principal da Aplicação (Ponto de Entrada)**
 * Versão atualizada:
 * 1. Timeout de 15s com handshake (Comando 08).
 * 2. Devolução/Consulta PIX sem confirmação final ("1").
 * 3. Auto-resposta "2" (NÃO) para reconsulta PIX.
 * 4. Auto-resposta "1" (SIM) para confirmação de cancelamento por timeout.
 */
public class App {

    private static final Scanner scanner = new Scanner(System.in, "CP850");
    private static final int BUFFER_SIZE = 99000;

    // Timeout de 15 segundos para o Pinpad
    private static final long TRANSACTION_TIMEOUT_MS = 15000;

    // Armazena a última mensagem/título exibido para contexto do menu
    private static String ultimaMensagemDisplay = "";

    public static void main(String[] args) {
        System.out.println("======================================");
        System.out.println("=   Sistema de Integração Auttar     =");
        System.out.println("======================================\n");

        AuttarIntegrationService auttarService = new AuttarIntegrationService();

        if (!auttarService.inicializarCliente()) {
            System.out.println("\nPressione Enter para sair...");
            scanner.nextLine();
            return;
        }

        while (handleMainMenu(auttarService)) {}

        System.out.println("Encerrando o programa...");
        scanner.close();
    }

    private static boolean handleMainMenu(AuttarIntegrationService service) {
        System.out.println("\n==========================================");
        System.out.println("              MENU PRINCIPAL            ");
        System.out.println("==========================================");
        System.out.println(" 1. Menu de Vendas e Servicos           ");
        System.out.println(" 2. Menu de Cancelamentos               ");
        System.out.println(" 3. Reimpressao do Ultimo Cupom         ");
        System.out.println(" 4. Configuracao CTFClient              ");
        System.out.println(" 5. Autenticacao do Terminal            ");
        System.out.println(" 0. Sair                                ");
        System.out.println("------------------------------------------");
        System.out.print("\nOpcao: ");
        String escolha = scanner.nextLine();
        TransactionRequest request = null;

        switch (escolha) {
            case "1": handleVendasMenu(service); break;
            case "2": handleCancelamentoMenu(service); break;
            case "3": request = criarRequestReimpressao(); break;
            case "4": request = criarRequestConfiguracao(); break;
            case "5": request = criarRequestAutenticacao(); break;
            case "0": return false;
            default: System.out.println("Opção inválida."); break;
        }

        if (request != null) {
            executarFluxoDeTransacao(service, request, "01");
        }
        return true;
    }

    private static void handleVendasMenu(AuttarIntegrationService service) {
        while (true) {
            System.out.println("\n------------------------------------------");
            System.out.println("         MENU DE VENDAS E SERVICOS      ");
            System.out.println("------------------------------------------");
            System.out.println(" 1. Credito (A Vista)                   ");
            System.out.println(" 2. Credito Parcelado Sem Juros         ");
            System.out.println(" 3. Credito Parcelado Com Juros         ");
            System.out.println(" 4. Debito                              ");
            System.out.println(" 5. Credito Digitado                    ");
            System.out.println(" 6. Venda PIX                           ");
            System.out.println(" 7. Credito Generico                    ");
            System.out.println(" 8. Debito Generico                     ");
            System.out.println(" 9. Venda com Multiplos Cartoes         ");
            System.out.println("10. Consulta PIX                        ");
            System.out.println(" 0. Voltar ao Menu Principal            ");
            System.out.print("\nOpcao: ");
            String escolha = scanner.nextLine();
            TransactionRequest request = null;
            String docFiscal = String.valueOf(System.currentTimeMillis());

            switch (escolha) {
                case "1": request = criarRequestPadrao(CodigoOperacao.Credito, docFiscal); break;
                case "2": request = criarRequestParcelado(CodigoOperacao.CreditoParceladoSemJuros, docFiscal); break;
                case "3": request = criarRequestParcelado(CodigoOperacao.CreditoParceladoComJuros, docFiscal); break;
                case "4": request = criarRequestPadrao(CodigoOperacao.Debito, docFiscal); break;
                case "5": request = criarRequestCreditoDigitado(docFiscal, null); break;
                case "6": request = criarRequestPadrao(CodigoOperacao.PagamentoPix, docFiscal); break;
                case "7": request = criarRequestPadrao(CodigoOperacao.CreditoGenerico, docFiscal); break;
                case "8": request = criarRequestPadrao(CodigoOperacao.DebitoGenerico, docFiscal); break;
                case "9": handleVendaMultiplosCartoes(service, docFiscal); break;
                case "10": request = criarRequestConsultaPix(docFiscal); break;
                case "0": return;
                default: System.out.println("Opção inválida."); break;
            }
            if (request != null) {
                executarFluxoDeTransacao(service, request, "01");
            }
        }
    }

    private static void handleCancelamentoMenu(AuttarIntegrationService service) {
        while (true) {
            System.out.println("\n------------------------------------------");
            System.out.println("           MENU DE CANCELAMENTO         ");
            System.out.println("------------------------------------------");
            System.out.println(" 1. Generico (Credito/Debito)           ");
            System.out.println(" 2. Credito Digitado                    ");
            System.out.println(" 3. Devolucao PIX                       ");
            System.out.println(" 0. Voltar ao Menu Principal            ");
            System.out.print("\nOpcao: ");
            String escolha = scanner.nextLine();
            TransactionRequest request = null;
            switch (escolha) {
                case "1": request = coletarDadosCancelamentoGenerico(); break;
                case "2": request = coletarDadosCancelamentoCreditoDigitado(); break;
                case "3": request = coletarDadosCancelamentoPix(); break;
                case "0": return;
                default: System.out.println("Opção inválida."); break;
            }
            if (request != null) {
                executarFluxoDeTransacao(service, request, "01");
            }
        }
    }

    private static void executarFluxoDeTransacao(AuttarIntegrationService service, TransactionRequest request, String numTrans) {
        Map<String, String> camposRetornados = new HashMap<>();
        ultimaMensagemDisplay = ""; // Reseta mensagem anterior

        int ret = service.iniciarTransacao(request, numTrans);
        if (ret != CodigoRetornoFuncao.Sucesso.valor) {
            exibirResultado(request, criarRespostaDeErro("Falha ao iniciar a transação. Código: " + ret, null), numTrans);
            return;
        }

        byte[] comando = new byte[256], campo = new byte[256], valor = new byte[BUFFER_SIZE],
                tamanho = new byte[256], display = new byte[256];
        System.arraycopy("00".getBytes(), 0, comando, 0, 2);

        long startTime = System.currentTimeMillis();

        ret = CodigoRetornoFuncao.AguardandoContinuacao.valor;
        while (ret == CodigoRetornoFuncao.AguardandoContinuacao.valor) {
            ret = service.continuarTransacao(comando, campo, valor, tamanho, display);
            if (ret == CodigoRetornoFuncao.AguardandoContinuacao.valor) {
                tratarComandoInterativo(comando, campo, valor, tamanho, display, camposRetornados, startTime, request);
            }
        }

        TransactionResponse response = criarResposta(camposRetornados);
        response.setOperacaoRequest(request);

        if (ret != CodigoRetornoFuncao.Sucesso.valor) {
            service.finalizarTransacao("0", numTrans, camposRetornados);
            String msg = "Erro/Cancelamento durante o processamento. Código: " + ret;
            if (response.getMensagemRetorno().contains("Cancelado")) {
                msg = "Transação Cancelada (Timeout ou Usuário).";
            }
            exibirResultado(request, criarRespostaDeErro(msg, camposRetornados), numTrans);
            return;
        }

        boolean precisaFinalizar = request.getOperacao() != CodigoOperacao.ConsultaPix &&
                request.getOperacao() != CodigoOperacao.DevolucaoPix &&
                request.getOperacao() != CodigoOperacao.ReimpressaoUltimoComprovante &&
                request.getOperacao() != CodigoOperacao.ConfiguracaoCtfClient &&
                request.getOperacao() != CodigoOperacao.AutenticacaoTerminal;

        if (precisaFinalizar) {
            String confirmar = response.isAprovada() ? "1" : "0";
            service.finalizarTransacao(confirmar, numTrans, camposRetornados);
        }

        exibirResultado(request, response, numTrans);
    }

    private static TransactionResponse executarTransacaoParcial(AuttarIntegrationService service, TransactionRequest request, int numeroTransacao) {
        String numTransStr = String.format("%02d", numeroTransacao);
        Map<String, String> camposRetornados = new HashMap<>();
        ultimaMensagemDisplay = "";

        int ret = service.iniciarTransacao(request, numTransStr);
        if (ret != CodigoRetornoFuncao.Sucesso.valor) {
            return criarRespostaDeErro("Falha ao iniciar a transação. Código: " + ret, null);
        }

        byte[] comando = new byte[256], campo = new byte[256], valor = new byte[BUFFER_SIZE],
                tamanho = new byte[256], display = new byte[256];
        System.arraycopy("00".getBytes(), 0, comando, 0, 2);

        long startTime = System.currentTimeMillis();

        ret = CodigoRetornoFuncao.AguardandoContinuacao.valor;
        while (ret == CodigoRetornoFuncao.AguardandoContinuacao.valor) {
            ret = service.continuarTransacao(comando, campo, valor, tamanho, display);
            if (ret == CodigoRetornoFuncao.AguardandoContinuacao.valor) {
                tratarComandoInterativo(comando, campo, valor, tamanho, display, camposRetornados, startTime, request);
            }
        }

        if (ret != CodigoRetornoFuncao.Sucesso.valor) {
            return criarRespostaDeErro("Erro/Cancelamento. Código: " + ret, camposRetornados);
        }

        camposRetornados.put("NumeroTransacao", numTransStr);
        TransactionResponse response = criarResposta(camposRetornados);
        response.setOperacaoRequest(request);
        return response;
    }

    private static void handleVendaMultiplosCartoes(AuttarIntegrationService service, String docFiscalDaVenda) {
        System.out.println("\n--- VENDA COM MÚLTIPLOS CARTÕES ---");
        BigDecimal valorTotal = obterValorDaTransacao();
        System.out.println("Documento Fiscal (num_doc) para esta venda: " + docFiscalDaVenda);

        BigDecimal valorPendente = valorTotal;
        int numeroDaTransacao = 1;
        List<TransactionResponse> transacoesAprovadas = new ArrayList<>();

        while (valorPendente.compareTo(new BigDecimal("0.005")) > 0) {
            System.out.printf("\nValor Total da Venda...: R$ %,.2f%n", valorTotal);
            System.out.printf("Valor Pendente.........: R$ %,.2f%n", valorPendente);
            System.out.println("-------------------------------------");

            BigDecimal valorPagamento = obterValorParcial(valorPendente);
            TransactionRequest request = escolherPagamentoParcial(valorPagamento, docFiscalDaVenda);
            if (request == null) break;

            TransactionResponse response = executarTransacaoParcial(service, request, numeroDaTransacao);

            if (response != null && response.isAprovada()) {
                exibirResultado(request, response, String.format("%02d", numeroDaTransacao));
                transacoesAprovadas.add(response);
                valorPendente = valorPendente.subtract(valorPagamento);
                numeroDaTransacao++;
            } else {
                boolean escolhaValida = false;
                while (!escolhaValida) {
                    System.out.println("\nPagamento NEGADO/ERRO: " + (response != null ? response.getMensagemRetorno() : "Erro desconhecido"));
                    System.out.print("Deseja tentar outro pagamento ou cancelar a venda? (T/Tentar ou C/Cancelar): ");
                    String escolha = scanner.nextLine().trim().toUpperCase();

                    if (escolha.startsWith("C")) {
                        if (!transacoesAprovadas.isEmpty()) {
                            service.finalizarTransacao("0", "01", new HashMap<>());
                            System.out.println("\nVENDA CANCELADA PELO UTILIZADOR. TRANSAÇÕES APROVADAS FORAM DESFEITAS.");
                        } else {
                            System.out.println("\nVENDA CANCELADA PELO UTILIZADOR.");
                        }
                        return;
                    } else if (escolha.startsWith("T") || escolha.isEmpty()) {
                        escolhaValida = true;
                    } else {
                        System.out.println("Opção inválida.");
                    }
                }
            }
        }

        if (valorPendente.compareTo(new BigDecimal("0.005")) < 0 && !transacoesAprovadas.isEmpty()) {
            System.out.println("\n--- FINALIZANDO VENDA ---");
            System.out.println("Venda totalmente paga. Confirmar todos os pagamentos? (S/N)");
            if (scanner.nextLine().trim().equalsIgnoreCase("S")) {
                System.out.println("\n--- RESUMO DOS PAGAMENTOS APROVADOS ---");
                for (TransactionResponse respAprovada : transacoesAprovadas) {
                    String numTrans = respAprovada.getCamposRetornados().get("NumeroTransacao");
                    exibirResultado(respAprovada.getOperacaoRequest(), respAprovada, numTrans);
                }
                System.out.println("Confirmando transações no sistema...");
                for (TransactionResponse respAprovada : transacoesAprovadas) {
                    String numTrans = respAprovada.getCamposRetornados().get("NumeroTransacao");
                    service.finalizarTransacao("1", numTrans, respAprovada.getCamposRetornados());
                }
                System.out.println("\nVENDA CONFIRMADA COM SUCESSO!");
            } else {
                service.finalizarTransacao("0", "01", new HashMap<>());
                System.out.println("\nVENDA CANCELADA PELO UTILIZADOR. TODAS AS TRANSAÇÕES FORAM DESFEITAS.");
            }
        } else if (!transacoesAprovadas.isEmpty()) {
            service.finalizarTransacao("0", "01", new HashMap<>());
            System.out.println("\nVENDA NÃO CONCLUÍDA. TODAS AS TRANSAÇÕES APROVADAS FORAM DESFEITAS.");
        } else {
            System.out.println("\nNenhum pagamento foi aprovado. Venda cancelada.");
        }
    }

    /**
     * 💬 **Processa os comandos interativos da DLL.**
     * Lógica aprimorada para distinguir menus via `ultimaMensagemDisplay`.
     */
    private static void tratarComandoInterativo(byte[] comando, byte[] campo, byte[] valor, byte[] tamanho, byte[] display, Map<String, String> camposRetornados, long startTime, TransactionRequest request) {
        String comandoStr = new String(comando, StandardCharsets.UTF_8).trim();
        int comandoAtual = 0;
        try {
            comandoAtual = Integer.parseInt(comandoStr);
        } catch (NumberFormatException e) { return; }

        ComandoContinua cmd = ComandoContinua.fromInt(comandoAtual);

        String campoStr = new String(campo, StandardCharsets.UTF_8).trim();
        String tamanhoStr = new String(tamanho, StandardCharsets.UTF_8).trim();
        int valorTamanho = 0;
        try { valorTamanho = Integer.parseInt(tamanhoStr); } catch (Exception e) {}
        String valorAtual = new String(valor, 0, valorTamanho, StandardCharsets.UTF_8);

        String comandoParaProximaChamada = "00";

        if (cmd != null) {
            switch (cmd) {
                case ObterSubcampo:
                    System.out.printf("INFO: Recebido Subcampo -> %s: %s%n", campoStr.substring(0, 4), valorAtual);
                    camposRetornados.put(campoStr.substring(0, 4), valorAtual);
                    break;

                case ExibirMensagem: case ExibirTituloMenu: case AguardarTecla:
                    // Atualiza a última mensagem exibida para contexto de menu
                    ultimaMensagemDisplay = valorAtual;
                    System.out.printf("INFO DLL: %s%n", valorAtual.replace(";", System.lineSeparator()));
                    break;

                case VerificarCancelamento: // Comando 08
                    if (System.currentTimeMillis() - startTime > TRANSACTION_TIMEOUT_MS) {
                        System.out.println("\n[TIMEOUT] 15s excedidos. Enviando comando de CANCELAMENTO (08) para a DLL...");
                        comandoParaProximaChamada = "08";
                    } else {
                        comandoParaProximaChamada = "00";
                    }
                    break;

                case ExibirMenu:
                    // Normaliza textos para verificação
                    String menuTextoUpper = valorAtual.toUpperCase().replace("Ã", "A");
                    String tituloUltimaMsg = ultimaMensagemDisplay.toUpperCase().replace("Ã", "A");
                    boolean isSimNao = menuTextoUpper.contains("SIM") && menuTextoUpper.contains("NAO");
                    boolean isTimeout = (System.currentTimeMillis() - startTime > TRANSACTION_TIMEOUT_MS);

                    String respostaAuto = null;

                    if (isSimNao) {
                        // CASO 1: Menu "OPERACAO CANCELADA?" disparado por Timeout -> Responder "1" (SIM)
                        if (isTimeout && tituloUltimaMsg.contains("CANCELADA")) {
                            System.out.println("\n[AUTO] Timeout Confirmado: Respondendo '1' (SIM) para cancelamento.");
                            respostaAuto = "1";
                        }
                        // CASO 2: Menu "DESEJA CONSULTA-LO NOVAMENTE?" do PIX -> Responder "2" (NÃO)
                        else if ((tituloUltimaMsg.contains("NOVAMENTE") || tituloUltimaMsg.contains("CONSULTA"))) {
                            System.out.println("\n[AUTO] Reconsulta PIX detectada: Respondendo '2' (NÃO).");
                            respostaAuto = "2";
                        }
                    }

                    if (respostaAuto != null) {
                        java.util.Arrays.fill(valor, (byte)0);
                        java.util.Arrays.fill(tamanho, (byte)0);
                        byte[] respBytes = respostaAuto.getBytes();
                        System.arraycopy(respBytes, 0, valor, 0, respBytes.length);
                        String tamStr = String.format("%05d", respBytes.length);
                        System.arraycopy(tamStr.getBytes(), 0, tamanho, 0, tamStr.length());
                    } else {
                        // Comportamento manual padrão
                        System.out.printf("MENU: %s%nSua opção: ", valorAtual.replace(";", " | "));
                        String escolhaMenu = scanner.nextLine();
                        java.util.Arrays.fill(valor, (byte)0);
                        java.util.Arrays.fill(tamanho, (byte)0);
                        byte[] escolhaBytes = escolhaMenu.getBytes();
                        System.arraycopy(escolhaBytes, 0, valor, 0, escolhaBytes.length);
                        String tamStr = String.format("%05d", escolhaBytes.length);
                        System.arraycopy(tamStr.getBytes(), 0, tamanho, 0, tamStr.length());
                    }
                    comandoParaProximaChamada = "05";
                    break;

                case CapturarDado:
                    System.out.print(valorAtual + ": ");
                    String dadoCapturado = scanner.nextLine();
                    java.util.Arrays.fill(valor, (byte)0);
                    java.util.Arrays.fill(tamanho, (byte)0);
                    byte[] dadoBytes = dadoCapturado.getBytes();
                    System.arraycopy(dadoBytes, 0, valor, 0, dadoBytes.length);
                    String tamDadoStr = String.format("%05d", dadoBytes.length);
                    System.arraycopy(tamDadoStr.getBytes(), 0, tamanho, 0, tamDadoStr.length());
                    comandoParaProximaChamada = "07";
                    break;
                default: break;
            }
        }
        java.util.Arrays.fill(comando, (byte)0);
        System.arraycopy(comandoParaProximaChamada.getBytes(), 0, comando, 0, comandoParaProximaChamada.length());
    }

    // ================= MÉTODOS AUXILIARES (Requests) =================
    // (Mantidos sem alterações na lógica, apenas inclusos para completude)

    private static TransactionRequest criarRequestPadrao(CodigoOperacao operacao, String docFiscal) {
        System.out.printf("\n--- %s ---%n", operacao.name());
        BigDecimal valor = obterValorDaTransacao();
        TransactionRequest req = new TransactionRequest();
        req.setOperacao(operacao);
        req.setValor(valor);
        req.setDocumentoFiscal(docFiscal);
        return req;
    }

    private static TransactionRequest criarRequestParcelado(CodigoOperacao operacao, String docFiscal) {
        System.out.printf("\n--- %s ---%n", operacao.name());
        BigDecimal valor = obterValorDaTransacao();
        int parcelas = obterNumeroParcelas();
        TransactionRequest req = new TransactionRequest();
        req.setOperacao(operacao);
        req.setValor(valor);
        req.setNumeroParcelas(parcelas);
        req.setDocumentoFiscal(docFiscal);
        return req;
    }

    private static TransactionRequest criarRequestCreditoDigitado(String docFiscal, BigDecimal valorFixo) {
        System.out.println("\n--- Crédito Digitado ---");
        BigDecimal valor = (valorFixo != null) ? valorFixo : obterValorDaTransacao();
        System.out.print("Digite o número do cartão: ");
        String cartao = scanner.nextLine();
        System.out.print("Digite a validade (MMAA): ");
        String validade = scanner.nextLine();
        TransactionRequest req = new TransactionRequest();
        req.setOperacao(CodigoOperacao.CreditoDigitado);
        req.setValor(valor);
        req.setDocumentoFiscal(docFiscal);
        req.setNumeroCartao(cartao);
        req.setVencimentoCartao(validade);
        return req;
    }

    private static TransactionRequest criarRequestConsultaPix(String docFiscal) {
        System.out.println("\n--- Consulta de Transação PIX ---");
        BigDecimal valorOriginal = obterValorDaTransacao();
        System.out.print("Digite a DATA original da transação (formato DDMMAA): ");
        String dataOriginal = scanner.nextLine();
        System.out.print("Digite o NSU CTF da transação PIX original: ");
        String nsuOriginal = scanner.nextLine();
        TransactionRequest req = new TransactionRequest();
        req.setOperacao(CodigoOperacao.ConsultaPix);
        req.setValor(valorOriginal);
        req.setDocumentoFiscal(docFiscal);
        req.setNsuOriginal(nsuOriginal);
        req.setDataOriginal(dataOriginal);
        return req;
    }

    private static TransactionRequest coletarDadosCancelamentoGenerico() {
        System.out.println("\n--- Cancelamento Genérico ---");
        TransactionRequest req = new TransactionRequest();
        req.setOperacao(CodigoOperacao.CancelamentoGenerico);
        req.setValor(obterValorDaTransacao());
        System.out.print("Digite a DATA original da transação (DDMMAA): ");
        req.setDataOriginal(scanner.nextLine());
        System.out.print("Digite o NSU CTF original da transação: ");
        req.setNsuOriginal(scanner.nextLine());
        req.setDocumentoFiscal("CANC-" + System.currentTimeMillis());
        return req;
    }

    private static TransactionRequest coletarDadosCancelamentoCreditoDigitado() {
        System.out.println("\n--- Cancelamento de Crédito Digitado ---");
        TransactionRequest req = new TransactionRequest();
        req.setOperacao(CodigoOperacao.CancelamentoCreditoDigitado);
        req.setValor(obterValorDaTransacao());
        System.out.print("Digite a DATA original da transação (formato DDMMAA): ");
        req.setDataOriginal(scanner.nextLine());
        System.out.print("Digite o NSU CTF original da transação: ");
        req.setNsuOriginal(scanner.nextLine());
        System.out.print("Digite o NÚMERO DO CARTÃO original: ");
        req.setNumeroCartao(scanner.nextLine());
        req.setDocumentoFiscal("CANC-DIG-" + System.currentTimeMillis());
        return req;
    }

    private static TransactionRequest coletarDadosCancelamentoPix() {
        System.out.println("\n--- Devolução PIX ---");
        TransactionRequest req = new TransactionRequest();
        req.setOperacao(CodigoOperacao.DevolucaoPix);
        req.setValor(obterValorDaTransacao());
        System.out.print("Digite a DATA original da transação (formato DDMMAA): ");
        req.setDataOriginal(scanner.nextLine());
        System.out.print("Digite o NSU CTF da transação PIX original: ");
        req.setNsuOriginal(scanner.nextLine());
        req.setDocumentoFiscal("DEV-PIX-" + System.currentTimeMillis());
        return req;
    }

    private static TransactionRequest criarRequestReimpressao() {
        System.out.println("\n--- Reimpressão do Último Comprovante ---");
        TransactionRequest req = new TransactionRequest();
        req.setOperacao(CodigoOperacao.ReimpressaoUltimoComprovante);
        req.setValor(BigDecimal.ZERO);
        req.setDocumentoFiscal("REIMPRESSAO");
        return req;
    }

    private static TransactionRequest criarRequestConfiguracao() {
        System.out.println("\n--- Configuração do CTFClient (800) ---");
        TransactionRequest req = new TransactionRequest();
        req.setOperacao(CodigoOperacao.ConfiguracaoCtfClient);
        req.setValor(BigDecimal.ZERO);
        req.setDocumentoFiscal("CONFIG-" + System.currentTimeMillis());
        return req;
    }

    private static TransactionRequest criarRequestAutenticacao() {
        System.out.println("\n--- Autenticação do Terminal (801) ---");
        TransactionRequest req = new TransactionRequest();
        req.setOperacao(CodigoOperacao.AutenticacaoTerminal);
        req.setValor(BigDecimal.ZERO);
        req.setDocumentoFiscal("AUTH-" + System.currentTimeMillis());
        return req;
    }

    private static BigDecimal obterValorDaTransacao() {
        while (true) {
            System.out.print("\nDigite o valor da transação (ex: 19,99): R$ ");
            String input = scanner.nextLine().replace(",", ".");
            try {
                BigDecimal valor = new BigDecimal(input);
                if (valor.compareTo(BigDecimal.ZERO) >= 0) return valor;
            } catch (NumberFormatException e) {
                System.out.println("Valor inválido.");
            }
        }
    }

    private static int obterNumeroParcelas() {
        while (true) {
            System.out.print("Digite o número de parcelas (padrão: 1): ");
            String input = scanner.nextLine();
            if (input == null || input.trim().isEmpty()) input = "1";
            try {
                int parcelas = Integer.parseInt(input);
                if (parcelas > 0) return parcelas;
            } catch (NumberFormatException e) {
                System.out.println("Número de parcelas inválido. Tente novamente.");
            }
        }
    }

    private static TransactionResponse criarResposta(Map<String, String> campos) {
        String codRetorno = Helpers.getValueOrDefault(campos, String.valueOf(Subcampo.CodigoRetornoTransacao.valor));
        TransactionResponse response = new TransactionResponse();
        response.setAprovada("00".equals(codRetorno));
        response.setCodigoRetorno(codRetorno != null ? codRetorno : "ERRO");
        response.setCodigoErro(Helpers.getValueOrDefault(campos, String.valueOf(Subcampo.CodigoErro.valor)));
        response.setMensagemRetorno(response.isAprovada() ? "Transação Aprovada" : "Transação Negada/Erro");
        response.setCamposRetornados(campos);
        return response;
    }

    private static TransactionResponse criarRespostaDeErro(String mensagem, Map<String, String> campos) {
        TransactionResponse response = new TransactionResponse();
        response.setAprovada(false);
        response.setCodigoRetorno("-1");
        response.setCodigoErro("INTEGRACAO");
        response.setMensagemRetorno(mensagem);
        response.setCamposRetornados(campos != null ? campos : new HashMap<>());
        return response;
    }

    private static void exibirResultado(TransactionRequest request, TransactionResponse response, String numTrans) {
        System.out.println("\n---------- RESULTADO DA TRANSAÇÃO ----------");
        System.out.println("Status: " + (response.isAprovada() ? "APROVADA" : "NEGADA / ERRO"));
        System.out.println("Mensagem: " + response.getMensagemRetorno());
        if (request != null && request.getValor() != null) System.out.printf("Valor: R$ %,.2f%n", request.getValor());
        if (request != null && request.getNumeroParcelas() != null && request.getNumeroParcelas() > 1) {
            System.out.printf("Parcelas: %dx%n", request.getNumeroParcelas());
        }
        String cartao = response.getCamposRetornados().get(String.valueOf(Subcampo.NumeroCartao.valor));
        System.out.println("Dados do Cartão: " + (cartao != null ? cartao : "N/A"));
        System.out.println("Código de Retorno: " + response.getCodigoRetorno());
        System.out.println("Código de Erro: " + (response.getCodigoErro() != null ? response.getCodigoErro() : "N/A"));
        System.out.println("NSU CTF: " + (response.getNsuCtf() != null ? response.getNsuCtf() : "N/A"));
        System.out.println("Num. Transação: " + numTrans);
        if (response.getComprovanteCliente() != null && !response.getComprovanteCliente().isEmpty()) {
            System.out.println("\n--- COMPROVANTE (VIA CLIENTE) ---");
            System.out.println(response.getComprovanteCliente());
            System.out.println("---------------------------------");
        }
        System.out.println("-------------------------------------------\n");
    }

    private static TransactionRequest escolherPagamentoParcial(BigDecimal valorPagamento, String docFiscal) {
        while (true) {
            System.out.println("\n--- Escolha a forma do pagamento parcial ---");
            System.out.println("1. Crédito");
            System.out.println("2. Débito");
            System.out.println("3. Crédito Digitado");
            System.out.println("0. Cancelar a venda");
            System.out.print("\nOpção: ");
            String escolha = scanner.nextLine();

            TransactionRequest req = new TransactionRequest();
            req.setValor(valorPagamento);
            req.setDocumentoFiscal(docFiscal);

            switch (escolha) {
                case "1": req.setOperacao(CodigoOperacao.Credito); return req;
                case "2": req.setOperacao(CodigoOperacao.Debito); return req;
                case "3": return criarRequestCreditoDigitado(docFiscal, valorPagamento);
                case "0": return null;
                default: System.out.println("Opção inválida. Tente novamente."); break;
            }
        }
    }

    private static BigDecimal obterValorParcial(BigDecimal valorPendente) {
        while(true) {
            System.out.printf("Valor do pagamento (ou Enter para R$ %,.2f): R$ ", valorPendente);
            String input = scanner.nextLine();
            if (input == null || input.trim().isEmpty()) {
                return valorPendente;
            }
            try {
                BigDecimal valor = new BigDecimal(input.replace(",", "."));
                if (valor.compareTo(BigDecimal.ZERO) > 0 && valor.compareTo(valorPendente.add(new BigDecimal("0.005"))) <= 0) {
                    return valor;
                }
            } catch (Exception e) {}
            System.out.printf("Valor inválido. Insira um valor entre R$ 0,01 e R$ %,.2f.%n", valorPendente);
        }
    }
}