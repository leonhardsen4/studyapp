package com.leonhardsen.studyapp.controller;

import com.leonhardsen.studyapp.model.*;
import com.leonhardsen.studyapp.service.ArquivoService;
import com.leonhardsen.studyapp.service.EventoService;
import com.leonhardsen.studyapp.service.PomodoroService;
import com.leonhardsen.studyapp.service.TarefaService;
import com.leonhardsen.studyapp.util.FormatadorData;
import com.leonhardsen.studyapp.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Consumer;

/**
 * Controlador do Dashboard — tela inicial da aplicação.
 * Agrega dados de todos os módulos (Pomodoro, Tarefas, Agenda, Plano de Estudos)
 * e os exibe em seções resumidas com atalhos de navegação.
 */
public class DashboardController {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private VBox conteudo;

    // ── Serviços ──────────────────────────────────────────────────────────────
    private final PomodoroService pomodoroService = new PomodoroService();
    private final TarefaService   tarefaService   = new TarefaService();
    private final EventoService   eventoService   = new EventoService();
    private final ArquivoService  arquivoService  = new ArquivoService();

    // ── Callbacks de navegação ────────────────────────────────────────────────
    /** Navega para o módulo de Tarefas. */
    private Runnable onVerTarefas;
    /** Navega para o módulo de Agenda. */
    private Runnable onVerAgenda;
    /** Navega para o módulo de Plano de Estudos. */
    private Runnable onVerPlanoEstudos;
    /** Navega para o módulo de Arquivos. */
    private Runnable onVerArquivos;
    /** Navega para o módulo Pomodoro. */
    private Runnable onVerPomodoro;
    /** Navega para o Bloco de Notas. */
    private Runnable onVerBlocoNotas;
    /** Navega para a Calculadora. */
    private Runnable onVerCalculadora;
    /** Navega para o Pomodoro pré-selecionando o assunto informado. */
    private Consumer<Assunto> onEstudarAssunto;

    // ── Dados agregados (para uso interno durante construção da UI) ───────────

    /**
     * Bundle imutável com todos os dados necessários para renderizar o Dashboard.
     */
    private record DashData(
        int sessoesHoje,
        int segundosHoje,
        List<Tarefa>                tarefasUrgentes,
        List<Evento>                eventosHoje,
        List<Disciplina>            disciplinas,
        Map<Integer, List<Assunto>> assuntosPorDisc,
        Map<Integer, Integer>       duracoesPorDisc,
        List<String[]>              sessoesRecentes,
        long                        numCadernos,
        long                        numNotas,
        long                        numPdfs
    ) {}

    // ─────────────────────────────────────────────────────────────────────────
    // INICIALIZAÇÃO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Inicializa o controlador carregando os dados ao montar o painel.
     */
    @FXML
    public void initialize() {
        atualizarView();
    }

    /**
     * Recarrega todos os dados e reconstrói a UI. Chamado ao navegar de volta ao Dashboard.
     */
    public void atualizarView() {
        carregarDados();
    }

    /** Handler do botão "↻ Atualizar" na toolbar. */
    @FXML
    private void handleAtualizar() {
        carregarDados();
    }

    // ── Setters de callback ───────────────────────────────────────────────────

    /**
     * Define o callback invocado ao clicar em "Ver tarefas".
     *
     * @param cb runnable que navega para o módulo de Tarefas
     */
    public void setOnVerTarefas(Runnable cb)         { this.onVerTarefas      = cb; }

    /**
     * Define o callback invocado ao clicar em "Ver agenda".
     *
     * @param cb runnable que navega para o módulo de Agenda
     */
    public void setOnVerAgenda(Runnable cb)          { this.onVerAgenda       = cb; }

    /**
     * Define o callback invocado ao clicar em "Ver plano".
     *
     * @param cb runnable que navega para o módulo de Plano de Estudos
     */
    public void setOnVerPlanoEstudos(Runnable cb)    { this.onVerPlanoEstudos = cb; }

    /**
     * Define o callback invocado ao clicar no card de Arquivos.
     *
     * @param cb runnable que navega para o módulo de Arquivos
     */
    public void setOnVerArquivos(Runnable cb)        { this.onVerArquivos     = cb; }

