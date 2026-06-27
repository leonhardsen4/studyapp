package com.leonhardsen.studyapp.controller;

import com.leonhardsen.studyapp.model.*;
import com.leonhardsen.studyapp.service.PomodoroService;
import com.leonhardsen.studyapp.util.SessionManager;
import com.leonhardsen.studyapp.util.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/**
 * Controlador do módulo Plano de Estudos.
 * Exibe uma visão consolidada de disciplinas e assuntos com progresso de sessões
 * Pomodoro, estatísticas de tempo estudado e integração com o timer Pomodoro.
 */
public class PlanoEstudosController {

    // ── FXML — painel esquerdo ────────────────────────────────────────────────
    @FXML private VBox    listaDisciplinas;
    @FXML private Button  btnNovoAssunto;

    // ── FXML — painel direito ────────────────────────────────────────────────
    @FXML private VBox       painelVazio;
    @FXML private VBox       headerAssuntos;
    @FXML private Label      lblNomeDisciplina;
    @FXML private Label      lblResumoDisc;
    @FXML private ScrollPane scrollAssuntos;
    @FXML private VBox       listaAssuntos;

    // ── Dados ─────────────────────────────────────────────────────────────────
    private final PomodoroService service = new PomodoroService();
    private List<Disciplina>              disciplinas          = new ArrayList<>();
    private Map<Integer, List<Assunto>>   assuntosPorDisciplina = new HashMap<>();
    private Map<Integer, Integer>         duracoesDisciplinas  = new HashMap<>();
    private Map<Integer, Integer>         duracoesAssuntos     = new HashMap<>();
    private Disciplina                    disciplinaSelecionada = null;

    /** Callback acionado quando o usuário clica em "Estudar agora". */
    private Consumer<Assunto> onEstudarAssunto;

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yy");

    // ─────────────────────────────────────────────────────────────────────────
    // INICIALIZAÇÃO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inicializa o controlador: carrega disciplinas, assuntos e estatísticas de sessões.
     */
    @FXML
    public void initialize() {
        carregarDados();
    }

    /**
     * Define o callback invocado ao clicar em "Estudar agora" em um assunto.
     * Normalmente definido pelo {@link MainController} para navegar ao Pomodoro
     * e pré-selecionar o assunto.
     *
     * @param callback consumidor que recebe o assunto escolhido
     */
    public void setOnEstudarAssunto(Consumer<Assunto> callback) {
        this.onEstudarAssunto = callback;
    }

