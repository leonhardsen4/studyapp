package com.leonhardsen.studyapp.controller;

import com.leonhardsen.studyapp.model.Evento;
import com.leonhardsen.studyapp.service.EventoService;
import com.leonhardsen.studyapp.util.FormatadorData;
import com.leonhardsen.studyapp.util.SessionManager;
import com.leonhardsen.studyapp.util.ThemeManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AgendaController {

    @FXML private Label      lblMesAno;
    @FXML private GridPane   gridCalendario;
    @FXML private VBox       painelDia;
    @FXML private Label      lblDiaSelecionado;
    @FXML private VBox       listEventosDia;
    @FXML private Button     btnNovoEvento;
    @FXML private TextField  tfBusca;
    @FXML private Button     btnLimparBusca;

    private final EventoService eventoService = new EventoService();
    private YearMonth mesAtual       = YearMonth.now();
    private LocalDate dataSelecionada = LocalDate.now();
    private final Map<LocalDate, List<Evento>> eventosPorData = new HashMap<>();
    private Runnable onEventoAlterado;

    private PauseTransition pausaBusca;
    private Popup           popupBusca;

    private static final DateTimeFormatter FMT_HORA_CURTA = DateTimeFormatter.ofPattern("H:mm");

    @FXML
    public void initialize() {
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            cc.setPercentWidth(100.0 / 7.0);
            gridCalendario.getColumnConstraints().add(cc);
        }

        // Busca com debounce de 350ms
        pausaBusca = new PauseTransition(Duration.millis(350));
        pausaBusca.setOnFinished(e -> executarBusca());

        tfBusca.textProperty().addListener((obs, ant, nov) -> {
            boolean temTexto = !nov.isBlank();
            btnLimparBusca.setVisible(temTexto);
            btnLimparBusca.setManaged(temTexto);
            if (!temTexto || nov.trim().length() < 2) {
                fecharPopupBusca();
            } else {
                pausaBusca.playFromStart();
            }
        });

        carregarMes();
    }

    public void setOnEventoAlterado(Runnable callback) {
        this.onEventoAlterado = callback;
    }

    public void atualizarView() {
        carregarMes();
    }

    /** Navega para a data especificada e, se necessário, troca o mês exibido. */
    public void selecionarDia(LocalDate data) {
        YearMonth mes = YearMonth.from(data);
        if (!mes.equals(mesAtual)) {
            mesAtual = mes;
        }
        dataSelecionada = data;
        carregarMes();
    }

    // ── Navegação ────────────────────────────────────────────────

    @FXML private void handleMesAnterior() { fecharPopupBusca(); mesAtual = mesAtual.minusMonths(1); carregarMes(); }
    @FXML private void handleMesProximo()  { fecharPopupBusca(); mesAtual = mesAtual.plusMonths(1);  carregarMes(); }

    @FXML
    private void handleHoje() {
        fecharPopupBusca();
        mesAtual       = YearMonth.now();
        dataSelecionada = LocalDate.now();
        carregarMes();
    }

    @FXML
    private void handleLimparBusca() {
        tfBusca.clear();
        fecharPopupBusca();
        tfBusca.requestFocus();
    }

    // ── Busca por título ─────────────────────────────────────────

    private void executarBusca() {
        String termo = tfBusca.getText().trim();
        if (termo.length() < 2) { fecharPopupBusca(); return; }
        int uid = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                List<Evento> resultados = eventoService.buscarPorTitulo(uid, termo);
                Platform.runLater(() -> mostrarResultadosBusca(resultados));
            } catch (Exception ex) {
                // falha silenciosa — busca não é crítica
            }
        }).start();
    }

    private void mostrarResultadosBusca(List<Evento> resultados) {
        fecharPopupBusca();

        VBox conteudo = new VBox(0);
        conteudo.getStyleClass().add("agenda-busca-popup");
        conteudo.getStylesheets().addAll(tfBusca.getScene().getStylesheets());
        conteudo.setPrefWidth(360);
        conteudo.setMaxWidth(360);

        if (resultados.isEmpty()) {
            Label lblVazio = new Label("Nenhum evento encontrado.");
            lblVazio.getStyleClass().add("agenda-busca-vazio");
            conteudo.getChildren().add(lblVazio);
        } else {
            VBox lista = new VBox(0);
            for (Evento ev : resultados) {
                lista.getChildren().add(criarLinhaResultadoBusca(ev));
            }
            ScrollPane scroll = new ScrollPane(lista);
            scroll.setFitToWidth(true);
            scroll.setMaxHeight(280);
            scroll.getStyleClass().add("agenda-busca-scroll");
            conteudo.getChildren().add(scroll);
        }

        popupBusca = new Popup();
        popupBusca.setAutoHide(true);
        popupBusca.getContent().add(conteudo);

        Bounds b = tfBusca.localToScreen(tfBusca.getBoundsInLocal());
        popupBusca.show(tfBusca.getScene().getWindow(), b.getMinX(), b.getMaxY() + 2);
    }

    private HBox criarLinhaResultadoBusca(Evento ev) {
        HBox row = new HBox(10);
        row.getStyleClass().add("agenda-busca-resultado");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 14, 8, 14));

        Label lblData = new Label(FormatadorData.formatarDataCurta(ev.getData()));
        lblData.getStyleClass().add("agenda-busca-data");
        lblData.setMinWidth(82);

        Label lblTitulo = new Label(ev.getTitulo());
        lblTitulo.getStyleClass().add("agenda-busca-titulo");
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);

        Label lblHora = new Label(ev.getHoraInicio() != null
                ? ev.getHoraInicio().format(FMT_HORA_CURTA) : "");
        lblHora.getStyleClass().add("agenda-busca-hora");

        row.getChildren().addAll(lblData, lblTitulo, lblHora);
        row.setStyle("-fx-cursor: hand;");
        row.setOnMouseClicked(e -> {
            fecharPopupBusca();
            tfBusca.clear();
            selecionarDia(ev.getData());
        });

        return row;
    }

    private void fecharPopupBusca() {
        if (pausaBusca != null) pausaBusca.stop();
        if (popupBusca != null && popupBusca.isShowing()) popupBusca.hide();
        popupBusca = null;
    }

    // ── Carregamento de dados ─────────────────────────────────────

    private void carregarMes() {
        int uid = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                List<Evento> eventos = eventoService.buscarPorMes(uid, mesAtual.getYear(), mesAtual.getMonthValue());
                Platform.runLater(() -> {
                    eventosPorData.clear();
                    for (Evento ev : eventos) {
                        eventosPorData.computeIfAbsent(ev.getData(), k -> new ArrayList<>()).add(ev);
                    }
                    construirGrid();
                    mostrarEventosDia(dataSelecionada);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro ao carregar agenda: " + ex.getMessage()));
            }
        }).start();
    }

    // ── Construção da grade do calendário ────────────────────────

    private void construirGrid() {
        gridCalendario.getChildren().clear();
        gridCalendario.getRowConstraints().clear();

        LocalDate primeiroDia  = mesAtual.atDay(1);
        int       col0         = primeiroDia.getDayOfWeek().getValue() % 7; // 0=Dom..6=Sáb
        int       diasNoMes    = mesAtual.lengthOfMonth();
        int       numSemanas   = (int) Math.ceil((col0 + diasNoMes) / 7.0);
        LocalDate startDate    = primeiroDia.minusDays(col0);
        LocalDate hoje         = LocalDate.now();

        for (int i = 0; i < numSemanas; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setFillHeight(true);
            rc.setMinHeight(80);
            gridCalendario.getRowConstraints().add(rc);
        }

        lblMesAno.setText(FormatadorData.formatarMesAno(mesAtual));

        for (int sem = 0; sem < numSemanas; sem++) {
            for (int col = 0; col < 7; col++) {
                LocalDate data       = startDate.plusDays((long) sem * 7 + col);
                boolean   mesCorrente = YearMonth.from(data).equals(mesAtual);
                gridCalendario.add(criarCelula(data, mesCorrente, hoje), col, sem);
            }
        }
    }

    private VBox criarCelula(LocalDate data, boolean mesCorrente, LocalDate hoje) {
        VBox celula = new VBox(2);
        celula.setPadding(new Insets(5, 4, 4, 4));
        celula.setMaxWidth(Double.MAX_VALUE);
        celula.setMaxHeight(Double.MAX_VALUE);
        celula.getStyleClass().add("agenda-celula");

        if (!mesCorrente)          celula.getStyleClass().add("agenda-celula-outro-mes");
        if (data.equals(hoje))     celula.getStyleClass().add("agenda-celula-hoje");
        if (data.equals(dataSelecionada)) celula.getStyleClass().add("agenda-celula-selecionada");

        // Número do dia
        Label lblDia = new Label(String.valueOf(data.getDayOfMonth()));
        lblDia.getStyleClass().add("agenda-num-dia");
        if (data.equals(hoje)) lblDia.getStyleClass().add("agenda-num-dia-hoje");

        HBox diaBox = new HBox(lblDia);
        diaBox.setAlignment(Pos.CENTER_RIGHT);
        celula.getChildren().add(diaBox);

        // Chips de eventos (máximo 3 + indicador de excesso)
        List<Evento> eventos = eventosPorData.getOrDefault(data, Collections.emptyList());
        int mostrar = Math.min(eventos.size(), 3);
        for (int i = 0; i < mostrar; i++) {
            Label chip = new Label(eventos.get(i).getTitulo());
            chip.getStyleClass().add("agenda-chip");
            chip.setMaxWidth(Double.MAX_VALUE);
            chip.setMinWidth(0);
            chip.setPrefWidth(1);
            celula.getChildren().add(chip);
        }
        if (eventos.size() > 3) {
            Label mais = new Label("+" + (eventos.size() - 3) + " mais");
            mais.getStyleClass().add("agenda-chip-mais");
            celula.getChildren().add(mais);
        }

        final LocalDate dataFinal = data;
        celula.setOnMouseClicked(e -> selecionarDiaNaGrid(dataFinal));
        celula.setStyle("-fx-cursor: hand;");

        return celula;
    }

    private void selecionarDiaNaGrid(LocalDate data) {
        if (!YearMonth.from(data).equals(mesAtual)) {
            mesAtual       = YearMonth.from(data);
            dataSelecionada = data;
            carregarMes();
        } else {
            dataSelecionada = data;
            construirGrid();
            mostrarEventosDia(data);
        }
    }

    // ── Painel de eventos do dia ──────────────────────────────────

    private void mostrarEventosDia(LocalDate data) {
        if (data == null) return;
        lblDiaSelecionado.setText(FormatadorData.formatarDataLonga(data));
        listEventosDia.getChildren().clear();

        List<Evento> eventos = eventosPorData.getOrDefault(data, Collections.emptyList());
        if (eventos.isEmpty()) {
            Label vazio = new Label("Nenhum evento neste dia.\nClique em \"+ Evento\" para adicionar.");
            vazio.getStyleClass().add("agenda-vazio");
            vazio.setWrapText(true);
            listEventosDia.getChildren().add(vazio);
        } else {
            for (Evento ev : eventos) {
                listEventosDia.getChildren().add(criarCardEvento(ev));
            }
        }
    }

    private VBox criarCardEvento(Evento ev) {
        VBox card = new VBox(5);
        card.getStyleClass().add("agenda-evento-card");
        card.setMaxWidth(Double.MAX_VALUE);

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);

        String textoHorario;
        if (ev.getHoraInicio() != null) {
            textoHorario = "🕐 " + ev.getHoraInicio().format(FMT_HORA_CURTA);
            if (ev.getHoraFim() != null) {
                textoHorario += " – " + ev.getHoraFim().format(FMT_HORA_CURTA);
            }
        } else {
            textoHorario = "📅 Dia inteiro";
        }
        Label lblHorario = new Label(textoHorario);
        lblHorario.getStyleClass().add("agenda-evento-horario");
        HBox.setHgrow(lblHorario, Priority.ALWAYS);

        Button btnEditar = new Button("✎");
        btnEditar.getStyleClass().add("btn-action");
        btnEditar.setStyle("-fx-padding: 2 8 2 8;");
        btnEditar.setOnAction(e -> mostrarDialogoEvento(ev));

        Button btnExcluir = new Button("✕");
        btnExcluir.getStyleClass().add("btn-action");
        btnExcluir.setStyle("-fx-padding: 2 8 2 8; -fx-text-fill: #c0392b;");
        btnExcluir.setOnAction(e -> confirmarExclusao(ev));

        header.getChildren().addAll(lblHorario, btnEditar, btnExcluir);

        Label lblTitulo = new Label(ev.getTitulo());
        lblTitulo.getStyleClass().add("agenda-evento-titulo");
        lblTitulo.setWrapText(true);
        lblTitulo.setMaxWidth(Double.MAX_VALUE);

        card.getChildren().addAll(header, lblTitulo);

        if (!ev.getDescricao().isBlank()) {
            Label lblDesc = new Label(ev.getDescricao());
            lblDesc.getStyleClass().add("agenda-evento-descricao");
            lblDesc.setWrapText(true);
            lblDesc.setMaxWidth(Double.MAX_VALUE);
            card.getChildren().add(lblDesc);
        }

        return card;
    }

    // ── Novo evento ───────────────────────────────────────────────

    @FXML
    private void handleNovoEvento() {
        mostrarDialogoEvento(null);
    }

    // ── Diálogo criar/editar evento ───────────────────────────────

    private void mostrarDialogoEvento(Evento original) {
        boolean criando = (original == null);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(criando ? "Novo Evento" : "Editar Evento");
        dialog.setHeaderText(null);
        dialog.initOwner(gridCalendario.getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ThemeManager.getInstance().aplicarTemaAoDialogo(dialog);

        TextField tfTitulo = new TextField(criando ? "" : original.getTitulo());
        tfTitulo.setPromptText("Título do evento (obrigatório)");

        DatePicker dpData = new DatePicker(criando ? dataSelecionada : original.getData());
        dpData.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate d) { return d != null ? FormatadorData.formatarDataCurta(d) : ""; }
            @Override public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) return null;
                try { return LocalDate.parse(s.trim(), FormatadorData.DATA_CURTA); } catch (Exception e) { return null; }
            }
        });

        TextField tfInicio = new TextField(
            (!criando && original.getHoraInicio() != null) ? original.getHoraInicio().format(FMT_HORA_CURTA) : "");
        tfInicio.setPromptText("HH:mm  (opcional)");

        TextField tfFim = new TextField(
            (!criando && original.getHoraFim() != null) ? original.getHoraFim().format(FMT_HORA_CURTA) : "");
        tfFim.setPromptText("HH:mm  (opcional)");

        TextArea taDesc = new TextArea(criando ? "" : original.getDescricao());
        taDesc.setPromptText("Descrição (opcional)");
        taDesc.setPrefRowCount(3);
        taDesc.setWrapText(true);

        Label lblErro = new Label();
        lblErro.getStyleClass().add("error-label");
        lblErro.setVisible(false);
        lblErro.setManaged(false);
        lblErro.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new Insets(16));

        ColumnConstraints cLabels = new ColumnConstraints();
        cLabels.setMinWidth(85);
        cLabels.setHgrow(Priority.NEVER);
        ColumnConstraints cFields = new ColumnConstraints();
        cFields.setHgrow(Priority.ALWAYS);
        cFields.setFillWidth(true);
        grid.getColumnConstraints().addAll(cLabels, cFields);

        grid.add(new Label("Título:"),    0, 0);  grid.add(tfTitulo, 1, 0);
        grid.add(new Label("Data:"),      0, 1);  grid.add(dpData,   1, 1);
        grid.add(new Label("Início:"),    0, 2);  grid.add(tfInicio, 1, 2);
        grid.add(new Label("Fim:"),       0, 3);  grid.add(tfFim,    1, 3);
        grid.add(new Label("Descrição:"), 0, 4);  grid.add(taDesc,   1, 4);
        grid.add(lblErro, 0, 5, 2, 1);

        GridPane.setHgrow(tfTitulo, Priority.ALWAYS);
        GridPane.setHgrow(dpData,   Priority.ALWAYS);
        GridPane.setHgrow(tfInicio, Priority.ALWAYS);
        GridPane.setHgrow(tfFim,    Priority.ALWAYS);
        GridPane.setHgrow(taDesc,   Priority.ALWAYS);
        GridPane.setHgrow(lblErro,  Priority.ALWAYS);

        dialog.getDialogPane().setPrefWidth(520);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(tfTitulo::requestFocus);

        // Validação inline — impede fechamento se há erro
        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.addEventFilter(ActionEvent.ACTION, evt -> {
            LocalTime inicio = parseHora(tfInicio.getText());
            LocalTime fim    = parseHora(tfFim.getText());
            String erroMsg   = null;

            if (tfTitulo.getText().isBlank()) {
                erroMsg = "O título é obrigatório.";
            } else if (dpData.getValue() == null) {
                erroMsg = "A data é obrigatória.";
            } else if (!tfInicio.getText().isBlank() && inicio == null) {
                erroMsg = "Horário de início inválido. Use o formato H:mm (ex.: 9:30 ou 14:00).";
            } else if (!tfFim.getText().isBlank() && fim == null) {
                erroMsg = "Horário de fim inválido. Use o formato H:mm (ex.: 10:30 ou 15:00).";
            } else if (!tfFim.getText().isBlank() && tfInicio.getText().isBlank()) {
                erroMsg = "Informe o horário de início antes do horário de fim.";
            } else if (inicio != null && fim != null && !fim.isAfter(inicio)) {
                erroMsg = "O horário de fim deve ser posterior ao horário de início.";
            }

            if (erroMsg != null) {
                lblErro.setText(erroMsg);
                lblErro.setVisible(true);
                lblErro.setManaged(true);
                evt.consume();
            }
        });

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            Evento ev = criando ? new Evento() : original;
            ev.setTitulo(tfTitulo.getText().trim());
            ev.setData(dpData.getValue());
            ev.setHoraInicio(parseHora(tfInicio.getText()));
            ev.setHoraFim(parseHora(tfFim.getText()));
            ev.setDescricao(taDesc.getText().trim());
            if (criando) ev.setUsuarioId(SessionManager.getInstance().getUsuarioLogado().getId());

            new Thread(() -> {
                try {
                    if (criando) eventoService.criarEvento(ev);
                    else         eventoService.atualizarEvento(ev);

                    YearMonth mesDoEvento = YearMonth.from(ev.getData());
                    Platform.runLater(() -> {
                        if (mesDoEvento.equals(mesAtual)) {
                            carregarMes();
                        } else {
                            mesAtual        = mesDoEvento;
                            dataSelecionada = ev.getData();
                            carregarMes();
                        }
                        if (onEventoAlterado != null) onEventoAlterado.run();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro("Erro ao salvar evento: " + ex.getMessage()));
                }
            }).start();
        });
    }

    private void confirmarExclusao(Evento ev) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir Evento");
        confirm.setHeaderText(null);
        confirm.setContentText("Excluir o evento \"" + ev.getTitulo() + "\"?");
        ThemeManager.getInstance().aplicarTemaAoDialogo(confirm);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            new Thread(() -> {
                try {
                    eventoService.excluirEvento(ev.getId());
                    Platform.runLater(() -> {
                        carregarMes();
                        if (onEventoAlterado != null) onEventoAlterado.run();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro("Erro ao excluir evento: " + ex.getMessage()));
                }
            }).start();
        });
    }

    // ── Utilitários ───────────────────────────────────────────────

    private LocalTime parseHora(String texto) {
        if (texto == null || texto.isBlank()) return null;
        try {
            return LocalTime.parse(texto.trim(), DateTimeFormatter.ofPattern("H:mm"));
        } catch (Exception e) {
            return null;
        }
    }

    private void mostrarErro(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        ThemeManager.getInstance().aplicarTemaAoDialogo(alert);
        alert.showAndWait();
    }
}
