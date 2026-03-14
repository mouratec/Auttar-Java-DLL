# Configuração CTFClient Portable x86 no Debian/Ubuntu (64-bit)

Como o pacote portable do CTFClient é compilado em 32-bit (x86), é necessário habilitar a arquitetura `i386` e instalar as bibliotecas de compatibilidade no seu sistema 64-bit.

### 1. Habilitar a arquitetura e atualizar repositórios
```bash
sudo dpkg --add-architecture i386
sudo apt update
```

### 2. Instalar bibliotecas de compatibilidade
Aplicações 32-bit dependem de bibliotecas vitais (como `libc6`, `libstdc++6`, etc). Execute o comando abaixo:
```bash
sudo apt install libc6:i386 libstdc++6:i386 zlib1g:i386 libx11-6:i386 libxext6:i386 libxi6:i386 libxtst6:i386 libgcc1:i386 libncurses5:i386 libnss3:i386 libasound2:i386 libgtk2.0-0:i386 libxrender1:i386 libxt6:i386 libxrandr2:i386
```

> **Nota:** Caso o comando acima apresente erro de pacote não encontrado, substitua `ncurses5` por `ncurses6` e `gcc1` por `gcc-s1`:
> ```bash
> sudo apt install libc6:i386 libstdc++6:i386 zlib1g:i386 libx11-6:i386 libxext6:i386 libxi6:i386 libxtst6:i386 libgcc-s1:i386 libncurses6:i386 libnss3:i386 libasound2t64:i386 libgtk2.0-0t64:i386 libxrender1:i386 libxt6t64:i386 libxrandr2:i386 -y
> ```

### 3. Configurar Variáveis de Ambiente
Execute os comandos abaixo no terminal (ou adicione-os ao final do seu `~/.bashrc`) para que o sistema localize o Java 32-bit e as dependências da Auttar antes de rodar a aplicação:

```bash
# Definir o diretório do CTFClient, dentro do pacote Portable
export CTFCLIENT_HOME=/opt/CTFClient

# JRE que vem empacotado no CTFClient
export JAVA_HOME=$CTFCLIENT_HOME/jre

# Adicione o JRE no PATH da aplicacao
export PATH=$JAVA_HOME/bin:$PATH

# Adicionar bibliotecas nativas ao Path do Linux
export LD_LIBRARY_PATH=$JAVA_HOME/lib:$JAVA_HOME/lib/i386:$JAVA_HOME/lib/i386/server:$CTFCLIENT_HOME/lib:$LD_LIBRARY_PATH

```
- **Nota:**  `$JAVA_HOME/lib:` Bibliotecas gerais do Java.
- **Nota:**  `$JAVA_HOME/lib/i386:` Bibliotecas específicas para a arquitetura de 32 bits (x86).
- **Nota:** `$JAVA_HOME/lib/i386/server:` Bibliotecas essenciais da Máquina Virtual Java (JVM).
- **Nota:** `$CTFCLIENT_HOME/lib:` Bibliotecas específicas que o seu software (CTF Client) precisa para funcionar



Se tiver inserido as exportações no `~/.bashrc`, recarregue-o com:
```bash
source ~/.bashrc
```

### 4. Executando a Aplicação
Com tudo configurado, depois de compactar o package Java, garanta que o `.jar` esteja no mesmo diretório da bibliotecas $CTFClient/lib  `.so` (ou passe o parâmetro do JNA) e execute:
```bash
java -jar dll-java-2.0.jar
```
> *Observação:* Antes de executar os comandos abaixo, é necessário criar o *package* do projeto Java.  
> Caso esteja utilizando o *IntelliJ IDEA*, siga os passos:

1. No canto superior direito, selecione a opção **Maven**.
2. Clique em **Lifecycle → Clean** para limpar o projeto.
3. Em seguida, clique em **Lifecycle → Package** para gerar o pacote final.  