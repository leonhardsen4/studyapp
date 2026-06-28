package com.leonhardsen.studyapp.controller;

import com.leonhardsen.studyapp.StudyApplication;
import com.leonhardsen.studyapp.model.*;
import com.leonhardsen.studyapp.service.PomodoroService;
import com.leonhardsen.studyapp.util.SessionManager;
import com.leonhardsen.studyapp.util.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controlador do módulo Plano de Estudos.
 * Exibe uma visão consolidada de disciplinas e assuntos com progresso de sessões
 * Pomodoro, estatísticas de tempo estudado, pesquisa por nome, arquivamento de
 * disciplinas e histórico de sessões por assunto ou disciplina.
 */
public class PlanoEstudosController {

    // ── FXML — barra de ações / sidebar ──────────────────────────────────────
    @FXML private SplitPane splitPlano;
    @FXML private TextField campoBusca;
    @FXML private Button    btnLimparBusca;
    @FXML private Button    btnNovoAssunto;
    @FXML private VBox      listaDisciplinas;
    @FXML private Button    btnVerArquivadas;

    // ── FXML — painel direito ─────────────────────────────────────────────────
    @FXML private VBox       painelVazio;
    @FXML private VBox       headerAssuntos;
    @FXML private Label      lblNomeDisciplina;
    @FXML private Label      lblResumoDisc;
    @FXML private ScrollPane scrollAssuntos;
    @FXML private VBox       listaAssuntos;

    // ── Dados ─────────────────────────────────────────────────────────────────
    private final PomodoroService service = new PomodoroService();
    private List<Disciplina>            disciplinas           = new ArrayList<>();
    private List<Disciplina>            disciplinasArquivadas = new ArrayList<>();
    private Map<Integer, List<Assunto>> assuntosPorDisciplina = new HashMap<>();
    private Map<Integer, Integer>       duracoesDisciplinas   = new HashMap<>();
    private Map<Integer, Integer>       duracoesAssuntos      = new HashMap<>();
    private Disciplina                  disciplinaSelecionada = null;
    private String                      termoBusca            = "";
    private boolean                     mostrandoArquivadas   = false;

    // ── Timer incorporado ─────────────────────────────────────────────────────
    private PomodoroTimerController timerController = null;
    private Node                    timerNode       = null;
    private static final double[] DIVIDERS_SEM_TIMER = {0.28};
    private static final double[] DIVIDERS_COM_TIMER = {0.20, 0.56};

    private static final DateTimeFormatter FMT_DATA      = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy  HH:mm");

    // ─────────────────────────────────────────────────────────────────────────
    // INICIALIZAÇÃO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inicializa o controlador: configura o listener da barra de pesquisa e carrega os dados.
     */
    @FXML
    public void initialize() {
        campoBusca.textProperty().addListener((obs, old, novo) -> {
            termoBusca = novo == null ? "" : novo.trim().toLowerCase();
            boolean temBusca = !termoBusca.isEmpty();
            btnLimparBusca.setVisible(temBusca);
            btnLimparBusca.setManaged(temBusca);
            recarregarListaDisciplinas();
            if (disciplinaSelecionada != null) recarregarAssuntos();
        });
        carregarDados();
    }

