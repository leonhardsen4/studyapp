package com.leonhardsen.studyapp.controller;

import com.leonhardsen.studyapp.model.*;
import com.leonhardsen.studyapp.service.PomodoroService;
import com.leonhardsen.studyapp.util.SessionManager;
import com.leonhardsen.studyapp.util.ThemeManager;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controlador do módulo Pomodoro.
 * Gerencia o temporizador Pomodoro com ciclos de foco e pausas,
 * a lista de disciplinas/assuntos e o registro de sessões concluídas.
 * As durações das sessões são configuráveis e persistidas em arquivo de propriedades.
 */
public class PomodoroController {

    // ── Raiz / container (destacar) ───────────────────────────────────────────
    @FXML private BorderPane container;
    @FXML private SplitPane  raiz;
    @FXML private Button     btnDetachar;
    private Stage janelaDestacada;

    // ── Painel esquerdo ───────────────────────────────────────────────────────
    @FXML private Button  btnNovoAssunto;
    @FXML private VBox    listaDisciplinas;
    @FXML private Label   lblSessoesHoje;
    @FXML private Label   lblTempoHoje;

    // ── Painel direito (timer) ────────────────────────────────────────────────
    @FXML private Button btnFoco;
    @FXML private Button btnPausaCurta;
    @FXML private Button btnPausaLonga;
    @FXML private Label  lblEstudando;
    @FXML private Label  lblTimer;
    @FXML private HBox   hboxCiclo;
    @FXML private Button btnIniciarPausar;
    @FXML private Button btnReiniciar;
    @FXML private Button btnPular;

    // ── Estado do timer ───────────────────────────────────────────────────────
    private Timeline timerTimeline;
    private TipoSessao faseAtual = TipoSessao.FOCO;
    private int segundosRestantes;
    private int sessoesNoCiclo = 0;
    private boolean rodando = false;
    private LocalDateTime inicioDaSessaoAtual;

    // ── Configurações (minutos) ───────────────────────────────────────────────
    private int minutosFoco       = 25;
    private int minutosPausaCurta = 5;
    private int minutosPausaLonga = 15;

    // ── Dados ─────────────────────────────────────────────────────────────────
    private final PomodoroService service = new PomodoroService();
    private List<Disciplina> disciplinas = new ArrayList<>();
    private Map<Integer, List<Assunto>> assuntosPorDisciplina = new HashMap<>();
    private final Set<Integer> expandidas = new HashSet<>();
    private Assunto assuntoSelecionado = null;

    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yy");
    private static final String PREFS_PATH =
            System.getProperty("user.home") + "/.studyapp/pomodoro.properties";

    // ─────────────────────────────────────────────────────────────────────────
    // INICIALIZAÇÃO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inicializa o controlador: carrega configurações, configura o timeline do timer,
     * inicia a fase de foco e carrega disciplinas/assuntos e estatísticas do dia.
     */
    @FXML
    public void initialize() {
        carregarConfiguracoes();
        configurarTimer();
        reiniciarFase(TipoSessao.FOCO);
        atualizarCiclo();
        carregarDados();
        atualizarEstatsHoje();
    }

