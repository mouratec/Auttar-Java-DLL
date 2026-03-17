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
 * **Classe Principal da Aplicação (Ponto de Entrada)**
 * Interface de console (PDV modelo) que orquestra as vendas e a comunicação com a DLL da Auttar.
 * * DESTAQUES DE ARQUITETURA NESTA VERSÃO:
 * 1. Timeout Inteligente: O tempo do PIX é ditado pela adquirente/DLL, e não por timers no Java.
 * 2. Eco de Comandos: Impede o Erro 5331 respondendo corretamente aos comandos de tela.
 * 3. Trava de Finalização (Anti-Erro 18): Impede que o PDV tente "desfazer" um PIX já morto pela DLL.
 */
public class App {

    private static final Scanner scanner = new Scanner(System.in, "UTF-8");

    // Tamanho seguro para receber dados extensos da Auttar, como imagens em Base64 ou cupons longos.
    private static final int BUFFER_SIZE = 99000;

    // Memória temporária para armazenar o contexto do último menu exibido pela DLL.
    // Usado pela inteligência de Auto-Resposta (ex: saber que estamos na tela de timeout do PIX).
    private static String ultimaMensagemDisplay = "";

    public static void main(String[] args) {

        //  CONFIGURAÇÕES GLOBAIS DO JNA (Multiplataforma)
        System.setProperty("jna.library.path", ".");
        System.setProperty("jna.nosys", "true");

        System.out.println("======================================");
        System.out.println("=   Sistema de Integração Auttar     =");
        System.out.println("======================================\n");

        AuttarIntegrationService auttarService = new AuttarIntegrationService();

        // Tenta acionar a função iniciaClientCTF. Se falhar (ex: DLL ausente, Java em arquitetura errada),
        // o programa é abortado antes de exibir o menu.
        if (!auttarService.inicializarCliente()) {
            System.out.println("\nPressione Enter para sair...");
            scanner.nextLine();
            return;
        }

        // VERIFICAÇÃO DE QUEDA DE ENERGIA/REDE
        // Se o sistema caiu no meio de uma venda na última vez, ele resolve aqui.
        GerenciadorRecuperacaoTef.verificarERecuperar(auttarService);

        // Segue o fluxo normal do PDV

        while (handleMainMenu(auttarService)) {}

        System.out.println("Encerrando o programa...");
        scanner.close();
    }

    // =========================================================================
    // CAMADA DE APRESENTAÇÃO (MENUS DO SISTEMA)
    // =========================================================================

