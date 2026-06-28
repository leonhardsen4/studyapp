# StudyApp — Documento de Progresso

**Projeto:** Aplicação desktop de estudos pessoal  
**Stack:** JavaFX 21 · SQLite (sqlite-jdbc) · BCrypt · Flexmark · PDFBox · Jakarta Mail · Java 17  
**Banco de dados:** `~/.studyapp/studyapp.db`  
**Especificações detalhadas:** `ESPECIFICACOES.md`

---

## Módulos planejados

| Módulo                    | Status        |
|---------------------------|---------------|
| Autenticação              | ✅ Concluído  |
| Sistema de arquivos       | ✅ Concluído  |
| Gerenciador de Tarefas    | ✅ Concluído  |
| Agenda                    | ✅ Concluído  |
| Temporizador Pomodoro     | ✅ Concluído  |
| Bloco de notas rápido     | ✅ Concluído  |
| Calculadora               | ✅ Concluído  |
| Plano de Estudos          | ✅ Concluído  |
| Dashboard                 | ✅ Concluído  |

---

## ✅ Módulo 1 — Autenticação

**Status:** Concluído

### O que foi implementado
- Tela de login (`login-view.fxml` / `LoginController`) com e-mail e senha
- Tela de cadastro (`cadastro-view.fxml` / `CadastroController`) com validações locais
- Tela de perfil (`perfil-view.fxml` / `PerfilController`) — janela modal separada
  - Editar e-mail (confirma senha atual, valida unicidade)
  - Alterar senha (confirma senha atual)
  - Excluir conta (confirma senha, cascata nos dados)
- Recuperação de senha por e-mail via SMTP (`EmailService`)
- `SessionManager` (singleton em memória — sem persistência em disco)
- Senhas armazenadas com BCrypt cost 12 (`HashUtil`)
- E-mails normalizados para minúsculas no cadastro e no login
- Tema (CLARO/ESCURO) salvo por usuário no banco e aplicado no login

### Bugs corrigidos
- **[jun/2026]** `handleEsqueceuSenha()` atualizava a senha no banco *antes* de tentar enviar o e-mail — se o SMTP não estivesse configurado, o usuário ficava travado sem saber a nova senha temporária. **Correção:** o banco só é atualizado se o envio do e-mail não lançar exceção.

---

## ✅ Módulo 2 — Sistema de Arquivos

**Status:** Concluído

### O que foi implementado
- Três tipos de item: `CADERNO` (pasta), `NOTA` (Markdown), `PDF`
- Hierarquia ilimitada de cadernos aninhados (`item_arvore` com adjacency list)
- Caderno padrão "Meus Cadernos" criado automaticamente no primeiro cadastro
- TreeView com células customizadas (`ItemArvoreCell`) e ícones Unicode
- Operações via botões na barra, menu de contexto (clique direito) e atalhos de teclado:
  - Criar caderno, nota e PDF
  - Renomear, excluir (com contagem de filhos), mover para outro caderno
  - Reordenar (↑ ↓) via botões, menu de contexto e Ctrl+↑ / Ctrl+↓
  - Excluir via botão e tecla Delete
- Editor Markdown com barra de ferramentas (negrito, itálico, H1/H2/H3, código, link, imagem, tabela, lista, fórmula LaTeX)
- Toggle Editar / Visualizar (WebView com Flexmark + KaTeX offline)
- Auto-save com debounce de 1,5s (PauseTransition + background thread)
- Imagens copiadas para `~/.studyapp/images/<usuario_id>/` e convertidas para data-URI base64 no preview
- Visualizador de PDF (PDFBox): navegação por páginas, zoom, renderização sob demanda
- Impressão: notas → HTML para browser; PDFs → abre no visualizador do sistema
- Isolamento por usuário em todas as queries
- **Cabeçalho do módulo** com barra padronizada (`.module-toolbar` / `.module-titulo`) — estilo unificado com todos os outros módulos

### Banco de dados
```
item_arvore  (id, usuario_id, pai_id, nome, tipo, posicao, criado_em, atualizado_em)
nota         (id, item_id, conteudo)
pdf_documento (id, item_id, caminho_arquivo, tamanho_bytes)
```

---

## ✅ Módulo 3 — Gerenciador de Tarefas

**Status:** Concluído

