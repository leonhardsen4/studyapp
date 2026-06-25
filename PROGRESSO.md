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
| Temporizador Pomodoro     | ⬜ Pendente   |
| Bloco de notas rápido     | ✅ Concluído  |
| Calculadora               | ✅ Concluído  |

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

## ⬜ Módulo 5 — Temporizador Pomodoro

**Status:** Pendente  
Ver seção 7.3 da `ESPECIFICACOES.md`.

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
