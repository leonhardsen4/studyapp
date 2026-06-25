package com.leonhardsen.studyapp.controller;

import com.leonhardsen.studyapp.model.*;
import com.leonhardsen.studyapp.service.TarefaService;
import com.leonhardsen.studyapp.util.SessionManager;
import com.leonhardsen.studyapp.util.ThemeManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controlador do módulo de Tarefas.
 * Exibe uma tabela com todas as tarefas do usuário, permite criar/editar/excluir
 * tarefas, gerenciar etiquetas e filtrar por texto, status ou etiqueta.
 */
public class TarefasController {

    // ── Filtros ───────────────────────────────────────────────────────────────
    @FXML private TextField tfBusca;
    @FXML private ComboBox<Object> cbFiltroStatus;
    @FXML private ComboBox<Object> cbFiltroEtiqueta;
    @FXML private Label lblContagem;

    // ── Etiquetas ─────────────────────────────────────────────────────────────
    @FXML private ListView<Etiqueta> listEtiquetas;
    @FXML private Button btnRenomearEtiqueta;
    @FXML private Button btnExcluirEtiqueta;

    // ── Tabela ────────────────────────────────────────────────────────────────
    @FXML private TableView<Tarefa> tableViewTarefas;
    @FXML private TableColumn<Tarefa, String> colStatus;
    @FXML private TableColumn<Tarefa, String> colPrioridade;
    @FXML private TableColumn<Tarefa, String> colTitulo;
    @FXML private TableColumn<Tarefa, String> colEtiquetas;
    @FXML private TableColumn<Tarefa, String> colVencimento;
    @FXML private TableColumn<Tarefa, String> colCriadoEm;
    @FXML private TableColumn<Tarefa, String> colAtualizadoEm;
    @FXML private Button btnEditarTarefa;
    @FXML private Button btnExcluirTarefa;

    // ── Dados ─────────────────────────────────────────────────────────────────
    private final TarefaService tarefaService = new TarefaService();
    private final ObservableList<Tarefa> tarefasLista = FXCollections.observableArrayList();
    private final ObservableList<Etiqueta> etiquetasLista = FXCollections.observableArrayList();
    private FilteredList<Tarefa> tarefasFiltradas;

    private static final DateTimeFormatter FMT_D  = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Integer tarefaIdPendente = null;
    private Runnable onTarefaAlterada;

    /**
     * Define o callback a ser invocado quando uma tarefa ou etiqueta for criada,
     * editada ou excluída. Permite que outras telas reajam às alterações.
     *
     * @param callback ação a executar após cada alteração
     */
    public void setOnTarefaAlterada(Runnable callback) {
        this.onTarefaAlterada = callback;
    }

    private void notificarAlteracao() {
        if (onTarefaAlterada != null) onTarefaAlterada.run();
    }

    /**
     * Inicializa o controlador: configura a tabela, filtros, lista de etiquetas,
     * atalhos de teclado e carrega os dados do usuário logado.
     */
    @FXML
    public void initialize() {
        configurarTabela();
        configurarFiltros();
        configurarListaEtiquetas();
        configurarAtalhos();
        carregarDados();
    }

    /** Chamado pelo MainController ao navegar para esta tela. */
    public void atualizarView() {
        carregarDados();
    }

    /** Seleciona a tarefa com o id informado, limpando filtros para garantir visibilidade. */
    public void selecionarTarefaPorId(int id) {
        tarefaIdPendente = id;
        tentarSelecionarPendente();
    }

    private void tentarSelecionarPendente() {
        if (tarefaIdPendente == null) return;
        int id = tarefaIdPendente;
        tfBusca.clear();
        cbFiltroStatus.setValue("Todos");
        cbFiltroEtiqueta.setValue("Todas");
        listEtiquetas.getSelectionModel().clearSelection();
        aplicarFiltro();
        for (Tarefa t : tableViewTarefas.getItems()) {
            if (t.getId() == id) {
                tableViewTarefas.getSelectionModel().select(t);
                tableViewTarefas.scrollTo(t);
                tarefaIdPendente = null;
                return;
            }
        }
    }

