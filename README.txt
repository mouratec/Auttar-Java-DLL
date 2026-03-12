Visão Geral da Arquitetura do Projeto
O projeto está estruturado de forma coesa para interagir com a DLL da Auttar:

pom.xml: Gerencia as dependências do projeto, como o JNA para comunicação com a DLL.

App.java: É a camada de "Apresentação" (View/Controller). Cuida de toda a interação com o usuário (menus do console) e orquestra o fluxo das transações.

AuttarIntegrationService.java: É a camada de "Serviço". Ela abstrai a complexidade da comunicação com a DLL, oferecendo métodos mais simples para a App.java consumir (ex: iniciarTransacao, finalizarTransacao).

CtfClientLibrary.java: É a camada de "Acesso a Dados" (Data Access Layer), neste caso, o acesso nativo. É a ponte direta entre o Java e a ctfclient.dll usando a tecnologia JNA.

TransactionRequest.java / TransactionResponse.java: São os "Modelos" ou "Objetos de Transferência de Dados" (DTOs). Eles estruturam os dados que são enviados e recebidos durante uma transação.

Enums.java / Helpers.java: São classes utilitárias que fornecem constantes (Enums) e funções de ajuda (Helpers) para manter o código limpo, organizado e legível.