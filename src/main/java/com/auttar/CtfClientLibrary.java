package com.auttar;

import com.sun.jna.Function;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * 🌉 A Ponte entre o Java e o C++ (JNA - Java Native Access)
 * Versão Multiplataforma (Windows/Linux) com Convenção de Chamada Dinâmica.
 */
public interface CtfClientLibrary extends Library {

    // -------------------------------------------------------------------------
    // 📍 LÓGICA DE LOCALIZAÇÃO (DIRETÓRIO ATUAL + SISTEMA)
    // -------------------------------------------------------------------------
    static String getLibraryPath() {
        String jnaPath = System.getProperty("jna.library.path");
        String libName = Platform.isWindows() ? "ctfclient.dll" : "libctfclient.so";

        // 1. Tenta pelo parâmetro de inicialização (Maior prioridade)
        if (jnaPath != null) {
            File forcedLib = new File(jnaPath, libName);
            if (forcedLib.exists()) {
                System.out.println("[CTFClient] INFO: Carregando biblioteca via jna.library.path -> " + forcedLib.getAbsolutePath());
                return forcedLib.getAbsolutePath();
            }
        }

        // 2. Fallback: Procura na pasta atual (Modo Portátil)
        File localLib = new File(libName);
        if (localLib.exists()) {
            System.out.println("[CTFClient] INFO: Carregando biblioteca local (Modo Portátil) -> " + localLib.getAbsolutePath());
            return localLib.getAbsolutePath();
        }

        // 3. Fallback Final: Padrões do sistema
        String defaultPath = Platform.isWindows()
                ? "C:\\Program Files (x86)\\Auttar\\CTFClient\\bin\\ctfclient.dll"
                : "/opt/CTFClient/lib/libctfclient.so";

        System.out.println("[CTFClient] WARN: Biblioteca não encontrada nos caminhos locais. Tentando caminho padrão -> " + defaultPath);
        return defaultPath;
    }

    String LIBRARY_PATH = getLibraryPath();

    // -------------------------------------------------------------------------
    // 🛠️ CONFIGURAÇÃO DE CONVENÇÃO (Windows: StdCall | Linux: Cdecl)
    // -------------------------------------------------------------------------
    static Map<String, Object> createLibraryOptions() {
        Map<String, Object> options = new HashMap<>();
        if (Platform.isWindows()) {
            options.put(Library.OPTION_CALLING_CONVENTION, Function.ALT_CONVENTION);
        }
        return options;
    }

    Map<String, Object> LIBRARY_OPTIONS = createLibraryOptions();

    // Carregamento da Instância
    CtfClientLibrary INSTANCE = Native.load(LIBRARY_PATH, CtfClientLibrary.class, LIBRARY_OPTIONS);

    // -------------------------------------------------------------------------
    // MAPEAMENTO DAS FUNÇÕES
    // -------------------------------------------------------------------------

    void iniciaClientCTF(
            byte[] resultado, String strTerminal, String strVersaoPDV,
            String strNomePDV, String strNumSites, String strListaIps, String strCriptografia, String strLog,
            String strInterativo, String strParametros);

    void iniciaTransacaoCTF(
            byte[] resultado, String strOperacao, String strValor, String strDocumento,
            String dataTransacao, String numTrans);

    void iniciaTransacaoCTFext(
            byte[] resultado, String strOperacao, String strValor, String strDocumento,
            String dataTransacao, String numTrans, String dados);

    void continuaTransacaoCTF(
            byte[] Resultado, byte[] Comando,
            byte[] Campo, byte[] Valor, byte[] Tamanho, byte[] Display);

    int finalizaTransacaoCTF(
            byte[] resultado, String confirmar, String numTrans, String dataTransacao);
}