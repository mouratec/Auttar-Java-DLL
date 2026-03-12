package com.auttar;

public class Enums {

    public enum CodigoOperacao {
        CreditoParceladoSemJuros(113),
        CreditoParceladoComJuros(114),
        Debito(101), Credito(112), CreditoDigitado(120), CreditoGenerico(223),
        DebitoGenerico(224), PagamentoPix(422),
        CancelamentoGenerico(128), CancelamentoCreditoDigitado(411), DevolucaoPix(431),
        ConsultaPix(423), ReimpressaoUltimoComprovante(12), ConfiguracaoCtfClient(800),
        AutenticacaoTerminal(801);
        public final int valor;
        CodigoOperacao(int v) { this.valor = v; }
    }

    public enum Subcampo {
        // Códigos oficiais de Retorno (Saída)
        CodigoRetornoTransacao(7000),
        CodigoTransacao(7001),
        ValorTransacao(7005),
        NumeroCartao(7006),
        NumeroParcelas(7008),
        VencimentoCartao(7010),
        NsuCtfOriginal(7012),
        NsuCtf(7031),
        DataTransacaoOriginal(7161),
        CodigoErro(7300),
        CupomCliente(7302),
        CupomLojista(7303),
        CodigoRepostaAutorizadora(7015),
        MensagemDisplay(7385);

        public final int valor;
        Subcampo(int v) { this.valor = v; }
    }

    public enum CodigoRetornoFuncao {
        Sucesso(0), ErroGenerico(-1), AguardandoContinuacao(99);
        public final int valor;
        CodigoRetornoFuncao(int v) { this.valor = v; }
    }

    public enum ComandoContinua {
        ObterSubcampo(0), ExibirMensagem(1), ExibirTituloMenu(2), LimparDisplay(3),
        ExibirMenu(5), AguardarTecla(6), CapturarDado(7), VerificarCancelamento(8);
        public final int valor;
        ComandoContinua(int v) { this.valor = v; }

        public static ComandoContinua fromInt(int id) {
            for (ComandoContinua cmd : values()) if (cmd.valor == id) return cmd;
            return null;
        }
    }
}