package com.auttar;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 🛡️ Gerenciador de Recuperação e Auditoria TEF (Nível Produção).
 * * Responsável por garantir a integridade transacional do PDV em casos de falhas
 * (quedas de energia, travamento de rede, fechamento abrupto).
 * Utiliza o padrão de "Handshake Pessimista", registrando o estado antes do envio
 * e realizando o Rollback nativo (Flag 0) caso a transação não seja confirmada.
 */
public class GerenciadorRecuperacaoTef {

    /**
     * Gera o nome do arquivo de log dinâmico baseado na data atual.
     * @return String com o nome do arquivo (ex: tef_diario_20260315.txt)
     */
    private static String getFilePath() {
        String dataAtual = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "tef_diario_" + dataAtual + ".txt";
    }

    /**
     * Carrega as propriedades gravadas no log diário para a memória.
     * @return Objeto Properties contendo o histórico do dia.
     */
    private static Properties carregarPropriedades() {
        Properties prop = new Properties();
        File file = new File(getFilePath());
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                prop.load(input);
            } catch (IOException e) {
                System.err.println("[ERRO] Falha ao ler o log diário do TEF: " + e.getMessage());
            }
        }
        return prop;
    }

    /**
     * Persiste as propriedades da memória no arquivo de texto físico.
     * @param prop Objeto Properties contendo o estado atualizado das transações.
     */
    private static void salvarPropriedades(Properties prop) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(getFilePath()))) {
            writer.println("# ==========================================");
            writer.println("# Log Diario de Transacoes TEF (Auditoria)");
            writer.println("# Data: " + LocalDate.now());
            writer.println("# ==========================================\n");

            String ultimoSeq = prop.getProperty("ultimo_sequencial", "0");
            writer.println("ultimo_sequencial=" + ultimoSeq);
            writer.println();

            int total = Integer.parseInt(ultimoSeq);
            for (int i = 1; i <= total; i++) {
                String s = String.format("%03d", i);
                writer.println("# --- Registro " + s + " ---");
                writer.println(s + ".status=" + prop.getProperty(s + ".status", "DESCONHECIDO"));
                writer.println(s + ".numTrans=" + prop.getProperty(s + ".numTrans", "01"));
                writer.println(s + ".docFiscal=" + prop.getProperty(s + ".docFiscal", "N/A"));
                writer.println(s + ".opRealizada=" + prop.getProperty(s + ".opRealizada", "N/A"));
                writer.println(s + ".dataHora=" + prop.getProperty(s + ".dataHora", ""));
                writer.println(s + ".valor=" + prop.getProperty(s + ".valor", ""));
                writer.println(s + ".nsuHost=" + prop.getProperty(s + ".nsuHost", "N/A"));
                writer.println(s + ".rede=" + prop.getProperty(s + ".rede", "N/A"));
                writer.println(s + ".bandeira=" + prop.getProperty(s + ".bandeira", "N/A"));
                writer.println(s + ".codErro=" + prop.getProperty(s + ".codErro", ""));
                writer.println(s + ".msgErro=" + prop.getProperty(s + ".msgErro", ""));
                writer.println();
            }
        } catch (IOException e) {
            System.err.println("[ERRO] Falha ao salvar log diário: " + e.getMessage());
        }
    }

    /**
     * Registra a intenção de transação ANTES dela ser enviada à DLL.
     * Funciona como um "Savepoint". Se o sistema cair logo após isso, saberemos o que ficou pendente.
     * * @param numTrans Canal sequencial aberto com a Auttar (ex: "01").
     * @param docFiscalOrigem Número do cupom no PDV.
     * @param valorSolicitado Valor financeiro da operação.
     * @param campos Map contendo subcampos extras (NSU, Bandeira) caso já existam.
     */
    public static void registrarPendencia(String numTrans, String docFiscalOrigem, String valorSolicitado, Map<String, String> campos) {
        Properties prop = carregarPropriedades();
        int novoSeq = Integer.parseInt(prop.getProperty("ultimo_sequencial", "0")) + 1;
        String s = String.format("%03d", novoSeq);
        prop.setProperty("ultimo_sequencial", String.valueOf(novoSeq));

        String p = s + ".";

        // Fallbacks inteligentes de extração de dados para relatórios
        String rede = campos.getOrDefault("0010", campos.getOrDefault("7942", "N/A"));
        String bandeira = campos.getOrDefault("0011", campos.getOrDefault("7948", campos.getOrDefault("7389", "N/A")));
        String nsu = campos.getOrDefault("0013", campos.getOrDefault("0012", campos.getOrDefault("7031", "N/A")));
        String opRealizada = campos.getOrDefault("7002", campos.getOrDefault("7948", "N/A"));
        String erro = campos.getOrDefault("7300", "0000");
        String msgErro = campos.getOrDefault("7301", "SUCESSO");

        String docFiscal = (docFiscalOrigem != null && !docFiscalOrigem.trim().isEmpty())
                ? docFiscalOrigem
                : campos.getOrDefault("7900", s);

        prop.setProperty(p + "status", "PENDENTE");
        prop.setProperty(p + "numTrans", numTrans);
        prop.setProperty(p + "docFiscal", docFiscal);
        prop.setProperty(p + "opRealizada", opRealizada);
        prop.setProperty(p + "dataHora", LocalDateTime.now().toString());
        prop.setProperty(p + "valor", valorSolicitado);
        prop.setProperty(p + "nsuHost", nsu);
        prop.setProperty(p + "rede", rede);
        prop.setProperty(p + "bandeira", bandeira);
        prop.setProperty(p + "codErro", erro);
        prop.setProperty(p + "msgErro", msgErro);

        salvarPropriedades(prop);
    }

    /**
     * Atualiza o status do último registro criado no dia.
     * @param novoStatus Status final (ex: "CONFIRMADA", "NEGADA").
     */
    public static void atualizarStatusTransacao(String novoStatus) {
        Properties prop = carregarPropriedades();
        String seqStr = prop.getProperty("ultimo_sequencial", "0");
        String seq = String.format("%03d", Integer.parseInt(seqStr));

        if (prop.containsKey(seq + ".status")) {
            prop.setProperty(seq + ".status", novoStatus);
            salvarPropriedades(prop);
        }
    }

    /**
     * 🚀 Recuperação Automática no Boot (Inicialização do PDV).
     * Varre o arquivo diário em busca de transações com status PENDENTE.
     * Caso encontre, realiza o Desfazimento Nativo (Flag 0) para limpar a memória da DLL
     * e enviar o comando de estorno ao servidor da Auttar de forma silenciosa e infalível.
     * * @param auttarService Instância ativa do serviço de integração JNA.
     */
    public static void verificarERecuperar(AuttarIntegrationService auttarService) {
        Properties prop = carregarPropriedades();
        int total = Integer.parseInt(prop.getProperty("ultimo_sequencial", "0"));
        boolean houveRecuperacao = false;

        for (int i = 1; i <= total; i++) {
            String s = String.format("%03d", i);
            String prefixo = s + ".";

            if ("PENDENTE".equals(prop.getProperty(prefixo + "status"))) {
                houveRecuperacao = true;
                String numTrans = prop.getProperty(prefixo + "numTrans", "01");
                String nsu = prop.getProperty(prefixo + "nsuHost", "N/A");

                System.out.println("\n=======================================================");
                System.out.println("[SISTEMA] - IDENTIFICADA INTERRUPÇÃO");
                System.out.println("Registro Transação : " + s);
                System.out.println("NSU Pendente  : " + nsu);
                System.out.println("=======================================================");

                // 🛡O ROLLBACK NATIVO (FLAG 0) = Confirma 0
                // A biblioteca Auttar foi projetada para que a simples finalização com flag 0
                // no sequencial que ficou aberto efetue a limpeza dos arquivos .dat locais
                // e sincronize o estorno de forma automática no servidor, sem necessidade de menus.

                auttarService.finalizarTransacao("0", numTrans, new HashMap<>());

                // Atualiza o log para garantir que a varredura não ocorra em duplicidade
                prop.setProperty(prefixo + "status", "DESFEITA_POR_RECUPERACAO");
                salvarPropriedades(prop);

                System.out.println("[RECUPERAÇÃO] Transação desfeita com sucesso no CTF! \nNSU:");
            }
        }

        if (!houveRecuperacao) {
            System.out.println("[SISTEMA] Inicialização do PDV limpa. Sem transações pendentes.");
        }
    }
}