    private static boolean handleMainMenu(AuttarIntegrationService service) {
        System.out.println("\n==========================================");
        System.out.println("              MENU PRINCIPAL            ");
        System.out.println("==========================================");
        System.out.println(" 1. Menu de Vendas e Servicos           ");
        System.out.println(" 2. Menu de Cancelamentos               ");
        System.out.println(" 3. Reimpressao do Ultimo Cupom         ");
        System.out.println(" 4. Configuracao CTFClient              ");
        System.out.println(" 5. Autenticacao do Terminal            ");
        System.out.println(" 6. Executar GerenciadorRecuperacao TEF ");
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
            case "6":
                System.out.println("\n[SISTEMA] Iniciando varredura manual de pendências...");
                GerenciadorRecuperacaoTef.verificarERecuperar(service);
                break;
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

            // Opcional e personalizavel : Aqui poderíamos gerar um número de cupom fiscal real vindo de um ERP/AC.
            String docFiscal = null;

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
        // Fluxos de cancelamento exigem coletar a "Data" e o "NSU" da transação original.
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

    // =========================================================================
    // CAMADA CORE: GERENCIADOR DE TRANSAÇÕES
    // =========================================================================

    /**
     * Motor principal que gerencia o ciclo de vida de UMA transação (Inicialização -> Loop -> Finalização).
     */
    private static void executarFluxoDeTransacao(AuttarIntegrationService service, TransactionRequest request, String numTrans) {
        // Dicionário usado para acumular todos os dados (Subcampos) que a DLL nos devolver ao longo do fluxo.
        Map<String, String> camposRetornados = new HashMap<>();
        ultimaMensagemDisplay = "";

        // 1. DÁ O "START" NA TRANSAÇÃO
        int ret = service.iniciarTransacao(request, numTrans);
        if (ret != CodigoRetornoFuncao.Sucesso.valor) {
            exibirResultado(request, criarRespostaDeErro("Falha ao iniciar a transação. Código: " + ret, null), numTrans);
            return;
        }

        // Buffers passados por referência para a DLL ler e escrever comandos e textos de tela.
        byte[] comando = new byte[256], campo = new byte[256], valor = new byte[BUFFER_SIZE],
                tamanho = new byte[256], display = new byte[256];

        // O primeiro comando para entrar no loop deve ser sempre "00" (Avançar/Prosseguir).
        System.arraycopy("00".getBytes(), 0, comando, 0, 2);

        // 2. LOOP DE CONTINUAÇÃO (A MÁQUINA DE ESTADOS)
        // Enquanto a DLL responder '99' (Aguardando), significa que o fluxo ainda não acabou.
        ret = CodigoRetornoFuncao.AguardandoContinuacao.valor;
        while (ret == CodigoRetornoFuncao.AguardandoContinuacao.valor) {
            ret = service.continuarTransacao(comando, campo, valor, tamanho, display);
            if (ret == CodigoRetornoFuncao.AguardandoContinuacao.valor) {
                // Passa a bola para o método que vai ler o comando da DLL e reagir de acordo.
                tratarComandoInterativo(comando, campo, valor, tamanho, display, camposRetornados, request);
            }
        }

        // 3. AVALIA O RESULTADO (Aprovada, Negada, Cancelada)
        TransactionResponse response = criarResposta(camposRetornados);
        response.setOperacaoRequest(request);

        // Se a transação abortou no meio do caminho (cancelada no pinpad, falta de rede, etc).
        if (ret != CodigoRetornoFuncao.Sucesso.valor) {
            // TRAVA 1 CONTRA ERRO 18:
            // O retorno 10 (Timeout do servidor) ou falhas no Pagamento PIX NÃO exigem o rollback "0",
            // pois o servidor da Auttar destrói essas transações internamente. Tentar desfazer causa Erro 18.
            if (ret != 10 && request.getOperacao() != CodigoOperacao.PagamentoPix) {
                service.finalizarTransacao("0", numTrans, camposRetornados);
            }

            String msg = "Erro/Cancelamento durante o processamento. Código: " + ret;
            if (response.getMensagemRetorno().contains("Cancelado")) {
                msg = "Transação Cancelada (Timeout ou Usuário).";
            }
            exibirResultado(request, criarRespostaDeErro(msg, camposRetornados), numTrans);
            return;
        }

        // Recupera o código REAL da operação que foi processada (Subcampo 7002).
        // Isso é vital porque, em caso de Timeout, a intenção original de "Pagamento PIX" (422)
        // pode ter se transformado em "Consulta PIX" (423) sob os panos pela própria DLL.
        int codOperacaoReal = request.getOperacao().valor;
        String codOperacaoFinalStr = camposRetornados.get(String.valueOf(Subcampo.CodigoOperacao.valor));

        if (codOperacaoFinalStr != null && !codOperacaoFinalStr.trim().isEmpty()) {
            try {
                codOperacaoReal = Integer.parseInt(codOperacaoFinalStr);
            } catch (NumberFormatException ignored) {
            }
        }

        // Verifica se a operação concluída é passível de Confirmação Final.
        // Operações administrativas, consultas e estornos de PIX não usam finalizaTransacao.
        boolean precisaFinalizar =
                codOperacaoReal != CodigoOperacao.ConsultaPix.valor &&
                        codOperacaoReal != CodigoOperacao.DevolucaoPix.valor &&
                        codOperacaoReal != CodigoOperacao.ReimpressaoUltimoComprovante.valor &&
                        codOperacaoReal != CodigoOperacao.ConfiguracaoCtfClient.valor &&
                        codOperacaoReal != CodigoOperacao.AutenticacaoTerminal.valor;

        // TRAVA  CONTRA ERRO 18 NO PIX:
        // Lemos a Flag que foi injetada no `tratarComandoInterativo` durante o timeout do PIX.
        // Se ocorreu a tela de reconsulta e a venda não foi paga, a DLL já encerrou o processo e não
        // aceitará um FinalizaTransacao("0").
        boolean pixReconsultado = "true".equals(camposRetornados.get("PixReconsultado"));

        if (codOperacaoReal == CodigoOperacao.PagamentoPix.valor) {
            if (!response.isAprovada() || pixReconsultado) {
                precisaFinalizar = false;
            }
        }

        // 4 - O COMMIT / ROLLBACK (Aperto de mãos final - Handshake)
        if (precisaFinalizar) {

            // Passo A: Aprovou? Registra no disco ANTES de enviar a confirmação para a Auttar.
            if (response.isAprovada()) {
                String valorStr = request.getValor() != null ? String.format("%.2f", request.getValor()) : "0.00";

                // Passando 'null' no segundo parâmetro, o próprio GerenciadorRecuperacaoTef
                // assume o controle do docFiscal (buscando o subcampo 7900 da DLL ou usando o sequencial).
                GerenciadorRecuperacaoTef.registrarPendencia(numTrans, null, valorStr, camposRetornados);
            }

            // IMPORTANTE: Passo B: Envia a confirmação (1) ou Desfazimento (0) para a Auttar E CAPTURA O RETORNO
            String confirmar = response.isAprovada() ? "1" : "0";
            boolean finalizacaoComSucesso = service.finalizarTransacao(confirmar, numTrans, camposRetornados);

            // Passo C: Tomada de decisão baseada no SUCESSO REAL da comunicação com a DLL
            if (response.isAprovada()) {
                if (finalizacaoComSucesso) {
                    // Cenário Perfeito: Aprovou no banco E a DLL confirmou com sucesso.
                    GerenciadorRecuperacaoTef.atualizarStatusTransacao("CONFIRMADA");
                } else {
                    // Cenário de Falha: Aprovou no banco, mas o CTFClient estava fechado ou travou.
                    System.out.println("\n🚨 [ALERTA CRÍTICO] A transação foi aprovada pelo banco, mas falhou ao confirmar localmente!");
                    System.out.println("🚨 O status permanecerá PENDENTE no log diário para desfazimento automático.");

                    // Alteramos a resposta para false para que o seu PDV saiba que não deve imprimir o cupom nem entregar a mercadoria.
                    response.setAprovada(false);
                }
            }
        }

// 5 - EXIBE RESULTADO NA TELA DO CAIXA
        exibirResultado(request, response, numTrans);

    }


    /**
     * Versão enxuta do `executarFluxoDeTransacao`, projetada para retornar o objeto TransactionResponse
     * diretamente para a rotina de Multiplos Cartões acumular os sucessos.
     */
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

        ret = CodigoRetornoFuncao.AguardandoContinuacao.valor;
        while (ret == CodigoRetornoFuncao.AguardandoContinuacao.valor) {
            ret = service.continuarTransacao(comando, campo, valor, tamanho, display);
            if (ret == CodigoRetornoFuncao.AguardandoContinuacao.valor) {
                tratarComandoInterativo(comando, campo, valor, tamanho, display, camposRetornados, request);
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
                String numTransStr = String.format("%02d", numeroDaTransacao);
                exibirResultado(request, response, numTransStr);
                transacoesAprovadas.add(response);

                // SALVA A PENDÊNCIA ASSIM QUE O CARTÃO É APROVADO
                String valorStr = String.format("%.2f", request.getValor());


                // ATUALIZADO: Passa o seu docFiscalDaVenda gerenciado pelo seu ERP!
                GerenciadorRecuperacaoTef.registrarPendencia(numTransStr, docFiscalDaVenda, valorStr, response.getCamposRetornados());


                String nsu = response.getNsuCtf() != null ? response.getNsuCtf() : "N/A";
                //String rede = response.getCamposRetornados().containsKey("010") ? response.getCamposRetornados().get("010") : "N/A";
                //String bandeira = response.getCamposRetornados().containsKey("011") ? response.getCamposRetornados().get("011") : "N/A";
                //GerenciadorRecuperacaoTef.registrarPendencia(numTransStr,

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
                            for (TransactionResponse respAprovada : transacoesAprovadas) {
                                String numTrans = respAprovada.getCamposRetornados().get("NumeroTransacao");
                                // TRAVA DE SEGURANÇA: Se a DLL apagou o mapa, forçamos o "01" (ou o sequencial correto)
                                if (numTrans == null || numTrans.isEmpty()) {
                                    numTrans = "01";
                                }
                                service.finalizarTransacao("0", numTrans, new HashMap<>());
                                GerenciadorRecuperacaoTef.atualizarStatusTransacao("DESFEITA_PELO_USUARIO");
                            }
                            System.out.println("\nVENDA CANCELADA. TRANSAÇÕES APROVADAS FORAM DESFEITAS.");
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
                System.out.println("Confirmando transações no sistema...");
                for (TransactionResponse respAprovada : transacoesAprovadas) {
                    String numTrans = respAprovada.getCamposRetornados().get("NumeroTransacao");
                    // TRAVA DE SEGURANÇA
                    if (numTrans == null || numTrans.isEmpty()) {
                        numTrans = "01";
                    }
                    service.finalizarTransacao("1", numTrans, respAprovada.getCamposRetornados());
                    GerenciadorRecuperacaoTef.atualizarStatusTransacao("CONFIRMADA");
                }
                System.out.println("\nVENDA CONFIRMADA COM SUCESSO!");
            } else {
                for (TransactionResponse respAprovada : transacoesAprovadas) {
                    String numTrans = respAprovada.getCamposRetornados().get("NumeroTransacao");
                    service.finalizarTransacao("0", numTrans, new HashMap<>());
                }

            }
        }
    }