    private void configurarTimer() {
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            segundosRestantes--;
            atualizarDisplay();
            if (segundosRestantes <= 0) onFaseCompleta();
        }));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LÓGICA DO TIMER
    // ─────────────────────────────────────────────────────────────────────────

    private void reiniciarFase(TipoSessao fase) {
        faseAtual = fase;
        segundosRestantes = getSegundosFase(fase);
        if (timerTimeline != null) timerTimeline.stop();
        rodando = false;
        inicioDaSessaoAtual = null;
        atualizarDisplay();
        atualizarBotoesFase();
        btnIniciarPausar.setText("▶  Iniciar");
        btnIniciarPausar.getStyleClass().remove("pomo-btn-pausar");
    }

    private void iniciarFaseAutomatica(TipoSessao fase) {
        faseAtual = fase;
        segundosRestantes = getSegundosFase(fase);
        atualizarDisplay();
        atualizarBotoesFase();
        inicioDaSessaoAtual = LocalDateTime.now();
        rodando = true;
        timerTimeline.play();
        btnIniciarPausar.setText("⏸  Pausar");
        if (!btnIniciarPausar.getStyleClass().contains("pomo-btn-pausar"))
            btnIniciarPausar.getStyleClass().add("pomo-btn-pausar");
    }

    private void onFaseCompleta() {
        timerTimeline.stop();
        rodando = false;

        if (faseAtual == TipoSessao.FOCO) {
            LocalDateTime fim = LocalDateTime.now();
            LocalDateTime inicio = inicioDaSessaoAtual != null
                    ? inicioDaSessaoAtual : fim.minusSeconds(getSegundosFase(TipoSessao.FOCO));
            int duracao = getSegundosFase(TipoSessao.FOCO);
            Integer assuntoId = assuntoSelecionado != null ? assuntoSelecionado.getId() : null;

            new Thread(() -> {
                try {
                    int uid = SessionManager.getInstance().getUsuarioLogado().getId();
                    service.registrarSessaoFoco(uid, assuntoId, inicio, fim, duracao);
                    Assunto atualizado = assuntoId != null ? service.incrementarSessao(assuntoId) : null;
                    Platform.runLater(() -> {
                        if (atualizado != null) {
                            assuntoSelecionado = atualizado;
                            carregarDados();
                            verificarMetaAssunto(atualizado);
                        }
                        atualizarEstatsHoje();
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();

            sessoesNoCiclo++;
            atualizarCiclo();

            TipoSessao proxima;
            if (sessoesNoCiclo >= 4) {
                proxima = TipoSessao.PAUSA_LONGA;
                sessoesNoCiclo = 0;
                atualizarCiclo();
            } else {
                proxima = TipoSessao.PAUSA_CURTA;
            }
            iniciarFaseAutomatica(proxima);
        } else {
            iniciarFaseAutomatica(TipoSessao.FOCO);
        }

        soarAlarme();
    }

    @FXML
    private void handleIniciarPausar() {
        if (rodando) {
            timerTimeline.pause();
            rodando = false;
            btnIniciarPausar.setText("▶  Retomar");
            btnIniciarPausar.getStyleClass().remove("pomo-btn-pausar");
        } else {
            if (inicioDaSessaoAtual == null) inicioDaSessaoAtual = LocalDateTime.now();
            timerTimeline.play();
            rodando = true;
            btnIniciarPausar.setText("⏸  Pausar");
            if (!btnIniciarPausar.getStyleClass().contains("pomo-btn-pausar"))
                btnIniciarPausar.getStyleClass().add("pomo-btn-pausar");
        }
    }

    @FXML
    private void handleReiniciar() {
        reiniciarFase(faseAtual);
    }

    @FXML
    private void handlePular() {
        if (faseAtual == TipoSessao.FOCO) {
            sessoesNoCiclo++;
            if (sessoesNoCiclo >= 4) {
                sessoesNoCiclo = 0;
                atualizarCiclo();
                reiniciarFase(TipoSessao.PAUSA_LONGA);
            } else {
                atualizarCiclo();
                reiniciarFase(TipoSessao.PAUSA_CURTA);
            }
        } else {
            reiniciarFase(TipoSessao.FOCO);
        }
    }

    @FXML private void handleFaseFoco()      { reiniciarFase(TipoSessao.FOCO); }
    @FXML private void handleFasePausaCurta() { reiniciarFase(TipoSessao.PAUSA_CURTA); }
    @FXML private void handleFasePausaLonga() { reiniciarFase(TipoSessao.PAUSA_LONGA); }

    private void atualizarDisplay() {
        int min = Math.max(0, segundosRestantes) / 60;
        int sec = Math.max(0, segundosRestantes) % 60;
        lblTimer.setText(String.format("%02d:%02d", min, sec));
    }

    private void atualizarBotoesFase() {
        btnFoco.getStyleClass().remove("pomo-fase-btn-ativo");
        btnPausaCurta.getStyleClass().remove("pomo-fase-btn-ativo");
        btnPausaLonga.getStyleClass().remove("pomo-fase-btn-ativo");
        switch (faseAtual) {
            case FOCO       -> btnFoco.getStyleClass().add("pomo-fase-btn-ativo");
            case PAUSA_CURTA -> btnPausaCurta.getStyleClass().add("pomo-fase-btn-ativo");
            case PAUSA_LONGA -> btnPausaLonga.getStyleClass().add("pomo-fase-btn-ativo");
        }
        if (faseAtual == TipoSessao.FOCO) {
            lblTimer.getStyleClass().remove("pomo-timer-pausa");
        } else {
            if (!lblTimer.getStyleClass().contains("pomo-timer-pausa"))
                lblTimer.getStyleClass().add("pomo-timer-pausa");
        }
    }

    private void atualizarCiclo() {
        hboxCiclo.getChildren().clear();
        for (int i = 0; i < 4; i++) {
            Label dot = new Label(i < sessoesNoCiclo ? "🍅" : "○");
            dot.getStyleClass().add("pomo-ciclo-dot");
            dot.getStyleClass().add(i < sessoesNoCiclo ? "pomo-ciclo-cheio" : "pomo-ciclo-vazio");
            hboxCiclo.getChildren().add(dot);
        }
    }

    private int getSegundosFase(TipoSessao fase) {
        return switch (fase) {
            case FOCO        -> minutosFoco * 60;
            case PAUSA_CURTA -> minutosPausaCurta * 60;
            case PAUSA_LONGA -> minutosPausaLonga * 60;
        };
    }

    private void soarAlarme() {
        new Thread(() -> java.awt.Toolkit.getDefaultToolkit().beep()).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DADOS E PAINEL ESQUERDO
    // ─────────────────────────────────────────────────────────────────────────

    private void carregarDados() {
        int uid = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                List<Disciplina> discs = service.buscarDisciplinas(uid);
                Map<Integer, List<Assunto>> mapa = new HashMap<>();
                for (Disciplina d : discs) {
                    mapa.put(d.getId(), service.buscarAssuntos(d.getId()));
                }
                Platform.runLater(() -> {
                    if (expandidas.isEmpty()) discs.forEach(d -> expandidas.add(d.getId()));
                    disciplinas = discs;
                    assuntosPorDisciplina = mapa;
                    sincronizarAssuntoSelecionado();
                    recarregarListaDisciplinas();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private void sincronizarAssuntoSelecionado() {
        if (assuntoSelecionado == null) return;
        for (List<Assunto> lista : assuntosPorDisciplina.values()) {
            for (Assunto a : lista) {
                if (a.getId() == assuntoSelecionado.getId()) {
                    assuntoSelecionado = a;
                    return;
                }
            }
        }
        assuntoSelecionado = null;
        lblEstudando.setText("Nenhum assunto selecionado");
    }

    private void recarregarListaDisciplinas() {
        listaDisciplinas.getChildren().clear();
        btnNovoAssunto.setDisable(disciplinas.isEmpty());
        for (Disciplina d : disciplinas) {
            listaDisciplinas.getChildren().add(criarItemDisciplina(d));
        }
        if (disciplinas.isEmpty()) {
            Label hint = new Label("Clique em '+ Disciplina' para começar.");
            hint.getStyleClass().add("pomo-vazio-hint");
            hint.setPadding(new Insets(16));
            listaDisciplinas.getChildren().add(hint);
        }
    }

    private VBox criarItemDisciplina(Disciplina d) {
        VBox container = new VBox();
        container.getStyleClass().add("pomo-disciplina-container");

        boolean aberta = expandidas.contains(d.getId());

        HBox header = new HBox(8);
        header.getStyleClass().add("pomo-disciplina-header");
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(8, 12, 8, 12));

        Label seta = new Label(aberta ? "▼" : "▶");
        seta.getStyleClass().add("pomo-seta");
        seta.setMinWidth(14);

        Label nome = new Label(d.getNome());
        nome.getStyleClass().add("pomo-disciplina-nome");

        // Contagem de assuntos concluídos
        List<Assunto> assuntos = assuntosPorDisciplina.getOrDefault(d.getId(), List.of());
        long concluidos = assuntos.stream().filter(a -> a.getStatus() == TipoStatusAssunto.CONCLUIDO).count();
        Label progresso = new Label(concluidos + "/" + assuntos.size());
        progresso.getStyleClass().add("pomo-disciplina-progresso");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnMenu = new Button("⋯");
        btnMenu.getStyleClass().add("btn-action");
        btnMenu.setOnAction(e -> {
            ContextMenu menu = new ContextMenu();
            MenuItem renomear = new MenuItem("✎  Renomear");
            renomear.setOnAction(ev -> handleRenomearDisciplina(d));
            MenuItem novoAssunto = new MenuItem("＋  Novo Assunto");
            novoAssunto.setOnAction(ev -> handleNovoAssuntoNaDisciplina(d));
            MenuItem excluir = new MenuItem("✕  Excluir Disciplina");
            excluir.setOnAction(ev -> handleExcluirDisciplina(d));
            menu.getItems().addAll(renomear, novoAssunto, new SeparatorMenuItem(), excluir);
            menu.show(btnMenu, javafx.geometry.Side.BOTTOM, 0, 0);
        });

        header.getChildren().addAll(seta, nome, spacer, progresso, btnMenu);
        header.setOnMouseClicked(e -> {
            if (aberta) expandidas.remove(d.getId());
            else        expandidas.add(d.getId());
            recarregarListaDisciplinas();
        });

        container.getChildren().add(header);

        if (aberta) {
            if (assuntos.isEmpty()) {
                Label vazio = new Label("Nenhum assunto. Clique em '+ Assunto'.");
                vazio.getStyleClass().add("pomo-vazio-hint");
                vazio.setPadding(new Insets(4, 12, 8, 32));
                container.getChildren().add(vazio);
            } else {
                for (Assunto a : assuntos) {
                    container.getChildren().add(criarItemAssunto(a, d));
                }
            }
        }
        return container;
    }

    private HBox criarItemAssunto(Assunto a, Disciplina d) {
        HBox row = new HBox(6);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(5, 12, 5, 28));
        row.getStyleClass().add("pomo-assunto-item");
        if (assuntoSelecionado != null && assuntoSelecionado.getId() == a.getId())
            row.getStyleClass().add("pomo-assunto-selecionado");

        // Status icon
        Label iconeStatus = new Label(getStatusIcon(a.getStatus()));
        iconeStatus.getStyleClass().add("pomo-status-" + a.getStatus().name().toLowerCase());
        iconeStatus.setMinWidth(16);

        // Nome (riscado se concluído)
        Label nome = new Label(a.getNome());
        nome.getStyleClass().add("pomo-assunto-nome");
        if (a.getStatus() == TipoStatusAssunto.CONCLUIDO)
            nome.setStyle("-fx-strikethrough: true; -fx-text-fill: #606060;");

        // Data limite (se definida e não concluído)
        if (a.getDataLimite() != null && a.getStatus() != TipoStatusAssunto.CONCLUIDO) {
            long dias = LocalDate.now().until(a.getDataLimite()).getDays();
            String txtData;
            String styleExtra;
            if (dias < 0)       { txtData = "Vencido"; styleExtra = "-fx-text-fill:#C04040;"; }
            else if (dias == 0) { txtData = "Hoje!";   styleExtra = "-fx-text-fill:#D4820A;"; }
            else if (dias <= 3) { txtData = dias + "d"; styleExtra = "-fx-text-fill:#D4A840;"; }
            else                { txtData = a.getDataLimite().format(FMT_DATA); styleExtra = "-fx-text-fill:#6060A0;"; }
            Label lblData = new Label("  ⏰" + txtData);
            lblData.setStyle("-fx-font-size:10px;" + styleExtra);
            nome.setGraphic(null);
            HBox nomeBox = new HBox(4, nome, lblData);
            nomeBox.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(nomeBox, Priority.ALWAYS);
            row.getChildren().add(iconeStatus);
            row.getChildren().add(nomeBox);
        } else {
            HBox.setHgrow(nome, Priority.ALWAYS);
            row.getChildren().addAll(iconeStatus, nome);
        }

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Sessões realizadas/mínimas
        Label lblSessoes = new Label(a.getSessoesRealizadas() + "/" + a.getSessoesMinimas() + " 🍅");
        lblSessoes.getStyleClass().add("pomo-sessoes-label");
        if (a.getSessoesRealizadas() >= a.getSessoesMinimas() && a.getStatus() != TipoStatusAssunto.CONCLUIDO)
            lblSessoes.setStyle("-fx-text-fill: #60B060; -fx-font-size: 11px;");

        // Botão −
        Button btnMenos = new Button("−");
        btnMenos.getStyleClass().add("btn-action");
        btnMenos.setTooltip(new Tooltip("Remover uma sessão"));
        btnMenos.setOnAction(e -> handleAjustarSessoes(a, -1));

        // Botão +
        Button btnMais = new Button("+");
        btnMais.getStyleClass().add("btn-action");
        btnMais.setTooltip(new Tooltip("Adicionar uma sessão"));
        btnMais.setOnAction(e -> handleAjustarSessoes(a, +1));

        // Botão selecionar (▶)
        Button btnSel = new Button(assuntoSelecionado != null && assuntoSelecionado.getId() == a.getId() ? "★" : "▶");
        btnSel.getStyleClass().add("btn-action");
        btnSel.setTooltip(new Tooltip("Selecionar este assunto para o timer"));
        btnSel.setOnAction(e -> selecionarAssunto(a));

        // Menu ⋯
        Button btnMenu = new Button("⋯");
        btnMenu.getStyleClass().add("btn-action");
        btnMenu.setOnAction(e -> mostrarMenuAssunto(btnMenu, a, d));

        row.getChildren().addAll(spacer, lblSessoes, btnMenos, btnMais, btnSel, btnMenu);
        row.setOnMouseClicked(e -> selecionarAssunto(a));
        return row;
    }

    private String getStatusIcon(TipoStatusAssunto status) {
        return switch (status) {
            case PENDENTE    -> "○";
            case EM_ANDAMENTO -> "◑";
            case CONCLUIDO   -> "✓";
        };
    }

    private void mostrarMenuAssunto(Button anchor, Assunto a, Disciplina d) {
        ContextMenu menu = new ContextMenu();
        MenuItem editar = new MenuItem("✎  Editar");
        editar.setOnAction(e -> handleEditarAssunto(a, d));
        MenuItem selecionar = new MenuItem("▶  Selecionar para o timer");
        selecionar.setOnAction(e -> selecionarAssunto(a));

        menu.getItems().addAll(editar, selecionar, new SeparatorMenuItem());

        if (a.getStatus() == TipoStatusAssunto.CONCLUIDO) {
            MenuItem reabrir = new MenuItem("↩  Reabrir assunto");
            reabrir.setOnAction(e -> handleReabrirAssunto(a));
            menu.getItems().add(reabrir);
        } else {
            MenuItem concluir = new MenuItem("✓  Marcar como concluído");
            concluir.setOnAction(e -> handleMarcarConcluido(a));
            menu.getItems().add(concluir);
        }
        MenuItem excluir = new MenuItem("✕  Excluir");
        excluir.setOnAction(e -> handleExcluirAssunto(a, d));
        menu.getItems().addAll(new SeparatorMenuItem(), excluir);
        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    private void selecionarAssunto(Assunto a) {
        assuntoSelecionado = a;
        lblEstudando.setText("Estudando: " + a.getNome());
        recarregarListaDisciplinas();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HANDLERS DE DISCIPLINA
    // ─────────────────────────────────────────────────────────────────────────

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
                    expandidas.add(nova.getId());
                    Platform.runLater(this::carregarDados);
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro(ex.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleNovoAssunto() {
        handleNovoAssuntoNaDisciplina(null);
    }

    private void handleNovoAssuntoNaDisciplina(Disciplina presel) {
        if (disciplinas.isEmpty()) { mostrarErro("Crie uma disciplina primeiro."); return; }
        mostrarDialogoAssunto(null, presel);
    }

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

    private void handleExcluirDisciplina(Disciplina d) {
        int contagem = assuntosPorDisciplina.getOrDefault(d.getId(), List.of()).size();
        String msg = contagem > 0
                ? "Excluir \"" + d.getNome() + "\" e seus " + contagem + " assunto(s)?"
                : "Excluir a disciplina \"" + d.getNome() + "\"?";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir Disciplina");
        confirm.setHeaderText(null);
        confirm.setContentText(msg);
        ThemeManager.getInstance().aplicarTemaAoDialogo(confirm);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            boolean assuntoSelecionadoNaDisciplina = assuntoSelecionado != null &&
                    assuntosPorDisciplina.getOrDefault(d.getId(), List.of())
                            .stream().anyMatch(a -> a.getId() == assuntoSelecionado.getId());
            new Thread(() -> {
                try {
                    service.excluirDisciplina(d.getId());
                    expandidas.remove(d.getId());
                    if (assuntoSelecionadoNaDisciplina) {
                        assuntoSelecionado = null;
                        Platform.runLater(() -> lblEstudando.setText("Nenhum assunto selecionado"));
                    }
                    Platform.runLater(this::carregarDados);
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro(ex.getMessage()));
                }
            }).start();
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HANDLERS DE ASSUNTO
    // ─────────────────────────────────────────────────────────────────────────

    private void handleEditarAssunto(Assunto a, Disciplina d) {
        mostrarDialogoAssunto(a, d);
    }

    private void handleExcluirAssunto(Assunto a, Disciplina d) {
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
                    if (assuntoSelecionado != null && assuntoSelecionado.getId() == a.getId()) {
                        assuntoSelecionado = null;
                        Platform.runLater(() -> lblEstudando.setText("Nenhum assunto selecionado"));
                    }
                    Platform.runLater(this::carregarDados);
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro(ex.getMessage()));
                }
            }).start();
        });
    }

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

    private void handleAjustarSessoes(Assunto a, int delta) {
        new Thread(() -> {
            try {
                Assunto atualizado = service.ajustarSessoes(a.getId(), a.getSessoesRealizadas() + delta);
                Platform.runLater(() -> {
                    if (assuntoSelecionado != null && assuntoSelecionado.getId() == a.getId())
                        assuntoSelecionado = atualizado;
                    carregarDados();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro(ex.getMessage()));
            }
        }).start();
    }

    private void verificarMetaAssunto(Assunto a) {
        if (a.getSessoesRealizadas() >= a.getSessoesMinimas()
                && a.getStatus() != TipoStatusAssunto.CONCLUIDO) {
            Alert alerta = new Alert(Alert.AlertType.CONFIRMATION);
            alerta.setTitle("Meta atingida!");
            alerta.setHeaderText("🎉 " + a.getNome());
            alerta.setContentText("Você completou " + a.getSessoesMinimas() + " sessão(ões) de estudo!\n"
                    + "Deseja marcar este assunto como concluído?");
            alerta.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);
            ThemeManager.getInstance().aplicarTemaAoDialogo(alerta);
            alerta.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) {
                    new Thread(() -> {
                        try {
                            service.marcarConcluido(a.getId());
                            if (assuntoSelecionado != null && assuntoSelecionado.getId() == a.getId())
                                assuntoSelecionado = null;
                            Platform.runLater(() -> {
                                if (assuntoSelecionado == null)
                                    lblEstudando.setText("Nenhum assunto selecionado");
                                carregarDados();
                            });
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }).start();
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DIÁLOGOS
    // ─────────────────────────────────────────────────────────────────────────

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
        if (presel != null) cbDisciplina.setValue(presel);
        else if (!disciplinas.isEmpty()) cbDisciplina.setValue(disciplinas.get(0));
        if (editando) cbDisciplina.setDisable(true);

        ComboBox<TipoDificuldade> cbDificuldade = new ComboBox<>();
        cbDificuldade.getItems().addAll(TipoDificuldade.values());
        cbDificuldade.setValue(editando ? existente.getDificuldade() : TipoDificuldade.MEDIO);

        Spinner<Integer> spinSessoes = new Spinner<>(1, 99,
                editando ? existente.getSessoesMinimas() : TipoDificuldade.MEDIO.getSessoesDefault());
        spinSessoes.setEditable(true);
        spinSessoes.setPrefWidth(80);

        // Dificuldade atualiza sugestão de sessões (só se não estiver editando)
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
        grid.add(new Label("Disciplina:"),       0, 0); grid.add(cbDisciplina,  1, 0);
        grid.add(new Label("Nome:"),             0, 1); grid.add(tfNome,        1, 1);
        grid.add(new Label("Dificuldade:"),      0, 2); grid.add(cbDificuldade, 1, 2);
        grid.add(new Label("Sessões mínimas:"),  0, 3); grid.add(spinSessoes,   1, 3);
        grid.add(new Label("Data limite:"),      0, 4); grid.add(dpLimite,      1, 4);

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
            TipoDificuldade dif = cbDificuldade.getValue();
            int sessMin = spinSessoes.getValue();
            LocalDate limite = dpLimite.getValue();

            new Thread(() -> {
                try {
                    if (editando) {
                        service.editarAssunto(existente.getId(), nome, dif, sessMin, limite);
                    } else {
                        service.criarAssunto(disc.getId(), nome, dif, sessMin, limite);
                        expandidas.add(disc.getId());
                    }
                    Platform.runLater(this::carregarDados);
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro(ex.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleConfigurar() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configurar Durações");
        dialog.setHeaderText("Ajustar duração das sessões (em minutos)");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ThemeManager.getInstance().aplicarTemaAoDialogo(dialog);

        Spinner<Integer> spFoco       = new Spinner<>(1, 120, minutosFoco);
        Spinner<Integer> spPausaCurta = new Spinner<>(1, 60,  minutosPausaCurta);
        Spinner<Integer> spPausaLonga = new Spinner<>(1, 120, minutosPausaLonga);
        spFoco.setEditable(true);
        spPausaCurta.setEditable(true);
        spPausaLonga.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(new Label("Foco:"),        0, 0); grid.add(spFoco,       1, 0);
        grid.add(new Label("Pausa curta:"), 0, 1); grid.add(spPausaCurta, 1, 1);
        grid.add(new Label("Pausa longa:"), 0, 2); grid.add(spPausaLonga, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            minutosFoco       = spFoco.getValue();
            minutosPausaCurta = spPausaCurta.getValue();
            minutosPausaLonga = spPausaLonga.getValue();
            salvarConfiguracoes();
            reiniciarFase(faseAtual);
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ESTATÍSTICAS
    // ─────────────────────────────────────────────────────────────────────────

    private void atualizarEstatsHoje() {
        int uid = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                int sessoes  = service.contarSessoesHoje(uid);
                int segundos = service.somarDuracaoHoje(uid);
                String tempoStr = formatarTempo(segundos);
                Platform.runLater(() -> {
                    lblSessoesHoje.setText(sessoes + " sessão" + (sessoes != 1 ? "ões" : ""));
                    lblTempoHoje.setText(tempoStr);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    private String formatarTempo(int segundos) {
        int h = segundos / 3600;
        int m = (segundos % 3600) / 60;
        if (h > 0) return h + "h " + m + "min";
        return m + " min";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIGURAÇÕES PERSISTIDAS
    // ─────────────────────────────────────────────────────────────────────────

    private void carregarConfiguracoes() {
        File f = new File(PREFS_PATH);
        if (!f.exists()) return;
        try (var in = new FileInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            minutosFoco       = Integer.parseInt(p.getProperty("foco",       "25"));
            minutosPausaCurta = Integer.parseInt(p.getProperty("pausa_curta", "5"));
            minutosPausaLonga = Integer.parseInt(p.getProperty("pausa_longa", "15"));
        } catch (Exception ignored) {}
    }

    private void salvarConfiguracoes() {
        try (var out = new FileOutputStream(PREFS_PATH)) {
            Properties p = new Properties();
            p.setProperty("foco",       String.valueOf(minutosFoco));
            p.setProperty("pausa_curta", String.valueOf(minutosPausaCurta));
            p.setProperty("pausa_longa", String.valueOf(minutosPausaLonga));
            p.store(out, null);
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DESTACAR / REINTEGRAR
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleDetachar() {
        if (janelaDestacada != null) reatachar();
        else destacar();
    }

    private void destacar() {
        Scene cenaPrincipal = container.getScene();

        VBox placeholder = new VBox(12);
        placeholder.setAlignment(Pos.CENTER);
        Label ico = new Label("🍅");
        ico.setStyle("-fx-font-size: 40px;");
        Label msg = new Label("Pomodoro aberto em\njanela separada");
        msg.setStyle("-fx-text-fill: #9090A8; -fx-font-size: 13px;");
        msg.setTextAlignment(TextAlignment.CENTER);
        placeholder.getChildren().addAll(ico, msg);
        container.setCenter(placeholder);
        btnDetachar.setDisable(true);

        Button btnReintegrar = new Button("↙ Reintegrar");
        btnReintegrar.getStyleClass().add("btn-action");
        btnReintegrar.setOnAction(e -> reatachar());
        HBox toolbarDest = new HBox(btnReintegrar);
        toolbarDest.setAlignment(Pos.CENTER_RIGHT);
        toolbarDest.setPadding(new Insets(6, 12, 6, 12));
        toolbarDest.getStyleClass().add("module-toolbar");

        BorderPane wrapperDest = new BorderPane();
        wrapperDest.setTop(toolbarDest);
        wrapperDest.setCenter(raiz);

        Scene cenaSep = new Scene(wrapperDest, 1020, 640);
        cenaSep.getStylesheets().addAll(cenaPrincipal.getStylesheets());

        janelaDestacada = new Stage();
        janelaDestacada.setTitle("🍅  Pomodoro — StudyApp");
        janelaDestacada.setAlwaysOnTop(true);
        janelaDestacada.setScene(cenaSep);
        janelaDestacada.setOnCloseRequest(e -> { e.consume(); reatachar(); });

        janelaDestacada.show();
    }

    private void reatachar() {
        if (janelaDestacada == null) return;
        ((BorderPane) janelaDestacada.getScene().getRoot()).setCenter(new Region());
        container.setCenter(raiz);
        btnDetachar.setDisable(false);
        janelaDestacada.close();
        janelaDestacada = null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CICLO DE VIDA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Para o timeline do timer e fecha a janela destacada (se aberta).
     * Deve ser chamado ao encerrar o aplicativo ou encerrar a sessão.
     */
    public void pararRecursos() {
        if (timerTimeline != null) timerTimeline.stop();
        if (janelaDestacada != null) { janelaDestacada.close(); janelaDestacada = null; }
    }

    /**
     * Recarrega as disciplinas/assuntos e atualiza as estatísticas do dia.
     * Deve ser chamado ao navegar para esta tela.
     */
    public void atualizarView() {
        carregarDados();
        atualizarEstatsHoje();
    }

    private void mostrarErro(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erro");
        a.setHeaderText(null);
        a.setContentText(msg);
        ThemeManager.getInstance().aplicarTemaAoDialogo(a);
        a.showAndWait();
    }
}