    // ── Configuração inicial ──────────────────────────────────────────────────

    private void configurarTabela() {
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().getDescricao()));
        colPrioridade.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPrioridade().getDescricao()));
        colTitulo.setCellValueFactory(new PropertyValueFactory<>("titulo"));
        colEtiquetas.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEtiquetasTexto()));
        colVencimento.setCellValueFactory(c -> {
            LocalDate d = c.getValue().getDataVencimento();
            return new SimpleStringProperty(d != null ? d.format(FMT_D) : "—");
        });
        colCriadoEm.setCellValueFactory(c -> {
            var dt = c.getValue().getCriadoEm();
            return new SimpleStringProperty(dt != null ? dt.format(FMT_DT) : "");
        });
        colAtualizadoEm.setCellValueFactory(c -> {
            var dt = c.getValue().getAtualizadoEm();
            return new SimpleStringProperty(dt != null ? dt.format(FMT_DT) : "");
        });

        // Coloração de células de prioridade
        colPrioridade.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item);
                getStyleClass().removeAll("cell-baixa","cell-media","cell-alta","cell-urgente");
                if (!empty && getTableRow() != null && getTableRow().getItem() != null) {
                    Tarefa t = getTableRow().getItem();
                    switch (t.getPrioridade()) {
                        case BAIXA   -> getStyleClass().add("cell-baixa");
                        case MEDIA   -> getStyleClass().add("cell-media");
                        case ALTA    -> getStyleClass().add("cell-alta");
                        case URGENTE -> getStyleClass().add("cell-urgente");
                    }
                }
            }
        });

        // Coloração de linhas pelo prazo
        tableViewTarefas.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Tarefa item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("task-row-vencida","task-row-hoje","task-row-em-breve");
                if (!empty && item != null && item.getDataVencimento() != null
                        && item.getStatus() != TipoStatus.CONCLUIDA) {
                    LocalDate hoje = LocalDate.now();
                    LocalDate venc = item.getDataVencimento();
                    if (venc.isBefore(hoje)) {
                        getStyleClass().add("task-row-vencida");
                    } else if (venc.isEqual(hoje) || venc.isEqual(hoje.plusDays(1))) {
                        getStyleClass().add("task-row-hoje");
                    } else if (venc.isBefore(hoje.plusDays(4))) {
                        getStyleClass().add("task-row-em-breve");
                    }
                }
            }
        });

        // Coluna de conclusão rápida
        TableColumn<Tarefa, Boolean> colConcluida = new TableColumn<>("");
        colConcluida.setPrefWidth(36);
        colConcluida.setMinWidth(36);
        colConcluida.setMaxWidth(36);
        colConcluida.setResizable(false);
        colConcluida.setSortable(false);
        colConcluida.setCellValueFactory(c ->
            new javafx.beans.property.SimpleBooleanProperty(c.getValue().getStatus() == TipoStatus.CONCLUIDA));
        colConcluida.setCellFactory(col -> new TableCell<>() {
            private final CheckBox cb = new CheckBox();
            {
                cb.setOnAction(e -> {
                    Tarefa t = getTableRow().getItem();
                    if (t != null) toggleConcluida(t, cb.isSelected());
                });
            }
            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                cb.setSelected(item);
                setGraphic(cb);
                setAlignment(javafx.geometry.Pos.CENTER);
            }
        });
        tableViewTarefas.getColumns().add(0, colConcluida);

        tarefasFiltradas = new FilteredList<>(tarefasLista, t -> true);
        SortedList<Tarefa> tarefasOrdenadas = new SortedList<>(tarefasFiltradas);
        tarefasOrdenadas.comparatorProperty().bind(tableViewTarefas.comparatorProperty());
        tableViewTarefas.setItems(tarefasOrdenadas);

        tableViewTarefas.getSelectionModel().selectedItemProperty().addListener(
            (obs, ant, novo) -> atualizarBotoesTarefa(novo));

        tableViewTarefas.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) handleEditarTarefaSelecionada();
        });
    }

    private void configurarFiltros() {
        cbFiltroStatus.setItems(FXCollections.observableArrayList());
        cbFiltroStatus.getItems().add("Todos");
        cbFiltroStatus.getItems().addAll((Object[]) TipoStatus.values());
        cbFiltroStatus.setValue("Todos");

        cbFiltroEtiqueta.setItems(FXCollections.observableArrayList());
        cbFiltroEtiqueta.getItems().add("Todas");
        cbFiltroEtiqueta.setValue("Todas");
    }

    private void configurarListaEtiquetas() {
        listEtiquetas.setItems(etiquetasLista);
        listEtiquetas.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Etiqueta item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getNome());
            }
        });

        listEtiquetas.getSelectionModel().selectedItemProperty().addListener(
            (obs, ant, novo) -> {
                boolean tem = novo != null;
                btnRenomearEtiqueta.setDisable(!tem);
                btnExcluirEtiqueta.setDisable(!tem);
                // filtrar tabela pela etiqueta selecionada na lista
                if (tem) {
                    cbFiltroEtiqueta.setValue(novo);
                    aplicarFiltro();
                }
            });
    }

    private void configurarAtalhos() {
        tableViewTarefas.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) handleEditarTarefaSelecionada();
            if (e.getCode() == KeyCode.DELETE) handleExcluirTarefaSelecionada();
        });
    }

    // ── Carregamento de dados ─────────────────────────────────────────────────

    private void carregarDados() {
        int uid = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                List<Tarefa>   tarefas   = tarefaService.listarTarefas(uid);
                List<Etiqueta> etiquetas = tarefaService.listarEtiquetas(uid);
                Platform.runLater(() -> {
                    tarefasLista.setAll(tarefas);
                    etiquetasLista.setAll(etiquetas);
                    atualizarComboEtiquetas(etiquetas);
                    aplicarFiltro();
                    tentarSelecionarPendente();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro ao carregar dados: " + ex.getMessage()));
            }
        }).start();
    }

    private void atualizarComboEtiquetas(List<Etiqueta> etiquetas) {
        Object selecionada = cbFiltroEtiqueta.getValue();
        cbFiltroEtiqueta.getItems().clear();
        cbFiltroEtiqueta.getItems().add("Todas");
        cbFiltroEtiqueta.getItems().addAll(etiquetas);
        if (selecionada instanceof Etiqueta e && etiquetas.stream().anyMatch(x -> x.getId() == e.getId())) {
            cbFiltroEtiqueta.setValue(etiquetas.stream().filter(x -> x.getId() == e.getId()).findFirst().orElse(null));
        } else {
            cbFiltroEtiqueta.setValue("Todas");
        }
    }

    // ── Filtro ────────────────────────────────────────────────────────────────

    @FXML
    private void handleFiltroAlterado() {
        aplicarFiltro();
    }

    @FXML
    private void handleLimparFiltros() {
        tfBusca.clear();
        cbFiltroStatus.setValue("Todos");
        cbFiltroEtiqueta.setValue("Todas");
        listEtiquetas.getSelectionModel().clearSelection();
        aplicarFiltro();
    }

    private void aplicarFiltro() {
        String busca  = tfBusca.getText().toLowerCase().trim();
        Object status = cbFiltroStatus.getValue();
        Object etiq   = cbFiltroEtiqueta.getValue();

        tarefasFiltradas.setPredicate(t -> {
            // Filtro por texto
            if (!busca.isBlank()) {
                boolean ok = t.getTitulo().toLowerCase().contains(busca)
                    || t.getAnotacoes().toLowerCase().contains(busca)
                    || t.getEtiquetasTexto().toLowerCase().contains(busca);
                if (!ok) return false;
            }
            // Filtro por status
            if (status instanceof TipoStatus ts && t.getStatus() != ts) return false;
            // Filtro por etiqueta
            if (etiq instanceof Etiqueta e) {
                boolean temEtiq = t.getEtiquetas().stream().anyMatch(x -> x.getId() == e.getId());
                if (!temEtiq) return false;
            }
            return true;
        });

        lblContagem.setText(tarefasFiltradas.size() + " tarefa" + (tarefasFiltradas.size() == 1 ? "" : "s"));
    }

    // ── Ações de tarefas ──────────────────────────────────────────────────────

    @FXML
    private void handleNovaTarefa() {
        mostrarDialogoTarefa(null);
    }

    @FXML
    private void handleEditarTarefaSelecionada() {
        Tarefa sel = tableViewTarefas.getSelectionModel().getSelectedItem();
        if (sel != null) mostrarDialogoTarefa(sel);
    }

    @FXML
    private void handleExcluirTarefaSelecionada() {
        Tarefa sel = tableViewTarefas.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.initOwner(tableViewTarefas.getScene().getWindow());
        conf.initModality(Modality.WINDOW_MODAL);
        conf.setTitle("Excluir Tarefa");
        conf.setHeaderText("Excluir \"" + sel.getTitulo() + "\"?");
        conf.setContentText("Esta ação não pode ser desfeita.");
        ThemeManager.getInstance().aplicarTemaAoDialogo(conf);
        conf.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            new Thread(() -> {
                try {
                    tarefaService.excluirTarefa(sel.getId());
                    Platform.runLater(() -> { carregarDados(); notificarAlteracao(); });
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro("Erro ao excluir: " + ex.getMessage()));
                }
            }).start();
        });
    }

    private void toggleConcluida(Tarefa t, boolean concluir) {
        TipoStatus novoStatus = concluir ? TipoStatus.CONCLUIDA : TipoStatus.PENDENTE;
        if (t.getStatus() == novoStatus) return;
        t.setStatus(novoStatus);
        List<Integer> etiqIds = t.getEtiquetas().stream().map(Etiqueta::getId).toList();
        new Thread(() -> {
            try {
                tarefaService.atualizarTarefa(t, etiqIds);
                Platform.runLater(() -> { carregarDados(); notificarAlteracao(); });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro ao atualizar tarefa: " + ex.getMessage()));
            }
        }).start();
    }

    private void atualizarBotoesTarefa(Tarefa t) {
        boolean tem = t != null;
        btnEditarTarefa.setDisable(!tem);
        btnExcluirTarefa.setDisable(!tem);
    }

    // ── Ações de etiquetas ────────────────────────────────────────────────────

    @FXML
    private void handleNovaEtiqueta() {
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Nova Etiqueta");
        d.setHeaderText("Nome da nova etiqueta:");
        d.setContentText("Nome:");
        ThemeManager.getInstance().aplicarTemaAoDialogo(d);
        d.showAndWait().ifPresent(nome -> {
            if (nome.isBlank()) return;
            int uid = SessionManager.getInstance().getUsuarioLogado().getId();
            new Thread(() -> {
                try {
                    tarefaService.criarEtiqueta(uid, nome.trim());
                    Platform.runLater(this::carregarDados);
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro("Erro ao criar etiqueta: " + ex.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleRenomearEtiquetaSelecionada() {
        Etiqueta sel = listEtiquetas.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        TextInputDialog d = new TextInputDialog(sel.getNome());
        d.setTitle("Renomear Etiqueta");
        d.setHeaderText("Novo nome para \"" + sel.getNome() + "\":");
        d.setContentText("Nome:");
        ThemeManager.getInstance().aplicarTemaAoDialogo(d);
        d.showAndWait().ifPresent(nome -> {
            if (nome.isBlank() || nome.trim().equals(sel.getNome())) return;
            new Thread(() -> {
                try {
                    tarefaService.renomearEtiqueta(sel.getId(), nome.trim());
                    Platform.runLater(this::carregarDados);
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro("Erro ao renomear: " + ex.getMessage()));
                }
            }).start();
        });
    }

    @FXML
    private void handleExcluirEtiquetaSelecionada() {
        Etiqueta sel = listEtiquetas.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        // Conta tarefas que serão excluídas junto
        long exclusivas = tarefasLista.stream()
            .filter(t -> t.getEtiquetas().stream().anyMatch(e -> e.getId() == sel.getId())
                && t.getEtiquetas().size() == 1)
            .count();

        String aviso = exclusivas > 0
            ? "\n\nAtenção: " + exclusivas + " tarefa(s) que pertencem SOMENTE a esta etiqueta também serão excluídas."
            : "";

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION);
        conf.initOwner(listEtiquetas.getScene().getWindow());
        conf.initModality(Modality.WINDOW_MODAL);
        conf.setTitle("Excluir Etiqueta");
        conf.setHeaderText("Excluir a etiqueta \"" + sel.getNome() + "\"?");
        conf.setContentText("Esta ação não pode ser desfeita." + aviso);
        ThemeManager.getInstance().aplicarTemaAoDialogo(conf);
        conf.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            new Thread(() -> {
                try {
                    tarefaService.excluirEtiqueta(sel.getId());
                    Platform.runLater(() -> { carregarDados(); notificarAlteracao(); });
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro("Erro ao excluir etiqueta: " + ex.getMessage()));
                }
            }).start();
        });
    }

    // ── Diálogo criar/editar tarefa ───────────────────────────────────────────

    private void mostrarDialogoTarefa(Tarefa original) {
        boolean editando = original != null;
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(editando ? "Editar Tarefa" : "Nova Tarefa");
        dialog.setHeaderText(editando ? "Editar: " + original.getTitulo() : "Criar nova tarefa");
        dialog.initOwner(tableViewTarefas.getScene().getWindow());
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        dialog.getDialogPane().setMinWidth(520);
        ThemeManager.getInstance().aplicarTemaAoDialogo(dialog);

        // Campos
        TextField tfTitulo = new TextField(editando ? original.getTitulo() : "");
        tfTitulo.setPromptText("Título da tarefa *");

        TextArea taAnotacoes = new TextArea(editando ? original.getAnotacoes() : "");
        taAnotacoes.setPromptText("Anotações (opcional)");
        taAnotacoes.setPrefRowCount(4);
        taAnotacoes.setWrapText(true);

        ComboBox<TipoPrioridade> cbPrioridade = new ComboBox<>(
            FXCollections.observableArrayList(TipoPrioridade.values()));
        cbPrioridade.setValue(editando ? original.getPrioridade() : TipoPrioridade.MEDIA);

        ComboBox<TipoStatus> cbStatus = new ComboBox<>(
            FXCollections.observableArrayList(TipoStatus.values()));
        cbStatus.setValue(editando ? original.getStatus() : TipoStatus.PENDENTE);

        DatePicker dpVencimento = new DatePicker(editando ? original.getDataVencimento() : null);
        dpVencimento.setPromptText("Sem prazo");
        dpVencimento.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate d) { return d != null ? d.format(FMT_D) : ""; }
            @Override public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) return null;
                try { return LocalDate.parse(s.trim(), FMT_D); } catch (Exception e) { return null; }
            }
        });

        // Seleção de até 3 etiquetas (ComboBoxes)
        List<Etiqueta> todasEtiquetas = new ArrayList<>(etiquetasLista);

        @SuppressWarnings("unchecked")
        ComboBox<Etiqueta>[] cbsEtiqueta = new ComboBox[3];
        for (int i = 0; i < 3; i++) {
            cbsEtiqueta[i] = new ComboBox<>(FXCollections.observableArrayList(todasEtiquetas));
            cbsEtiqueta[i].setPromptText(i == 0 ? "Etiqueta 1 *" : "Etiqueta " + (i + 1) + " (opcional)");
            cbsEtiqueta[i].setMaxWidth(Double.MAX_VALUE);
        }

        if (editando) {
            List<Etiqueta> etiq = original.getEtiquetas();
            for (int i = 0; i < Math.min(etiq.size(), 3); i++) {
                Etiqueta e = etiq.get(i);
                todasEtiquetas.stream().filter(x -> x.getId() == e.getId()).findFirst()
                    .ifPresent(cbsEtiqueta[i]::setValue);
            }
        } else {
            Etiqueta etiqSelecionada = listEtiquetas.getSelectionModel().getSelectedItem();
            if (etiqSelecionada != null) {
                todasEtiquetas.stream()
                    .filter(x -> x.getId() == etiqSelecionada.getId())
                    .findFirst()
                    .ifPresent(cbsEtiqueta[0]::setValue);
            }
        }

        Label lblEtiqAviso = new Label();
        lblEtiqAviso.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 11px;");

        if (todasEtiquetas.isEmpty()) {
            lblEtiqAviso.setText("Nenhuma etiqueta cadastrada. Crie uma etiqueta antes de adicionar tarefas.");
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));

        int row = 0;
        grid.add(new Label("Título *"), 0, row);      grid.add(tfTitulo, 1, row++);
        grid.add(new Label("Anotações"), 0, row);     grid.add(taAnotacoes, 1, row++);
        grid.add(new Label("Prioridade"), 0, row);    grid.add(cbPrioridade, 1, row++);
        grid.add(new Label("Status"), 0, row);        grid.add(cbStatus, 1, row++);
        grid.add(new Label("Vencimento"), 0, row);    grid.add(dpVencimento, 1, row++);
        grid.add(new Label("Etiqueta 1 *"), 0, row);  grid.add(cbsEtiqueta[0], 1, row++);
        grid.add(new Label("Etiqueta 2"), 0, row);    grid.add(cbsEtiqueta[1], 1, row++);
        grid.add(new Label("Etiqueta 3"), 0, row);    grid.add(cbsEtiqueta[2], 1, row++);
        if (!lblEtiqAviso.getText().isEmpty()) {
            grid.add(lblEtiqAviso, 0, row, 2, 1);
        }

        GridPane.setHgrow(tfTitulo, Priority.ALWAYS);
        GridPane.setHgrow(taAnotacoes, Priority.ALWAYS);
        GridPane.setHgrow(cbPrioridade, Priority.ALWAYS);
        GridPane.setHgrow(cbStatus, Priority.ALWAYS);
        GridPane.setHgrow(dpVencimento, Priority.ALWAYS);
        for (ComboBox<Etiqueta> cb : cbsEtiqueta) GridPane.setHgrow(cb, Priority.ALWAYS);

        dialog.getDialogPane().setContent(new ScrollPane(grid));

        // Desabilita OK enquanto título vazio
        Button btnOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btnOk.setDisable(tfTitulo.getText().isBlank() || todasEtiquetas.isEmpty());
        tfTitulo.textProperty().addListener((obs, a, b) ->
            btnOk.setDisable(b.isBlank() || todasEtiquetas.isEmpty()));

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            // Coleta etiquetas selecionadas (sem duplicatas)
            List<Integer> etiqIds = new ArrayList<>();
            Set<Integer> vistos = new HashSet<>();
            for (ComboBox<Etiqueta> cb : cbsEtiqueta) {
                Etiqueta e = cb.getValue();
                if (e != null && vistos.add(e.getId())) etiqIds.add(e.getId());
            }

            if (etiqIds.isEmpty()) {
                mostrarErro("Selecione ao menos uma etiqueta.");
                return;
            }

            Tarefa t = editando ? original : new Tarefa();
            t.setTitulo(tfTitulo.getText().trim());
            t.setAnotacoes(taAnotacoes.getText());
            t.setPrioridade(cbPrioridade.getValue());
            t.setStatus(cbStatus.getValue());
            t.setDataVencimento(dpVencimento.getValue());
            if (!editando) t.setUsuarioId(SessionManager.getInstance().getUsuarioLogado().getId());

            new Thread(() -> {
                try {
                    if (editando) {
                        tarefaService.atualizarTarefa(t, etiqIds);
                    } else {
                        tarefaService.criarTarefa(t, etiqIds);
                    }
                    Platform.runLater(() -> { carregarDados(); notificarAlteracao(); });
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro("Erro ao salvar tarefa: " + ex.getMessage()));
                }
            }).start();
        });
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    private void mostrarErro(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erro");
        a.setHeaderText(null);
        a.setContentText(msg);
        ThemeManager.getInstance().aplicarTemaAoDialogo(a);
        a.showAndWait();
    }
}
