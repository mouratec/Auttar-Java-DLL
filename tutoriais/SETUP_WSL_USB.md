# Compartilhando Pinpad (USB) para o WSL 2

Para utilizar dispositivos USB diretamente no Windows Subsystem for Linux (WSL), utilizamos a ferramenta oficial da Microsoft: **usbipd**.

### 1. Pré-requisitos
* Instalar o utilitário USB na sua distro Linux (WSL):
  ```bash
  sudo apt install usbutils
  ```
* Ter o software `usbipd-win` instalado no Windows.

### 2. Listar e Compartilhar no Windows
Abra o **PowerShell/CMD como Administrador** e digite o comando abaixo para ver o que está conectado. Identifique o **BUSID** (ex: `4-4`) do pinpad que deseja usar:
```powershell
usbipd list
```

Para que o WSL consiga enxergar o USB, você precisa autorizar o compartilhamento no Windows:
```powershell
usbipd bind --busid <BUSID>
```
*(Dica: Rode `usbipd list` novamente para confirmar se o status mudou para "Shared").*

### 3. Anexar ao WSL (Attach)
Agora, você vai entregar o dispositivo para o Linux. Certifique-se de que uma janela do seu terminal Linux esteja aberta antes de rodar o comando.
```powershell
usbipd attach --wsl --busid <BUSID>
```
A partir deste momento, o Windows deixará de ter acesso ao dispositivo para que o Linux assuma o controle.

### 4. Verificar no Linux
Dentro do seu terminal **WSL/Linux**, digite o comando abaixo para confirmar que o sistema reconheceu o hardware:
```bash
lsusb
```

### 5. Como desconectar e remover
Existem três formas de encerrar a conexão, via PowerShell, dependendo do que você precisa:

* **Apenas desconectar do Linux (Detach):** O dispositivo volta a ser usado pelo Windows, mas continua "disponível" para futuras conexões com o WSL.
  ```powershell
  usbipd detach --busid <BUSID>
  ```
* **Remover o compartilhamento (Unbind):** Remove a permissão de compartilhamento dada no Passo 2.
  ```powershell
  usbipd unbind --busid <BUSID>
  ```
* **Remoção física:** Simplesmente desconecte o cabo USB.