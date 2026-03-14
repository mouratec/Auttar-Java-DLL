# Configuração de Pinpad Gertec via Regras UDEV

Uma forma robusta e profissional de configurar um pinpad da Gertec no Linux é utilizando **regras udev**. Este método garante um nome de dispositivo estável (ex: `/dev/pinpad`) e aplica permissões automaticamente, sem precisar alterar usuários ou grupos manualmente a cada reinicialização.

### Passo 1: Identificar os IDs do Pinpad
1. Conecte o pinpad na porta USB.
2. Abra o terminal e execute:
   ```bash
   lsusb
   ```
3. Procure a linha correspondente ao seu dispositivo (ex: `Gertec PPC 930`). A saída será algo como `Bus 001 Device 005: ID 0ca6:a030 Gertec ...`
4. Anote os valores que aparecem depois de ID:
    * **idVendor:** `0ca6`
    * **idProduct:** `a030`

### Passo 2: Criar a Regra udev
Crie um arquivo de regra como administrador. O nome deve começar com número (ex: 99) para ser processado por último.
```bash
sudo nano /etc/udev/rules.d/99-gertec-pinpad.rules
```
Cole a linha abaixo dentro do arquivo (substituindo os valores de idVendor e idProduct se os seus forem diferentes):
```text
SUBSYSTEM=="tty", ATTRS{idVendor}=="0ca6", ATTRS{idProduct}=="a030", MODE="0666", SYMLINK+="pinpad"
```
* **MODE="0666"**: Define permissão de leitura e escrita para qualquer usuário.
* **SYMLINK+="pinpad"**: Cria um atalho fixo `/dev/pinpad` que aponta para o dispositivo real.

Salve e feche o editor.

### Passo 3: Aplicar as Novas Regras
Para que o sistema carregue sua nova regra sem precisar reiniciar, execute:
```bash
sudo udevadm control --reload-rules
sudo udevadm trigger
```
Para garantir, desconecte e reconecte o pinpad. Agora você pode apontar sua aplicação sempre para a porta `/dev/pinpad`.

---

### Alternativa: Método Padrão (Manual)

### 1. Método Padrão via Grupo `dialout` 
Se preferir não utilizar regras **udev**, você pode adicionar seu usuário ao grupo `dialout`. Isso concede permissão permanente (para o seu usuário) de acessar dispositivos seriais, como `/dev/ttyACM0`:

```bash
sudo usermod -a -G dialout $USER
```
> ⚠️ **Importante:** É necessário fazer logoff e login novamente na sua sessão do Linux para que a nova permissão de grupo entre em vigor, apenas um vez.

---

### 2. Método Direto com `chmod` (Temporário)
Outra forma é atribuir permissão de leitura e escrita manualmente ao dispositivo de forma direta:

```bash
sudo chmod 666 /dev/ttyACM0
```
> ⚠️ **Observações sobre este método:**
> * Essa abordagem concede acesso imediato (não exige logoff).
> * **É temporária:** A permissão volta ao padrão restrito assim que o dispositivo for desconectado do USB ou o sistema for reiniciado.
> * Use apenas em casos pontuais ou para testes rápidos, já que não é a forma definitiva ou mais segura.