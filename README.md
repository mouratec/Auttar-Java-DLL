Aqui está uma proposta detalhada e profissional para o seu arquivo `README.md`, focada na estabilidade da integração, na arquitetura do projeto e nas soluções técnicas aplicadas (como a correção do erro de sincronia e o gerenciamento do PIX).

Este documento exclui as lógicas de simulação de desligamento/perda de rede (Testes 4.0 e 4.1), mantendo o foco no núcleo de funcionamento da integração com a Auttar.

---

# Integração Auttar TEF (CTFClient) - Java via JNA

Este projeto é uma aplicação modelo de Ponto de Venda (PDV) em console construída em **Java**, que realiza a integração direta com a biblioteca nativa da Auttar (`ctfclient.dll`) utilizando **JNA (Java Native Access)**.

O objetivo deste projeto é fornecer uma arquitetura limpa, robusta e orientada a objetos para o processamento de transações TEF (Transferência Eletrônica de Fundos), gerenciando adequadamente o ciclo de vida da transação, interatividade de tela e comunicação de baixo nível com o servidor CTF.

## 🚀 Funcionalidades Suportadas

* **Pagamentos Financeiros:** Crédito (À vista, Parcelado sem juros, Parcelado com juros), Débito e Crédito Digitado.
* **Transações PIX:** Geração e consulta de PIX integrados nativamente.
* **Múltiplos Cartões:** Rotina robusta para pagamento de uma única venda rateada em diversas formas de pagamento (com rollback automático em caso de cancelamento parcial).
* **Cancelamentos e Devoluções:** Cancelamento Genérico, Cancelamento de Crédito Digitado e Devolução PIX.
* **Funções Administrativas:** Configuração do Terminal (Operação 800), Autenticação (Operação 801) e Reimpressão do último comprovante.
* **Tratamento Autônomo (Auto-Responder):** Gerenciamento inteligente de menus de cancelamento e timeout, respondendo automaticamente à DLL da Auttar para manter a fluidez do caixa.

## ⚙️ Pré-requisitos

1. **Java Development Kit (JDK) 8 ou superior.**
* ⚠️ **MUITO IMPORTANTE:** A arquitetura do seu Java (32-bit ou 64-bit) **DEVE ser exatamente a mesma** da versão da DLL `ctfclient.dll` que você está utilizando. Executar uma DLL x86 em uma JVM x64 (ou vice-versa) resultará no erro `UnsatisfiedLinkError`.


2. **Biblioteca JNA (Java Native Access).** * Se usar Maven, adicione a dependência do `net.java.dev.jna:jna`.
3. **Ambiente Auttar:**
* O aplicativo CTFClient deve estar instalado no computador local.
* A pasta contendo a DLL deve estar acessível e com as permissões corretas do sistema operacional.



## 🏗️ Arquitetura do Projeto

O código foi dividido em camadas lógicas para separar a apresentação da comunicação nativa:

* `App.java`: Ponto de entrada da aplicação. Contém os menus de navegação, coleta os dados digitados pelo operador (valor, parcelas) e orquestra a comunicação interativa com a DLL (Loop de continuação e respostas de menu).
* `AuttarIntegrationService.java`: Camada de serviço responsável por montar os parâmetros exigidos pela Auttar (incluindo as strings de dados estendidos `[chave=valor]`) e invocar as funções da DLL.
* `CtfClientLibrary.java`: Interface JNA contendo a assinatura dos métodos exportados nativamente pela `ctfclient.dll` (`iniciaClientCTF`, `iniciaTransacaoCTF`, `continuaTransacaoCTF`, etc.).
* `TransactionRequest.java` / `TransactionResponse.java`: Classes POJO para o tráfego limpo de dados de requisição e resposta entre o menu e o serviço.
* `Enums.java`: Dicionários tipados contendo Códigos de Retorno, Códigos de Operação, Subcampos e Comandos de Tela, evitando a utilização de *Magic Numbers* no código.
* `Helpers.java`: Funções utilitárias (formatação de valores financeiros, extração de subcampos, formatação de datas).

## 🔧 Configuração e Instalação

Antes de rodar a aplicação, é necessário apontar o caminho correto da DLL da Auttar na sua máquina.

1. Abra o arquivo `src/main/java/com/auttar/CtfClientLibrary.java`.
2. Localize a constante `DLL_PATH`:
```java
String DLL_PATH = "C:\\Caminho\\Para\\A\\Sua\\ctfclient.dll";

```


3. Altere este caminho para refletir o diretório real de instalação do CTFClient (ex: `C:\Program Files (x86)\Auttar\CTFClient\bin\ctfclient.dll` ou pasta Portable equivalente).

## 💡 Lições Técnicas e Comportamentos (Troubleshooting)

Ao integrar Java com C++ via JNA usando as especificações da Auttar, foram tratados os seguintes cenários críticos neste código:

### 1. O Erro 5331 / CT02 (Erro ao Confirmar Dado)

Este erro ocorre quando o PDV "quebra a sincronia" de comandos com a DLL. Comandos de tela (ex: `03 - Limpar Display`, `06 - Aguardar Tecla`, `01 - Exibir Mensagem`) exigem um *Acknowledge* (Eco).

* **A Solução:** O método `tratarComandoInterativo` sempre devolve o **próprio comando recebido** para a DLL, a menos que seja um comando de entrada de dados (como um Menu ou Captura de Senha). Nunca force o retorno para `"00"` em todos os cenários.

### 2. Limpeza de Buffers JNA (Sujeira de Memória)

Ao contrário do C# (que gere o tamanho de `StringBuilders` automaticamente), no Java nós enviamos ponteiros de arrays brutos (`byte[]`). Se a DLL escreve 20 caracteres em uma iteração e apenas 5 na seguinte, os 15 caracteres antigos continuam lá.

* **A Solução:** O projeto zera os buffers (`Arrays.fill(valor, (byte)0)`) explicitamente nos eventos interativos antes de enviar qualquer resposta de usuário para a DLL, garantindo que nenhum "lixo de memória" afete a próxima requisição.

### 3. Timeout do PIX e Pinpad

O projeto **não utiliza contadores de tempo (timers) em Java** para forçar o cancelamento por inatividade. A responsabilidade do *Timeout* pertence exclusivamente ao Servidor CTF / Pinpad.

* **A Solução:** A aplicação escuta silenciosamente pela pergunta *"DESEJA CONSULTA-LO NOVAMENTE?"* disparada pelo CTFClient após a expiração do PIX e aplica a Auto-Resposta `"2"` (Não). Assim, o fluxo de cancelamento segue as regras e a temporização oficial da adquirente.

---

**Desenvolvido como arquitetura de referência para integrações de automação comercial TEF com o Gateway Auttar.**