### O que foi implementado
- **Model:** `Tarefa` (titulo, anotacoes, prioridade, status, data_vencimento, etiquetas), `Etiqueta`
- **Enums:** `TipoPrioridade` (BAIXA / MEDIA / ALTA / URGENTE), `TipoStatus` (PENDENTE / EM_ANDAMENTO / CONCLUIDA)
- **DAO:** `TarefaDAO` (inserir, atualizar, excluir, buscarTodas, vincular/desvincular etiquetas, busca com alerta de prazo), `EtiquetaDAO` (CRUD completo)
- **Service:** `TarefaService` — validações, gerenciamento de etiquetas por tarefa (min 1, max 3), exclusão em cascata de tarefas que pertencem somente a uma etiqueta excluída
- **UI:** `TarefasController` + `tarefas-view.fxml` com:
  - Cabeçalho do módulo com barra padronizada (`.module-toolbar`)
  - Painel lateral de etiquetas (criar, renomear, excluir com aviso de cascata)
  - Tabela de tarefas com colunas: checkbox (concluir), Status, Prioridade (colorida), Título, Etiquetas, Vencimento, Criado em, Atualizado
  - **Coluna de checkbox** para marcar/desmarcar tarefas como concluídas diretamente na tabela
  - **Ordenação e ocultação de colunas** habilitadas via botão ☰ no cabeçalho da tabela (`tableMenuButtonVisible="true"`)
  - Coloração de linhas por prazo: vermelho (vencida), amarelo (hoje/amanhã), laranja (em breve)
  - Filtros: busca por texto, por status, por etiqueta — em tempo real
  - Diálogo criar/editar tarefa (título, anotações, prioridade, status, data, 3 ComboBoxes de etiqueta)
  - Atalhos: Enter para editar, Delete para excluir
- **Alertas de prazo** na abertura da tela principal: exibe aviso com tarefas vencidas / vencem hoje / vencem em 3 dias
- **Callback `onTarefaAlterada`:** ao criar, editar, excluir ou marcar concluída qualquer tarefa, o sistema de notificações é atualizado imediatamente (badge do sino recalculado)
- **Navegação:** botão "✓ Tarefas" na topbar com carregamento lazy (FXML carregado só na primeira vez)
- **CSS:** classes de estilo para prioridade, prazo, lista de etiquetas e cabeçalho de tabela presentes em ambos os temas (claro e escuro)

### Banco de dados
```
etiqueta      (id, usuario_id, nome, criado_em) — UNIQUE(usuario_id, nome)
tarefa        (id, usuario_id, titulo, anotacoes, prioridade, status, data_vencimento, criado_em, atualizado_em)
tarefa_etiqueta (tarefa_id, etiqueta_id) — chave primária composta, CASCADE
```

---

## ✅ Módulo 4 — Agenda

**Status:** Concluído

### O que foi implementado
- **Model:** `Evento` (titulo, descricao, data, hora_inicio, hora_fim — nullable → evento de dia inteiro)
- **DAO:** `EventoDAO` (inserir, atualizar, excluir, buscarPorMes, buscarHoje)
- **Service:** `EventoService` — validações (título obrigatório, data obrigatória, coerência de horários)
- **UI:** `AgendaController` + `agenda-view.fxml` com:
  - Cabeçalho do módulo com barra padronizada (`.module-toolbar`)
  - Cabeçalho de navegação: botões ◀ ▶ + label "Junho 2026" + botão "Hoje"
  - Grade mensal: `GridPane` 7 colunas, 4–6 linhas conforme o mês, células geradas dinamicamente
  - Células mostram: número do dia (destacado em azul escuro se for hoje), até 3 chips de evento + "+N mais"
  - Dia selecionado fica destacado visualmente; clicar em dia de outro mês navega para esse mês
  - Painel lateral com lista de eventos do dia selecionado em cards (horário, título, descrição)
  - Botões ✎ (editar) e ✕ (excluir) por evento, com confirmação de exclusão
  - Diálogo criar/editar: título (obrigatório), data (DatePicker), horário início/fim (H:mm, opcionais), descrição (TextArea) — validação inline sem fechar o diálogo em caso de erro