    // =========================================================================
    // 🧠 INTELIGÊNCIA E MANIPULAÇÃO DE TELA
    // =========================================================================

    /**
     * Analisa o comando requisitado pela DLL (Ex: Mostrar Msg, Menu, Digitar Senha)
     * e prepara a resposta correta nos buffers de C++ (JNA) para a próxima iteração.
     */
    private static void tratarComandoInterativo(byte[] comando, byte[] campo, byte[] valor, byte[] tamanho, byte[] display, Map<String, String> camposRetornados, TransactionRequest request) {
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

        // CORREÇÃO DOS ERROS 5317 E 5331 (CT02): A REGRA DO ECO
        // A DLL exige um "eco" (Acknowledge) para comandos visuais/de controle.
        // Forçar "00" em comandos como "Limpar Tela (03)" quebra a sincronia. O correto é ecoar.
        String comandoParaProximaChamada = comandoStr;

        if (cmd != null) {
            switch (cmd) {
                case ObterSubcampo:
                    // Quando a DLL tiver dados prontos (ex: NSU, Códigos, Comprovantes), nós guardamos no Map.
                    camposRetornados.put(campoStr.substring(0, 4), valorAtual);
                    break;

                case ExibirMensagem: case ExibirTituloMenu: case AguardarTecla:
                    ultimaMensagemDisplay = valorAtual;
                    System.out.printf("INFO DLL: %s%n", valorAtual.replace(";", System.lineSeparator()));
                    break;

                case VerificarCancelamento:
                    comandoParaProximaChamada = "00";
                    break;

                case ExibirMenu:
                    String menuTextoUpper = valorAtual.toUpperCase().replace("Ã", "A");
                    String tituloUltimaMsg = ultimaMensagemDisplay.toUpperCase().replace("Ã", "A");
                    boolean isSimNao = menuTextoUpper.contains("SIM") && menuTextoUpper.contains("NAO");

                    String respostaAuto = null;

                    // INTELIGÊNCIA DE RESPOSTA AUTOMÁTICA (AUTO-RESPONDER)
                    if (isSimNao) {
                        // Trata o Timeout nativo do PIX perguntando se deseja reconsultar o status.
                        if ((tituloUltimaMsg.contains("NOVAMENTE") || tituloUltimaMsg.contains("CONSULTA"))) {
                            System.out.println("\n[AUTO] Reconsulta PIX detectada: Respondendo '1' (SIM).");
                            respostaAuto = "1";

                            // FLAG ANTI-ERRO 18: Informa ao 'executarFluxoDeTransacao' que o PIX entrou em
                            // modo de reconsulta nativa e, portanto, NÃO DEVE sofrer 'finalizaTransacao'.
                            camposRetornados.put("PixReconsultado", "true");
                        }
                    }

                    if (respostaAuto != null) {
                        // Aplica a resposta automática preenchendo os arrays JNA
                        java.util.Arrays.fill(valor, (byte)0);
                        java.util.Arrays.fill(tamanho, (byte)0);
                        byte[] respBytes = respostaAuto.getBytes();
                        System.arraycopy(respBytes, 0, valor, 0, respBytes.length);
                        String tamStr = String.format("%05d", respBytes.length);
                        System.arraycopy(tamStr.getBytes(), 0, tamanho, 0, tamStr.length());
                    } else {
                        // Caso seja um menu genérico, pede pro Caixa digitar a opção.
                        System.out.printf("MENU: %s%nSua opção: ", valorAtual.replace(";", " | "));
                        String escolhaMenu = scanner.nextLine();
                        java.util.Arrays.fill(valor, (byte)0);
                        java.util.Arrays.fill(tamanho, (byte)0);
                        byte[] escolhaBytes = escolhaMenu.getBytes();
                        System.arraycopy(escolhaBytes, 0, valor, 0, escolhaBytes.length);
                        String tamStr = String.format("%05d", escolhaBytes.length);
                        System.arraycopy(tamStr.getBytes(), 0, tamanho, 0, tamStr.length());
                    }
                    comandoParaProximaChamada = "05"; // Comando: "Opção de Menu Selecionada"
                    break;

                case CapturarDado:
                    System.out.print(valorAtual + ": ");
                    String dadoCapturado = scanner.nextLine();

                    // Limpeza explícita do buffer (vital no JNA/Java para não sujar variáveis nativas)
                    java.util.Arrays.fill(valor, (byte)0);
                    java.util.Arrays.fill(tamanho, (byte)0);

                    byte[] dadoBytes = dadoCapturado.getBytes();
                    System.arraycopy(dadoBytes, 0, valor, 0, dadoBytes.length);
                    String tamDadoStr = String.format("%05d", dadoBytes.length);
                    System.arraycopy(tamDadoStr.getBytes(), 0, tamanho, 0, tamDadoStr.length());
                    comandoParaProximaChamada = "07"; // Comando: "Dado Capturado Enviado"
                    break;
                default: break;
            }
        }

        // Insere o código que o Java definiu (Ex: 00, 05, 03) no array 'comando' que vai pra DLL na próxima rodada.
        java.util.Arrays.fill(comando, (byte)0);
        System.arraycopy(comandoParaProximaChamada.getBytes(), 0, comando, 0, comandoParaProximaChamada.length());
    }

