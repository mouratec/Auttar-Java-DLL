package com.auttar;

/**
 * 📖 Dicionário de Integração Auttar (Evita "Magic Numbers" no código).
 * * Esta classe centraliza todos os códigos numéricos exigidos pela DLL da Auttar.
 * Usar Enums torna o código mais legível: é muito mais fácil entender
 * 'Subcampo.CodigoErro' do que tentar lembrar o que significa o número '7300'.
 */
public class Enums {

    /**
     * 🛠️ Códigos de Operação (O QUE VAMOS FAZER?)
     * * Estes códigos são enviados na função 'iniciaTransacaoCTF'.
     * Eles dizem à DLL qual é a intenção do PDV (ex: Fazer um débito,
     * configurar o terminal, cancelar uma venda, etc).
     */
    public enum CodigoOperacao {
        // Vendas - Crédito e Débito
        CreditoParceladoSemJuros(113),
        CreditoParceladoComJuros(114),
        Debito(101),
        Credito(112),
        CreditoDigitado(120),
        CreditoGenerico(223),
        DebitoGenerico(224),

        // Vendas -  PIX
        PagamentoPix(422),

        // Estornos e Cancelamentos
        CancelamentoGenerico(128),
        CancelamentoCreditoDigitado(411),
        DevolucaoPix(431),

        // Desfazimento por NSU (Usado na Recuperação de Desastres)
        DesfazimentoPorNsu(281),
        ConfirmacaoPorNSU(280),

        // Consultas e Funções Administrativas
        ConsultaPix(423),
        ReimpressaoUltimoComprovante(12),
        ConfiguracaoCtfClient(800),
        AutenticacaoTerminal(801);

        public final int valor;
        CodigoOperacao(int v) { this.valor = v; }
    }

    /**
     * 📥 Subcampos de Retorno (O QUE A DLL NOS DEVOLVEU?)
     * * Quando a transação termina (ou durante o processo), a DLL armazena as
     * informações da venda (NSU, número do cartão, comprovante) na memória.
     * Estes são os "IDs" que usamos para pedir à DLL: "Me dê o dado X".
     */
    public enum Subcampo {
        // Status e Identificação
        CodigoRetornoTransacao(7000), // Ex: "00" = Aprovado. Qualquer outra coisa = Negado/Erro.
        CodigoTransacao(7001),        // Código interno da transação no servidor.
        CodigoOperacao(7002),         // Operação real que a DLL executou (útil para detectar PIX que virou Consulta por timeout).

        // Dados Financeiros e Cartão
        ValorTransacao(7005),         // Valor real processado (ex: 1000 = R$ 10,00).
        NumeroCartao(7006),           // PAN mascarado (Ex: 401234******9010).
        NumeroParcelas(7008),         // Quantidade de parcelas aprovadas.
        VencimentoCartao(7010),       // Validade do cartão (MMAA).


        // NSUs (Número Sequencial Único - Identificador do recibo)
        NsuCtfOriginal(7012),         // Usado na hora de fazer cancelamentos (NSU da venda original).
        NsuCtf(7031),                 // NSU da transação atual.
        DataTransacaoOriginal(7161),  // Data da venda original (DDMMAA), exigido para cancelamentos.

        // Erros e Mensagens
        CodigoErro(7300),             // Detalhe técnico do erro (ex: 5317 = cancelado pelo usuário, 5331 = erro de buffer).
        MensagemDisplay(7385),        // Mensagem amigável para exibir na tela ("SENHA INVALIDA", "SALDO INSUFICIENTE").
        CodigoRepostaAutorizadora(7015), // Código devolvido pelo banco/adquirente (ex: 51 = Sem Saldo).

        // Comprovantes para Impressão
        CupomCliente(7302),           // Texto formatado do recibo que fica com o cliente.
        CupomLojista(7303);           // Texto formatado do recibo que fica na loja.

        public final int valor;
        Subcampo(int v) { this.valor = v; }
    }

    /**
     * 🚦 Código de Retorno da Função (O LOOP CONTINUA?)
     * * Este é o retorno imediato do C++ (da função 'continuaTransacaoCTF').
     * Serve EXCLUSIVAMENTE para controlar o laço 'while' no Java.
     */
    public enum CodigoRetornoFuncao {
        Sucesso(0),               // A transação/função terminou o fluxo com sucesso (Sair do loop).
        ErroGenerico(-1),         // Houve uma falha crítica ou de comunicação (Sair do loop e abortar).
        AguardandoContinuacao(99); // O fluxo ainda não acabou. A DLL quer falar com o PDV (Manter o loop rodando).

        public final int valor;
        CodigoRetornoFuncao(int v) { this.valor = v; }
    }

    /**
     * 🎮 Comandos Interativos (A DLL ESTÁ PEDINDO ALGO)
     * * Enquanto o retorno for 99 (AguardandoContinuacao), a DLL manda um comando numérico.
     * Esse comando diz ao PDV o que ele deve mostrar na tela ou pedir ao operador.
     */
    public enum ComandoContinua {
        ObterSubcampo(0),         // A DLL está entregando um dado pronto (ex: NSU, Cupom). Devemos guardar no Map.
        ExibirMensagem(1),        // Mostrar um texto simples na tela ("Aguarde...", "Insira o Cartão").
        ExibirTituloMenu(2),      // Título de um menu que virá logo a seguir ("Deseja tentar novamente?").
        LimparDisplay(3),         // Apagar a tela do PDV (Não exige ação do operador, apenas devolver '03' como eco).
        ExibirMenu(5),            // Mostrar opções (1: Sim, 2: Não) e aguardar o operador digitar a escolha.
        AguardarTecla(6),         // Pausar o fluxo e esperar o operador apertar [ENTER].
        CapturarDado(7),          // Pedir ao operador para digitar algo livremente (ex: Senha do supervisor, CVV).
        VerificarCancelamento(8); // A DLL pergunta silenciosamente: "O usuário apertou o botão de cancelar (ESC) por aí?".

        public final int valor;
        ComandoContinua(int v) { this.valor = v; }

        /**
         * Traduz o número bruto recebido do C++ (ex: 5) para o objeto Enum (ExibirMenu).
         */
        public static ComandoContinua fromInt(int id) {
            for (ComandoContinua cmd : values()) {
                if (cmd.valor == id) return cmd;
            }
            return null; // Caso a DLL envie um comando novo/desconhecido
        }
    }
}