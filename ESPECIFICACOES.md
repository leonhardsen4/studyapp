# StudyApp — Especificações do Sistema

**Versão:** 0.5
**Tecnologias:** JavaFX 21, SQLite, Maven, Java 17+

---

## 1. Visão Geral

Aplicação desktop de estudos pessoal com sistema de arquivos hierárquico, editor Markdown, visualizador de PDFs, gerenciamento de tarefas, agenda, bloco de notas, temporizador Pomodoro, calculadora e plano de estudos. Acesso protegido por login individual — cada usuário vê apenas seus próprios dados.

---

## 2. Estrutura de Pacotes

```
com.leonhardsen.studyapp
├── model/          → Classes de dados (POJOs): Usuario, ItemArvore, Nota, PdfDocumento, Tarefa…
├── controller/     → Controllers JavaFX ligados a arquivos .fxml
├── view/           → Componentes visuais customizados (células de TreeView, etc.)
├── database/       → DatabaseManager (conexão) + classes DAO (uma por entidade)
├── service/        → Lógica de negócio desacoplada dos controllers
└── util/           → Utilitários: hash de senha, formatação de datas, constantes, SessionManager
```

**Regra de dependência:** `controller` usa `service`; `service` usa `database` e `model`; nenhuma camada inferior conhece as superiores.

---

## 3. Design Visual e Temas

### 3.1 Paleta de cores

| Papel                  | Modo Claro              | Modo Escuro              |
|------------------------|-------------------------|--------------------------|
| Fundo principal        | `#FFFFFF` (branco)      | `#1E1E2E` (cinza escuro) |
| Fundo secundário       | `#F4F4F6` (cinza claro) | `#2A2A3C` (cinza médio)  |
| Texto principal        | `#1A1A2E` (quase preto) | `#E8E8F0` (quase branco) |
| Texto secundário       | `#6B6B80` (cinza)       | `#9090A8` (cinza claro)  |
| Acento / destaque      | `#1B3A6B` (azul marinho)| `#4A78C4` (azul médio)   |
| Borda / divisor        | `#DCDCE8`               | `#3A3A50`                |
| Hover de botão         | `#EAF0FB`               | `#2E3A52`                |

### 3.2 Padrão de datas e horários

Todos os formatos de data e hora no sistema seguem o padrão brasileiro, usando `DateTimeFormatter` com `Locale.forLanguageTag("pt-BR")`:

| Dado              | Formato                         | Exemplo                    |
|-------------------|---------------------------------|----------------------------|
| Hora              | `HH:mm:ss`                      | `14:32:05`                 |
| Data curta        | `dd/MM/yyyy`                    | `21/06/2026`               |
| Data longa        | `EEEE, dd/MM/yyyy`              | `Sábado, 21/06/2026`       |
| Data + hora       | `dd/MM/yyyy HH:mm`              | `21/06/2026 14:32`         |

A classe `util.FormatadorData` centraliza todos os formatadores e é usada por todo o sistema.

### 3.3 Tipografia e estilo geral
- Fonte: padrão do sistema (sem dependência de fonte externa).
- Bordas arredondadas nos campos de input, botões e cards (`-fx-background-radius: 6`).
- Sem sombras excessivas — visual flat e limpo.
- Ícones: caracteres Unicode / emoji simples ou imagens SVG em `resources`.

### 3.4 Implementação dos temas
- Dois arquivos CSS em `resources/com/leonhardsen/studyapp/css/`:
  - `light-theme.css`
  - `dark-theme.css`
- O `Scene` carrega um arquivo por vez; troca de tema = remover o CSS atual e adicionar o outro.
- A preferência de tema é salva no banco por usuário (coluna `tema` na tabela `usuario`).
- A `util.ThemeManager` centraliza a lógica de troca e mantém referência à `Scene` ativa.

---

## 4. Janela Principal

### 4.1 Comportamento da janela
- Abre **maximizada** por padrão (`stage.setMaximized(true)`).
- É **redimensionável** pelo usuário.
- **Tamanho mínimo:** metade da largura da tela × metade da altura da tela, calculados em tempo de execução:
  ```java
  Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
  stage.setMinWidth(bounds.getWidth() / 2);
  stage.setMinHeight(bounds.getHeight() / 2);
  ```
