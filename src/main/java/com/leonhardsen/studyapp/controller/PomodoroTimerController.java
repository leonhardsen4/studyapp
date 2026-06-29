package com.leonhardsen.studyapp.controller;

import com.leonhardsen.studyapp.model.*;
import com.leonhardsen.studyapp.service.PomodoroService;
import com.leonhardsen.studyapp.database.DatabaseManager;
import com.leonhardsen.studyapp.util.FormatadorData;
import com.leonhardsen.studyapp.util.SessionManager;
import com.leonhardsen.studyapp.util.ThemeManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
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
import java.time.LocalDateTime;
import java.util.Properties;

/**
 * Controlador do painel de timer Pomodoro incorporado ao módulo Plano de Estudos.
 *
 * <p>Gerencia o temporizador com ciclos de foco e pausas, registra sessões concluídas
 * e suporta abertura em janela separada (destacar/reintegrar). Diferente do antigo
 * {@code PomodoroController}, esta versão não possui lista de disciplinas: o assunto
 * a estudar é injetado pelo {@code PlanoEstudosController} via
 * {@link #selecionarAssunto(Assunto)}.
 *
 * <p>Callbacks disponíveis:
 * <ul>
 *   <li>{@link #setOnEncerrar(Runnable)} — chamado quando o usuário fecha o painel.</li>
 *   <li>{@link #setOnSessaoConcluida(Runnable)} — chamado após cada sessão de foco,
 *       para que o painel do Plano recarregue os dados.</li>
 * </ul>
 */
public class PomodoroTimerController {

    // ── Campos FXML ───────────────────────────────────────────────────────────
    @FXML private BorderPane container;
    @FXML private BorderPane raiz;
    @FXML private Button     btnDetachar;
    @FXML private Label      lblEstudando;
    @FXML private Label      lblTimer;
    @FXML private HBox       hboxCiclo;
    @FXML private Button     btnFoco;
    @FXML private Button     btnPausaCurta;
    @FXML private Button     btnPausaLonga;
    @FXML private Button     btnIniciarPausar;
    @FXML private Button     btnReiniciar;
    @FXML private Button     btnPular;
    @FXML private Label      lblSessoesHoje;
    @FXML private Label      lblTempoHoje;

    // ── Janela destacada ──────────────────────────────────────────────────────
    private Stage janelaDestacada;

    // ── Estado do timer ───────────────────────────────────────────────────────
    private Timeline      timerTimeline;
    private TipoSessao    faseAtual            = TipoSessao.FOCO;
    private int           segundosRestantes;
    private int           sessoesNoCiclo       = 0;
    private boolean       rodando              = false;
    private LocalDateTime inicioDaSessaoAtual;

    // ── Configurações de duração (em minutos) ─────────────────────────────────
    private int minutosFoco       = 25;
    private int minutosPausaCurta = 5;
    private int minutosPausaLonga = 15;

    private static final String PREFS_PATH =
            DatabaseManager.getDirApp() + "/pomodoro.properties";

    // ── Serviço e dados ───────────────────────────────────────────────────────
    private final PomodoroService service = new PomodoroService();
    private Assunto assuntoSelecionado = null;

    // ── Callbacks ─────────────────────────────────────────────────────────────
    private Runnable onEncerrar;
    private Runnable onSessaoConcluida;

    // ─────────────────────────────────────────────────────────────────────────
    // INICIALIZAÇÃO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inicializa o controlador: carrega configurações salvas, configura o timeline,
     * inicia a fase de foco e carrega as estatísticas do dia.
     */
    @FXML
    public void initialize() {
        carregarConfiguracoes();
        configurarTimer();
        reiniciarFase(TipoSessao.FOCO);
        atualizarCiclo();
        atualizarEstatsHoje();
    }

    /** Cria o {@link Timeline} de contagem regressiva com tiques de 1 segundo. */
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

    /**
     * Para o timer, redefine os segundos restantes para a fase indicada
     * e atualiza a interface para o estado "parado".
     *
     * @param fase fase a reiniciar (FOCO, PAUSA_CURTA ou PAUSA_LONGA)
     */
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

    /**
     * Inicia automaticamente uma nova fase (chamado ao terminar a fase anterior).
     *
     * @param fase fase a iniciar automaticamente
     */
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