    // =========================================================================
    //  MÉTODOS AUXILIARES: CONSTRUÇÃO DE REQUISIÇÕES
    // =========================================================================

    private static TransactionRequest criarRequestPadrao(CodigoOperacao operacao, String docFiscal) {
        System.out.printf("\n--- %s ---%n", operacao.name());
        BigDecimal valor = obterValorDaTransacao();
        TransactionRequest req = new TransactionRequest();
        req.setOperacao(operacao);
        req.setValor(valor);
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
        return req;
    }

    private static TransactionRequest criarRequestReimpressao() {
        System.out.println("\n--- Reimpressão do Último Comprovante ---");
        TransactionRequest req = new TransactionRequest();
        req.setOperacao(CodigoOperacao.ReimpressaoUltimoComprovante);
        req.setValor(BigDecimal.ZERO);
        return req;
    }

    private static TransactionRequest criarRequestConfiguracao() {
        System.out.println("\n--- Configuração do CTFClient (800) ---");
        TransactionRequest req = new TransactionRequest();
        req.setOperacao(CodigoOperacao.ConfiguracaoCtfClient);
        req.setValor(BigDecimal.ZERO);
        return req;
    }

    private static TransactionRequest criarRequestAutenticacao() {
        System.out.println("\n--- Autenticação do Terminal (801) ---");
        TransactionRequest req = new TransactionRequest();
        req.setOperacao(CodigoOperacao.AutenticacaoTerminal);
        req.setValor(BigDecimal.ZERO);
        return req;
    }