- Todos os painéis usam `HBox`/`VBox`/`SplitPane` com `Hgrow`/`Vgrow` configurados, garantindo que o layout se ajuste ao redimensionamento.

### 4.2 Layout geral

```
┌─────────────────────────────────────────────────────────────────────┐
│  StudyApp       14:32:15  •  Sábado, 21/06/2026  •  João Silva  [⚙]│ ← Topbar
├──────────────────┬──────────────────────────────────────────────────┤
│ [+Caderno][+Nota]│                                                  │
│ [+PDF     ]      │                                                  │
│ ──────────────── │   Área de conteúdo                               │
│ ▼ 📁 Caderno A   │   (vazio / Editor Markdown / Visualizador PDF)   │
│   ├ 📄 Nota 1    │                                                  │
│   ├ 📄 Nota 2    │                                                  │
│   └ 📑 PDF 1     │                                                  │
│ ▶ 📁 Caderno B   │                                                  │
│                  │                                                  │
└──────────────────┴──────────────────────────────────────────────────┘
```

- **Topbar** (`topbar-view.fxml`): `HBox` fixo no topo, altura constante (~48px).
- **Painel de arquivos** (esquerda): `VBox` com botões de ação + `TreeView`.
- **Área de conteúdo** (direita): `StackPane` que alterna entre três painéis.
- Divisão esquerda/direita via `SplitPane` — o usuário pode arrastar o divisor.

### 4.3 Topbar — Barra de Status e Configurações

**Conteúdo da topbar (esquerda → direita):**

| Elemento           | Descrição                                                      |
|--------------------|----------------------------------------------------------------|
| Nome do app        | Texto "StudyApp" em negrito, alinhado à esquerda               |
| Espaço flexível    | `HBox.setHgrow(spacer, Priority.ALWAYS)`                       |
| Hora atual         | `Label` atualizado a cada segundo via `Timeline` (formato `HH:mm:ss`) |
| Separador `•`      | Texto estático                                                 |
| Data atual         | `Label` com dia da semana + data (ex.: "Sábado, 21/06/2026")   |
| Separador `•`      | Texto estático                                                 |
| Nome do usuário    | `Label` com o nome do usuário logado (da sessão em memória)    |
| Botão ⚙           | Abre o menu de configurações (ver abaixo)                      |

**Atualização do relógio:** `Timeline` com `KeyFrame` de 1 segundo, iniciado ao abrir a tela principal e parado ao fechá-la.

### 4.4 Menu de Configurações (botão ⚙)

Abre um `ContextMenu` ancorado no botão, com as opções:

| Opção                         | Ação                                                              |
|-------------------------------|-------------------------------------------------------------------|
| Perfil                        | Abre a tela de perfil do usuário (ver seção 4.5)                  |
| Modo Claro / Modo Escuro      | Alterna o tema e salva a preferência no banco                     |
| Encerrar Sessão               | Limpa a sessão em memória e navega de volta para `login-view.fxml`|
| Sair do Aplicativo            | Exibe diálogo de confirmação e chama `Platform.exit()`            |

### 4.5 Tela de Perfil

Aberta a partir do menu de configurações. Exibida como um `Stage` modal separado (`profile-view.fxml`), não como diálogo — permite edição confortável.

**Seção: Dados pessoais**
- Campo e-mail (pré-preenchido com o valor atual, editável).
- Botão "Salvar alterações de e-mail": confirma a senha atual antes de persistir. Valida unicidade do novo e-mail no banco.

**Seção: Senha**
- Campo "Senha atual" (obrigatório para qualquer alteração).
- Campo "Nova senha".
- Campo "Confirmar nova senha".
- Botão "Alterar senha": valida os campos e persiste o novo hash BCrypt.

**Seção: Zona de perigo**
- Botão "Excluir minha conta" (estilizado em vermelho discreto).
- Ao clicar: diálogo de confirmação exige que o usuário digite sua senha para autorizar.
- Ao confirmar: exclui em cascata todos os dados do usuário (itens da árvore, notas, PDFs do disco, eventos, tarefas) e retorna para a tela de login.

---

## 5. Autenticação