    /**
     * Define o callback invocado ao clicar no card do Pomodoro.
     *
     * @param cb runnable que navega para o módulo Pomodoro
     */
    public void setOnVerPomodoro(Runnable cb)        { this.onVerPomodoro     = cb; }

    /**
     * Define o callback invocado ao clicar no botão de acesso rápido ao Bloco de Notas.
     *
     * @param cb runnable que navega para o Bloco de Notas
     */
    public void setOnVerBlocoNotas(Runnable cb)      { this.onVerBlocoNotas   = cb; }

    /**
     * Define o callback invocado ao clicar no botão de acesso rápido à Calculadora.
     *
     * @param cb runnable que navega para a Calculadora
     */
    public void setOnVerCalculadora(Runnable cb)     { this.onVerCalculadora  = cb; }

    /**
     * Define o callback invocado ao clicar em "▶ Estudar" em uma disciplina.
     *
     * @param cb consumidor que recebe o assunto pré-selecionado e navega para o Pomodoro
     */
    public void setOnEstudarAssunto(Consumer<Assunto> cb) { this.onEstudarAssunto = cb; }

    // ─────────────────────────────────────────────────────────────────────────
    // CARREGAMENTO DE DADOS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Carrega todos os dados agregados em background e reconstrói a UI ao concluir.
     */
    private void carregarDados() {
        mostrarCarregando();
        int uid = SessionManager.getInstance().getUsuarioLogado().getId();

        new Thread(() -> {
            try {
                int sessoesHoje  = pomodoroService.contarSessoesHoje(uid);
                int segundosHoje = pomodoroService.somarDuracaoHoje(uid);

                List<Tarefa> tarefasUrgentes = tarefaService.buscarTarefasComAlerta(uid);
                List<Evento> eventosHoje     = eventoService.buscarEventosHoje(uid);

                List<Disciplina>              disciplinas     = pomodoroService.buscarDisciplinas(uid);
                Map<Integer, List<Assunto>>   assuntosPorDisc = new HashMap<>();
                Map<Integer, Integer>         duracoesPorDisc = new HashMap<>();
                for (Disciplina d : disciplinas) {
                    assuntosPorDisc.put(d.getId(), pomodoroService.buscarAssuntos(d.getId()));
                    duracoesPorDisc.put(d.getId(), pomodoroService.somarDuracaoPorDisciplina(d.getId()));
                }

                List<String[]> sessoesRecentes = pomodoroService.buscarResumoSessoesRecentes(uid, 5);

                List<ItemArvore> todosItens = arquivoService.buscarTodos(uid);
                long numCadernos = todosItens.stream().filter(i -> i.getTipo() == TipoItem.CADERNO).count();
                long numNotas    = todosItens.stream().filter(i -> i.getTipo() == TipoItem.NOTA).count();
                long numPdfs     = todosItens.stream().filter(i -> i.getTipo() == TipoItem.PDF).count();

                DashData data = new DashData(
                    sessoesHoje, segundosHoje,
                    tarefasUrgentes, eventosHoje,
                    disciplinas, assuntosPorDisc, duracoesPorDisc,
                    sessoesRecentes,
                    numCadernos, numNotas, numPdfs
                );

                Platform.runLater(() -> construirUI(data));
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErroCarregamento(ex.getMessage()));
            }
        }).start();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CONSTRUÇÃO DA UI
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Exibe um estado de "carregando..." enquanto os dados ainda não chegaram.
     */
    private void mostrarCarregando() {
        conteudo.getChildren().clear();
        Label lbl = new Label("Carregando…");
        lbl.getStyleClass().add("dash-carregando");
        lbl.setPadding(new Insets(40));
        conteudo.getChildren().add(lbl);
    }

