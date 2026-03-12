package com.auttar;

import com.sun.jna.Native;
import com.sun.jna.win32.StdCallLibrary;

/**
 * Interface que mapeia as funções da 'ctfclient.dll' para métodos Java utilizando a biblioteca JNA.
 * A JNA usará esta interface para carregar a DLL e permitir a chamada de suas funções de forma direta.
 * StdCallLibrary é a convenção de chamada de função padrão para a maioria das DLLs do Windows.
 */
public interface CtfClientLibrary extends StdCallLibrary {

    // Define o caminho completo para a DLL. É crucial que este caminho esteja correto.
    // Usar a versão 64 bits da DLL requer uma JVM de 64 bits.
    String DLL_PATH = "C:\\Program Files (x86)\\Auttar\\CTFClient\\bin\\ctfclient.dll";

    // Carrega a DLL em memória e a associa a esta interface.
    // A partir deste ponto, podemos chamar os métodos definidos aqui como se fossem métodos Java.
    CtfClientLibrary INSTANCE = Native.load(DLL_PATH, CtfClientLibrary.class);

    // Mapeamento da função 'iniciaClientCTF' da DLL.
    // Usamos byte[] (buffers) para passar e receber strings, pois a DLL manipula ponteiros de memória,
    // e o JNA faz a conversão de forma eficiente.
    void iniciaClientCTF(
            byte[] resultado, String strTerminal, String strVersaoPDV,
            String strNomePDV, String strNumSites, String strListaIps, String strCriptografia, String strLog,
            String strInterativo, String strParametros);

    // Mapeamento da função 'iniciaTransacaoCTF' da DLL.
    void iniciaTransacaoCTF(
            byte[] resultado, String strOperacao, String strValor, String strDocumento,
            String dataTransacao, String numTrans);

    // Mapeamento da função 'iniciaTransacaoCTFext', versão estendida para enviar dados adicionais.
    void iniciaTransacaoCTFext(
            byte[] resultado, String strOperacao, String strValor, String strDocumento,
            String dataTransacao, String numTrans, String dados);

    // Mapeamento da função 'continuaTransacaoCTF', usada para a comunicação interativa durante a transação.
    void continuaTransacaoCTF(
            byte[] Resultado, byte[] Comando,
            byte[] Campo, byte[] Valor, byte[] Tamanho, byte[] Display);

    // Mapeamento da função 'finalizaTransacaoCTF', para confirmar ou desfazer uma transação.
    int finalizaTransacaoCTF(
            byte[] resultado, String confirmar, String numTrans, String dataTransacao);
}