### 5.1 Telas
- **Login** (`login-view.fxml`) — e-mail + senha, botão entrar, link para cadastro, link "Esqueceu a senha?".
- **Cadastro** (`register-view.fxml`) — nome, e-mail, senha, confirmar senha.
- Ambas as telas seguem a mesma paleta e ficam centralizadas na tela (janela não maximizada no login, tamanho fixo ~480×360px).

### 5.2 Recuperação de Senha por E-mail

**Fluxo:**
1. Na tela de login, o usuário clica em "Esqueceu a senha?".
2. Um pequeno diálogo solicita o **e-mail cadastrado**.
3. O sistema verifica se o e-mail existe no banco. Se não existir, exibe mensagem genérica ("Se o e-mail estiver cadastrado, uma nova senha será enviada") — sem revelar se o e-mail existe ou não.
4. Se existir: gera uma senha temporária aleatória de 12 caracteres (letras + números), faz o hash BCrypt e atualiza o banco.
5. Envia a senha temporária para o e-mail do usuário via SMTP.
6. O usuário recebe o e-mail, faz login com a senha temporária e, em seguida, deve alterá-la na tela de Perfil.

**Envio de e-mail:**
- Biblioteca: `jakarta.mail` (Jakarta Mail / JavaMail).
- Configuração SMTP em arquivo `~/.studyapp/mail.properties` (criado na primeira execução se não existir, com valores padrão comentados).
- Campos de configuração: `smtp.host`, `smtp.port`, `smtp.user`, `smtp.password`, `smtp.starttls` (boolean).
- A classe `util.EmailService` encapsula o envio. Se o envio falhar (SMTP não configurado, sem conexão), uma mensagem clara é exibida ao usuário.

**Nota:** por ser uma aplicação desktop pessoal, a configuração do SMTP é responsabilidade do usuário (ex.: usar Gmail com senha de aplicativo).

### 5.3 Banco de dados
```sql
CREATE TABLE usuario (
    id         INTEGER PRIMARY KEY AUTOINCREMENT,
    nome       TEXT    NOT NULL,
    email      TEXT    NOT NULL UNIQUE,
    senha_hash TEXT    NOT NULL,   -- BCrypt
    tema       TEXT    NOT NULL DEFAULT 'CLARO',  -- 'CLARO' ou 'ESCURO'
    criado_em  TEXT    NOT NULL DEFAULT (datetime('now'))
);
```

### 5.4 Sessão
- `util.SessionManager` — singleton em memória que guarda o objeto `Usuario` logado.
- Encerrar sessão = `SessionManager.logout()` + carregar `login-view.fxml` no `Stage` atual.
- Nenhuma informação de sessão é gravada em disco.

### 5.5 Segurança
- Senhas armazenadas com **BCrypt** (`at.favre.lib:bcrypt`).
- Todos os `SELECT` de dados (itens, notas, PDFs) incluem `WHERE usuario_id = ?` — isolamento total entre usuários garantido na camada de banco.

---

## 6. Sistema de Arquivos (foco principal)

### 6.1 Tipos de itens

| Tipo       | Ícone | Pode conter           |
|------------|-------|-----------------------|
| `CADERNO`  | 📁    | Cadernos, Notas, PDFs |
| `NOTA`     | 📄    | (folha) — sem filhos  |
| `PDF`      | 📑    | (folha) — sem filhos  |

**Regras de organização:**
- Notas e PDFs **nunca ficam soltos** na raiz — todo arquivo pertence a um caderno.
- Cadernos podem ser aninhados em outros cadernos sem limite de profundidade.
- **Na primeira execução** (após o cadastro), um caderno padrão "Meus Cadernos" é criado automaticamente pelo `UsuarioService`.
- Para criar nota ou PDF, deve existir pelo menos um caderno; caso contrário, o sistema solicita criar um antes de prosseguir.

### 6.2 Banco de dados