    // =========================================================================
    // FORMATAÇÕES E COLETA DE INFORMAÇÃO BÁSICA
    // =========================================================================

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

    /**
     * Avalia os dados devolvidos pela DLL (no Map) para definir se a transação
     * foi Aprovada Financeiramente ou se foi Negada/Abortada.
     */
    private static TransactionResponse criarResposta(Map<String, String> campos) {
        String codRetorno = Helpers.getValueOrDefault(campos, String.valueOf(Subcampo.CodigoRetornoTransacao.valor));
        String codErro = Helpers.getValueOrDefault(campos, String.valueOf(Subcampo.CodigoErro.valor));

        // CORREÇÃO PIX: Para barrar o "Falso Positivo", a aprovação exige duplo-check:
        // O código principal (7000) DEVE ser "00", e o de erro (7300) NÃO DEVE conter bloqueios.
        boolean isAprovada = "00".equals(codRetorno) && (codErro == null || codErro.trim().isEmpty() || "0000".equals(codErro));

        TransactionResponse response = new TransactionResponse();
        response.setAprovada(isAprovada);
        response.setCodigoRetorno(codRetorno != null ? codRetorno : "ERRO");
        response.setCodigoErro(codErro);
        response.setMensagemRetorno(isAprovada ? "Transação Aprovada" : "Transação Negada/Pendente");
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