    /**
     * Executado quando a contagem chega a zero. Se era fase de foco, registra a
     * sessão no banco e notifica via {@code onSessaoConcluida}; depois inicia a
     * pausa correspondente. Se era pausa, inicia nova sessão de foco.
     */
    private void onFaseCompleta() {
        timerTimeline.stop();
        rodando = false;

        if (faseAtual == TipoSessao.FOCO) {
            LocalDateTime fim    = LocalDateTime.now();
            LocalDateTime inicio = inicioDaSessaoAtual != null
                    ? inicioDaSessaoAtual : fim.minusSeconds(getSegundosFase(TipoSessao.FOCO));
            int duracao   = getSegundosFase(TipoSessao.FOCO);
            Integer assuntoId = assuntoSelecionado != null ? assuntoSelecionado.getId() : null;

            new Thread(() -> {
                try {
                    int uid = SessionManager.getInstance().getUsuarioLogado().getId();
                    service.registrarSessaoFoco(uid, assuntoId, inicio, fim, duracao);
                    Assunto atualizado = assuntoId != null ? service.incrementarSessao(assuntoId) : null;
                    Platform.runLater(() -> {
                        if (atualizado != null) {
                            assuntoSelecionado = atualizado;
                            verificarMetaAssunto(atualizado);
                        }
                        atualizarEstatsHoje();
                        if (onSessaoConcluida != null) onSessaoConcluida.run();
                    });
                } catch (Exception ignored) {}
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

    /** Alterna entre iniciar, pausar e retomar o timer. */
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

    /** Reinicia a fase atual sem avançar no ciclo. */
    @FXML private void handleReiniciar()       { reiniciarFase(faseAtual); }

    /** Muda manualmente para a fase de foco. */
    @FXML private void handleFaseFoco()        { reiniciarFase(TipoSessao.FOCO); }

    /** Muda manualmente para a fase de pausa curta. */
    @FXML private void handleFasePausaCurta()  { reiniciarFase(TipoSessao.PAUSA_CURTA); }

    /** Muda manualmente para a fase de pausa longa. */
    @FXML private void handleFasePausaLonga()  { reiniciarFase(TipoSessao.PAUSA_LONGA); }

    /**
     * Pula a fase atual: se for foco, avança o contador do ciclo; se for pausa,
     * reinicia diretamente na fase de foco.
     */
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

    /**
     * Para o timer e dispara o callback {@code onEncerrar} para que o
     * {@code PlanoEstudosController} remova o painel do {@code SplitPane}.
     */
    @FXML
    private void handleEncerrar() {
        if (timerTimeline != null) timerTimeline.stop();
        rodando = false;
        if (onEncerrar != null) onEncerrar.run();
    }

    // ── Helpers de UI ─────────────────────────────────────────────────────────

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
            case FOCO        -> btnFoco.getStyleClass().add("pomo-fase-btn-ativo");
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
    // META DE ASSUNTO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Verifica se o assunto atingiu a meta de sessões. Se sim, exibe diálogo
     * perguntando se deve ser marcado como concluído.
     *
     * @param a assunto recém atualizado após uma sessão de foco
     */
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
                            Platform.runLater(() -> {
                                assuntoSelecionado = null;
                                lblEstudando.setText("Selecione um assunto");
                                if (onSessaoConcluida != null) onSessaoConcluida.run();
                            });
                        } catch (Exception ignored) {}
                    }).start();
                }
            });
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DIÁLOGO DE CONFIGURAÇÃO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Abre diálogo para ajustar a duração das sessões (foco, pausa curta, pausa longa).
     * Salva as novas configurações e reinicia a fase atual.
     */
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
    // ESTATÍSTICAS DO DIA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Carrega em background o número de sessões e o tempo total de hoje
     * e atualiza as labels correspondentes.
     */
    private void atualizarEstatsHoje() {
        int uid = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                int sessoes  = service.contarSessoesHoje(uid);
                int segundos = service.somarDuracaoHoje(uid);
                String tempoStr = FormatadorData.formatarTempo(segundos);
                Platform.runLater(() -> {
                    lblSessoesHoje.setText(sessoes + " sessão" + (sessoes != 1 ? "ões" : ""));
                    lblTempoHoje.setText(tempoStr);
                });
            } catch (Exception ignored) {}
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONFIGURAÇÕES PERSISTIDAS
    // ─────────────────────────────────────────────────────────────────────────

    /** Carrega as durações salvas em {@code ~/.studyapp/pomodoro.properties}. */
    private void carregarConfiguracoes() {
        File f = new File(PREFS_PATH);
        if (!f.exists()) return;
        try (var in = new FileInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            minutosFoco       = Integer.parseInt(p.getProperty("foco",        "25"));
            minutosPausaCurta = Integer.parseInt(p.getProperty("pausa_curta", "5"));
            minutosPausaLonga = Integer.parseInt(p.getProperty("pausa_longa", "15"));
        } catch (Exception ignored) {}
    }

    /** Persiste as durações em {@code ~/.studyapp/pomodoro.properties}. */
    private void salvarConfiguracoes() {
        try (var out = new FileOutputStream(PREFS_PATH)) {
            Properties p = new Properties();
            p.setProperty("foco",        String.valueOf(minutosFoco));
            p.setProperty("pausa_curta", String.valueOf(minutosPausaCurta));
            p.setProperty("pausa_longa", String.valueOf(minutosPausaLonga));
            p.store(out, null);
        } catch (Exception ignored) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DESTACAR / REINTEGRAR
    // ─────────────────────────────────────────────────────────────────────────

    /** Alterna entre destacar o timer em janela flutuante e reintegrá-lo ao painel. */
    @FXML
    private void handleDetachar() {
        if (janelaDestacada != null) reatachar();
        else destacar();
    }

    /**
     * Move o {@code raiz} (conteúdo do timer) para uma nova janela flutuante
     * e exibe um placeholder no container original.
     */
    private void destacar() {
        Scene cenaPrincipal = raiz.getScene();

        VBox placeholder = new VBox(12);
        placeholder.setAlignment(Pos.CENTER);
        Label ico = new Label("🍅");
        ico.setStyle("-fx-font-size: 40px;");
        Label msg = new Label("Timer aberto em janela separada");
        msg.setStyle("-fx-text-fill: #9090A8; -fx-font-size: 13px;");
        msg.setTextAlignment(TextAlignment.CENTER);
        placeholder.getChildren().addAll(ico, msg);
        container.setCenter(placeholder);

        Scene cenaSep = new Scene(raiz, 480, 520);
        cenaSep.getStylesheets().addAll(cenaPrincipal.getStylesheets());

        janelaDestacada = new Stage();
        janelaDestacada.setTitle("🍅  Pomodoro — StudyApp");
        janelaDestacada.setAlwaysOnTop(true);
        janelaDestacada.setScene(cenaSep);
        janelaDestacada.setOnCloseRequest(e -> { e.consume(); reatachar(); });

        btnDetachar.setText("↙ Reintegrar");
        janelaDestacada.show();
    }

    /**
     * Devolve o {@code raiz} para o container original e fecha a janela flutuante.
     */
    private void reatachar() {
        if (janelaDestacada == null) return;
        janelaDestacada.getScene().setRoot(new Region());
        container.setCenter(raiz);
        btnDetachar.setText("↗");
        janelaDestacada.close();
        janelaDestacada = null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // API PÚBLICA
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registra o callback chamado quando o usuário encerra a sessão (botão "✕ Encerrar").
     *
     * @param callback ação a executar ao encerrar (ex.: remover painel do SplitPane)
     */
    public void setOnEncerrar(Runnable callback) {
        this.onEncerrar = callback;
    }

    /**
     * Registra o callback chamado após cada sessão de foco concluída.
     *
     * @param callback ação a executar (ex.: recarregar dados no Plano de Estudos)
     */
    public void setOnSessaoConcluida(Runnable callback) {
        this.onSessaoConcluida = callback;
    }

    /**
     * Define o assunto a ser estudado e atualiza o cabeçalho do timer.
     * Chamado pelo {@code PlanoEstudosController} quando o usuário clica em "▶ Estudar".
     *
     * @param assunto assunto selecionado no Plano de Estudos
     */
    public void selecionarAssunto(Assunto assunto) {
        assuntoSelecionado = assunto;
        lblEstudando.setText("Estudando: " + assunto.getNome());
    }

    /**
     * Para o timeline do timer e fecha a janela destacada (se aberta).
     * Deve ser chamado pelo {@code PlanoEstudosController} ao encerrar o aplicativo.
     */
    public void pararRecursos() {
        if (timerTimeline != null) timerTimeline.stop();
        if (janelaDestacada != null) { janelaDestacada.close(); janelaDestacada = null; }
    }
}