```sql
-- Árvore genérica (adjacency list)
CREATE TABLE item_arvore (
    id            INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id    INTEGER NOT NULL REFERENCES usuario(id),
    pai_id        INTEGER REFERENCES item_arvore(id) ON DELETE CASCADE,
    nome          TEXT    NOT NULL,
    tipo          TEXT    NOT NULL CHECK(tipo IN ('CADERNO','NOTA','PDF')),
    posicao       INTEGER NOT NULL DEFAULT 0,   -- ordem entre irmãos
    criado_em     TEXT    NOT NULL DEFAULT (datetime('now')),
    atualizado_em TEXT    NOT NULL DEFAULT (datetime('now'))
);

-- Conteúdo Markdown (carregado apenas ao abrir a nota)
CREATE TABLE nota (
    id       INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id  INTEGER NOT NULL UNIQUE REFERENCES item_arvore(id) ON DELETE CASCADE,
    conteudo TEXT    NOT NULL DEFAULT ''
);

-- Referência ao arquivo PDF em disco
CREATE TABLE pdf_documento (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id         INTEGER NOT NULL UNIQUE REFERENCES item_arvore(id) ON DELETE CASCADE,
    caminho_arquivo TEXT    NOT NULL,
    tamanho_bytes   INTEGER
);
```

**Armazenamento de PDFs:** copiados para `~/.studyapp/pdfs/<usuario_id>/` no upload. O banco guarda apenas o caminho absoluto.

### 6.3 Operações disponíveis

#### Botões na barra do painel de arquivos
| Botão      | Ação                                                                |
|------------|---------------------------------------------------------------------|
| + Caderno  | Cria caderno no item selecionado (ou na raiz se nada selecionado)   |
| + Nota     | Abre diálogo de criação (requer caderno selecionado ou existente)   |
| + PDF      | Abre file chooser, copia o arquivo e insere no caderno selecionado  |

#### Menu de contexto (clique direito)
| Opção             | Disponível para        |
|-------------------|------------------------|
| Renomear          | Caderno, Nota, PDF     |
| Excluir           | Caderno, Nota, PDF     |
| Novo caderno aqui | Caderno                |
| Nova nota aqui    | Caderno                |
| Adicionar PDF     | Caderno                |
| Mover para…       | Caderno, Nota, PDF     |

#### Mover itens
- **Arrastar e soltar** (drag & drop) dentro do `TreeView`.
- **"Mover para…"** abre diálogo com `TreeView` mostrando apenas cadernos disponíveis como destino.
- Ao mover um caderno, todos os filhos são movidos junto (apenas `pai_id` do caderno raiz muda; filhos não são tocados).

### 6.4 Editor de Notas (Markdown)

**Fluxo de criação:**
1. "+ Nota" com caderno selecionado → diálogo solicita **nome** e confirma o **caderno de destino**.
2. Ao confirmar, o item é inserido na árvore e salvo no banco imediatamente (nota existe, mesmo vazia).
3. A área de conteúdo abre o editor pronto para digitação.

**Modos (botão toggle no canto superior direito da área de conteúdo):**
- **Editar** — `TextArea` para digitação em Markdown.
- **Visualizar** — `WebView` com HTML gerado a partir do Markdown (biblioteca Flexmark).
- Botão visível apenas quando uma nota está aberta.

**Barra de ferramentas do editor:**

Uma barra de botões acima do `TextArea`, visível somente no modo Editar, com atalhos para inserção de elementos:

