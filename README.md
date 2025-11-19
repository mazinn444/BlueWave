# 🌊 BlueWave Pro

> **O Player de Música Moderno, Leve e Estiloso feito em JavaFX.**

O **BlueWave Pro** é um reprodutor de música desktop, focado em performance e funcionalidades essenciais para quem ama música. Ele suporta arquivos locais, playlists, equalização visual e até um modo "Slowed" nativo.

![Java](https://img.shields.io/badge/Java-21%2B-orange) ![Status](https://img.shields.io/badge/Status-Stable-green) ![License](https://img.shields.io/badge/License-MIT-blue)

---

## ✨ Funcionalidades Principais

### 🎧 Reprodução & Áudio
*   **Efeito Slowed + Reverb:** Controle de velocidade (Pitch/Speed) em tempo real.
*   **Visualizer:** Barras de espectro de áudio sincronizadas com a música.
*   **Shuffle Inteligente:** Algoritmo que não repete músicas até que todas da lista tenham tocado.
*   **Loop de 3 Estados:** Sem repetição, Repetir Playlist, Repetir Uma Música (🔂).

### 📂 Gerenciamento de Biblioteca
*   **Playlists Ilimitadas:** Crie, renomeie e exclua suas playlists.
*   **Drag & Drop:** Arraste músicas para reordenar sua fila de reprodução facilmente.
*   **Pesquisa Avançada:** Filtre por Título, Artista ou Duração.
*   **Metadados:** Leitura automática de Capa do Álbum, Artista e Título (ID3 Tags).

### 🎨 Interface & Performance
*   **Temas:** Alterne entre **Modo Escuro** (Dark) e **Modo Claro** (Light).
*   **Modo Batata 🥔:** Tem um PC mais modesto? Ative este modo para desligar animações e visualizadores, economizando RAM e CPU.
*   **Drawer Lateral:** Visualize e edite a fila de reprodução sem sair da tela principal.
*   **Design Responsivo:** Interface fluida baseada no tema *Primer* (GitHub/Win11 style).

---

## ⌨️ Teclas de Atalho

| Tecla | Ação |
| :--- | :--- |
| **Espaço** | Tocar / Pausar |
| **Seta Direita** | Avançar 10 segundos |
| **Seta Esquerda** | Voltar 10 segundos |
| **Seta Cima** | Aumentar Volume |
| **Seta Baixo** | Diminuir Volume |
| **M** | Mutar / Desmutar |

---

## 🛠️ Como Rodar e Compilar

### Pré-requisitos
*   **Java JDK 21** ou superior.
*   **Maven** instalado e configurado no PATH.

### 1. Rodando em modo de Desenvolvimento
Clone o repositório e execute o comando na raiz do projeto:

```bash
mvn clean javafx:run
```

### 2. Criando um Executável (.exe / Instalação)
Para distribuir o aplicativo sem exigir que o usuário tenha Java instalado:

1.  Gere o arquivo JAR único ("Fat Jar"):
    ```bash
    mvn clean package
    ```
2.  Gere o executável (necessário ter JDK 14+ instalado):
    ```bash
    jpackage --type app-image --input target --name "BlueWave" --main-jar BlueWave-1.0.0.jar --main-class com.music.Launcher --icon icon.ico --dest dist --win-dir-chooser --win-menu --win-shortcut
    ```
    *O executável estará na pasta `dist/BlueWave`.*

---

## ⚙️ Configurações Avançadas

O aplicativo cria dois arquivos na pasta de execução:
1.  `bluewave_data.json`: Salva suas playlists, volume e última música tocada.
2.  `config.json` (Interno): Define nome do app, versão e modo debug.

Se precisar resetar o app, basta deletar o arquivo `bluewave_data.json`.

---

## 🥔 O que é o "Modo Batata"?

O **Modo Batata** é uma funcionalidade de otimização. O JavaFX processa o espectro de áudio (as barrinhas dançantes) 60 vezes por segundo. Em computadores mais antigos, isso pode consumir CPU.

Ao ativar o Modo Batata nas configurações:
*   O Visualizer é desligado.
*   Listeners de animação são removidos.
*   O consumo de recursos cai drasticamente.

---

## 🤝 Contribuição

Sinta-se à vontade para fazer um **Fork** e enviar **Pull Requests**. Sugestões de melhorias no CSS ou novos algoritmos de DSP são bem-vindas!

## OBS

> Algumas funções podem não funcionar, pois estamos na versão beta. Trabalhando nisso para entregar o melhor pra você!

---

**Desenvolvido com ☕ e JavaFX.**