    /**
     * Recarrega os dados ao navegar de volta para este módulo.
     */
    public void atualizarView() {
        carregarDados();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INTEGRAÇÃO DO TIMER POMODORO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Seleciona o assunto a estudar, carrega o timer (se necessário) e o exibe
     * como terceiro painel do {@code SplitPane}. Ponto de entrada público chamado
     * pelo {@code MainController} e pelo próprio controller ao clicar em "Estudar agora".
     *
     * @param assunto assunto a ser estudado no timer Pomodoro
     */
    public void estudarAssunto(Assunto assunto) {
        carregarTimerSeNecessario();
        if (timerController != null) {
            timerController.selecionarAssunto(assunto);
            mostrarPainelTimer();
        }
    }

    /**
     * Carrega {@code pomodoro-timer-view.fxml} na primeira chamada e configura os callbacks.
     * Nas chamadas subsequentes é um no-op (lazy initialization).
     */
    private void carregarTimerSeNecessario() {
        if (timerController != null) return;
        try {
            FXMLLoader loader = new FXMLLoader(StudyApplication.class.getResource("pomodoro-timer-view.fxml"));
            timerNode = loader.load();
            timerController = loader.getController();
            timerController.setOnEncerrar(this::ocultarPainelTimer);
            timerController.setOnSessaoConcluida(this::carregarDados);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Adiciona o painel do timer ao {@code SplitPane} se ainda não estiver presente
     * e ajusta as posições dos divisores para acomodar os três painéis.
     */
    private void mostrarPainelTimer() {
        if (!splitPlano.getItems().contains(timerNode)) {
            splitPlano.getItems().add(timerNode);
            Platform.runLater(() -> splitPlano.setDividerPositions(DIVIDERS_COM_TIMER));
        }
    }

    /**
     * Remove o painel do timer do {@code SplitPane} e restaura os dois painéis originais.
     * Chamado via callback {@code onEncerrar} do {@code PomodoroTimerController}.
     */
    public void ocultarPainelTimer() {
        splitPlano.getItems().remove(timerNode);
        splitPlano.setDividerPositions(DIVIDERS_SEM_TIMER);
    }

    /**
     * Para o timer e fecha a janela destacada (se aberta).
     * Deve ser chamado pelo {@code MainController} ao encerrar o aplicativo.
     */
    public void pararTimer() {
        if (timerController != null) timerController.pararRecursos();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HANDLERS DA TOOLBAR / SIDEBAR
    // ─────────────────────────────────────────────────────────────────────────

    /** Limpa o campo de busca. */
    @FXML
    private void handleLimparBusca() {
        campoBusca.clear();
        campoBusca.requestFocus();
    }

    /** Exibe o diálogo de criação de nova disciplina. */
    @FXML
    private void handleNovaDisciplina() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("Nova Disciplina");
        dlg.setHeaderText("Adicionar disciplina");
        dlg.setContentText("Nome:");
        ThemeManager.getInstance().aplicarTemaAoDialogo(dlg);
        dlg.showAndWait().ifPresent(nome -> {
            if (nome.isBlank()) return;
            new Thread(() -> {
                try {
                    int uid = SessionManager.getInstance().getUsuarioLogado().getId();
                    Disciplina nova = service.criarDisciplina(uid, nome);
                    Platform.runLater(() -> {
                        disciplinaSelecionada = nova;
                        carregarDados();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro(ex.getMessage()));
                }
            }).start();
        });
    }

    /** Exibe o diálogo de criação de novo assunto na disciplina selecionada. */
    @FXML
    private void handleNovoAssunto() {
        if (disciplinaSelecionada == null) { mostrarErro("Selecione uma disciplina primeiro."); return; }
        mostrarDialogoAssunto(null, disciplinaSelecionada);
    }

    /** Alterna a exibição da seção de disciplinas arquivadas no painel esquerdo. */
    @FXML
    private void handleToggleArquivadas() {
        mostrandoArquivadas = !mostrandoArquivadas;
        if (mostrandoArquivadas) {
            int uid = SessionManager.getInstance().getUsuarioLogado().getId();
            new Thread(() -> {
                try {
                    List<Disciplina> arq = service.buscarDisciplinasArquivadas(uid);
                    Platform.runLater(() -> {
                        disciplinasArquivadas = arq;
                        atualizarBotaoArquivadas();
                        recarregarListaDisciplinas();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro(ex.getMessage()));
                }
            }).start();
        } else {
            disciplinasArquivadas.clear();
            atualizarBotaoArquivadas();
            recarregarListaDisciplinas();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CARREGAMENTO DE DADOS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Carrega disciplinas ativas, assuntos e durações de sessões em background e atualiza a UI.
     */
    private void carregarDados() {
        int uid = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                List<Disciplina> discs        = service.buscarDisciplinas(uid);
                Map<Integer, List<Assunto>> mapaAssuntos = new HashMap<>();
                Map<Integer, Integer>       duracDisc    = new HashMap<>();
                Map<Integer, Integer>       duracAssunto = new HashMap<>();

                for (Disciplina d : discs) {
                    List<Assunto> assuntos = service.buscarAssuntos(d.getId());
                    mapaAssuntos.put(d.getId(), assuntos);
                    duracDisc.put(d.getId(), service.somarDuracaoPorDisciplina(d.getId()));
                    for (Assunto a : assuntos) {
                        duracAssunto.put(a.getId(), service.somarDuracaoPorAssunto(a.getId()));
                    }
                }

                List<Disciplina> arq = mostrandoArquivadas
                        ? service.buscarDisciplinasArquivadas(uid) : disciplinasArquivadas;

                Platform.runLater(() -> {
                    disciplinas           = discs;
                    assuntosPorDisciplina = mapaAssuntos;
                    duracoesDisciplinas   = duracDisc;
                    duracoesAssuntos      = duracAssunto;
                    disciplinasArquivadas = arq;

                    if (disciplinaSelecionada != null) {
                        final int idSel = disciplinaSelecionada.getId();
                        // Tenta encontrar na lista ativa; senão, nas arquivadas
                        disciplinaSelecionada = discs.stream()
                                .filter(d -> d.getId() == idSel).findFirst()
                                .orElseGet(() -> arq.stream()
                                        .filter(d -> d.getId() == idSel).findFirst()
                                        .orElse(null));
                    }

                    atualizarBotaoArquivadas();
                    recarregarListaDisciplinas();
                    if (disciplinaSelecionada != null) recarregarAssuntos();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAINEL ESQUERDO — LISTA DE DISCIPLINAS
    // ─────────────────────────────────────────────────────────────────────────

    /** Atualiza o texto do botão de arquivadas conforme o estado atual. */
    private void atualizarBotaoArquivadas() {
        if (mostrandoArquivadas) {
            int qtd = disciplinasArquivadas.size();
            btnVerArquivadas.setText("📦 Ocultar arquivadas (" + qtd + ")");
        } else {
            btnVerArquivadas.setText("📦 Ver arquivadas");
        }
    }

    /**
     * Reconstrói a lista de disciplinas no painel esquerdo aplicando o filtro de busca.
     */
    private void recarregarListaDisciplinas() {
        listaDisciplinas.getChildren().clear();
        btnNovoAssunto.setDisable(
                disciplinaSelecionada == null || disciplinaSelecionada.isArquivado());

        List<Disciplina> visiveis = filtrarDisciplinas(disciplinas);

        if (visiveis.isEmpty() && disciplinas.isEmpty()) {
            Label hint = new Label("Clique em '+ Disciplina' para começar.");
            hint.getStyleClass().add("plano-sidebar-vazio");
            hint.setPadding(new Insets(16));
            hint.setWrapText(true);
            listaDisciplinas.getChildren().add(hint);
        } else if (visiveis.isEmpty()) {
            Label hint = new Label("Sem resultado para \"" + campoBusca.getText().trim() + "\".");
            hint.getStyleClass().add("plano-sidebar-vazio");
            hint.setPadding(new Insets(16));
            hint.setWrapText(true);
            listaDisciplinas.getChildren().add(hint);
        } else {
            for (Disciplina d : visiveis) {
                listaDisciplinas.getChildren().add(criarCardDisciplina(d, false));
            }
        }

        // Seção de arquivadas (exibida quando toggle ativo)
        if (mostrandoArquivadas && !disciplinasArquivadas.isEmpty()) {
            Separator sep = new Separator();
            sep.setPadding(new Insets(4, 0, 4, 0));
            Label lblArq = new Label("Arquivadas");
            lblArq.getStyleClass().add("plano-sidebar-vazio");
            lblArq.setPadding(new Insets(4, 8, 2, 8));
            listaDisciplinas.getChildren().addAll(sep, lblArq);

            for (Disciplina d : filtrarDisciplinas(disciplinasArquivadas)) {
                listaDisciplinas.getChildren().add(criarCardDisciplina(d, true));
            }
        }
    }

    /**
     * Filtra uma lista de disciplinas pelo termo de busca atual.
     * Inclui a disciplina se seu nome ou o nome de algum assunto corresponder ao termo.
     *
     * @param fonte lista de disciplinas a filtrar
     * @return lista filtrada (igual à fonte se não houver busca ativa)
     */
    private List<Disciplina> filtrarDisciplinas(List<Disciplina> fonte) {
        if (termoBusca.isEmpty()) return fonte;
        List<Disciplina> resultado = new ArrayList<>();
        for (Disciplina d : fonte) {
            boolean nomeMatch = d.getNome().toLowerCase().contains(termoBusca);
            boolean assuntoMatch = assuntosPorDisciplina
                    .getOrDefault(d.getId(), List.of()).stream()
                    .anyMatch(a -> a.getNome().toLowerCase().contains(termoBusca));
            if (nomeMatch || assuntoMatch) resultado.add(d);
        }
        return resultado;
    }

    /**
     * Constrói o card de uma disciplina para o painel esquerdo.
     *
     * @param d         disciplina a ser exibida
     * @param arquivada {@code true} se a disciplina está arquivada (aplica estilo visual diferente)
     * @return nó {@link VBox} representando o card
     */
    private VBox criarCardDisciplina(Disciplina d, boolean arquivada) {
        List<Assunto> assuntos = assuntosPorDisciplina.getOrDefault(d.getId(), List.of());
        long concluidos = assuntos.stream().filter(a -> a.getStatus() == TipoStatusAssunto.CONCLUIDO).count();
        double pct      = assuntos.isEmpty() ? 0.0 : (double) concluidos / assuntos.size();
        int segundos    = duracoesDisciplinas.getOrDefault(d.getId(), 0);
        boolean sel     = disciplinaSelecionada != null && disciplinaSelecionada.getId() == d.getId();

        VBox card = new VBox(5);
        card.getStyleClass().add("plano-disc-card");
        if (arquivada) card.getStyleClass().add("plano-disc-card-arquivada");
        if (sel)       card.getStyleClass().add("plano-disc-card-selecionado");

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblNome = new Label(d.getNome());
        lblNome.getStyleClass().add("plano-disc-nome");
        HBox.setHgrow(lblNome, Priority.ALWAYS);

        Button btnMenu = new Button("⋯");
        btnMenu.getStyleClass().add("btn-action");
        btnMenu.setOnAction(e -> mostrarMenuDisciplina(btnMenu, d, arquivada));

        header.getChildren().addAll(lblNome, btnMenu);

        ProgressBar progressBar = new ProgressBar(pct);
        progressBar.getStyleClass().add("plano-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        if (pct >= 1.0 && !assuntos.isEmpty())
            progressBar.getStyleClass().add("plano-progress-concluido");
        else if (pct > 0)
            progressBar.getStyleClass().add("plano-progress-andamento");

        Label lblStats = new Label(concluidos + "/" + assuntos.size() + " assuntos  •  " + formatarTempo(segundos));
        lblStats.getStyleClass().add("plano-disc-stats");

        card.getChildren().addAll(header, progressBar, lblStats);
        card.setOnMouseClicked(e -> selecionarDisciplina(d));
        return card;
    }

    /**
     * Exibe o menu de contexto de uma disciplina com opções que variam conforme o estado arquivado.
     *
     * @param anchor    botão âncora para posicionar o menu
     * @param d         disciplina alvo
     * @param arquivada {@code true} se a disciplina está arquivada
     */
    private void mostrarMenuDisciplina(Button anchor, Disciplina d, boolean arquivada) {
        ContextMenu menu = new ContextMenu();
        if (!arquivada) {
            MenuItem renomear    = new MenuItem("✎  Renomear");
            renomear.setOnAction(ev -> handleRenomearDisciplina(d));
            MenuItem novoAssunto = new MenuItem("＋  Novo Assunto");
            novoAssunto.setOnAction(ev -> mostrarDialogoAssunto(null, d));
            MenuItem historico   = new MenuItem("📊  Histórico de sessões");
            historico.setOnAction(ev -> mostrarHistoricoDisciplina(d));
            MenuItem arquivar    = new MenuItem("📦  Arquivar disciplina");
            arquivar.setOnAction(ev -> handleArquivarDisciplina(d));
            MenuItem excluir     = new MenuItem("✕  Excluir Disciplina");
            excluir.setOnAction(ev -> handleExcluirDisciplina(d));
            menu.getItems().addAll(renomear, novoAssunto, historico,
                    new SeparatorMenuItem(), arquivar, new SeparatorMenuItem(), excluir);
        } else {
            MenuItem desarquivar = new MenuItem("↩  Desarquivar disciplina");
            desarquivar.setOnAction(ev -> handleDesarquivarDisciplina(d));
            MenuItem historico   = new MenuItem("📊  Histórico de sessões");
            historico.setOnAction(ev -> mostrarHistoricoDisciplina(d));
            MenuItem excluir     = new MenuItem("✕  Excluir Disciplina");
            excluir.setOnAction(ev -> handleExcluirDisciplina(d));
            menu.getItems().addAll(desarquivar, historico, new SeparatorMenuItem(), excluir);
        }
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PAINEL DIREITO — ASSUNTOS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Seleciona uma disciplina e exibe seus assuntos no painel direito.
     *
     * @param d disciplina selecionada
     */
    private void selecionarDisciplina(Disciplina d) {
        disciplinaSelecionada = d;
        btnNovoAssunto.setDisable(d.isArquivado());
        recarregarListaDisciplinas();
        recarregarAssuntos();
    }

    /**
     * Reconstrói a lista de assuntos da disciplina selecionada, aplicando o filtro de busca ativo.
     */
    private void recarregarAssuntos() {
        if (disciplinaSelecionada == null) { mostrarPainelVazio(); return; }

        List<Assunto> todosAssuntos = assuntosPorDisciplina.getOrDefault(disciplinaSelecionada.getId(), List.of());
        List<Assunto> assuntos = termoBusca.isEmpty() ? todosAssuntos
                : todosAssuntos.stream().filter(a -> a.getNome().toLowerCase().contains(termoBusca)).toList();

        long concluidos = todosAssuntos.stream().filter(a -> a.getStatus() == TipoStatusAssunto.CONCLUIDO).count();
        int  totalSeg   = duracoesDisciplinas.getOrDefault(disciplinaSelecionada.getId(), 0);

        lblNomeDisciplina.setText(disciplinaSelecionada.getNome()
                + (disciplinaSelecionada.isArquivado() ? "  [Arquivada]" : ""));
        lblResumoDisc.setText(concluidos + "/" + todosAssuntos.size()
                + " assuntos concluídos  •  " + formatarTempo(totalSeg) + " de estudo no total");

        headerAssuntos.setVisible(true);  headerAssuntos.setManaged(true);
        painelVazio.setVisible(false);    painelVazio.setManaged(false);
        scrollAssuntos.setVisible(true);  scrollAssuntos.setManaged(true);

        listaAssuntos.getChildren().clear();

        if (assuntos.isEmpty()) {
            VBox vazio = new VBox(10);
            vazio.setAlignment(Pos.CENTER);
            vazio.setPadding(new Insets(40));
            Label ico  = new Label("📖");
            ico.setStyle("-fx-font-size: 36px;");
            String msgTxt = termoBusca.isEmpty()
                    ? "Nenhum assunto nesta disciplina."
                    : "Nenhum assunto corresponde a \"" + campoBusca.getText().trim() + "\".";
            Label msg  = new Label(msgTxt);
            msg.getStyleClass().add("plano-vazio-titulo");
            String hintTxt = termoBusca.isEmpty()
                    ? "Use '+ Assunto' para adicionar um assunto." : "Tente um termo diferente.";
            Label hint = new Label(hintTxt);
            hint.getStyleClass().add("plano-vazio-hint");
            vazio.getChildren().addAll(ico, msg, hint);
            listaAssuntos.getChildren().add(vazio);
        } else {
            for (Assunto a : assuntos) {
                int duracaoSeg = duracoesAssuntos.getOrDefault(a.getId(), 0);
                listaAssuntos.getChildren().add(criarCardAssunto(a, duracaoSeg));
            }
        }
    }

    /**
     * Constrói o card de um assunto para o painel direito.
     *
     * @param a          assunto a ser exibido
     * @param duracaoSeg total de segundos de foco registrados para este assunto
     * @return nó {@link VBox} representando o card
     */
    private VBox criarCardAssunto(Assunto a, int duracaoSeg) {
        VBox card = new VBox(8);
        card.getStyleClass().add("plano-assunto-card");

        // Linha 1: chips de status / dificuldade + data limite
        HBox row1 = new HBox(6);
        row1.setAlignment(Pos.CENTER_LEFT);

        Label chipStatus = new Label(getStatusLabel(a.getStatus()));
        chipStatus.getStyleClass().addAll("plano-chip", getStatusChipClass(a.getStatus()));

        Label chipDif = new Label(a.getDificuldade().getLabel());
        chipDif.getStyleClass().addAll("plano-chip", getDificuldadeChipClass(a.getDificuldade()));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row1.getChildren().addAll(chipStatus, chipDif, spacer);

        if (a.getDataLimite() != null && a.getStatus() != TipoStatusAssunto.CONCLUIDO) {
            long dias = LocalDate.now().until(a.getDataLimite()).getDays();
            String txtData;
            String corStyle;
            if (dias < 0)       { txtData = "⏰ Vencido";                               corStyle = "-fx-text-fill:#C04040;"; }
            else if (dias == 0) { txtData = "⏰ Hoje!";                                 corStyle = "-fx-text-fill:#D4820A;"; }
            else if (dias <= 3) { txtData = "⏰ " + dias + "d";                         corStyle = "-fx-text-fill:#D4A840;"; }
            else                { txtData = "⏰ " + a.getDataLimite().format(FMT_DATA); corStyle = "-fx-text-fill:#6060A0;"; }
            Label lblData = new Label(txtData);
            lblData.setStyle("-fx-font-size: 11px; " + corStyle);
            row1.getChildren().add(lblData);
        }

        // Linha 2: nome do assunto
        Label lblNome = new Label(a.getNome());
        lblNome.getStyleClass().add("plano-assunto-nome");
        lblNome.setWrapText(true);
        if (a.getStatus() == TipoStatusAssunto.CONCLUIDO)
            lblNome.setStyle("-fx-strikethrough: true; -fx-opacity: 0.55;");

        // Linha 3: barra de progresso de sessões
        double pct    = a.getSessoesMinimas() > 0
                ? Math.min(1.0, (double) a.getSessoesRealizadas() / a.getSessoesMinimas()) : 0.0;
        int    pctInt = (int) Math.round(pct * 100);

        ProgressBar bar = new ProgressBar(pct);
        bar.getStyleClass().add("plano-progress-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        if (pct >= 1.0)    bar.getStyleClass().add("plano-progress-concluido");
        else if (pct > 0)  bar.getStyleClass().add("plano-progress-andamento");

        HBox row3 = new HBox(8);
        row3.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label lblSessoes = new Label(
                a.getSessoesRealizadas() + "/" + a.getSessoesMinimas()
                + " 🍅 (" + pctInt + "%)  •  " + formatarTempo(duracaoSeg));
        lblSessoes.getStyleClass().add("plano-assunto-sessoes");
        row3.getChildren().addAll(bar, lblSessoes);

        // Linha 4: botões de ação
        HBox row4 = new HBox(8);
        row4.setAlignment(Pos.CENTER_RIGHT);

        boolean discArquivada = disciplinaSelecionada != null && disciplinaSelecionada.isArquivado();
        if (!discArquivada) {
            Button btnEstudar = new Button("▶  Estudar agora");
            btnEstudar.getStyleClass().add("btn-primary");
            btnEstudar.setOnAction(e -> handleEstudarAssunto(a));
            btnEstudar.setTooltip(new Tooltip("Abrir o Pomodoro com este assunto selecionado"));
            row4.getChildren().add(btnEstudar);
        }

        Button btnMenu = new Button("⋯");
        btnMenu.getStyleClass().add("btn-action");
        btnMenu.setOnAction(e -> mostrarMenuAssunto(btnMenu, a, discArquivada));
        row4.getChildren().add(btnMenu);

        card.getChildren().addAll(row1, lblNome, row3, row4);
        return card;
    }

    /**
     * Exibe o menu de contexto de um assunto.
     *
     * @param anchor      botão âncora para posicionar o menu
     * @param a           assunto alvo
     * @param discArquivada {@code true} se a disciplina pai está arquivada (omite ações de edição/estudo)
     */
    private void mostrarMenuAssunto(Button anchor, Assunto a, boolean discArquivada) {
        ContextMenu menu = new ContextMenu();

        MenuItem historico = new MenuItem("📊  Histórico de sessões");
        historico.setOnAction(e -> mostrarHistoricoAssunto(a));
        menu.getItems().add(historico);

        if (!discArquivada) {
            MenuItem editar  = new MenuItem("✎  Editar");
            editar.setOnAction(e -> mostrarDialogoAssunto(a, null));
            MenuItem estudar = new MenuItem("▶  Estudar agora");
            estudar.setOnAction(e -> handleEstudarAssunto(a));
            menu.getItems().addAll(editar, estudar, new SeparatorMenuItem());

            if (a.getStatus() == TipoStatusAssunto.CONCLUIDO) {
                MenuItem reabrir = new MenuItem("↩  Reabrir assunto");
                reabrir.setOnAction(ev -> handleReabrirAssunto(a));
                menu.getItems().add(reabrir);
            } else {
                MenuItem concluir = new MenuItem("✓  Marcar como concluído");
                concluir.setOnAction(ev -> handleMarcarConcluido(a));
                menu.getItems().add(concluir);
            }
        }

        MenuItem excluir = new MenuItem("✕  Excluir");
        excluir.setOnAction(e -> handleExcluirAssunto(a));
        menu.getItems().addAll(new SeparatorMenuItem(), excluir);

        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    /** Oculta o painel direito (estado sem disciplina selecionada). */
    private void mostrarPainelVazio() {
        headerAssuntos.setVisible(false); headerAssuntos.setManaged(false);
        scrollAssuntos.setVisible(false); scrollAssuntos.setManaged(false);
        painelVazio.setVisible(true);     painelVazio.setManaged(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HANDLERS DE DISCIPLINA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Exibe o diálogo de renomeação de uma disciplina.
     *
     * @param d disciplina a renomear
     */
    private void handleRenomearDisciplina(Disciplina d) {
        TextInputDialog dlg = new TextInputDialog(d.getNome());
        dlg.setTitle("Renomear Disciplina");
        dlg.setHeaderText("Renomear: " + d.getNome());
        dlg.setContentText("Novo nome:");
        ThemeManager.getInstance().aplicarTemaAoDialogo(dlg);
        dlg.showAndWait().ifPresent(nome -> {
            if (nome.isBlank() || nome.equals(d.getNome())) return;
            new Thread(() -> {
                try {
                    service.renomearDisciplina(d.getId(), nome);
                    Platform.runLater(this::carregarDados);
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro(ex.getMessage()));
                }
            }).start();
        });
    }

    /**
     * Arquiva a disciplina, ocultando-a do painel principal.
     *
     * @param d disciplina a arquivar
     */
    private void handleArquivarDisciplina(Disciplina d) {
        new Thread(() -> {
            try {
                service.arquivarDisciplina(d.getId());
                Platform.runLater(() -> {
                    if (disciplinaSelecionada != null && disciplinaSelecionada.getId() == d.getId()) {
                        disciplinaSelecionada = null;
                        mostrarPainelVazio();
                    }
                    carregarDados();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro(ex.getMessage()));
            }
        }).start();
    }

    /**
     * Desarquiva a disciplina, tornando-a visível no painel principal.
     *
     * @param d disciplina a desarquivar
     */
    private void handleDesarquivarDisciplina(Disciplina d) {
        new Thread(() -> {
            try {
                service.desarquivarDisciplina(d.getId());
                Platform.runLater(this::carregarDados);
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro(ex.getMessage()));
            }
        }).start();
    }

    /**
     * Exibe confirmação e exclui a disciplina com todos os seus assuntos.
     *
     * @param d disciplina a excluir
     */
    private void handleExcluirDisciplina(Disciplina d) {
        int qtd = assuntosPorDisciplina.getOrDefault(d.getId(), List.of()).size();
        String msg = qtd > 0
                ? "Excluir \"" + d.getNome() + "\" e seus " + qtd + " assunto(s)?"
                : "Excluir a disciplina \"" + d.getNome() + "\"?";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir Disciplina");
        confirm.setHeaderText(null);
        confirm.setContentText(msg);
        ThemeManager.getInstance().aplicarTemaAoDialogo(confirm);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            boolean eraSelecionada = disciplinaSelecionada != null
                    && disciplinaSelecionada.getId() == d.getId();
            new Thread(() -> {
                try {
                    service.excluirDisciplina(d.getId());
                    Platform.runLater(() -> {
                        if (eraSelecionada) {
                            disciplinaSelecionada = null;
                            mostrarPainelVazio();
                        }
                        carregarDados();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro(ex.getMessage()));
                }
            }).start();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HANDLERS DE ASSUNTO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Delega para {@link #estudarAssunto(Assunto)} ao clicar em "▶ Estudar agora".
     *
     * @param a assunto escolhido pelo usuário
     */
    private void handleEstudarAssunto(Assunto a) {
        estudarAssunto(a);
    }

    /**
     * Marca o assunto como concluído.
     *
     * @param a assunto a concluir
     */
    private void handleMarcarConcluido(Assunto a) {
        new Thread(() -> {
            try {
                service.marcarConcluido(a.getId());
                Platform.runLater(this::carregarDados);
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro(ex.getMessage()));
            }
        }).start();
    }

    /**
     * Reabre um assunto marcado como concluído.
     *
     * @param a assunto a reabrir
     */
    private void handleReabrirAssunto(Assunto a) {
        new Thread(() -> {
            try {
                service.reabrirAssunto(a.getId());
                Platform.runLater(this::carregarDados);
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro(ex.getMessage()));
            }
        }).start();
    }

    /**
     * Exibe confirmação e exclui o assunto.
     *
     * @param a assunto a excluir
     */
    private void handleExcluirAssunto(Assunto a) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir Assunto");
        confirm.setHeaderText(null);
        confirm.setContentText("Excluir o assunto \"" + a.getNome() + "\"?");
        ThemeManager.getInstance().aplicarTemaAoDialogo(confirm);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            new Thread(() -> {
                try {
                    service.excluirAssunto(a.getId());
                    Platform.runLater(this::carregarDados);
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro(ex.getMessage()));
                }
            }).start();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DIÁLOGO DE ASSUNTO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Exibe o diálogo de criação ou edição de um assunto.
     *
     * @param existente assunto a editar, ou {@code null} para criar um novo
     * @param presel    disciplina pré-selecionada no combo, ou {@code null} para usar a selecionada
     */
    private void mostrarDialogoAssunto(Assunto existente, Disciplina presel) {
        boolean editando = existente != null;

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editando ? "Editar Assunto" : "Novo Assunto");
        dialog.setHeaderText(editando ? "Editar: " + existente.getNome() : "Adicionar novo assunto");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ThemeManager.getInstance().aplicarTemaAoDialogo(dialog);

        TextField tfNome = new TextField(editando ? existente.getNome() : "");
        tfNome.setPromptText("Nome do assunto");

        ComboBox<Disciplina> cbDisciplina = new ComboBox<>();
        cbDisciplina.getItems().addAll(disciplinas);
        Disciplina discInic = presel != null ? presel : disciplinaSelecionada;
        if (discInic != null) cbDisciplina.setValue(discInic);
        else if (!disciplinas.isEmpty()) cbDisciplina.setValue(disciplinas.get(0));
        if (editando) cbDisciplina.setDisable(true);

        ComboBox<TipoDificuldade> cbDificuldade = new ComboBox<>();
        cbDificuldade.getItems().addAll(TipoDificuldade.values());
        cbDificuldade.setValue(editando ? existente.getDificuldade() : TipoDificuldade.MEDIO);

        Spinner<Integer> spinSessoes = new Spinner<>(1, 99,
                editando ? existente.getSessoesMinimas() : TipoDificuldade.MEDIO.getSessoesDefault());
        spinSessoes.setEditable(true);
        spinSessoes.setPrefWidth(80);

        if (!editando) {
            cbDificuldade.setOnAction(e -> {
                TipoDificuldade dif = cbDificuldade.getValue();
                if (dif != null) spinSessoes.getValueFactory().setValue(dif.getSessoesDefault());
            });
        }

        DatePicker dpLimite = new DatePicker(editando ? existente.getDataLimite() : null);
        dpLimite.setPromptText("Opcional — data da avaliação");

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(new Label("Disciplina:"),      0, 0); grid.add(cbDisciplina,  1, 0);
        grid.add(new Label("Nome:"),            0, 1); grid.add(tfNome,        1, 1);
        grid.add(new Label("Dificuldade:"),     0, 2); grid.add(cbDificuldade, 1, 2);
        grid.add(new Label("Sessões mínimas:"), 0, 3); grid.add(spinSessoes,   1, 3);
        grid.add(new Label("Data limite:"),     0, 4); grid.add(dpLimite,      1, 4);

        GridPane.setHgrow(tfNome,        Priority.ALWAYS);
        GridPane.setHgrow(cbDisciplina,  Priority.ALWAYS);
        GridPane.setHgrow(cbDificuldade, Priority.ALWAYS);
        GridPane.setHgrow(dpLimite,      Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setMinWidth(400);
        Platform.runLater(tfNome::requestFocus);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String nome = tfNome.getText().trim();
            if (nome.isBlank()) { mostrarErro("O nome do assunto é obrigatório."); return; }
            Disciplina disc = cbDisciplina.getValue();
            if (disc == null) { mostrarErro("Selecione uma disciplina."); return; }
            TipoDificuldade dif    = cbDificuldade.getValue();
            int             sessMin = spinSessoes.getValue();
            LocalDate       limite  = dpLimite.getValue();

            new Thread(() -> {
                try {
                    if (editando) {
                        service.editarAssunto(existente.getId(), nome, dif, sessMin, limite);
                    } else {
                        service.criarAssunto(disc.getId(), nome, dif, sessMin, limite);
                        Platform.runLater(() -> disciplinaSelecionada = disc);
                    }
                    Platform.runLater(this::carregarDados);
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro(ex.getMessage()));
                }
            }).start();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HISTÓRICO DE SESSÕES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Carrega e exibe o histórico de sessões de foco de um assunto em um diálogo.
     *
     * @param a assunto cujo histórico será exibido
     */
    private void mostrarHistoricoAssunto(Assunto a) {
        new Thread(() -> {
            try {
                List<String[]> sessoes = service.buscarHistoricoPorAssunto(a.getId());
                Platform.runLater(() -> exibirDialogoHistorico(
                        "📊 Histórico — " + a.getNome(), sessoes, false));
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro(ex.getMessage()));
            }
        }).start();
    }

    /**
     * Carrega e exibe o histórico de sessões de foco de todos os assuntos de uma disciplina.
     *
     * @param d disciplina cujo histórico será exibido
     */
    private void mostrarHistoricoDisciplina(Disciplina d) {
        new Thread(() -> {
            try {
                List<String[]> sessoes = service.buscarHistoricoPorDisciplina(d.getId());
                Platform.runLater(() -> exibirDialogoHistorico(
                        "📊 Histórico — " + d.getNome(), sessoes, true));
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro(ex.getMessage()));
            }
        }).start();
    }

    /**
     * Exibe um diálogo com o histórico de sessões de foco formatado em lista.
     *
     * @param titulo     título do diálogo
     * @param sessoes    lista de sessões; cada elemento é {@code [concluido_em, duracao_segundos]}
     *                   ou {@code [concluido_em, duracao_segundos, assunto_nome]} se {@code comAssunto = true}
     * @param comAssunto {@code true} para exibir a coluna do nome do assunto (modo disciplina)
     */
    private void exibirDialogoHistorico(String titulo, List<String[]> sessoes, boolean comAssunto) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(titulo);
        dialog.setHeaderText(null);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ThemeManager.getInstance().aplicarTemaAoDialogo(dialog);

        VBox corpo = new VBox(10);
        corpo.setPadding(new Insets(16));

        if (sessoes.isEmpty()) {
            Label vazio = new Label("Nenhuma sessão de foco registrada ainda.");
            vazio.getStyleClass().add("plano-vazio-hint");
            corpo.getChildren().add(vazio);
        } else {
            int totalSeg = sessoes.stream().mapToInt(s -> Integer.parseInt(s[1])).sum();
            Label lblStats = new Label(
                    sessoes.size() + " sessão(ões)  •  " + formatarTempo(totalSeg) + " de foco no total");
            lblStats.getStyleClass().add("plano-hist-total");
            corpo.getChildren().add(lblStats);
            corpo.getChildren().add(new Separator());

            LocalDate hoje  = LocalDate.now();
            LocalDate ontem = hoje.minusDays(1);

            for (String[] s : sessoes) {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.getStyleClass().add("plano-hist-row");

                Label ico = new Label("🍅");

                String dataLabel;
                try {
                    LocalDateTime ldt        = LocalDateTime.parse(s[0].replace(" ", "T"));
                    LocalDate     dataSessao = ldt.toLocalDate();
                    String        horaStr    = ldt.format(DateTimeFormatter.ofPattern("HH:mm"));
                    if      (dataSessao.isEqual(hoje))  dataLabel = "Hoje  " + horaStr;
                    else if (dataSessao.isEqual(ontem)) dataLabel = "Ontem  " + horaStr;
                    else                                dataLabel = ldt.format(FMT_DATA_HORA);
                } catch (Exception ex) { dataLabel = s[0]; }

                Label lblData = new Label(dataLabel);
                lblData.getStyleClass().add("plano-hist-data");
                lblData.setMinWidth(145);

                Label lblDuracao = new Label(formatarTempo(Integer.parseInt(s[1])));
                lblDuracao.getStyleClass().add("plano-hist-duracao");
                lblDuracao.setMinWidth(65);

                row.getChildren().addAll(ico, lblData, lblDuracao);

                if (comAssunto && s.length > 2 && s[2] != null) {
                    Label lblAssunto = new Label(s[2]);
                    lblAssunto.getStyleClass().add("plano-hist-assunto");
                    HBox.setHgrow(lblAssunto, Priority.ALWAYS);
                    row.getChildren().add(lblAssunto);
                }

                corpo.getChildren().add(row);
            }
        }

        ScrollPane scroll = new ScrollPane(corpo);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(420);
        scroll.setPrefWidth(comAssunto ? 520 : 400);
        scroll.getStyleClass().add("plano-hist-scroll");

        dialog.getDialogPane().setContent(scroll);
        dialog.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITÁRIOS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Formata uma duração em segundos para o formato legível "Xh Ymin" ou "Y min".
     *
     * @param segundos duração em segundos
     * @return string formatada
     */
    private String formatarTempo(int segundos) {
        if (segundos == 0) return "0 min";
        int h = segundos / 3600;
        int m = (segundos % 3600) / 60;
        if (h > 0) return h + "h " + m + "min";
        return m + " min";
    }

    private String getStatusLabel(TipoStatusAssunto status) {
        return switch (status) {
            case PENDENTE     -> "○  Pendente";
            case EM_ANDAMENTO -> "◑  Em Andamento";
            case CONCLUIDO    -> "✓  Concluído";
        };
    }

    private String getStatusChipClass(TipoStatusAssunto status) {
        return switch (status) {
            case PENDENTE     -> "plano-chip-pendente";
            case EM_ANDAMENTO -> "plano-chip-andamento";
            case CONCLUIDO    -> "plano-chip-concluido";
        };
    }

    private String getDificuldadeChipClass(TipoDificuldade dif) {
        return switch (dif) {
            case FACIL         -> "plano-chip-facil";
            case MEDIO         -> "plano-chip-medio";
            case DIFICIL       -> "plano-chip-dificil";
            case MUITO_DIFICIL -> "plano-chip-muito-dificil";
        };
    }

    /**
     * Exibe um alerta de erro ao usuário.
     *
     * @param msg mensagem de erro
     */
    private void mostrarErro(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erro");
        a.setHeaderText(null);
        a.setContentText(msg);
        ThemeManager.getInstance().aplicarTemaAoDialogo(a);
        a.showAndWait();
    }
}