| Botao       | O que insere no cursor                                              |
|-------------|---------------------------------------------------------------------|
| Negrito     | `**texto**`                                                         |
| Italico     | `*texto*`                                                           |
| H1 / H2 / H3 | `# `, `## `, `### ` no inicio da linha                            |
| Codigo      | Bloco de codigo ` ``` `                                             |
| Link        | `[texto](url)`                                                      |
| Imagem      | Abre file chooser, copia a imagem para disco e insere `![alt](caminho)` |
| Tabela      | Abre dialogo (linhas x colunas) e insere tabela Markdown GFM       |
| Formula     | Insere `$...$` (inline) ou `$$...$$` (bloco) para formulas LaTeX   |
| Lista       | `- item` (lista nao ordenada)                                       |
| Lista num.  | `1. item` (lista ordenada)                                          |

**Suporte a imagens:**
- File chooser aceita `.png`, `.jpg`, `.jpeg`, `.gif`, `.webp`.
- O arquivo e copiado para `~/.studyapp/images/<usuario_id>/` com nome UUID.
- O Markdown armazena o caminho absoluto: `![alt](file:///caminho/imagem.png)`.
- O `WebView` exibe as imagens normalmente via URL `file://`.
- Nao ha tabela separada no banco para imagens — o vinculo e implicito pelo caminho no conteudo.

**Suporte a tabelas:**
- Dialogo solicita numero de colunas e linhas e gera a sintaxe GFM.
- Flexmark renderiza com `TablesExtension`.

**Suporte a formulas matematicas:**
- Delimitadores: `$formula$` (inline) e `$$formula$$` (bloco), padrao LaTeX/KaTeX.
- O HTML gerado para o `WebView` inclui **KaTeX** carregado de arquivos locais em `resources` — sem dependencia de internet.
- Flexmark preserva os delimitadores; o KaTeX faz o render no browser embutido.

**Auto-save:**
- Cada alteracao no `TextArea` reinicia um `PauseTransition` de **1,5 segundo**.
- Ao disparar, o conteudo e salvo em `Task<Void>` (fora da UI thread).
- Indicador discreto no rodape da area de conteudo: "Salvo" ou "Salvando...".

Biblioteca de renderizacao Markdown: **`com.vladsch.flexmark:flexmark-all`**

### 6.5 Visualizador de PDF

- PDFBox renderiza cada página como `BufferedImage` → `ImageView` em `ScrollPane` vertical.
- Controles: `◀ Anterior` / `▶ Próxima`, campo de número de página, botões de zoom (+/−).
- Renderização sob demanda: apenas a página visível é processada para evitar lentidão em arquivos grandes.

---

## 7. Demais Funcionalidades (visão geral — detalhamento posterior)

### 7.1 Gerenciador de Tarefas
- Título, descrição, prazo (data + hora), prioridade (Alta / Média / Baixa).
- Status: Pendente → Em andamento → Concluída.
- Alertas visuais quando o prazo se aproxima ou vence.

### 7.2 Agenda
- Visualização mensal (grade) e diária (lista).
- Eventos com título, data, hora início/fim e descrição.

### 7.3 Módulo Pomodoro (detalhamento completo)

#### 7.3.1 Visão geral

Combina o temporizador Pomodoro clássico com um sistema de **metas de estudo** organizado em Disciplina → Assunto. O progresso do assunto é vinculado às sessões de foco completadas pelo timer.

#### 7.3.2 Hierarquia de metas de estudo

**Disciplina**
- Representa uma área de conhecimento (ex.: "Matemática", "Programação").
- Cada usuário pode ter quantas disciplinas quiser.
- Nomes únicos por usuário (`UNIQUE(usuario_id, nome)`).

**Assunto**
- Pertence a uma disciplina.
- Campos: nome, nível de dificuldade, número mínimo de sessões, sessões realizadas, status, data limite (opcional).
- **`TipoDificuldade`** (enum):
  | Valor           | Label           | Sessões sugeridas |
  |-----------------|-----------------|-------------------|
  | `FACIL`         | Fácil           | 2                 |
  | `MEDIO`         | Médio           | 4                 |
  | `DIFICIL`       | Difícil         | 6                 |
  | `MUITO_DIFICIL` | Muito Difícil   | 8                 |
  - Ao criar um assunto, o campo "sessões mínimas" é pré-preenchido com o valor sugerido pela dificuldade, mas pode ser ajustado manualmente.
- **`TipoStatusAssunto`** (enum): `PENDENTE` → `EM_ANDAMENTO` → `CONCLUIDO`
  - `PENDENTE`: nenhuma sessão realizada.
  - `EM_ANDAMENTO`: ao menos 1 sessão concluída.
  - `CONCLUIDO`: marcado manualmente pelo usuário (não automático ao atingir o mínimo — o sistema oferece a opção em diálogo, mas não força a conclusão).
  - Se sessões forem reduzidas abaixo do mínimo, `CONCLUIDO` é rebaixado automaticamente para `EM_ANDAMENTO`.

#### 7.3.3 Timer Pomodoro

**Fases:**
| Fase          | Duração padrão | Configurável |
|---------------|----------------|--------------|
| Foco          | 25 min         | Sim          |
| Pausa curta   | 5 min          | Sim          |
| Pausa longa   | 15 min         | Sim          |

**Ciclo:** 4 sessões de foco → pausa longa; sessões intermediárias → pausa curta.  
Indicadores visuais dos slots do ciclo (🍅 preenchido / vazio).

**Comportamento:**
- Ao fim de cada fase, o timer toca um alarme sonoro (beep do sistema via `java.awt.Toolkit`) e avança automaticamente para a próxima fase já iniciada.
- O usuário pode pausar, retomar (▶/⏸), reiniciar (↺) ou pular (⏭) a fase atual a qualquer momento.
- Ao fim de uma sessão de **foco**, a sessão é registrada no banco e o contador do assunto selecionado é incrementado.

**Configuração:**
- Botão "⚙ Configurar" abre diálogo com `Spinner` para cada uma das três durações.
- Preferências persistidas em `~/.studyapp/pomodoro.properties` entre sessões.

#### 7.3.4 Painel esquerdo (metas)

Construído programaticamente (`VBox listaDisciplinas`) com colunas expansíveis:
- Cabeçalho de disciplina clicável (expande/colapsa a lista de assuntos).
- Cada linha de assunto exibe: ícone de status, nome, indicador de data limite, contagem "X/Y 🍅", botões `[−]` `[+]` `[▶]` `[⋯]`.
  - `[−]` / `[+]`: ajusta manualmente o contador de sessões do assunto.
  - `[▶]`: seleciona o assunto para vinculação com o timer (destaca a linha em azul).
  - `[⋯]`: abre menu de contexto com opções Editar, Marcar Concluído / Reabrir, Excluir.
- Rodapé exibe estatísticas do dia: sessões de foco e tempo total de foco acumulados.

#### 7.3.5 Layout do módulo

```
┌──────────────────────────────────────────────────────────┐
│  🍅 Pomodoro                              [⚙ Configurar] │ ← module-toolbar
├─────────────────────┬────────────────────────────────────┤
│ [+ Nova Disciplina] │                                    │
│ ─────────────────── │      [Foco] [Pausa Curta] [Pausa]  │
│ ▼ Matemática        │                                    │
│   ○ Álgebra  2/4🍅  │        Estudando: Álgebra          │
│   ● Geometria 0/2🍅 │                                    │
│ ▶ Programação       │           25:00                    │
│                     │                                    │
│                     │      🍅 🍅 ○ ○  (ciclo atual)      │
│                     │                                    │
│ ─────────────────── │   [↺]  [▶ Iniciar]  [⏭]           │
│ Hoje: 3🍅  75 min   │                                    │
└─────────────────────┴────────────────────────────────────┘
```

- Layout `BorderPane` externo (`container`) + `BorderPane` interno (`raiz`) + `SplitPane` dividerPositions=0.30.
- Painel esquerdo: `VBox` + `ScrollPane` (lista de disciplinas/assuntos).
- Painel direito: timer centralizado com botões de controle.
- **Botão "↗ Destacar"** na toolbar: move `raiz` (toolbar + SplitPane) para uma `Stage` separada sempre no topo; `container` exibe placeholder. O botão passa a exibir "↙ Reintegrar" na janela destacada. Fechar a janela ou clicar em "↙ Reintegrar" devolve tudo ao painel principal — padrão idêntico ao da Calculadora e do Bloco de Notas.

#### 7.3.6 Banco de dados

```sql
CREATE TABLE disciplina (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id  INTEGER NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    nome        TEXT    NOT NULL,
    criado_em   TEXT    NOT NULL DEFAULT (datetime('now')),
    UNIQUE(usuario_id, nome)
);

CREATE TABLE assunto (
    id                INTEGER PRIMARY KEY AUTOINCREMENT,
    disciplina_id     INTEGER NOT NULL REFERENCES disciplina(id) ON DELETE CASCADE,
    nome              TEXT    NOT NULL,
    dificuldade       TEXT    NOT NULL CHECK(dificuldade IN ('FACIL','MEDIO','DIFICIL','MUITO_DIFICIL')),
    sessoes_minimas   INTEGER NOT NULL,
    sessoes_realizadas INTEGER NOT NULL DEFAULT 0,
    status            TEXT    NOT NULL DEFAULT 'PENDENTE' CHECK(status IN ('PENDENTE','EM_ANDAMENTO','CONCLUIDO')),
    data_limite       TEXT,   -- YYYY-MM-DD, nullable
    criado_em         TEXT    NOT NULL DEFAULT (datetime('now')),
    atualizado_em     TEXT    NOT NULL DEFAULT (datetime('now'))
);

CREATE TABLE sessao_pomodoro (
    id               INTEGER PRIMARY KEY AUTOINCREMENT,
    usuario_id       INTEGER NOT NULL REFERENCES usuario(id),
    assunto_id       INTEGER REFERENCES assunto(id) ON DELETE SET NULL,
    tipo             TEXT    NOT NULL CHECK(tipo IN ('FOCO','PAUSA_CURTA','PAUSA_LONGA')),
    iniciado_em      TEXT    NOT NULL,
    concluido_em     TEXT    NOT NULL,
    duracao_segundos INTEGER NOT NULL
);
```

#### 7.3.7 Classes principais

| Classe                      | Tipo       | Responsabilidade                                                       |
|-----------------------------|------------|------------------------------------------------------------------------|
| `model/Disciplina`          | Model      | POJO — id, usuarioId, nome, criadoEm                                   |
| `model/Assunto`             | Model      | POJO — todos os campos do assunto + status + dataLimite                |
| `model/SessaoPomodoro`      | Model      | POJO — registro de uma sessão concluída                                |
| `model/TipoDificuldade`     | Enum       | FACIL/MEDIO/DIFICIL/MUITO_DIFICIL com label e sessõesDefault           |
| `model/TipoStatusAssunto`   | Enum       | PENDENTE/EM_ANDAMENTO/CONCLUIDO com label                              |
| `model/TipoSessao`          | Enum       | FOCO/PAUSA_CURTA/PAUSA_LONGA com label                                 |
| `database/DisciplinaDAO`    | DAO        | CRUD + contarAssuntos                                                  |
| `database/AssuntoDAO`       | DAO        | CRUD + buscarPorId + buscarPorDisciplina                               |
| `database/SessaoPomodoroDAO`| DAO        | registrar + contarSessoesHoje + somarDuracaoHoje                       |
| `service/PomodoroService`   | Service    | Lógica de negócio; coordena DAOs; regras de status do assunto          |
| `controller/PomodoroController` | Controller | Timer (`Timeline` 1s), painel esquerdo, navegação, persistência de configurações |

#### 7.3.8 CSS

Estilos próprios do módulo em ambos os temas:
- `.pomo-panel-esq`, `.pomo-disciplina-*`, `.pomo-assunto-item`, `.pomo-assunto-selecionado`
- `.pomo-timer` (fonte 72px monospace), `.pomo-timer-pausa` (verde durante pausas)
- `.pomo-btn-principal`, `.pomo-btn-pausar`, `.pomo-fase-btn`, `.pomo-fase-btn-ativo`
- `.pomo-ciclo-cheio` (vermelho tomate), `.pomo-ciclo-vazio`
- `.pomo-status-pendente`, `.pomo-status-em_andamento`, `.pomo-status-concluido`
- `.pomodoro-stats` (rodapé de estatísticas)

### 7.4 Bloco de Notas Rápido
- `TextArea` simples, sem formatação.
- Auto-save contínuo vinculado ao usuário logado.

### 7.5 Calculadora
- Operações: +, −, ×, ÷, %, inversão de sinal.
- Histórico da sessão atual.

### 7.6 Plano de Estudos
- Hierarquia Disciplina → Assunto com progresso de sessões Pomodoro.
- Sidebar com cards de disciplina (barra de progresso % assuntos concluídos, total de tempo de foco).
- Painel direito com cards de assunto: chips de status/dificuldade, data limite com alertas visuais, barra de sessões (realizadas/mínimas), botão "▶ Estudar agora".
- Integração direta com o Pomodoro: `PomodoroController.selecionarAssuntoExterno()` pré-seleciona o assunto ao clicar em "Estudar agora".
- Sem novas tabelas — reutiliza `disciplina`, `assunto` e `sessao_pomodoro`.

---

## 8. Banco de Dados — Visão Consolidada

```
usuario
item_arvore      (→ usuario)
nota             (→ item_arvore)
pdf_documento    (→ item_arvore)
etiqueta         (→ usuario)
tarefa           (→ usuario)
tarefa_etiqueta  (→ tarefa, etiqueta)
evento           (→ usuario)
bloco_notas      (→ usuario)     -- uma linha por usuário
disciplina       (→ usuario)
assunto          (→ disciplina)
sessao_pomodoro  (→ usuario, assunto)
```

Arquivo: `~/.studyapp/studyapp.db`

`DatabaseManager` executa `CREATE TABLE IF NOT EXISTS …` ao iniciar — sem ferramenta de migração externa.

---

## 9. Dependências Maven (a adicionar ao pom.xml)

| Dependencia                                   | Uso                               |
|-----------------------------------------------|-----------------------------------|
| `org.xerial:sqlite-jdbc:3.47.1`               | Driver JDBC para SQLite           |
| `at.favre.lib:bcrypt:0.10.2`                  | Hash de senha (BCrypt)            |
| `com.vladsch.flexmark:flexmark-all:0.64.8`    | Markdown para HTML                |
| `org.apache.pdfbox:pdfbox:3.0.3`              | Renderizacao de PDFs              |
| `com.sun.mail:jakarta.mail:2.0.1`             | Envio de e-mail via SMTP          |
| `javafx-web` (OpenJFX 21)                     | WebView para preview Markdown     |

KaTeX sera empacotado como recursos estaticos em `resources/.../katex/` (JS + CSS), sem dependencia Maven.

---

## 10. Convencao de Documentacao (Javadoc)

**Obrigatorio em todas as classes e metodos publicos e protegidos.**

- Idioma: **portugues brasileiro**.
- **Classes:** descricao do que a classe representa ou faz, anotacao `@author` e `@version`.
- **Metodos:** descricao clara do proposito, `@param` para cada parametro, `@return` quando houver retorno, `@throws` para excecoes declaradas.
- Metodos privados simples (getters/setters gerados) podem omitir Javadoc.

**Exemplo de padrao esperado:**

```java
/**
 * Servico responsavel pela autenticacao de usuarios no sistema.
 * Realiza validacao de credenciais, gerenciamento de sessao
 * e comunicacao com o banco de dados via {@link UsuarioDAO}.
 *
 * @author StudyApp
 * @version 1.0
 */
public class AutenticacaoService {

    /**
     * Autentica um usuario com base no e-mail e senha fornecidos.
     * Verifica o hash BCrypt da senha antes de autorizar o acesso.
     *
     * @param email e-mail do usuario cadastrado
     * @param senha senha em texto puro fornecida pelo usuario
     * @return o objeto {@link Usuario} autenticado, ou {@code null} se as credenciais forem invalidas
     * @throws SQLException se ocorrer erro de acesso ao banco de dados
     */
    public Usuario autenticar(String email, String senha) throws SQLException { ... }
}
```

---

## 11. Pontos de Atencao

1. **Java Version:** migrar `pom.xml` de Java 9 para **Java 17** (LTS).
2. **`module-info.java`:** declarar modulos SQLite, BCrypt, PDFBox, Jakarta Mail e abrir pacotes ao JavaFX.
3. **Thread safety:** banco de dados roda em `Task<>`; UI so e atualizada no `Platform.runLater()`.
4. **Responsividade:** todos os paineis com `HGrow`/`VGrow = ALWAYS`; evitar tamanhos fixos em pixels.
5. **Tamanho minimo da janela:** lido do `Screen.getPrimary()` em tempo de execucao.
6. **Auto-save com debounce:** `PauseTransition` reiniciado a cada keystroke; o save efetivo ocorre fora da UI thread.
7. **Troca de tema em runtime:** `ThemeManager` remove o CSS atual e adiciona o novo na `Scene`; preferencia persistida na coluna `tema` da tabela `usuario`.
8. **Isolamento de dados:** todo DAO recebe `usuarioId` como parametro; nenhuma query retorna dados sem filtrar por usuario.
9. **Exclusao de caderno:** confirmar informando quantos itens filhos serao removidos.
10. **Caderno padrao:** criado pelo `UsuarioService` logo apos o primeiro cadastro.
11. **SMTP nao configurado:** `EmailService` captura a excecao e exibe mensagem amigavel — nunca deixa a tela travar.
12. **Limpeza de imagens orfas:** ao excluir uma nota, as imagens referenciadas em seu conteudo devem ser removidas do disco. O `NoteService` extrai os caminhos `file://` do Markdown antes de deletar a nota.
13. **KaTeX offline:** os arquivos do KaTeX (katex.min.js, katex.min.css, fonts/) devem ser copiados para `resources` e carregados via `getClass().getResource()` no template HTML.