    /**
     * Recarrega os dados ao navegar de volta para este módulo.
     */
    public void atualizarView() {
        carregarDados();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CARREGAMENTO DE DADOS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Carrega disciplinas, assuntos e durações de sessões em background e atualiza a UI.
     */
    private void carregarDados() {
        int uid = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                List<Disciplina> discs = service.buscarDisciplinas(uid);
                Map<Integer, List<Assunto>> mapaAssuntos  = new HashMap<>();
                Map<Integer, Integer>       duracDisc     = new HashMap<>();
                Map<Integer, Integer>       duracAssunto  = new HashMap<>();

                for (Disciplina d : discs) {
                    List<Assunto> assuntos = service.buscarAssuntos(d.getId());
                    mapaAssuntos.put(d.getId(), assuntos);
                    duracDisc.put(d.getId(), service.somarDuracaoPorDisciplina(d.getId()));
                    for (Assunto a : assuntos) {
                        duracAssunto.put(a.getId(), service.somarDuracaoPorAssunto(a.getId()));
                    }
                }

                Platform.runLater(() -> {
                    disciplinas          = discs;
                    assuntosPorDisciplina = mapaAssuntos;
                    duracoesDisciplinas  = duracDisc;
                    duracoesAssuntos     = duracAssunto;

                    // Sincroniza a referência da disciplina selecionada
                    if (disciplinaSelecionada != null) {
                        final int idSel = disciplinaSelecionada.getId();
                        disciplinaSelecionada = discs.stream()
                                .filter(d -> d.getId() == idSel)
                                .findFirst().orElse(null);
                    }

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

    /**
     * Reconstrói a lista de disciplinas no painel esquerdo.
     */
    private void recarregarListaDisciplinas() {
        listaDisciplinas.getChildren().clear();
        btnNovoAssunto.setDisable(disciplinaSelecionada == null);

        if (disciplinas.isEmpty()) {
            Label hint = new Label("Clique em '+ Disciplina' para começar.");
            hint.getStyleClass().add("plano-sidebar-vazio");
            hint.setPadding(new Insets(16));
            hint.setWrapText(true);
            listaDisciplinas.getChildren().add(hint);
            return;
        }

        for (Disciplina d : disciplinas) {
            listaDisciplinas.getChildren().add(criarCardDisciplina(d));
        }
    }

    /**
     * Constrói o card de uma disciplina para o painel esquerdo.
     *
     * @param d disciplina a ser exibida
     * @return nó {@link VBox} representando o card
     */
    private VBox criarCardDisciplina(Disciplina d) {
        List<Assunto> assuntos  = assuntosPorDisciplina.getOrDefault(d.getId(), List.of());
        long concluidos = assuntos.stream().filter(a -> a.getStatus() == TipoStatusAssunto.CONCLUIDO).count();
        double pct      = assuntos.isEmpty() ? 0.0 : (double) concluidos / assuntos.size();
        int segundos    = duracoesDisciplinas.getOrDefault(d.getId(), 0);
        boolean sel     = disciplinaSelecionada != null && disciplinaSelecionada.getId() == d.getId();

        VBox card = new VBox(5);
        card.getStyleClass().add("plano-disc-card");
        if (sel) card.getStyleClass().add("plano-disc-card-selecionado");

        // Linha superior: nome + botão menu
        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblNome = new Label(d.getNome());
        lblNome.getStyleClass().add("plano-disc-nome");
        HBox.setHgrow(lblNome, Priority.ALWAYS);

        Button btnMenu = new Button("⋯");
        btnMenu.getStyleClass().add("btn-action");
        btnMenu.setOnAction(e -> {
            ContextMenu menu = new ContextMenu();
            MenuItem renomear    = new MenuItem("✎  Renomear");
            renomear.setOnAction(ev -> handleRenomearDisciplina(d));
            MenuItem novoAssunto = new MenuItem("＋  Novo Assunto");
            novoAssunto.setOnAction(ev -> mostrarDialogoAssunto(null, d));
            MenuItem excluir     = new MenuItem("✕  Excluir Disciplina");
            excluir.setOnAction(ev -> handleExcluirDisciplina(d));
            menu.getItems().addAll(renomear, novoAssunto, new SeparatorMenuItem(), excluir);
            menu.show(btnMenu, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        header.getChildren().addAll(lblNome, btnMenu);

        // Barra de progresso (% assuntos concluídos)
        ProgressBar progressBar = new ProgressBar(pct);
        progressBar.getStyleClass().add("plano-progress-bar");
        progressBar.setMaxWidth(Double.MAX_VALUE);
        if (pct >= 1.0 && !assuntos.isEmpty())
            progressBar.getStyleClass().add("plano-progress-concluido");
        else if (pct > 0)
            progressBar.getStyleClass().add("plano-progress-andamento");

        // Estatísticas
        String tempoStr = formatarTempo(segundos);
        Label lblStats = new Label(concluidos + "/" + assuntos.size() + " assuntos  •  " + tempoStr);
        lblStats.getStyleClass().add("plano-disc-stats");

        card.getChildren().addAll(header, progressBar, lblStats);
        card.setOnMouseClicked(e -> selecionarDisciplina(d));
        return card;
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
        btnNovoAssunto.setDisable(false);
        recarregarListaDisciplinas();
        recarregarAssuntos();
    }

    /**
     * Reconstrói a lista de assuntos da disciplina selecionada no painel direito.
     */
    private void recarregarAssuntos() {
        if (disciplinaSelecionada == null) { mostrarPainelVazio(); return; }

        List<Assunto> assuntos = assuntosPorDisciplina.getOrDefault(disciplinaSelecionada.getId(), List.of());
        long concluidos  = assuntos.stream().filter(a -> a.getStatus() == TipoStatusAssunto.CONCLUIDO).count();
        int  totalSeg    = duracoesDisciplinas.getOrDefault(disciplinaSelecionada.getId(), 0);

        lblNomeDisciplina.setText(disciplinaSelecionada.getNome());
        lblResumoDisc.setText(concluidos + "/" + assuntos.size()
                + " assuntos concluídos  •  " + formatarTempo(totalSeg) + " de estudo no total");

        headerAssuntos.setVisible(true);   headerAssuntos.setManaged(true);
        painelVazio.setVisible(false);     painelVazio.setManaged(false);
        scrollAssuntos.setVisible(true);   scrollAssuntos.setManaged(true);

        listaAssuntos.getChildren().clear();

        if (assuntos.isEmpty()) {
            VBox vazio = new VBox(10);
            vazio.setAlignment(Pos.CENTER);
            vazio.setPadding(new Insets(40));
            Label ico  = new Label("📖");
            ico.setStyle("-fx-font-size: 36px;");
            Label msg  = new Label("Nenhum assunto nesta disciplina.");
            msg.getStyleClass().add("plano-vazio-titulo");
            Label hint = new Label("Use '+ Assunto' para adicionar um assunto.");
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
     * @param a         assunto a ser exibido
     * @param duracaoSeg total de segundos de foco registrados para este assunto
     * @return nó {@link VBox} representando o card
     */
    private VBox criarCardAssunto(Assunto a, int duracaoSeg) {
        VBox card = new VBox(8);
        card.getStyleClass().add("plano-assunto-card");

        // ── Linha 1: chips de status e dificuldade + data limite ──────────────
        HBox row1 = new HBox(6);
        row1.setAlignment(Pos.CENTER_LEFT);

        Label chipStatus = new Label(getStatusLabel(a.getStatus()));
        chipStatus.getStyleClass().addAll("plano-chip", getStatusChipClass(a.getStatus()));

        Label chipDif = new Label(a.getDificuldade().getLabel());
        chipDif.getStyleClass().addAll("plano-chip", getDificuldadeChipClass(a.getDificuldade()));

        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        row1.getChildren().addAll(chipStatus, chipDif, spacer1);

        if (a.getDataLimite() != null && a.getStatus() != TipoStatusAssunto.CONCLUIDO) {
            long dias = LocalDate.now().until(a.getDataLimite()).getDays();
            String txtData;
            String corStyle;
            if (dias < 0)       { txtData = "⏰ Vencido";                          corStyle = "-fx-text-fill:#C04040;"; }
            else if (dias == 0) { txtData = "⏰ Hoje!";                             corStyle = "-fx-text-fill:#D4820A;"; }
            else if (dias <= 3) { txtData = "⏰ " + dias + "d";                    corStyle = "-fx-text-fill:#D4A840;"; }
            else                { txtData = "⏰ " + a.getDataLimite().format(FMT_DATA); corStyle = "-fx-text-fill:#6060A0;"; }
            Label lblData = new Label(txtData);
            lblData.setStyle("-fx-font-size: 11px; " + corStyle);
            row1.getChildren().add(lblData);
        }

        // ── Linha 2: nome do assunto ──────────────────────────────────────────
        Label lblNome = new Label(a.getNome());
        lblNome.getStyleClass().add("plano-assunto-nome");
        lblNome.setWrapText(true);
        if (a.getStatus() == TipoStatusAssunto.CONCLUIDO)
            lblNome.setStyle("-fx-strikethrough: true; -fx-opacity: 0.55;");

        // ── Linha 3: barra de progresso de sessões ────────────────────────────
        double pct    = a.getSessoesMinimas() > 0
                ? Math.min(1.0, (double) a.getSessoesRealizadas() / a.getSessoesMinimas()) : 0.0;
        int    pctInt = (int) Math.round(pct * 100);

        ProgressBar bar = new ProgressBar(pct);
        bar.getStyleClass().add("plano-progress-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        if (pct >= 1.0)  bar.getStyleClass().add("plano-progress-concluido");
        else if (pct > 0) bar.getStyleClass().add("plano-progress-andamento");

        HBox row3 = new HBox(8);
        row3.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(bar, Priority.ALWAYS);

        Label lblSessoes = new Label(
                a.getSessoesRealizadas() + "/" + a.getSessoesMinimas() + " 🍅 ("
                + pctInt + "%)  •  " + formatarTempo(duracaoSeg));
        lblSessoes.getStyleClass().add("plano-assunto-sessoes");

        row3.getChildren().addAll(bar, lblSessoes);

        // ── Linha 4: botões de ação ───────────────────────────────────────────
        HBox row4 = new HBox(8);
        row4.setAlignment(Pos.CENTER_RIGHT);

        Button btnEstudar = new Button("▶  Estudar agora");
        btnEstudar.getStyleClass().add("btn-primary");
        btnEstudar.setOnAction(e -> handleEstudarAssunto(a));
        btnEstudar.setTooltip(new Tooltip("Abrir o Pomodoro com este assunto selecionado"));

        Button btnMenu = new Button("⋯");
        btnMenu.getStyleClass().add("btn-action");
        btnMenu.setOnAction(e -> mostrarMenuAssunto(btnMenu, a));

        row4.getChildren().addAll(btnEstudar, btnMenu);

        card.getChildren().addAll(row1, lblNome, row3, row4);
        return card;
    }

    /**
     * Exibe o menu de contexto de um assunto com opções de editar, concluir/reabrir e excluir.
     *
     * @param anchor botão âncora para posicionar o menu
     * @param a      assunto alvo
     */
    private void mostrarMenuAssunto(Button anchor, Assunto a) {
        ContextMenu menu = new ContextMenu();

        MenuItem editar    = new MenuItem("✎  Editar");
        editar.setOnAction(e -> mostrarDialogoAssunto(a, null));

        MenuItem estudar   = new MenuItem("▶  Estudar agora");
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

        MenuItem excluir = new MenuItem("✕  Excluir");
        excluir.setOnAction(e -> handleExcluirAssunto(a));
        menu.getItems().addAll(new SeparatorMenuItem(), excluir);

        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    /**
     * Exibe o painel vazio (estado sem disciplina selecionada).
     */
    private void mostrarPainelVazio() {
        headerAssuntos.setVisible(false);  headerAssuntos.setManaged(false);
        scrollAssuntos.setVisible(false);  scrollAssuntos.setManaged(false);
        painelVazio.setVisible(true);      painelVazio.setManaged(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HANDLERS DE DISCIPLINA
    // ─────────────────────────────────────────────────────────────────────────

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
     * Invoca o callback de navegação para o Pomodoro com o assunto selecionado.
     *
     * @param a assunto a ser estudado
     */
    private void handleEstudarAssunto(Assunto a) {
        if (onEstudarAssunto != null) onEstudarAssunto.accept(a);
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
                TipoDificuldade d = cbDificuldade.getValue();
                if (d != null) spinSessoes.getValueFactory().setValue(d.getSessoesDefault());
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

        GridPane.setHgrow(tfNome, Priority.ALWAYS);
        GridPane.setHgrow(cbDisciplina, Priority.ALWAYS);
        GridPane.setHgrow(cbDificuldade, Priority.ALWAYS);
        GridPane.setHgrow(dpLimite, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setMinWidth(400);
        Platform.runLater(tfNome::requestFocus);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String nome = tfNome.getText().trim();
            if (nome.isBlank()) { mostrarErro("O nome do assunto é obrigatório."); return; }
            Disciplina disc = cbDisciplina.getValue();
            if (disc == null) { mostrarErro("Selecione uma disciplina."); return; }
            TipoDificuldade dif   = cbDificuldade.getValue();
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
    // UTILITÁRIOS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Formata uma duração em segundos para o formato legível "Xh Ymin" ou "Y min".
     *
     * @param segundos duração em segundos
     * @return string formatada
     */
    private String formatarTempo(int segundos) {
        int h = segundos / 3600;
        int m = (segundos % 3600) / 60;
        if (segundos == 0) return "0 min";
        if (h > 0) return h + "h " + m + "min";
        return m + " min";
    }

    /**
     * Retorna o rótulo textual do status do assunto.
     *
     * @param status status do assunto
     * @return rótulo com ícone
     */
    private String getStatusLabel(TipoStatusAssunto status) {
        return switch (status) {
            case PENDENTE     -> "○  Pendente";
            case EM_ANDAMENTO -> "◑  Em Andamento";
            case CONCLUIDO    -> "✓  Concluído";
        };
    }

    /**
     * Retorna a classe CSS do chip de status do assunto.
     *
     * @param status status do assunto
     * @return nome da classe CSS
     */
    private String getStatusChipClass(TipoStatusAssunto status) {
        return switch (status) {
            case PENDENTE     -> "plano-chip-pendente";
            case EM_ANDAMENTO -> "plano-chip-andamento";
            case CONCLUIDO    -> "plano-chip-concluido";
        };
    }

    /**
     * Retorna a classe CSS do chip de dificuldade do assunto.
     *
     * @param dif nível de dificuldade
     * @return nome da classe CSS
     */
    private String getDificuldadeChipClass(TipoDificuldade dif) {
        return switch (dif) {
            case FACIL        -> "plano-chip-facil";
            case MEDIO        -> "plano-chip-medio";
            case DIFICIL      -> "plano-chip-dificil";
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
