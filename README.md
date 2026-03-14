# Integração Auttar TEF (CTFClient) - Java via JNA

Este projeto é uma aplicação modelo de Ponto de Venda (PDV) em console construída em **Java**, que realiza a integração direta com a biblioteca nativa da Auttar (`ctfclient.dll` / `libctfclient.so`) utilizando **JNA (Java Native Access)**.

O objetivo deste projeto é fornecer uma arquitetura limpa, robusta e orientada a objetos para o processamento de transações TEF (Transferência Eletrônica de Fundos), gerenciando adequadamente o ciclo de vida da transação, interatividade de tela e comunicação de baixo nível com o servidor CTF em ambientes **Windows e Linux**.

## 🚀 Funcionalidades Suportadas

* **Pagamentos Financeiros:** Crédito (À vista, Parcelado sem juros, Parcelado com juros), Débito e Crédito Digitado.
* **Transações PIX:** Geração e consulta de PIX integrados nativamente.
* **Múltiplos Cartões:** Rotina robusta para pagamento de uma única venda rateada em diversas formas de pagamento (com rollback automático em caso de cancelamento parcial).
* **Cancelamentos e Devoluções:** Cancelamento Genérico, Cancelamento de Crédito Digitado e Devolução PIX.
* **Funções Administrativas:** Configuração do Terminal (Operação 800), Autenticação (Operação 801) e Reimpressão do último comprovante.
* **Tratamento Autônomo (Auto-Responder):** Gerenciamento inteligente de menus de cancelamento e timeout, respondendo automaticamente à DLL da Auttar para manter a fluidez do caixa.

## ⚙️ Pré-requisitos

1. **Java Development Kit (JDK) 8 ou superior.**
    * ⚠️ **MUITO IMPORTANTE:** A arquitetura do seu Java (32-bit ou 64-bit) **DEVE ser exatamente a mesma** da versão da biblioteca (`ctfclient.dll` ou `libctfclient.so`) que você está utilizando. Executar uma lib x86 em uma JVM x64 (ou vice-versa) resultará no erro `UnsatisfiedLinkError`.
2. **Biblioteca JNA (Java Native Access).** * Se usar Maven, adicione a dependência `net.java.dev.jna:jna`.
3. **Ambiente Auttar:**
    * O aplicativo CTFClient deve estar instalado no computador local (ou executado em modo Portátil).

## 🏗️ Arquitetura do Projeto

O código foi dividido em camadas lógicas para separar a apresentação da comunicação nativa:

* `App.java`: Ponto de entrada da aplicação. Contém os menus de navegação e orquestra a comunicação interativa.
* `AuttarIntegrationService.java`: Camada de serviço responsável por montar os parâmetros exigidos pela Auttar e invocar as funções nativas.
* `CtfClientLibrary.java`: Interface JNA multiplataforma contendo a assinatura dos métodos exportados nativamente.
* `TransactionRequest.java` / `TransactionResponse.java`: Classes POJO (Plain Old Java Object) para o tráfego limpo de dados de requisição e resposta.
* `Enums.java`: Dicionários tipados contendo Códigos de Retorno e Comandos de Tela, evitando *Magic Numbers*.

## 🔧 Configuração e Instalação da Biblioteca Nativa

A interface `CtfClientLibrary.java` foi projetada para localizar a biblioteca nativa da Auttar automaticamente. A aplicação buscará a biblioteca na seguinte ordem:

1. **Via Linha de Comando (Prioridade Máxima):** ```bash
   java -Djna.library.path=/caminho/para/sua/pasta/ -jar dll-java-2.0.jar
   ```
2. **Modo Portátil (Diretório Atual):** Se a biblioteca estiver na mesma pasta onde o `.jar` está sendo executado.
3. **Padrões do Sistema Operacional:**
    * **Windows:** `C:\Program Files (x86)\Auttar\CTFClient\bin\ctfclient.dll`
    * **Linux:** `/opt/CTFClient/lib/libctfclient.so`

## 📚 Guias e Tutoriais de Infraestrutura

Dependendo do seu sistema operacional e setup físico, consulte os guias detalhados na pasta `tutoriais/`:

* 🐧 [Configurando CTFClient x86 no Linux (Debian/Ubuntu)](tutoriais/SETUP_LINUX_X86.md)
* 🔌 [Configurando Pinpad Gertec via regras UDEV no Linux](tutoriais/SETUP_PINPAD.md)
* 💻 [Compartilhando USB (Pinpad) no WSL 2 do Windows](tutoriais/SETUP_WSL_USB.md)

## 💡 Lições Técnicas (Troubleshooting)

* **Erro 5331 / CT02 (Erro ao Confirmar Dado):** Ocorre quando há quebra de sincronia. A solução aplicada foi sempre devolver o **próprio comando recebido** como *Acknowledge* (Eco) para a DLL, exceto em comandos de entrada de dados.
* **Sujeira de Memória JNA:** Arrays brutos (`byte[]`) são zerados explicitamente (`Arrays.fill`) antes de cada envio para evitar que o lixo de memória da iteração anterior corrompa a requisição atual.
* **Timeout do PIX e Pinpad:** Não utilizamos timers no Java. A aplicação escuta a pergunta "DESEJA CONSULTA-LO NOVAMENTE?" após a expiração do PIX e aplica a resposta automática "2" (Não).