- **Notificações:** eventos de hoje aparecem no sino (🔔) com strip azul (#1976D2), acima dos alertas de tarefa; clicar navega para a Agenda com o dia de hoje selecionado; botão X dispensa por sessão
- **Navegação:** botão "📅 Agenda" na topbar com carregamento lazy; callback `onEventoAlterado` atualiza o badge do sino ao criar/editar/excluir eventos
- **CSS:** estilos `.agenda-*` em ambos os temas (claro e escuro)

### Banco de dados
```
evento (id, usuario_id, titulo, descricao, data TEXT YYYY-MM-DD, hora_inicio TEXT HH:mm, hora_fim TEXT HH:mm, criado_em)
```

---

## ✅ Módulo 5 — Temporizador Pomodoro

**Status:** Concluído

### O que foi implementado

#### Timer
- `Timeline` com `KeyFrame` de 1 segundo; estado: `segundosRestantes`, `rodando`, `faseAtual`, `sessoesNoCiclo`
- Fases: **Foco** (25 min), **Pausa Curta** (5 min), **Pausa Longa** (15 min) — durações configuráveis
- Ciclo de 4 sessões: sessões 1–3 → pausa curta; sessão 4 → pausa longa; indicadores visuais do ciclo (🍅/○)
- Ao fim de cada fase: alarme sonoro (`java.awt.Toolkit.beep()` em thread própria), avanço automático para a próxima fase já iniciada
- Botões: ▶/⏸ (iniciar/pausar), ↺ (reiniciar fase), ⏭ (pular para próxima fase)
- Configuração de durações via diálogo com `Spinner`; persistida em `~/.studyapp/pomodoro.properties`

#### Sistema de metas de estudo (Disciplina → Assunto)
- **`TipoDificuldade`** (enum): FACIL (2 sessões), MEDIO (4), DIFICIL (6), MUITO_DIFICIL (8) — pré-preenche o campo "sessões mínimas"
- **`TipoStatusAssunto`** (enum): PENDENTE → EM_ANDAMENTO → CONCLUIDO
  - Transições automáticas: ao completar 1ª sessão → EM_ANDAMENTO; ao reduzir sessões abaixo do mínimo → CONCLUIDO rebaixado para EM_ANDAMENTO
  - CONCLUIDO marcado manualmente; ao atingir o mínimo de sessões, diálogo oferece a opção (sem forçar)
- **Data limite opcional** por assunto, exibida como indicador colorido na lista

#### Painel esquerdo (programático)
- `VBox listaDisciplinas` construído dinamicamente em `recarregarListaDisciplinas()`
- Cabeçalhos de disciplina clicáveis (expande/colapsa); estado de expansão mantido em `Set<Integer> expandidas`
- Linha de assunto: ícone de status, nome, indicador de data limite, "X/Y 🍅", botões `[−]` `[+]` `[▶]` `[⋯]`
  - `[−]`/`[+]`: chama `service.ajustarSessoes()` em background thread
  - `[▶]`: seleciona assunto para vinculação ao timer (destaca `.pomo-assunto-selecionado`)
  - `[⋯]`: menu de contexto com Editar, Marcar Concluído / Reabrir, Excluir
- Rodapé: sessões de foco do dia + tempo total de foco (atualizados a cada sessão concluída)

#### Integração com banco
- Ao fim de cada sessão de foco: `service.registrarSessaoFoco()` em background + `service.incrementarSessao(assuntoId)` (se assunto selecionado) + `verificarMetaAssunto()` (diálogo de conclusão se atingiu mínimo)
- Estatísticas diárias via `service.contarSessoesHoje()` e `service.somarDuracaoHoje()`

#### CSS e tema
- Classes `.pomo-*` adicionadas a `dark-theme.css` e `light-theme.css`
- Timer em fonte 72px monospaced; cor verde (`.pomo-timer-pausa`) durante pausas
- Todos os controles (Spinner, botões, headers) respeitam modo claro/escuro

#### Infraestrutura
- Navegação via botão "🍅 Pomodoro" na topbar com carregamento lazy (mesmo padrão dos demais módulos)
- **Janela destacável:** botão "↗ Destacar" na toolbar move toda a view (toolbar + conteúdo) para `Stage` separada sempre no topo; placeholder exibido na tela principal; "↙ Reintegrar" devolve ao painel — padrão idêntico à Calculadora e ao Bloco de Notas
- `pararRecursos()` para o `Timeline` e fecha janela destacada ao fazer logout/fechar
- `atualizarView()` chamado pelo `MainController` ao navegar de volta ao módulo

### Model / DAO / Service

| Classe                       | Responsabilidade                                                          |
|------------------------------|---------------------------------------------------------------------------|
| `model/Disciplina`           | POJO — id, usuarioId, nome, criadoEm                                      |
| `model/Assunto`              | POJO — nome, dificuldade, sessõesMinimas/Realizadas, status, dataLimite   |
| `model/SessaoPomodoro`       | POJO — usuarioId, assuntoId (nullable), tipo, início/fim, duração         |
| `model/TipoDificuldade`      | Enum com label e sessõesDefault                                            |
| `model/TipoStatusAssunto`    | Enum PENDENTE / EM_ANDAMENTO / CONCLUIDO                                  |
| `model/TipoSessao`           | Enum FOCO / PAUSA_CURTA / PAUSA_LONGA                                     |
| `database/DisciplinaDAO`     | inserir, atualizar, excluir, buscarPorUsuario, contarAssuntos              |
| `database/AssuntoDAO`        | inserir, atualizar, excluir, buscarPorId, buscarPorDisciplina              |
| `database/SessaoPomodoroDAO` | registrar, contarSessoesHoje, somarDuracaoHoje                            |
| `service/PomodoroService`    | Orquestra DAOs; regras de status; métodos: criarDisciplina, criarAssunto, incrementarSessao, ajustarSessoes, marcarConcluido, reabrirAssunto, registrarSessaoFoco… |

### Banco de dados
```
disciplina      (id, usuario_id, nome, criado_em) — UNIQUE(usuario_id, nome)
assunto         (id, disciplina_id, nome, dificuldade, sessoes_minimas, sessoes_realizadas, status, data_limite, criado_em, atualizado_em)
sessao_pomodoro (id, usuario_id, assunto_id, tipo, iniciado_em, concluido_em, duracao_segundos)
```

### Bugs corrigidos durante implementação
- **`handleExcluirDisciplina`:** chamar `service.excluirDisciplina()` antes de mostrar diálogo de confirmação excluía a disciplina sem consentimento. **Correção:** contagem de assuntos obtida do mapa em memória; exclusão só ocorre após confirmação do usuário no FX thread.

---

## ✅ Módulo 6 — Bloco de Notas Rápido

**Status:** Concluído

### O que foi implementado
- `TextArea` simples, sem formatação, com auto-save contínuo vinculado ao usuário logado
- Arquivo persistido em `~/.studyapp/bloconotas_<uid>.txt`
- Barra de ações com botões **Salvar .txt** (exportar) e **Limpar** (apaga todo o conteúdo)
- Botão **↗ Destacar**: abre o bloco de notas em janela flutuante separada (sempre no topo); "↙ Reintegrar" para devolver ao painel principal — a cena separada herda o CSS do tema atual
- Indicador de status ("Salvo" / "Salvando...") no canto inferior direito
- **Cabeçalho do módulo** com barra padronizada (`.module-toolbar` / `.module-titulo`)

---

## ✅ Módulo 7 — Calculadora

**Status:** Concluído

### O que foi implementado
- **UI:** `CalculadoraController` + `calculadora-view.fxml` com:
  - Display com `TextArea` de expressão multilinha + label de resultado em tempo real (preview ao digitar)
  - Grade dinâmica 8×7 de botões construída em código
  - Memória: MC, MR, M+, M− com badge "M: valor" visível enquanto há valor armazenado
  - Toggle DEG/RAD com badge de modo no display
  - Botão "↗ Destacar": abre a calculadora em janela separada sempre no topo; "↙ Reintegrar" para devolver ao painel principal
- **Funções suportadas (via exp4j):**
  - Aritméticas: +, −, ×, ÷, % (módulo), ^, parênteses automáticos
  - Trig: sin, cos, tan, asin, acos, atan (respeita modo DEG/RAD)
  - Hiperbólicas: sinh, cosh, tanh
  - Logaritmos: ln, log (base 10), log₂
  - Potências/raízes: √, ∛, x², xⁿ
  - Combinatória: n!, nCr, nPr
  - Arredondamento: floor, ceil, round, abs
  - Constantes: π, e
- **Tooltips em todos os botões de função** com sintaxe e exemplos de uso (ex.: `sin(x)` — "Seno de x em graus/radianos. Ex: sin(90) → 1")
- **Histórico:** persiste entre sessões em `~/.studyapp/calc_hist_<uid>.txt`; clicável para reutilizar expressão; botão "Limpar"; menu de contexto (clique direito) com opções "Copiar resultado" e "Copiar expressão"
- **Atalhos:** Enter calcula, Shift+Enter insere quebra de linha
- **CSS:** estilos `.calc-*` em ambos os temas (claro e escuro); cabeçalho padronizado com `.module-toolbar`
- **Navegação:** botão "🧮 Calculadora" na topbar com carregamento lazy; `pararRecursos()` fecha janela destacada ao fazer logout

---

## ✅ Módulo 9 — Dashboard

**Status:** Concluído

### O que foi implementado
- **UI:** `DashboardController` + `dashboard-view.fxml` — tela inicial exibida logo após o login
- **Saudação contextual:** "Bom dia/Boa tarde/Boa noite, [primeiro nome]!" com a data longa no padrão pt-BR
- **4 cards de resumo** (linha responsiva com `FlowPane`):
  - 🍅 Sessões de foco hoje (contagem + tempo total)
  - ✓ Tarefas urgentes (vencidas + próximas 3 dias, contagem)
  - 📅 Eventos hoje (contagem + nome do primeiro evento)
  - 📚 Progresso do Plano (assuntos concluídos / total + disciplinas)
  - Cards clicáveis navegam diretamente para o módulo correspondente
- **Seção Tarefas urgentes:** lista com ícone de alerta, badge de prazo (Vencida/Hoje/Em Nd) e botão "→ Ver"
- **Seção Eventos de hoje:** lista com horário e botão "→ Ver"
- **Seção Plano de Estudos:** disciplinas com barra de progresso e botão "▶ Estudar" que abre o Pomodoro com o primeiro assunto não-concluído
- **Seção Sessões recentes:** últimas 5 sessões FOCO com disciplina·assunto, data relativa (hoje/ontem) e duração
- **Botão ↻ Atualizar** na toolbar recarrega todos os dados
- **Navegação cruzada:** callbacks `onVerTarefas`, `onVerAgenda`, `onVerPlanoEstudos`, `onEstudarAssunto` injetados por `MainController`
- **Tela inicial:** `MainController.initialize()` chama `handleNavDashboard()` ao fazer login; `painelArquivosContainer` oculto por padrão
- **Dados carregados em background** via thread + `Platform.runLater()`; `DashData` (record Java 17) agrega todos os resultados
- **CSS:** prefixo `.dash-*` em ambos os temas; cards com borda e sombra sutil; seções com fundo branco/escuro e separador
- **Javadoc em português** em todos os métodos públicos e privados

### Arquivos novos / modificados (além de docs)
| Arquivo | Alteração |
|---|---|
| `controller/DashboardController.java` | Novo |
| `dashboard-view.fxml` | Novo |
| `database/SessaoPomodoroDAO.java` | `buscarResumoRecentes()` adicionado |
| `service/PomodoroService.java` | `buscarResumoSessoesRecentes()` adicionado |
| `controller/MainController.java` | `dashboardController`, `handleNavDashboard()`, callbacks, default navigation |
| `main-view.fxml` | Botão 🏠 Início + container Dashboard |
| `light-theme.css` / `dark-theme.css` | Seção `.dash-*` |

---

## ✅ Módulo 8 — Plano de Estudos

**Status:** Concluído

### O que foi implementado
- **UI:** `PlanoEstudosController` + `plano-estudos-view.fxml` com layout SplitPane (sidebar esquerda 28% + painel direito 72%)
- **Painel esquerdo (disciplinas):**
  - Card por disciplina com barra de progresso (% assuntos concluídos), stats de sessões e tempo total de estudo
  - Card da disciplina selecionada destacado com borda azul
  - CRUD de disciplinas: criar (TextInputDialog), renomear, excluir (com confirmação e contagem de assuntos)
- **Painel direito (assuntos):**
  - Cabeçalho com nome da disciplina, contagem e tempo total
  - Card por assunto com:
    - Chips de status: ○ Pendente / ◑ Em Andamento / ✓ Concluído (cor por estado)
    - Chips de dificuldade: Fácil / Médio / Difícil / Muito Difícil (cores distintas)
    - Indicador de data limite com alertas visuais: vermelho (vencido), laranja (hoje), amarelo (≤ 3 dias)
    - Barra de progresso de sessões Pomodoro com percentual e total de tempo de foco
    - Botão "▶ Estudar agora" — navega para o Pomodoro e pré-seleciona o assunto
    - Menu ⋯ com editar, marcar concluído/reabrir, excluir
  - Estado vazio com instrução quando nenhuma disciplina está selecionada
- **Diálogo de assunto:** disciplina (readonly ao editar), nome, dificuldade (auto-sugere sessões mínimas), spinner de sessões mínimas, DatePicker de data limite
- **Integração com Pomodoro (após merge de 28/jun/2026):**
  - `PomodoroService` reutilizado integralmente (sem service novo)
  - `SessaoPomodoroDAO.somarDuracaoPorAssunto()` e `somarDuracaoPorDisciplina()` (adicionados)
  - `PomodoroTimerController.selecionarAssunto(Assunto)` — define o assunto ativo e atualiza label
  - Timer adicionado/removido programaticamente do `SplitPane` via `mostrarPainelTimer()` / `ocultarPainelTimer()`
  - `MainController.estudarAssuntoNoPlano()` — navega ao Plano e chama `estudarAssunto()`
  - Callback `onSessaoConcluida` notifica o `PlanoEstudosController` para recarregar dados
- **CSS:** prefixo `.plano-*` em ambos os temas (`.plano-root`, `.plano-disc-card`, `.plano-assunto-card`, `.plano-chip-*`, `.plano-progress-*`, `.plano-header`, `.plano-vazio`)
- **Carregamento lazy** via `handleNavPlanoEstudos()` no MainController; `atualizarView()` recarrega dados ao renavigar
- **Background threads** para todas as operações de banco; UI atualizada via `Platform.runLater()`
- **Javadoc em português** em todos os métodos e campos públicos

### Funcionalidades adicionadas em 28/jun/2026
- **Barra de pesquisa** — `TextField campoBusca` com listener em tempo real; `filtrarDisciplinas()` inclui disciplina se nome OU nome de algum assunto corresponder; painel direito também filtra assuntos. Botão ✕ limpa a busca.
- **Arquivamento de disciplinas** — menu ⋯ → "📦 Arquivar"; `DisciplinaDAO.arquivar/desarquivar()` + `migrarTabelas()` para coluna `arquivado`; botão toggle no rodapé da sidebar; cards arquivados com opacidade e cor diferenciados; opção de desarquivar pelo menu ⋯.
- **Histórico de sessões** — `SessaoPomodoroDAO.buscarHistoricoPorAssunto()` e `buscarHistoricoPorDisciplina()`; diálogo `exibirDialogoHistorico()` com cabeçalho de total, linhas `🍅 data/hora • duração [• assunto]`; modo disciplina inclui coluna de nome do assunto.
- **Timer Pomodoro integrado (merge)** — `PomodoroTimerController` + `pomodoro-timer-view.fxml` incorporados ao SplitPane do Plano de Estudos; loader lazy via `carregarTimerSeNecessario()`; callbacks `onEncerrar` / `onSessaoConcluida`; botão "✕ Encerrar" remove o painel; destaque em janela flutuante mantido.

### Banco de dados
Sem novas tabelas — reutiliza integralmente as tabelas existentes do Pomodoro.
Migração aplicada: coluna `arquivado INTEGER NOT NULL DEFAULT 0` adicionada à tabela `disciplina` via `DatabaseManager.migrarTabelas()` (idempotente).
```
disciplina      (id, usuario_id, nome, criado_em, arquivado)
assunto         (id, disciplina_id, nome, dificuldade, sessoes_minimas, sessoes_realizadas, status, data_limite, criado_em, atualizado_em)
sessao_pomodoro (id, usuario_id, assunto_id, tipo, iniciado_em, concluido_em, duracao_segundos)
```

Queries adicionadas ao `SessaoPomodoroDAO`:
- `somarDuracaoPorAssunto(int assuntoId)` — `SUM(duracao_segundos)` por assunto e tipo FOCO
- `somarDuracaoPorDisciplina(int disciplinaId)` — JOIN `assunto` para somar toda a disciplina
- `buscarHistoricoPorAssunto(int assuntoId)` — retorna `[concluido_em, duracao_segundos]` DESC
- `buscarHistoricoPorDisciplina(int disciplinaId)` — retorna `[concluido_em, duracao_segundos, assunto_nome]` DESC

---

## Infraestrutura e arquitetura

| Item                            | Status | Observação |
|---------------------------------|--------|------------|
| Maven + Java 17                 | ✅     | `pom.xml` configurado com release 17 |
| Dois temas (claro/escuro)       | ✅     | `ThemeManager` + `light-theme.css` / `dark-theme.css` |
| Thread safety                   | ✅     | Toda operação de banco roda em background thread; UI atualizada via `Platform.runLater()` |
| Tamanho mínimo da janela        | ✅     | Calculado em runtime a partir de `Screen.getPrimary()` |
| Auto-save com debounce          | ✅     | `PauseTransition` de 1,5s reiniciado a cada keystroke |
| Isolamento de dados por usuário | ✅     | Todas as queries filtram por `usuario_id` |
| SMTP configurável               | ✅     | `~/.studyapp/mail.properties` + diálogo de configuração na UI |
| 69 testes unitários             | ✅     | `UsuarioDAOTest`, `NotaDAOTest`, `ItemArvoreDAOTest`, `UsuarioServiceTest`, `HashUtilTest`, `FormatadorDataTest` |
| Modo escuro nos diálogos        | ✅     | `ThemeManager.aplicarTemaAoDialogo()` aplica CSS ao DialogPane **e** à cena (via `sceneProperty`), cobrindo inclusive popups de DatePicker e ComboBox |
| Cabeçalhos de módulo padronizados | ✅   | Classes compartilhadas `.module-toolbar` / `.module-titulo` em todos os módulos |

---

## Correções de modo escuro (jun/2026)

Todas as partes do sistema que apareciam com cores claras no modo escuro foram corrigidas:

| Componente                                  | Causa raiz                                                   | Correção                                                                      |
|---------------------------------------------|--------------------------------------------------------------|-------------------------------------------------------------------------------|
| Diálogos/Alertas (fundo branco)             | JavaFX cria Stage/Scene próprio que não herda o CSS da cena principal | `aplicarTemaAoDialogo()` aplica CSS ao `DialogPane` e à `Scene` via listener |
| ScrollPane dentro de diálogos (tarefa)      | `ScrollPane` cobre o fundo escuro com branco padrão do Modena | CSS `.dialog-pane .scroll-pane` e `> .viewport` |
| TextArea nos diálogos                       | Nó interno `.content` usa fundo branco do Modena             | CSS `.dialog-pane .text-area .content` e `.scroll-pane` internos              |
| DatePicker nos diálogos                     | Controle e popup sem estilo                                  | CSS `.dialog-pane .date-picker` + `.date-picker-popup` global na cena         |
| ComboBox (dropdown) nos diálogos e filtros  | Popup em janela separada não herda CSS do nó                 | `.combo-box-popup > .list-view` global; sceneProperty listener propaga CSS    |
| Lista de etiquetas (painel esquerdo tarefas)| Células do ListView usavam padrão Modena (branco)            | `.labels-list .list-cell` com fundo `#16162A`                                 |
| Cabeçalho/linhas da TableView de tarefas    | Sem estilos de tabela no tema escuro                         | CSS `.table-view .column-header-background`, `.table-row-cell`, etc.          |
| Menu de contexto (⚙ configurações)          | ContextMenu em popup próprio sem CSS                         | CSS `.context-menu` e `.menu-item` no tema escuro                             |
| Diálogo "Configurar E-mail SMTP"            | `SmtpConfigDialog` não aplicava CSS                          | `aplicarTemaAoDialogo()` adicionado                                           |
| CheckBox e Spinner nos diálogos             | Controles sem estilo no tema escuro                          | CSS `.dialog-pane .check-box` e `.dialog-pane .spinner`                       |

---

## Histórico de sessões

| Data         | O que foi feito |
|--------------|-----------------|
| jun/2026     | Sistema de arquivos completo (CADERNO, NOTA, PDF), login/cadastro/perfil, 69 testes |
| jun/2026     | Módulo de tarefas implementado completo (model, DAO, service, controller, FXML, CSS) |
| 21/jun/2026  | Correção do bug "esqueceu senha" (senha trocada antes do e-mail ser enviado); reset de senha de emergência; criação deste documento |
| 21/jun/2026  | Sistema de notificações de prazo: botão 🔔 na topbar com badge de contagem, popup com itens coloridos (vermelho/laranja/amarelo), botão X por sessão, clique navega e seleciona a tarefa, animação vermelha piscante quando há tarefas vencidas |
| 21/jun/2026  | Melhoria no formulário de nova tarefa: pré-seleciona a etiqueta ativa do painel lateral no campo "Etiqueta 1" |
| 21/jun/2026  | Módulo Agenda implementado: grade mensal interativa, CRUD de eventos com horários, notificações de eventos do dia no sino (strip azul), integrado com sistema de notificações existente |
| 24/jun/2026  | Módulo Calculadora integrado: botão na topbar, CSS em ambos os temas, carregamento lazy, janela destacável, histórico persistente |
| 24/jun/2026  | Melhorias na calculadora: tooltips detalhados em todos os botões de função (sintaxe + exemplos); menu de contexto no histórico ("Copiar resultado" / "Copiar expressão") |
| 24/jun/2026  | Padronização de cabeçalhos: criadas classes CSS `.module-toolbar` / `.module-titulo`; adicionado cabeçalho em Tarefas, Agenda, Arquivos e Bloco de Notas; Calculadora atualizada para usar as mesmas classes |
| 24/jun/2026  | Melhorias no módulo de tarefas: coluna de checkbox para marcar como concluída diretamente na tabela; callback `onTarefaAlterada` corrige bug onde o badge de notificações não era atualizado ao alterar tarefas; ordenação e ocultação de colunas habilitadas (`tableMenuButtonVisible="true"`) |
| 24/jun/2026  | Botão "Limpar" adicionado ao Bloco de Notas |
| 24/jun/2026  | Correção completa do modo escuro: diálogos/alertas, menus de contexto, TableView, DatePicker, TextArea, ComboBox popups, lista de etiquetas — ver tabela de correções acima |
| 24/jun/2026  | Módulo Pomodoro implementado completo: timer com ciclo automático, sistema de metas Disciplina → Assunto, painel esquerdo programático com botões [−][+][▶][⋯], vinculação sessão/assunto, persistência de configurações em `pomodoro.properties`, CSS em ambos os temas |
| 24/jun/2026  | Documentação Javadoc adicionada em português a todos os 51 arquivos Java do projeto (model, DAO, service, controller, util, view) |
| 25/jun/2026  | Botão "↗ Destacar" / "↙ Reintegrar" adicionado ao módulo Pomodoro: toolbar + conteúdo migram juntos para janela separada, igualando o comportamento da Calculadora e do Bloco de Notas |
| 28/jun/2026  | Módulo Dashboard implementado: tela inicial com saudação contextual, 4 cards de resumo (Pomodoro/Tarefas/Agenda/Plano), seções de tarefas urgentes, eventos de hoje, progresso do plano e sessões recentes; botão "🏠 Início" na topbar; modo claro e escuro; Javadoc em português |
| 26/jun/2026  | Módulo Plano de Estudos implementado: disciplinas com barra de progresso e total de tempo, assuntos com chips de status/dificuldade, barra de sessões Pomodoro, data limite com alertas visuais, botão "Estudar agora" com integração ao Pomodoro, CRUD completo, CSS em ambos os temas, Javadoc em português |
| 28/jun/2026  | Melhorias no Plano de Estudos: (1) barra de pesquisa na sidebar filtra disciplinas e assuntos por nome em tempo real; (2) arquivamento de disciplinas — menu ⋯ > "Arquivar", toggle "📦 Ver arquivadas" no rodapé da sidebar, colspan visual diferenciado, opção de desarquivar; (3) histórico de sessões — diálogo "📊 Histórico" por assunto (data/hora + duração) e por disciplina (idem + nome do assunto), total acumulado no cabeçalho. Migração de esquema: coluna `arquivado` na tabela `disciplina` via `migrarTabelas()`. CSS em ambos os temas (busca, arquivadas, histórico). Javadoc em português em todos os métodos novos. |
| 28/jun/2026  | Merge Pomodoro + Plano de Estudos: o botão "🍅 Pomodoro" foi removido da topbar; `PomodoroController` e `pomodoro-view.fxml` foram excluídos. O timer foi extraído para `PomodoroTimerController` + `pomodoro-timer-view.fxml` (sem painel de disciplinas). Ao clicar em "▶ Estudar agora", o timer é carregado lazily e inserido como terceiro painel do `SplitPane` do Plano de Estudos; o botão "✕ Encerrar" o remove. O callback `onSessaoConcluida` recarrega os dados do Plano após cada sessão. O destaque em janela flutuante (↗/↙) permanece funcional. `MainController` atualizado: `estudarAssuntoNoPlano()` substitui `navegarParaPomodoroComAssunto()`; `pararRecursos()` delega para `planoEstudosController.pararTimer()`. CSS novo: `.plano-timer-raiz`, `.plano-timer-estudando`. Javadoc em português em todos os métodos. |
| 28/jun/2026  | Tema Lúdico + acesso direto a temas + escala de fonte: (1) `ludic-theme.css` criado com palette pastel roxo/amarelo; (2) menu ⚙ exibe 3 itens de tema diretos (Claro/Escuro/Lúdico) em vez de botão de ciclo; (3) botões A+/A− no menu de configurações escalam toda a UI via `setScaleX/Y` na raiz da cena (0,85× a 1,25×, passo 0,05), persistido em `~/.studyapp/ui.properties`; calculadora com fonte maior (22px/32px) em todos os temas. |
| 28/jun/2026  | Correção race condition PDFBox: substituído sleep fixo de 300ms por `AtomicInteger renderizacoesAtivas` (incrementado antes da thread de renderização, decrementado no finally); `fecharPdfAtual()` aguarda contador zerar com loop de até 7,5s (150 × 50ms). |
| 28/jun/2026  | Melhorias no módulo de arquivos: (1) campo de busca por nome/conteúdo — `TextField` acima da `TreeView`; ao digitar, exibe `ListView` de resultados (UNION SQL nome + conteúdo de nota); (2) arquivamento de cadernos via menu de contexto "📦 Arquivar"/"📬 Desarquivar", toggle "📦 Ver arquivados" no rodapé; coluna `arquivado` adicionada a `item_arvore` via `migrarTabelas()`; (3) botão "⬛ Ajustar" no toolbar do PDF ajusta zoom para caber a largura da página na viewport; (4) rótulos de navegação PDF (`labelPdfPagina`, `labelPdfZoom`) mudaram styleClass para `pdf-info-label` com cor visível nos temas claro e lúdico. CSS atualizado nos 3 temas. Javadoc em português em todos os métodos novos. |