    /**
     * Reconstrói todo o conteúdo do Dashboard com os dados carregados.
     *
     * @param data dados agregados de todos os módulos
     */
    private void construirUI(DashData data) {
        conteudo.getChildren().clear();

        conteudo.getChildren().add(criarSaudacao());
        conteudo.getChildren().add(criarAcessoRapido());
        conteudo.getChildren().add(criarCardsResumo(data));

        if (!data.tarefasUrgentes().isEmpty()) {
            conteudo.getChildren().add(criarSecaoTarefas(data.tarefasUrgentes()));
        }

        if (!data.eventosHoje().isEmpty()) {
            conteudo.getChildren().add(criarSecaoEventos(data.eventosHoje()));
        }

        if (!data.disciplinas().isEmpty()) {
            conteudo.getChildren().add(criarSecaoPlano(data));
        }

        if (!data.sessoesRecentes().isEmpty()) {
            conteudo.getChildren().add(criarSecaoSessoes(data.sessoesRecentes()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEÇÃO: SAUDAÇÃO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Constrói o bloco de saudação com o nome do usuário e a data atual.
     *
     * @return nó {@link VBox} com a saudação
     */
    private VBox criarSaudacao() {
        String nome = SessionManager.getInstance().getUsuarioLogado().getNome();
        String primeiroNome = nome.split(" ")[0];
        int hora = LocalTime.now().getHour();
        String cumprimento = hora < 12 ? "Bom dia" : hora < 18 ? "Boa tarde" : "Boa noite";

        VBox box = new VBox(4);

        Label lblCumprimento = new Label(cumprimento + ", " + primeiroNome + "!");
        lblCumprimento.getStyleClass().add("dash-saudacao");

        Label lblData = new Label(FormatadorData.formatarDataLonga(LocalDate.now()));
        lblData.getStyleClass().add("dash-data-label");

        box.getChildren().addAll(lblCumprimento, lblData);
        return box;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACESSO RÁPIDO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Constrói a linha de botões de acesso rápido ao Bloco de Notas e à Calculadora.
     *
     * @return {@link HBox} com os botões de atalho
     */
    private HBox criarAcessoRapido() {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label("Acesso rápido:");
        lbl.getStyleClass().add("dash-atalho-label");

        Button btnNotas = new Button("📝  Bloco de Notas");
        btnNotas.getStyleClass().add("dash-atalho-btn");
        if (onVerBlocoNotas != null) btnNotas.setOnAction(e -> onVerBlocoNotas.run());

        Button btnCalc = new Button("🧮  Calculadora");
        btnCalc.getStyleClass().add("dash-atalho-btn");
        if (onVerCalculadora != null) btnCalc.setOnAction(e -> onVerCalculadora.run());

        row.getChildren().addAll(lbl, btnNotas, btnCalc);
        return row;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEÇÃO: CARDS DE RESUMO
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Constrói a linha de cards de resumo (Tarefas, Agenda, Arquivos, Plano de Estudos, Pomodoro).
     *
     * @param data dados carregados
     * @return {@link FlowPane} com os cards
     */
    private FlowPane criarCardsResumo(DashData data) {
        FlowPane flow = new FlowPane(12, 12);
        flow.setAlignment(Pos.TOP_LEFT);

        // Card Tarefas
        long vencidas = data.tarefasUrgentes().stream()
            .filter(t -> t.getDataVencimento().isBefore(LocalDate.now())).count();
        String subTarefa = vencidas > 0 ? vencidas + " vencida(s)" : "próximas do prazo";
        flow.getChildren().add(criarCard(
            "✓", "Tarefas urgentes",
            data.tarefasUrgentes().size() + " tarefa(s)",
            subTarefa,
            onVerTarefas
        ));

        // Card Eventos
        String subEvento = data.eventosHoje().isEmpty() ? "nenhum evento"
            : data.eventosHoje().get(0).getTitulo();
        flow.getChildren().add(criarCard(
            "📅", "Agenda hoje",
            data.eventosHoje().size() + " evento(s)",
            subEvento,
            onVerAgenda
        ));

        // Card Arquivos
        long totalArquivos = data.numNotas() + data.numPdfs();
        flow.getChildren().add(criarCard(
            "📁", "Sistema de Arquivos",
            data.numCadernos() + " caderno(s)",
            totalArquivos + " arquivo(s)  (" + data.numNotas() + " notas · " + data.numPdfs() + " PDFs)",
            onVerArquivos
        ));

        // Card Plano de Estudos
        long totalAssuntos   = data.assuntosPorDisc().values().stream().mapToLong(List::size).sum();
        long concluidosTotal = data.assuntosPorDisc().values().stream()
            .flatMap(List::stream)
            .filter(a -> a.getStatus() == TipoStatusAssunto.CONCLUIDO).count();
        flow.getChildren().add(criarCard(
            "📚", "Plano de Estudos",
            concluidosTotal + "/" + totalAssuntos + " assuntos",
            data.disciplinas().size() + " disciplina(s)",
            onVerPlanoEstudos
        ));

        // Card Pomodoro (ao lado do Plano de Estudos)
        int h = data.segundosHoje() / 3600;
        int m = (data.segundosHoje() % 3600) / 60;
        String tempoHoje = data.segundosHoje() == 0 ? "0 min"
            : h > 0 ? h + "h " + m + "min" : m + " min";
        flow.getChildren().add(criarCard(
            "🍅", "Pomodoro hoje",
            data.sessoesHoje() + " sessão(ões)",
            tempoHoje,
            onVerPomodoro
        ));

        return flow;
    }

    /**
     * Constrói um card individual para a linha de resumo.
     *
     * @param icone     ícone Unicode exibido no canto superior esquerdo
     * @param titulo    título do card
     * @param valorPrin valor principal (em destaque)
     * @param valorSec  valor secundário (subtítulo)
     * @param onClick   ação ao clicar no card (ou {@code null} para inativo)
     * @return {@link VBox} representando o card
     */
    private VBox criarCard(String icone, String titulo, String valorPrin, String valorSec, Runnable onClick) {
        VBox card = new VBox(5);
        card.getStyleClass().add("dash-card");
        card.setPrefWidth(210);

        HBox header = new HBox(6);
        header.setAlignment(Pos.CENTER_LEFT);
        Label lblIcone  = new Label(icone);
        lblIcone.getStyleClass().add("dash-card-icone");
        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("dash-card-titulo");
        header.getChildren().addAll(lblIcone, lblTitulo);

        Label lblValor = new Label(valorPrin);
        lblValor.getStyleClass().add("dash-card-valor");

        Label lblSub = new Label(valorSec);
        lblSub.getStyleClass().add("dash-card-sub");
        lblSub.setWrapText(true);

        card.getChildren().addAll(header, lblValor, lblSub);

        if (onClick != null) {
            card.getStyleClass().add("dash-card-clicavel");
            card.setOnMouseClicked(e -> onClick.run());
        }
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEÇÃO: TAREFAS URGENTES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Constrói a seção de tarefas urgentes (vencidas ou próximas do prazo).
     *
     * @param tarefas lista de tarefas urgentes
     * @return {@link VBox} da seção
     */
    private VBox criarSecaoTarefas(List<Tarefa> tarefas) {
        VBox itens = new VBox(4);
        LocalDate hoje = LocalDate.now();

        for (Tarefa t : tarefas) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-lista-row");

            boolean vencida = t.getDataVencimento().isBefore(hoje);
            boolean eHoje   = t.getDataVencimento().isEqual(hoje);

            Label lblStatus;
            if (vencida) {
                lblStatus = new Label("⚠");
                lblStatus.setStyle("-fx-text-fill: #C04040; -fx-font-size: 13px;");
            } else {
                lblStatus = new Label("●");
                lblStatus.setStyle("-fx-text-fill: #8090B0; -fx-font-size: 10px;");
            }

            Label lblTitulo = new Label(t.getTitulo());
            lblTitulo.getStyleClass().add("dash-lista-titulo");
            HBox.setHgrow(lblTitulo, Priority.ALWAYS);
            lblTitulo.setMaxWidth(Double.MAX_VALUE);

            String textoData = vencida ? "Vencida"
                : eHoje ? "Hoje"
                : "Em " + hoje.until(t.getDataVencimento()).getDays() + "d";
            Label lblData = new Label(textoData);
            lblData.getStyleClass().add(vencida ? "dash-badge-vencida" : eHoje ? "dash-badge-hoje" : "dash-badge-breve");

            Button btnVer = new Button("→ Ver");
            btnVer.getStyleClass().add("btn-action");
            btnVer.setOnAction(e -> { if (onVerTarefas != null) onVerTarefas.run(); });

            row.getChildren().addAll(lblStatus, lblTitulo, lblData, btnVer);
            itens.getChildren().add(row);
        }

        if (onVerTarefas != null) {
            Button btnTodas = new Button("Ver todas as tarefas →");
            btnTodas.getStyleClass().add("btn-action");
            btnTodas.setOnAction(e -> onVerTarefas.run());
            HBox footer = new HBox(btnTodas);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.setPadding(new Insets(4, 0, 0, 0));
            itens.getChildren().add(footer);
        }

        return criarSecao("✓  Tarefas urgentes", itens);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEÇÃO: EVENTOS DE HOJE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Constrói a seção de eventos do dia de hoje.
     *
     * @param eventos lista de eventos de hoje
     * @return {@link VBox} da seção
     */
    private VBox criarSecaoEventos(List<Evento> eventos) {
        VBox itens = new VBox(4);

        for (Evento ev : eventos) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-lista-row");

            Label lblIcone = new Label("📅");

            Label lblTitulo = new Label(ev.getTitulo());
            lblTitulo.getStyleClass().add("dash-lista-titulo");
            HBox.setHgrow(lblTitulo, Priority.ALWAYS);
            lblTitulo.setMaxWidth(Double.MAX_VALUE);

            String horario = ev.getHoraInicio() != null
                ? ev.getHoraInicio().toString().substring(0, 5)
                  + (ev.getHoraFim() != null ? "–" + ev.getHoraFim().toString().substring(0, 5) : "")
                : "";
            Label lblHora = new Label(horario);
            lblHora.getStyleClass().add("dash-evento-hora");

            Button btnVer = new Button("→ Ver");
            btnVer.getStyleClass().add("btn-action");
            btnVer.setOnAction(e -> { if (onVerAgenda != null) onVerAgenda.run(); });

            row.getChildren().addAll(lblIcone, lblTitulo, lblHora, btnVer);
            itens.getChildren().add(row);
        }

        if (onVerAgenda != null) {
            Button btnAgenda = new Button("Ver agenda completa →");
            btnAgenda.getStyleClass().add("btn-action");
            btnAgenda.setOnAction(e -> onVerAgenda.run());
            HBox footer = new HBox(btnAgenda);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.setPadding(new Insets(4, 0, 0, 0));
            itens.getChildren().add(footer);
        }

        return criarSecao("📅  Eventos de hoje", itens);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEÇÃO: PLANO DE ESTUDOS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Constrói a seção do Plano de Estudos com as disciplinas e seu progresso.
     *
     * @param data dados carregados (inclui disciplinas, assuntos e durações)
     * @return {@link VBox} da seção
     */
    private VBox criarSecaoPlano(DashData data) {
        VBox itens = new VBox(8);

        List<Disciplina> discs = data.disciplinas();
        int maxDiscs = Math.min(discs.size(), 5);

        for (int i = 0; i < maxDiscs; i++) {
            Disciplina d = discs.get(i);
            List<Assunto> assuntos = data.assuntosPorDisc().getOrDefault(d.getId(), List.of());
            long concluidos = assuntos.stream().filter(a -> a.getStatus() == TipoStatusAssunto.CONCLUIDO).count();
            double pct      = assuntos.isEmpty() ? 0.0 : (double) concluidos / assuntos.size();
            int duracaoSeg  = data.duracoesPorDisc().getOrDefault(d.getId(), 0);

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-plano-row");

            // Nome + barra
            VBox infoBox = new VBox(4);
            HBox.setHgrow(infoBox, Priority.ALWAYS);

            Label lblNome = new Label(d.getNome());
            lblNome.getStyleClass().add("dash-plano-nome");

            ProgressBar bar = new ProgressBar(pct);
            bar.getStyleClass().add("dash-plano-bar");
            bar.setMaxWidth(Double.MAX_VALUE);
            if (pct >= 1.0 && !assuntos.isEmpty()) bar.getStyleClass().add("dash-plano-bar-concluida");
            else if (pct > 0) bar.getStyleClass().add("dash-plano-bar-andamento");

            Label lblStats = new Label(
                concluidos + "/" + assuntos.size() + " assuntos  •  " + FormatadorData.formatarTempo(duracaoSeg));
            lblStats.getStyleClass().add("dash-plano-stats");

            infoBox.getChildren().addAll(lblNome, bar, lblStats);

            // Botão estudar — abre o primeiro assunto não-concluído
            Optional<Assunto> proximo = assuntos.stream()
                .filter(a -> a.getStatus() != TipoStatusAssunto.CONCLUIDO)
                .findFirst();

            if (proximo.isPresent() && onEstudarAssunto != null) {
                Assunto alvo = proximo.get();
                Button btnEstudar = new Button("▶ Estudar");
                btnEstudar.getStyleClass().add("btn-primary");
                btnEstudar.setOnAction(e -> onEstudarAssunto.accept(alvo));
                row.getChildren().addAll(infoBox, btnEstudar);
            } else {
                row.getChildren().add(infoBox);
            }

            itens.getChildren().add(row);
        }

        if (onVerPlanoEstudos != null) {
            Button btnPlano = new Button("Ver plano completo →");
            btnPlano.getStyleClass().add("btn-action");
            btnPlano.setOnAction(e -> onVerPlanoEstudos.run());
            HBox footer = new HBox(btnPlano);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.setPadding(new Insets(4, 0, 0, 0));
            itens.getChildren().add(footer);
        }

        return criarSecao("📚  Plano de Estudos", itens);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SEÇÃO: SESSÕES RECENTES
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Constrói a seção das últimas sessões de foco Pomodoro registradas.
     *
     * @param sessoes lista de arrays {@code [duracao_seg, concluido_em, assunto_nome, disciplina_nome]}
     * @return {@link VBox} da seção
     */
    private VBox criarSecaoSessoes(List<String[]> sessoes) {
        VBox itens = new VBox(4);
        LocalDate hoje = LocalDate.now();

        for (String[] s : sessoes) {
            int duracaoSeg = Integer.parseInt(s[0]);
            String assuntoNome    = s[2];
            String disciplinaNome = s[3];

            // Parse da data/hora de conclusão
            String dataLabel = "";
            try {
                LocalDateTime ldt = LocalDateTime.parse(s[1]);
                LocalDate dataSessao = ldt.toLocalDate();
                if (dataSessao.isEqual(hoje))
                    dataLabel = "hoje " + ldt.toLocalTime().toString().substring(0, 5);
                else if (dataSessao.isEqual(hoje.minusDays(1)))
                    dataLabel = "ontem " + ldt.toLocalTime().toString().substring(0, 5);
                else
                    dataLabel = FormatadorData.formatarDataCurta(dataSessao);
            } catch (Exception ignored) { dataLabel = s[1]; }

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dash-lista-row");

            Label lblIcone = new Label("🍅");

            String descricao = assuntoNome != null
                ? (disciplinaNome != null ? disciplinaNome + " · " + assuntoNome : assuntoNome)
                : "Sessão livre";
            Label lblDesc = new Label(descricao);
            lblDesc.getStyleClass().add("dash-lista-titulo");
            HBox.setHgrow(lblDesc, Priority.ALWAYS);
            lblDesc.setMaxWidth(Double.MAX_VALUE);

            Label lblData = new Label(dataLabel);
            lblData.getStyleClass().add("dash-sessao-data");

            Label lblDuracao = new Label(FormatadorData.formatarTempo(duracaoSeg));
            lblDuracao.getStyleClass().add("dash-sessao-duracao");

            row.getChildren().addAll(lblIcone, lblDesc, lblData, lblDuracao);
            itens.getChildren().add(row);
        }

        return criarSecao("🍅  Sessões recentes de foco", itens);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITÁRIOS DE LAYOUT
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Monta um bloco de seção com título, separador e conteúdo.
     *
     * @param titulo  texto do cabeçalho da seção
     * @param conteudo nó com o conteúdo da seção
     * @return {@link VBox} da seção completa
     */
    private VBox criarSecao(String titulo, VBox conteudo) {
        VBox secao = new VBox(8);
        secao.getStyleClass().add("dash-secao");

        Label lblTitulo = new Label(titulo);
        lblTitulo.getStyleClass().add("dash-secao-titulo");

        Separator sep = new Separator();
        sep.getStyleClass().add("dash-sep");

        secao.getChildren().addAll(lblTitulo, sep, conteudo);
        return secao;
    }

    /**
     * Exibe uma mensagem de erro ao carregar os dados.
     *
     * @param msg mensagem de erro
     */
    private void mostrarErroCarregamento(String msg) {
        conteudo.getChildren().clear();
        Label lbl = new Label("Erro ao carregar dados: " + msg);
        lbl.getStyleClass().add("dash-carregando");
        lbl.setPadding(new Insets(40));
        conteudo.getChildren().add(lbl);
    }

}
