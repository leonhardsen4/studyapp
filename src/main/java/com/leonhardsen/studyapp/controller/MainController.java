package com.leonhardsen.studyapp.controller;

import com.leonhardsen.studyapp.StudyApplication;
import com.leonhardsen.studyapp.database.DatabaseManager;
import com.leonhardsen.studyapp.database.UsuarioDAO;
import com.leonhardsen.studyapp.model.Assunto;
import com.leonhardsen.studyapp.model.Evento;
import com.leonhardsen.studyapp.model.ItemArvore;
import com.leonhardsen.studyapp.model.Nota;
import com.leonhardsen.studyapp.model.PdfDocumento;
import com.leonhardsen.studyapp.model.Tarefa;
import com.leonhardsen.studyapp.model.TipoItem;
import com.leonhardsen.studyapp.service.EventoService;
import com.leonhardsen.studyapp.service.ArquivoService;
import com.leonhardsen.studyapp.service.NotaService;
import com.leonhardsen.studyapp.service.UsuarioService;
import com.leonhardsen.studyapp.util.FormatadorData;
import com.leonhardsen.studyapp.util.SessionManager;
import com.leonhardsen.studyapp.util.SmtpConfigDialog;
import com.leonhardsen.studyapp.util.ThemeManager;
import com.leonhardsen.studyapp.view.ItemArvoreCell;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.*;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.util.data.MutableDataSet;

/**
 * Controlador da tela principal da aplicação.
 * Gerencia a topbar com relógio e dados do usuário, o painel de arquivos com a TreeView,
 * o editor Markdown com auto-save, o visualizador de PDF e o menu de configurações.
 *
 * @author StudyApp
 * @version 1.0
 */
public class MainController {

    // ── Topbar ──────────────────────────────────────────────────────────────
    @FXML private Label labelHora;
    @FXML private Label labelData;
    @FXML private Label labelUsuario;
    @FXML private Button btnConfiguracoes;

    // ── Navegação ─────────────────────────────────────────────────────────────
    @FXML private Button     btnNavDashboard;
    @FXML private Button     btnNavArquivos;
    @FXML private Button     btnNavTarefas;
    @FXML private Button     btnNavAgenda;
    @FXML private Button     btnNavBlocoNotas;
    @FXML private Button     btnNavCalculadora;
    @FXML private Button     btnNavPomodoro;
    @FXML private Button     btnNavPlanoEstudos;
    @FXML private SplitPane  splitPane;
    @FXML private VBox       painelArquivosContainer;
    @FXML private BorderPane painelTarefasContainer;
    @FXML private BorderPane painelAgendaContainer;
    @FXML private BorderPane painelBlocoNotasContainer;
    @FXML private BorderPane painelCalculadoraContainer;
    @FXML private BorderPane painelDashboardContainer;
    @FXML private BorderPane painelPomodoroContainer;
    @FXML private BorderPane painelPlanoEstudosContainer;

    // ── Painel de Arquivos ───────────────────────────────────────────────────
    @FXML private TreeView<ItemArvore> treeViewArquivos;
    @FXML private Button btnSubirItem;
    @FXML private Button btnDescerItem;
    @FXML private Button btnExcluirItem;
    @FXML private Button btnImprimirItem;

    // ── Painel Editor ────────────────────────────────────────────────────────
    @FXML private VBox painelVazio;
    @FXML private VBox painelEditor;
    @FXML private VBox painelPdf;
    @FXML private Label labelNomeNota;
    @FXML private Button btnToggleEditor;
    @FXML private HBox toolbarEditor;
    @FXML private TextArea textAreaEditor;
    @FXML private WebView webViewPreview;
    @FXML private Label labelStatusSave;

    // ── Visualizador PDF ─────────────────────────────────────────────────────
    @FXML private ScrollPane scrollPdf;
    @FXML private ImageView imagePdf;
    @FXML private Label labelPdfPagina;
    @FXML private Label labelPdfZoom;

    // ── Serviços ─────────────────────────────────────────────────────────────
    private final ArquivoService arquivoService = new ArquivoService();
    private final NotaService notaService = new NotaService();
    private final UsuarioService usuarioService = new UsuarioService();

    // ── Estado interno ────────────────────────────────────────────────────────
    private Timeline relogio;
    private PauseTransition pausaAutoSave;
    private ItemArvore itemAtual;
    private boolean emModoVisualizacao = false;

    // ── Carregamento lazy dos módulos ─────────────────────────────────────────
    private TarefasController    tarefasController;
    private AgendaController     agendaController;
    private BlocoNotasController blocoNotasController;
    private CalculadoraController calculadoraController;
    private DashboardController     dashboardController;
    private PomodoroController      pomodoroController;
    private PlanoEstudosController  planoEstudosController;

    // ── Notificações ──────────────────────────────────────────────────────────
    @FXML private Button btnNotificacoes;
    @FXML private Label lblBadgeNotif;
    private final Set<Integer> tarefasDismissadas = new HashSet<>();
    private final Set<Integer> eventosDismissados  = new HashSet<>();
    private List<Tarefa> tarefasAlerta = new ArrayList<>();
    private List<Evento> eventosHoje   = new ArrayList<>();
    private Timeline animacaoCampainhaUrgente;
    private Popup popupNotif;

    /** Documento PDF aberto atualmente no visualizador. */
    private PDDocument documentoPdfAberto;
    private PDFRenderer renderizadorPdf;
    private int paginaPdfAtual = 0;
    private float zoomPdf = 1.0f;

    /** Parser e renderer Flexmark para Markdown. */
    private final Parser parserMarkdown;
    private final HtmlRenderer rendererMarkdown;

    /**
     * Construtor que inicializa o parser e renderer Markdown com suporte a tabelas e riscado.
     */
    public MainController() {
        MutableDataSet opcoes = new MutableDataSet();
        opcoes.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create()
        ));
        parserMarkdown = Parser.builder(opcoes).build();
        rendererMarkdown = HtmlRenderer.builder(opcoes).build();
    }

    /**
     * Inicializa a tela principal: configura o relógio, a TreeView, o auto-save e carrega os dados.
     */
    @FXML
    public void initialize() {
        configurarRelogio();
        configurarUsuario();
        configurarTreeView();
        configurarAutoSave();
        carregarArvore();
        verificarPrazos();
        handleNavDashboard();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TOPBAR
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Configura o relógio da topbar para atualizar a cada segundo via {@link Timeline}.
     */
    private void configurarRelogio() {
        relogio = new Timeline(new KeyFrame(Duration.seconds(1), e -> atualizarRelogio()));
        relogio.setCycleCount(Timeline.INDEFINITE);
        relogio.play();
        atualizarRelogio();
    }

    /**
     * Atualiza os labels de hora e data na topbar com o horário e data atuais.
     */
    private void atualizarRelogio() {
        labelHora.setText(FormatadorData.formatarHora(LocalTime.now()));
        labelData.setText(FormatadorData.formatarDataLonga(LocalDate.now()));
    }

    /**
     * Exibe o nome do usuário logado na topbar.
     */
    private void configurarUsuario() {
        var usuario = SessionManager.getInstance().getUsuarioLogado();
        if (usuario != null) {
            labelUsuario.setText(usuario.getNome());
        }
    }

    /**
     * Trata o clique no botão de configurações, exibindo o menu de opções.
     */
    @FXML
    private void handleAbrirConfiguracoes() {
        ContextMenu menu = new ContextMenu();

        String temaAtual = ThemeManager.getInstance().getTemaAtual();
        MenuItem itemTema = new MenuItem("CLARO".equals(temaAtual) ? "🌙  Modo Escuro" : "☀  Modo Claro");
        itemTema.setOnAction(e -> handleAlternarTema());

        MenuItem itemPerfil = new MenuItem("👤  Perfil");
        itemPerfil.setOnAction(e -> handleAbrirPerfil());

        MenuItem itemEmail = new MenuItem("📧  Configurar E-mail SMTP");
        itemEmail.setOnAction(e -> SmtpConfigDialog.mostrar(StudyApplication.getStagePrincipal()));

        MenuItem itemLogout = new MenuItem("↩  Encerrar Sessão");
        itemLogout.setOnAction(e -> handleLogout());

        MenuItem itemSair = new MenuItem("✕  Sair do Aplicativo");
        itemSair.setOnAction(e -> handleSair());

        menu.getItems().addAll(itemPerfil, itemTema, itemEmail, new SeparatorMenuItem(), itemLogout, itemSair);
        menu.show(btnConfiguracoes, javafx.geometry.Side.BOTTOM, 0, 0);
    }

    /**
     * Alterna entre os temas claro e escuro e salva a preferência no banco de dados.
     */
    private void handleAlternarTema() {
        String novoTema = ThemeManager.getInstance().alternarTema();
        var usuario = SessionManager.getInstance().getUsuarioLogado();
        if (usuario != null) {
            new Thread(() -> {
                try {
                    usuarioService.atualizarTema(usuario.getId(), novoTema);
                    usuario.setTema(novoTema);
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro("Erro ao salvar tema: " + ex.getMessage()));
                }
            }).start();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // NAVEGAÇÃO ENTRE FUNCIONALIDADES
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleNavArquivos() {
        painelTarefasContainer.setVisible(false);
        painelTarefasContainer.setManaged(false);
        painelAgendaContainer.setVisible(false);
        painelAgendaContainer.setManaged(false);
        painelBlocoNotasContainer.setVisible(false);
        painelBlocoNotasContainer.setManaged(false);
        painelCalculadoraContainer.setVisible(false);
        painelCalculadoraContainer.setManaged(false);
        painelPomodoroContainer.setVisible(false);
        painelPomodoroContainer.setManaged(false);
        painelDashboardContainer.setVisible(false);
        painelDashboardContainer.setManaged(false);
        painelPlanoEstudosContainer.setVisible(false);
        painelPlanoEstudosContainer.setManaged(false);
        painelArquivosContainer.setVisible(true);
        painelArquivosContainer.setManaged(true);
        btnNavArquivos.getStyleClass().add("btn-nav-ativo");
        btnNavDashboard.getStyleClass().remove("btn-nav-ativo");
        btnNavTarefas.getStyleClass().remove("btn-nav-ativo");
        btnNavAgenda.getStyleClass().remove("btn-nav-ativo");
        btnNavBlocoNotas.getStyleClass().remove("btn-nav-ativo");
        btnNavCalculadora.getStyleClass().remove("btn-nav-ativo");
        btnNavPomodoro.getStyleClass().remove("btn-nav-ativo");
        btnNavPlanoEstudos.getStyleClass().remove("btn-nav-ativo");
    }

    @FXML
    private void handleNavTarefas() {
        if (tarefasController == null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                    StudyApplication.class.getResource("tarefas-view.fxml"));
                javafx.scene.Node view = loader.load();
                tarefasController = loader.getController();
                tarefasController.setOnTarefaAlterada(this::verificarPrazos);
                painelTarefasContainer.setCenter(view);
            } catch (Exception ex) {
                mostrarErro("Erro ao carregar painel de tarefas: " + ex.getMessage());
                return;
            }
        } else {
            tarefasController.atualizarView();
        }
        painelArquivosContainer.setVisible(false);
        painelArquivosContainer.setManaged(false);
        painelAgendaContainer.setVisible(false);
        painelAgendaContainer.setManaged(false);
        painelBlocoNotasContainer.setVisible(false);
        painelBlocoNotasContainer.setManaged(false);
        painelCalculadoraContainer.setVisible(false);
        painelCalculadoraContainer.setManaged(false);
        painelPomodoroContainer.setVisible(false);
        painelPomodoroContainer.setManaged(false);
        painelDashboardContainer.setVisible(false);
        painelDashboardContainer.setManaged(false);
        painelPlanoEstudosContainer.setVisible(false);
        painelPlanoEstudosContainer.setManaged(false);
        painelTarefasContainer.setVisible(true);
        painelTarefasContainer.setManaged(true);
        btnNavTarefas.getStyleClass().add("btn-nav-ativo");
        btnNavDashboard.getStyleClass().remove("btn-nav-ativo");
        btnNavArquivos.getStyleClass().remove("btn-nav-ativo");
        btnNavAgenda.getStyleClass().remove("btn-nav-ativo");
        btnNavBlocoNotas.getStyleClass().remove("btn-nav-ativo");
        btnNavCalculadora.getStyleClass().remove("btn-nav-ativo");
        btnNavPomodoro.getStyleClass().remove("btn-nav-ativo");
        btnNavPlanoEstudos.getStyleClass().remove("btn-nav-ativo");
    }

    @FXML
    private void handleNavAgenda() {
        if (agendaController == null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                    StudyApplication.class.getResource("agenda-view.fxml"));
                javafx.scene.Node view = loader.load();
                agendaController = loader.getController();
                agendaController.setOnEventoAlterado(this::verificarPrazos);
                painelAgendaContainer.setCenter(view);
            } catch (Exception ex) {
                mostrarErro("Erro ao carregar agenda: " + ex.getMessage());
                return;
            }
        } else {
            agendaController.atualizarView();
        }
        painelArquivosContainer.setVisible(false);
        painelArquivosContainer.setManaged(false);
        painelTarefasContainer.setVisible(false);
        painelTarefasContainer.setManaged(false);
        painelBlocoNotasContainer.setVisible(false);
        painelBlocoNotasContainer.setManaged(false);
        painelCalculadoraContainer.setVisible(false);
        painelCalculadoraContainer.setManaged(false);
        painelPomodoroContainer.setVisible(false);
        painelPomodoroContainer.setManaged(false);
        painelDashboardContainer.setVisible(false);
        painelDashboardContainer.setManaged(false);
        painelPlanoEstudosContainer.setVisible(false);
        painelPlanoEstudosContainer.setManaged(false);
        painelAgendaContainer.setVisible(true);
        painelAgendaContainer.setManaged(true);
        btnNavAgenda.getStyleClass().add("btn-nav-ativo");
        btnNavDashboard.getStyleClass().remove("btn-nav-ativo");
        btnNavArquivos.getStyleClass().remove("btn-nav-ativo");
        btnNavTarefas.getStyleClass().remove("btn-nav-ativo");
        btnNavBlocoNotas.getStyleClass().remove("btn-nav-ativo");
        btnNavCalculadora.getStyleClass().remove("btn-nav-ativo");
        btnNavPomodoro.getStyleClass().remove("btn-nav-ativo");
        btnNavPlanoEstudos.getStyleClass().remove("btn-nav-ativo");
    }

    @FXML
    private void handleNavBlocoNotas() {
        if (blocoNotasController == null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                    StudyApplication.class.getResource("bloco-notas-view.fxml"));
                javafx.scene.Node view = loader.load();
                blocoNotasController = loader.getController();
                blocoNotasController.setContainer(painelBlocoNotasContainer);
                painelBlocoNotasContainer.setCenter(view);
            } catch (Exception ex) {
                mostrarErro("Erro ao carregar bloco de notas: " + ex.getMessage());
                return;
            }
        }
        painelArquivosContainer.setVisible(false);
        painelArquivosContainer.setManaged(false);
        painelTarefasContainer.setVisible(false);
        painelTarefasContainer.setManaged(false);
        painelAgendaContainer.setVisible(false);
        painelAgendaContainer.setManaged(false);
        painelCalculadoraContainer.setVisible(false);
        painelCalculadoraContainer.setManaged(false);
        painelPomodoroContainer.setVisible(false);
        painelPomodoroContainer.setManaged(false);
        painelDashboardContainer.setVisible(false);
        painelDashboardContainer.setManaged(false);
        painelPlanoEstudosContainer.setVisible(false);
        painelPlanoEstudosContainer.setManaged(false);
        painelBlocoNotasContainer.setVisible(true);
        painelBlocoNotasContainer.setManaged(true);
        btnNavBlocoNotas.getStyleClass().add("btn-nav-ativo");
        btnNavDashboard.getStyleClass().remove("btn-nav-ativo");
        btnNavArquivos.getStyleClass().remove("btn-nav-ativo");
        btnNavTarefas.getStyleClass().remove("btn-nav-ativo");
        btnNavAgenda.getStyleClass().remove("btn-nav-ativo");
        btnNavCalculadora.getStyleClass().remove("btn-nav-ativo");
        btnNavPomodoro.getStyleClass().remove("btn-nav-ativo");
        btnNavPlanoEstudos.getStyleClass().remove("btn-nav-ativo");
    }

    @FXML
    private void handleNavCalculadora() {
        if (calculadoraController == null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                    StudyApplication.class.getResource("calculadora-view.fxml"));
                javafx.scene.Node view = loader.load();
                calculadoraController = loader.getController();
                calculadoraController.setContainer(painelCalculadoraContainer);
                painelCalculadoraContainer.setCenter(view);
            } catch (Exception ex) {
                mostrarErro("Erro ao carregar calculadora: " + ex.getMessage());
                return;
            }
        }
        painelArquivosContainer.setVisible(false);
        painelArquivosContainer.setManaged(false);
        painelTarefasContainer.setVisible(false);
        painelTarefasContainer.setManaged(false);
        painelAgendaContainer.setVisible(false);
        painelAgendaContainer.setManaged(false);
        painelBlocoNotasContainer.setVisible(false);
        painelBlocoNotasContainer.setManaged(false);
        painelPomodoroContainer.setVisible(false);
        painelPomodoroContainer.setManaged(false);
        painelDashboardContainer.setVisible(false);
        painelDashboardContainer.setManaged(false);
        painelPlanoEstudosContainer.setVisible(false);
        painelPlanoEstudosContainer.setManaged(false);
        painelCalculadoraContainer.setVisible(true);
        painelCalculadoraContainer.setManaged(true);
        btnNavCalculadora.getStyleClass().add("btn-nav-ativo");
        btnNavDashboard.getStyleClass().remove("btn-nav-ativo");
        btnNavArquivos.getStyleClass().remove("btn-nav-ativo");
        btnNavTarefas.getStyleClass().remove("btn-nav-ativo");
        btnNavAgenda.getStyleClass().remove("btn-nav-ativo");
        btnNavBlocoNotas.getStyleClass().remove("btn-nav-ativo");
        btnNavPomodoro.getStyleClass().remove("btn-nav-ativo");
        btnNavPlanoEstudos.getStyleClass().remove("btn-nav-ativo");
    }

    @FXML
    private void handleNavPomodoro() {
        if (pomodoroController == null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                    StudyApplication.class.getResource("pomodoro-view.fxml"));
                javafx.scene.Node view = loader.load();
                pomodoroController = loader.getController();
                painelPomodoroContainer.setCenter(view);
            } catch (Exception ex) {
                mostrarErro("Erro ao carregar Pomodoro: " + ex.getMessage());
                return;
            }
        } else {
            pomodoroController.atualizarView();
        }
        painelArquivosContainer.setVisible(false);
        painelArquivosContainer.setManaged(false);
        painelTarefasContainer.setVisible(false);
        painelTarefasContainer.setManaged(false);
        painelAgendaContainer.setVisible(false);
        painelAgendaContainer.setManaged(false);
        painelBlocoNotasContainer.setVisible(false);
        painelBlocoNotasContainer.setManaged(false);
        painelCalculadoraContainer.setVisible(false);
        painelCalculadoraContainer.setManaged(false);
        painelDashboardContainer.setVisible(false);
        painelDashboardContainer.setManaged(false);
        painelPlanoEstudosContainer.setVisible(false);
        painelPlanoEstudosContainer.setManaged(false);
        painelPomodoroContainer.setVisible(true);
        painelPomodoroContainer.setManaged(true);
        btnNavPomodoro.getStyleClass().add("btn-nav-ativo");
        btnNavDashboard.getStyleClass().remove("btn-nav-ativo");
        btnNavArquivos.getStyleClass().remove("btn-nav-ativo");
        btnNavTarefas.getStyleClass().remove("btn-nav-ativo");
        btnNavAgenda.getStyleClass().remove("btn-nav-ativo");
        btnNavBlocoNotas.getStyleClass().remove("btn-nav-ativo");
        btnNavCalculadora.getStyleClass().remove("btn-nav-ativo");
        btnNavPlanoEstudos.getStyleClass().remove("btn-nav-ativo");
    }

    @FXML
    private void handleNavDashboard() {
        if (dashboardController == null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                    StudyApplication.class.getResource("dashboard-view.fxml"));
                javafx.scene.Node view = loader.load();
                dashboardController = loader.getController();
                dashboardController.setOnVerTarefas(this::handleNavTarefas);
                dashboardController.setOnVerAgenda(this::handleNavAgenda);
                dashboardController.setOnVerPlanoEstudos(this::handleNavPlanoEstudos);
                dashboardController.setOnEstudarAssunto(this::navegarParaPomodoroComAssunto);
                painelDashboardContainer.setCenter(view);
            } catch (Exception ex) {
                mostrarErro("Erro ao carregar Dashboard: " + ex.getMessage());
                return;
            }
        } else {
            dashboardController.atualizarView();
        }
        painelArquivosContainer.setVisible(false);
        painelArquivosContainer.setManaged(false);
        painelTarefasContainer.setVisible(false);
        painelTarefasContainer.setManaged(false);
        painelAgendaContainer.setVisible(false);
        painelAgendaContainer.setManaged(false);
        painelBlocoNotasContainer.setVisible(false);
        painelBlocoNotasContainer.setManaged(false);
        painelCalculadoraContainer.setVisible(false);
        painelCalculadoraContainer.setManaged(false);
        painelPomodoroContainer.setVisible(false);
        painelPomodoroContainer.setManaged(false);
        painelPlanoEstudosContainer.setVisible(false);
        painelPlanoEstudosContainer.setManaged(false);
        painelDashboardContainer.setVisible(true);
        painelDashboardContainer.setManaged(true);
        btnNavDashboard.getStyleClass().add("btn-nav-ativo");
        btnNavArquivos.getStyleClass().remove("btn-nav-ativo");
        btnNavTarefas.getStyleClass().remove("btn-nav-ativo");
        btnNavAgenda.getStyleClass().remove("btn-nav-ativo");
        btnNavBlocoNotas.getStyleClass().remove("btn-nav-ativo");
        btnNavCalculadora.getStyleClass().remove("btn-nav-ativo");
        btnNavPomodoro.getStyleClass().remove("btn-nav-ativo");
        btnNavPlanoEstudos.getStyleClass().remove("btn-nav-ativo");
    }

    @FXML
    private void handleNavPlanoEstudos() {
        if (planoEstudosController == null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                    StudyApplication.class.getResource("plano-estudos-view.fxml"));
                javafx.scene.Node view = loader.load();
                planoEstudosController = loader.getController();
                planoEstudosController.setOnEstudarAssunto(this::navegarParaPomodoroComAssunto);
                painelPlanoEstudosContainer.setCenter(view);
            } catch (Exception ex) {
                mostrarErro("Erro ao carregar Plano de Estudos: " + ex.getMessage());
                return;
            }
        } else {
            planoEstudosController.atualizarView();
        }
        painelArquivosContainer.setVisible(false);
        painelArquivosContainer.setManaged(false);
        painelTarefasContainer.setVisible(false);
        painelTarefasContainer.setManaged(false);
        painelAgendaContainer.setVisible(false);
        painelAgendaContainer.setManaged(false);
        painelBlocoNotasContainer.setVisible(false);
        painelBlocoNotasContainer.setManaged(false);
        painelCalculadoraContainer.setVisible(false);
        painelCalculadoraContainer.setManaged(false);
        painelDashboardContainer.setVisible(false);
        painelDashboardContainer.setManaged(false);
        painelPomodoroContainer.setVisible(false);
        painelPomodoroContainer.setManaged(false);
        painelPlanoEstudosContainer.setVisible(true);
        painelPlanoEstudosContainer.setManaged(true);
        btnNavPlanoEstudos.getStyleClass().add("btn-nav-ativo");
        btnNavDashboard.getStyleClass().remove("btn-nav-ativo");
        btnNavArquivos.getStyleClass().remove("btn-nav-ativo");
        btnNavTarefas.getStyleClass().remove("btn-nav-ativo");
        btnNavAgenda.getStyleClass().remove("btn-nav-ativo");
        btnNavBlocoNotas.getStyleClass().remove("btn-nav-ativo");
        btnNavCalculadora.getStyleClass().remove("btn-nav-ativo");
        btnNavPomodoro.getStyleClass().remove("btn-nav-ativo");
    }

    /**
     * Navega para o Pomodoro pré-selecionando o assunto informado.
     * Invocado como callback pelo {@link PlanoEstudosController}.
     *
     * @param assunto assunto a pré-selecionar no timer Pomodoro
     */
    private void navegarParaPomodoroComAssunto(Assunto assunto) {
        handleNavPomodoro();
        if (pomodoroController != null) {
            pomodoroController.selecionarAssuntoExterno(assunto);
        }
    }

    /**
     * Carrega tarefas com prazo vencido/próximo e eventos de hoje, atualizando o badge do sino.
     * Executado assincronamente após inicialização ou quando um evento é criado/editado.
     */
    private void verificarPrazos() {
        var usuario = SessionManager.getInstance().getUsuarioLogado();
        if (usuario == null) return;
        new Thread(() -> {
            try {
                var tarefaService = new com.leonhardsen.studyapp.service.TarefaService();
                List<Tarefa> alertas = tarefaService.buscarTarefasComAlerta(usuario.getId());
                List<Evento> eventos = new EventoService().buscarEventosHoje(usuario.getId());
                Platform.runLater(() -> {
                    tarefasAlerta = alertas;
                    eventosHoje   = eventos;
                    atualizarBadge();
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void atualizarBadge() {
        List<Tarefa> tarefasVis = new ArrayList<>(tarefasAlerta);
        tarefasVis.removeIf(t -> tarefasDismissadas.contains(t.getId()));
        List<Evento> eventosVis = new ArrayList<>(eventosHoje);
        eventosVis.removeIf(e -> eventosDismissados.contains(e.getId()));

        int total = tarefasVis.size() + eventosVis.size();
        if (total == 0) {
            lblBadgeNotif.setVisible(false);
            lblBadgeNotif.setManaged(false);
            pararAnimacaoCampainha();
            return;
        }

        lblBadgeNotif.setText(total > 9 ? "9+" : String.valueOf(total));
        lblBadgeNotif.setVisible(true);
        lblBadgeNotif.setManaged(true);

        LocalDate hoje = LocalDate.now();
        boolean temVencida = tarefasVis.stream().anyMatch(t -> t.getDataVencimento().isBefore(hoje));
        if (temVencida) iniciarAnimacaoCampainha();
        else pararAnimacaoCampainha();
    }

    @FXML
    private void handleNotificacoes() {
        if (popupNotif != null && popupNotif.isShowing()) {
            popupNotif.hide();
            return;
        }

        LocalDate hoje = LocalDate.now();
        List<Tarefa> tarefasVis = new ArrayList<>(tarefasAlerta);
        tarefasVis.removeIf(t -> tarefasDismissadas.contains(t.getId()));
        List<Evento> eventosVis = new ArrayList<>(eventosHoje);
        eventosVis.removeIf(e -> eventosDismissados.contains(e.getId()));
        int total = tarefasVis.size() + eventosVis.size();

        VBox conteudo = new VBox();
        conteudo.setMinWidth(320);
        conteudo.setMaxWidth(320);
        conteudo.getStyleClass().add("notif-popup-root");
        conteudo.getStylesheets().addAll(btnNotificacoes.getScene().getStylesheets());

        HBox header = new HBox();
        header.getStyleClass().add("notif-header");
        Label lblTitulo = new Label("🔔  Notificações (" + total + ")");
        lblTitulo.getStyleClass().add("notif-header-label");
        HBox.setHgrow(lblTitulo, Priority.ALWAYS);
        header.getChildren().add(lblTitulo);
        conteudo.getChildren().add(header);

        if (total == 0) {
            Label vazio = new Label("Nenhuma notificação no momento.");
            vazio.getStyleClass().add("notif-empty");
            conteudo.getChildren().add(vazio);
        } else {
            VBox lista = new VBox();
            for (Evento ev : eventosVis) {
                lista.getChildren().add(criarItemNotificacaoEvento(ev));
            }
            for (Tarefa t : tarefasVis) {
                lista.getChildren().add(criarItemNotificacao(t, hoje));
            }
            ScrollPane scroll = new ScrollPane(lista);
            scroll.setFitToWidth(true);
            scroll.setMaxHeight(380);
            scroll.getStyleClass().add("notif-scroll");
            conteudo.getChildren().add(scroll);
        }

        popupNotif = new Popup();
        popupNotif.setAutoHide(true);
        popupNotif.getContent().add(conteudo);

        javafx.geometry.Bounds b = btnNotificacoes.localToScreen(btnNotificacoes.getBoundsInLocal());
        popupNotif.show(btnNotificacoes.getScene().getWindow(),
                b.getMaxX() - 320, b.getMaxY() + 4);
    }

    private HBox criarItemNotificacao(Tarefa t, LocalDate hoje) {
        LocalDate venc = t.getDataVencimento();
        long diff = venc.toEpochDay() - hoje.toEpochDay();
        String corStrip;
        String subTexto;

        if (diff < 0) {
            corStrip = "#E53935";
            subTexto = "Venceu há " + (-diff) + " dia(s)";
        } else if (diff == 0) {
            corStrip = "#FB8C00";
            subTexto = "Vence HOJE!";
        } else if (diff == 1) {
            corStrip = "#FB8C00";
            subTexto = "Vence amanhã";
        } else {
            corStrip = "#F9A825";
            subTexto = "Em " + diff + " dia(s)";
        }

        Region strip = new Region();
        strip.setPrefWidth(5);
        strip.setMinWidth(5);
        strip.setMaxWidth(5);
        strip.setStyle("-fx-background-color: " + corStrip + ";");

        Label lblTit = new Label(t.getTitulo());
        lblTit.getStyleClass().add("notif-item-text-titulo");
        lblTit.setMaxWidth(215);
        lblTit.setWrapText(true);

        Label lblSub = new Label(subTexto + "  •  " + t.getPrioridade().getDescricao());
        lblSub.getStyleClass().add("notif-item-text-sub");

        VBox textos = new VBox(3);
        textos.setPadding(new javafx.geometry.Insets(8, 8, 8, 10));
        textos.getChildren().addAll(lblTit, lblSub);
        HBox.setHgrow(textos, Priority.ALWAYS);
        textos.setStyle("-fx-cursor: hand;");

        Button btnX = new Button("×");
        btnX.getStyleClass().add("notif-btn-fechar");

        HBox item = new HBox(strip, textos, btnX);
        item.getStyleClass().add("notif-item");

        final int tarefaId = t.getId();
        textos.setOnMouseClicked(e -> {
            popupNotif.hide();
            navegarParaTarefaComId(tarefaId);
        });
        btnX.setOnAction(e -> {
            tarefasDismissadas.add(tarefaId);
            atualizarBadge();
            if (popupNotif != null) popupNotif.hide();
        });

        return item;
    }

    private HBox criarItemNotificacaoEvento(Evento ev) {
        String subTexto;
        if (ev.getHoraInicio() != null) {
            subTexto = "🕐 " + ev.getHoraInicio().format(java.time.format.DateTimeFormatter.ofPattern("H:mm"));
            if (ev.getHoraFim() != null)
                subTexto += " – " + ev.getHoraFim().format(java.time.format.DateTimeFormatter.ofPattern("H:mm"));
        } else {
            subTexto = "📅 Dia inteiro";
        }

        Region strip = new Region();
        strip.setPrefWidth(5);
        strip.setMinWidth(5);
        strip.setMaxWidth(5);
        strip.setStyle("-fx-background-color: #1976D2;");

        Label lblTit = new Label(ev.getTitulo());
        lblTit.getStyleClass().add("notif-item-text-titulo");
        lblTit.setMaxWidth(215);
        lblTit.setWrapText(true);

        Label lblSub = new Label(subTexto + "  •  Evento de hoje");
        lblSub.getStyleClass().add("notif-item-text-sub");

        VBox textos = new VBox(3);
        textos.setPadding(new javafx.geometry.Insets(8, 8, 8, 10));
        textos.getChildren().addAll(lblTit, lblSub);
        HBox.setHgrow(textos, Priority.ALWAYS);
        textos.setStyle("-fx-cursor: hand;");

        Button btnX = new Button("×");
        btnX.getStyleClass().add("notif-btn-fechar");

        HBox item = new HBox(strip, textos, btnX);
        item.getStyleClass().add("notif-item");

        final int eventoId = ev.getId();
        textos.setOnMouseClicked(e -> {
            popupNotif.hide();
            navegarParaAgendaHoje();
        });
        btnX.setOnAction(e -> {
            eventosDismissados.add(eventoId);
            atualizarBadge();
            if (popupNotif != null) popupNotif.hide();
        });

        return item;
    }

    private void navegarParaAgendaHoje() {
        handleNavAgenda();
        if (agendaController != null) {
            agendaController.selecionarDia(LocalDate.now());
        }
    }

    private void navegarParaTarefaComId(int tarefaId) {
        handleNavTarefas();
        if (tarefasController != null) {
            tarefasController.selecionarTarefaPorId(tarefaId);
        }
    }

    private void iniciarAnimacaoCampainha() {
        if (animacaoCampainhaUrgente != null
                && animacaoCampainhaUrgente.getStatus() == javafx.animation.Animation.Status.RUNNING) {
            return;
        }
        final boolean[] flag = {false};
        animacaoCampainhaUrgente = new Timeline(new KeyFrame(Duration.seconds(0.7), e -> {
            flag[0] = !flag[0];
            btnNotificacoes.setStyle(flag[0]
                    ? "-fx-background-color: #E53935; -fx-background-radius: 6; -fx-text-fill: white;"
                    : "");
        }));
        animacaoCampainhaUrgente.setCycleCount(Timeline.INDEFINITE);
        animacaoCampainhaUrgente.play();
    }

    private void pararAnimacaoCampainha() {
        if (animacaoCampainhaUrgente != null) {
            animacaoCampainhaUrgente.stop();
            animacaoCampainhaUrgente = null;
        }
        if (btnNotificacoes != null) btnNotificacoes.setStyle("");
    }

    /**
     * Abre a tela de perfil do usuário como janela modal.
     */
    private void handleAbrirPerfil() {
        try {
            FXMLLoader loader = new FXMLLoader(StudyApplication.class.getResource("perfil-view.fxml"));
            Scene cena = new Scene(loader.load());
            ThemeManager.getInstance().setCena(cena);
            ThemeManager.getInstance().aplicarTemaAtual();
            Stage stagePerfil = new Stage();
            stagePerfil.setTitle("Perfil");
            stagePerfil.setScene(cena);
            stagePerfil.initModality(Modality.APPLICATION_MODAL);
            stagePerfil.initOwner(StudyApplication.getStagePrincipal());
            stagePerfil.setWidth(480);
            stagePerfil.setHeight(580);
            stagePerfil.showAndWait();
            // Reaplica tema caso o usuário tenha alterado no perfil
            ThemeManager.getInstance().setCena(btnConfiguracoes.getScene());
            ThemeManager.getInstance().aplicarTemaAtual();
        } catch (Exception ex) {
            mostrarErro("Erro ao abrir perfil: " + ex.getMessage());
        }
    }

    /**
     * Encerra a sessão do usuário e retorna para a tela de login.
     */
    private void handleLogout() {
        pararRecursos();
        SessionManager.getInstance().logout();
        try {
            StudyApplication.mostrarTelaLogin();
        } catch (Exception ex) {
            mostrarErro("Erro ao retornar ao login: " + ex.getMessage());
        }
    }

    /**
     * Exibe diálogo de confirmação e encerra a aplicação.
     */
    private void handleSair() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Sair");
        confirm.setHeaderText(null);
        confirm.setContentText("Deseja fechar o StudyApp?");
        ThemeManager.getInstance().aplicarTemaAoDialogo(confirm);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                pararRecursos();
                javafx.application.Platform.exit();
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SISTEMA DE ARQUIVOS / TREEVIEW
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Configura a TreeView: raiz invisível, factory de células customizadas e seleção por clique.
     */
    private void configurarTreeView() {
        TreeItem<ItemArvore> raiz = new TreeItem<>();
        raiz.setExpanded(true);
        treeViewArquivos.setRoot(raiz);
        treeViewArquivos.setShowRoot(false);

        treeViewArquivos.setCellFactory(tv -> {
            ItemArvoreCell celula = new ItemArvoreCell();
            celula.setOnNovoCaderno(() -> handleNovoCadernoNoItem(celula.getItem()));
            celula.setOnNovaNota(() -> handleNovaNota());
            celula.setOnNovoPdf(() -> handleNovoPdf());
            celula.setOnRenomear(() -> handleRenomear(celula.getItem()));
            celula.setOnExcluir(() -> handleExcluir(celula.getItem()));
            celula.setOnMover(() -> handleMover(celula.getItem()));
            celula.setOnSubir(() -> handleSubir(celula.getItem()));
            celula.setOnDescer(() -> handleDescer(celula.getItem()));
            celula.setOnImprimir(() -> handleImprimir(celula.getItem()));
            return celula;
        });

        treeViewArquivos.getSelectionModel().selectedItemProperty().addListener(
            (obs, anterior, novo) -> atualizarBotoesSelecao(novo));

        treeViewArquivos.setOnKeyPressed(event -> {
            TreeItem<ItemArvore> sel = treeViewArquivos.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            switch (event.getCode()) {
                case DELETE -> { handleExcluir(sel.getValue()); event.consume(); }
                case UP -> { if (event.isControlDown()) { handleSubir(sel.getValue()); event.consume(); } }
                case DOWN -> { if (event.isControlDown()) { handleDescer(sel.getValue()); event.consume(); } }
                default -> {}
            }
        });
    }

    /**
     * Carrega todos os itens da árvore do banco de dados e reconstrói a hierarquia na TreeView.
     */
    public void carregarArvore() {
        int usuarioId = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                List<ItemArvore> itens = arquivoService.buscarTodos(usuarioId);
                Platform.runLater(() -> construirArvore(itens));
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro ao carregar arquivos: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Reconstrói a hierarquia da TreeView a partir de uma lista plana de itens.
     *
     * @param itens lista plana de todos os itens do usuário
     */
    private void construirArvore(List<ItemArvore> itens) {
        treeViewArquivos.getRoot().getChildren().clear();
        Map<Integer, TreeItem<ItemArvore>> mapa = new LinkedHashMap<>();
        for (ItemArvore item : itens) {
            mapa.put(item.getId(), new TreeItem<>(item));
        }
        for (ItemArvore item : itens) {
            TreeItem<ItemArvore> treeItem = mapa.get(item.getId());
            if (item.getPaiId() == null) {
                treeViewArquivos.getRoot().getChildren().add(treeItem);
            } else {
                TreeItem<ItemArvore> pai = mapa.get(item.getPaiId());
                if (pai != null) pai.getChildren().add(treeItem);
            }
            if (item.getTipo() == TipoItem.CADERNO) treeItem.setExpanded(true);
        }
    }

    /**
     * Trata o clique simples na TreeView, abrindo o item selecionado na área de conteúdo.
     */
    @FXML
    private void handleSelecaoTree() {
        TreeItem<ItemArvore> selecionado = treeViewArquivos.getSelectionModel().getSelectedItem();
        if (selecionado == null) return;
        abrirItem(selecionado.getValue());
    }

    /**
     * Abre um item da árvore na área de conteúdo conforme seu tipo.
     *
     * @param item item a ser aberto
     */
    private void abrirItem(ItemArvore item) {
        itemAtual = item;
        switch (item.getTipo()) {
            case NOTA -> abrirNota(item);
            case PDF -> abrirPdf(item);
            case CADERNO -> mostrarPainelVazio();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // OPERAÇÕES DA ÁRVORE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Trata o botão "+ Caderno" da barra de ações.
     * Cria um sub-caderno dentro do caderno selecionado, ou um caderno raiz se nada estiver selecionado.
     */
    @FXML
    private void handleNovoCaderno() {
        TreeItem<ItemArvore> sel = treeViewArquivos.getSelectionModel().getSelectedItem();
        if (sel != null && sel.getValue().getTipo() == TipoItem.CADERNO) {
            criarCaderno(sel.getValue().getId());
        } else {
            criarCaderno(null);
        }
    }

    /**
     * Cria um caderno filho dentro do item selecionado via menu de contexto.
     *
     * @param pai item pai onde o novo caderno será criado
     */
    private void handleNovoCadernoNoItem(ItemArvore pai) {
        criarCaderno(pai != null ? pai.getId() : null);
    }

    /**
     * Exibe diálogo para criar um novo caderno com o pai especificado.
     *
     * @param paiId id do caderno pai, ou {@code null} para caderno raiz
     */
    private void criarCaderno(Integer paiId) {
        TextInputDialog dialog = new TextInputDialog("Novo Caderno");
        dialog.setTitle("Novo Caderno");
        dialog.setHeaderText(null);
        dialog.setContentText("Nome do caderno:");
        ThemeManager.getInstance().aplicarTemaAoDialogo(dialog);
        dialog.showAndWait().ifPresent(nome -> {
            if (nome.isBlank()) return;
            int usuarioId = SessionManager.getInstance().getUsuarioLogado().getId();
            ItemArvore item = new ItemArvore();
            item.setUsuarioId(usuarioId);
            item.setPaiId(paiId);
            item.setNome(nome.trim());
            item.setTipo(TipoItem.CADERNO);
            item.setPosicao(0);
            new Thread(() -> {
                try {
                    arquivoService.criarItem(item);
                    Platform.runLater(this::carregarArvore);
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro("Erro ao criar caderno: " + ex.getMessage()));
                }
            }).start();
        });
    }

    /**
     * Trata o botão "+ Nota" da barra de ações ou do menu de contexto,
     * abrindo o diálogo de criação de nova nota.
     */
    @FXML
    public void handleNovaNota() {
        int usuarioId = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                List<ItemArvore> cadernos = arquivoService.buscarCadernos(usuarioId);
                Platform.runLater(() -> {
                    if (cadernos.isEmpty()) {
                        mostrarErro("Crie um caderno antes de adicionar notas.");
                        return;
                    }
                    exibirDialogoNovaNota(cadernos);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Exibe o diálogo de criação de nova nota com seleção de caderno.
     *
     * @param cadernos lista de cadernos disponíveis como destino
     */
    private void exibirDialogoNovaNota(List<ItemArvore> cadernos) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Nova Nota");
        dialog.setHeaderText("Criar nova nota");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ThemeManager.getInstance().aplicarTemaAoDialogo(dialog);

        TextField tfNome = new TextField();
        tfNome.setPromptText("Nome da nota");
        ComboBox<ItemArvore> cbCaderno = new ComboBox<>();
        cbCaderno.getItems().addAll(cadernos);
        cbCaderno.setValue(obterCadernoSelecionado(cadernos));
        cbCaderno.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ItemArvore it, boolean empty) {
                super.updateItem(it, empty);
                setText(empty || it == null ? null : it.getNome());
            }
        });
        cbCaderno.setButtonCell(cbCaderno.getCellFactory().call(null));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(12));
        grid.add(new Label("Nome:"), 0, 0);
        grid.add(tfNome, 1, 0);
        grid.add(new Label("Caderno:"), 0, 1);
        grid.add(cbCaderno, 1, 1);
        GridPane.setHgrow(tfNome, Priority.ALWAYS);
        GridPane.setHgrow(cbCaderno, Priority.ALWAYS);
        dialog.getDialogPane().setContent(grid);
        Platform.runLater(tfNome::requestFocus);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String nome = tfNome.getText().trim();
            ItemArvore caderno = cbCaderno.getValue();
            if (nome.isBlank() || caderno == null) return;

            int usuarioId = SessionManager.getInstance().getUsuarioLogado().getId();
            ItemArvore item = new ItemArvore();
            item.setUsuarioId(usuarioId);
            item.setPaiId(caderno.getId());
            item.setNome(nome);
            item.setTipo(TipoItem.NOTA);
            item.setPosicao(0);
            new Thread(() -> {
                try {
                    arquivoService.criarItem(item);
                    Platform.runLater(() -> {
                        carregarArvore();
                        abrirNota(item);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro("Erro ao criar nota: " + ex.getMessage()));
                }
            }).start();
        });
    }

    /**
     * Retorna o caderno atualmente selecionado na TreeView, ou o primeiro disponível.
     *
     * @param cadernos lista de cadernos disponíveis
     * @return caderno selecionado ou o primeiro da lista
     */
    private ItemArvore obterCadernoSelecionado(List<ItemArvore> cadernos) {
        TreeItem<ItemArvore> sel = treeViewArquivos.getSelectionModel().getSelectedItem();
        if (sel != null) {
            ItemArvore item = sel.getValue();
            if (item.getTipo() == TipoItem.CADERNO) return item;
            // Busca o pai
            for (ItemArvore c : cadernos) {
                if (c.getId() == (item.getPaiId() != null ? item.getPaiId() : -1)) return c;
            }
        }
        return cadernos.isEmpty() ? null : cadernos.get(0);
    }

    /**
     * Trata o botão "+ PDF" da barra de ações ou do menu de contexto.
     * Abre o seletor de arquivo e realiza o upload do PDF selecionado.
     */
    @FXML
    public void handleNovoPdf() {
        int usuarioId = SessionManager.getInstance().getUsuarioLogado().getId();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecionar PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documentos PDF", "*.pdf"));
        File arquivo = chooser.showOpenDialog(StudyApplication.getStagePrincipal());
        if (arquivo == null) return;

        new Thread(() -> {
            try {
                List<ItemArvore> cadernos = arquivoService.buscarCadernos(usuarioId);
                Platform.runLater(() -> {
                    if (cadernos.isEmpty()) {
                        mostrarErro("Crie um caderno antes de adicionar PDFs.");
                        return;
                    }
                    exibirDialogoNovoPdf(cadernos, arquivo);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Exibe diálogo para confirmar nome e caderno de destino do PDF antes do upload.
     *
     * @param cadernos lista de cadernos disponíveis
     * @param arquivo  arquivo PDF selecionado
     */
    private void exibirDialogoNovoPdf(List<ItemArvore> cadernos, File arquivo) {
        String nomeSemExtensao = arquivo.getName().replaceAll("\\.pdf$", "");
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Adicionar PDF");
        dialog.setHeaderText("Importar documento PDF");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ThemeManager.getInstance().aplicarTemaAoDialogo(dialog);

        TextField tfNome = new TextField(nomeSemExtensao);
        ComboBox<ItemArvore> cbCaderno = new ComboBox<>();
        cbCaderno.getItems().addAll(cadernos);
        cbCaderno.setValue(obterCadernoSelecionado(cadernos));
        cbCaderno.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ItemArvore it, boolean empty) {
                super.updateItem(it, empty);
                setText(empty || it == null ? null : it.getNome());
            }
        });
        cbCaderno.setButtonCell(cbCaderno.getCellFactory().call(null));

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(12));
        grid.add(new Label("Nome:"), 0, 0);
        grid.add(tfNome, 1, 0);
        grid.add(new Label("Caderno:"), 0, 1);
        grid.add(cbCaderno, 1, 1);
        GridPane.setHgrow(tfNome, Priority.ALWAYS);
        GridPane.setHgrow(cbCaderno, Priority.ALWAYS);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String nome = tfNome.getText().trim();
            ItemArvore caderno = cbCaderno.getValue();
            if (nome.isBlank() || caderno == null) return;

            int usuarioId = SessionManager.getInstance().getUsuarioLogado().getId();
            ItemArvore item = new ItemArvore();
            item.setUsuarioId(usuarioId);
            item.setPaiId(caderno.getId());
            item.setNome(nome);
            item.setTipo(TipoItem.PDF);
            item.setPosicao(0);
            new Thread(() -> {
                try {
                    arquivoService.criarItem(item);
                    arquivoService.uploadPdf(item, arquivo);
                    Platform.runLater(() -> {
                        carregarArvore();
                        abrirItem(item);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro("Erro ao importar PDF: " + ex.getMessage()));
                }
            }).start();
        });
    }

    /**
     * Trata a solicitação de renomeação de um item via menu de contexto.
     *
     * @param item item a ser renomeado
     */
    private void handleRenomear(ItemArvore item) {
        if (item == null) return;
        TextInputDialog dialog = new TextInputDialog(item.getNome());
        dialog.setTitle("Renomear");
        dialog.setHeaderText(null);
        dialog.setContentText("Novo nome:");
        ThemeManager.getInstance().aplicarTemaAoDialogo(dialog);
        dialog.showAndWait().ifPresent(novoNome -> {
            if (novoNome.isBlank()) return;
            new Thread(() -> {
                try {
                    arquivoService.renomear(item.getId(), novoNome.trim());
                    item.setNome(novoNome.trim());
                    Platform.runLater(() -> {
                        treeViewArquivos.refresh();
                        if (itemAtual != null && itemAtual.getId() == item.getId()) {
                            labelNomeNota.setText(item.getNome());
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro("Erro ao renomear: " + ex.getMessage()));
                }
            }).start();
        });
    }

    /**
     * Trata a solicitação de exclusão de um item via menu de contexto.
     * Para cadernos com filhos, exibe aviso com a quantidade de itens que serão removidos.
     *
     * @param item item a ser excluído
     */
    private void handleExcluir(ItemArvore item) {
        if (item == null) return;
        new Thread(() -> {
            try {
                int usuarioId = SessionManager.getInstance().getUsuarioLogado().getId();
                int filhos = item.getTipo() == TipoItem.CADERNO
                        ? arquivoService.contarDescendentes(item.getId(), usuarioId) : 0;
                Platform.runLater(() -> {
                    String msg = filhos > 0
                            ? "Excluir \"" + item.getNome() + "\" e seus " + filhos + " itens internos?"
                            : "Excluir \"" + item.getNome() + "\"?";
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirmar Exclusão");
                    confirm.setHeaderText(null);
                    confirm.setContentText(msg);
                    ThemeManager.getInstance().aplicarTemaAoDialogo(confirm);
                    confirm.showAndWait().ifPresent(btn -> {
                        if (btn != ButtonType.OK) return;
                        new Thread(() -> {
                            try {
                                arquivoService.excluir(item);
                                Platform.runLater(() -> {
                                    if (itemAtual != null && itemAtual.getId() == item.getId()) {
                                        mostrarPainelVazio();
                                    }
                                    carregarArvore();
                                });
                            } catch (Exception ex) {
                                Platform.runLater(() -> mostrarErro("Erro ao excluir: " + ex.getMessage()));
                            }
                        }).start();
                    });
                });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Trata a solicitação de mover um item para outro caderno via menu de contexto.
     *
     * @param item item a ser movido
     */
    private void handleMover(ItemArvore item) {
        if (item == null) return;
        int usuarioId = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                List<ItemArvore> cadernos = arquivoService.buscarCadernos(usuarioId);
                Platform.runLater(() -> {
                    ChoiceDialog<ItemArvore> dialog = new ChoiceDialog<>(null, cadernos);
                    dialog.setTitle("Mover para...");
                    dialog.setHeaderText("Selecione o caderno de destino:");
                    dialog.setContentText("Caderno:");
                    dialog.getItems().remove(item); // Remove o próprio item se for caderno
                    ThemeManager.getInstance().aplicarTemaAoDialogo(dialog);
                    dialog.showAndWait().ifPresent(destino -> {
                        new Thread(() -> {
                            try {
                                arquivoService.mover(item.getId(), destino.getId());
                                Platform.runLater(this::carregarArvore);
                            } catch (Exception ex) {
                                Platform.runLater(() -> mostrarErro("Erro ao mover: " + ex.getMessage()));
                            }
                        }).start();
                    });
                });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Move o item uma posição acima entre seus irmãos e recarrega a árvore.
     *
     * @param item item a ser movido
     */
    private void handleSubir(ItemArvore item) {
        if (item == null) return;
        int usuarioId = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                arquivoService.subirPosicao(item.getId(), usuarioId, item.getPaiId());
                Platform.runLater(this::carregarArvore);
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro ao reordenar: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Move o item uma posição abaixo entre seus irmãos e recarrega a árvore.
     *
     * @param item item a ser movido
     */
    private void handleDescer(ItemArvore item) {
        if (item == null) return;
        int usuarioId = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                arquivoService.descerPosicao(item.getId(), usuarioId, item.getPaiId());
                Platform.runLater(this::carregarArvore);
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro ao reordenar: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Ativa ou desativa os botões ↑ ↓ Excluir conforme o item selecionado na árvore.
     *
     * @param selecionado item selecionado, ou {@code null} se nada estiver selecionado
     */
    private void atualizarBotoesSelecao(TreeItem<ItemArvore> selecionado) {
        boolean temItem = selecionado != null;
        boolean ehCaderno = temItem && selecionado.getValue().getTipo() == TipoItem.CADERNO;
        btnSubirItem.setDisable(!temItem);
        btnDescerItem.setDisable(!temItem);
        btnExcluirItem.setDisable(!temItem);
        // Imprimir: desabilitado somente para cadernos; ativo para notas e PDFs
        btnImprimirItem.setDisable(ehCaderno);
    }

    /** Botão ↑: sobe o item selecionado uma posição. */
    @FXML
    private void handleSubirItemSelecionado() {
        TreeItem<ItemArvore> sel = treeViewArquivos.getSelectionModel().getSelectedItem();
        if (sel != null) handleSubir(sel.getValue());
    }

    /** Botão ↓: desce o item selecionado uma posição. */
    @FXML
    private void handleDescerItemSelecionado() {
        TreeItem<ItemArvore> sel = treeViewArquivos.getSelectionModel().getSelectedItem();
        if (sel != null) handleDescer(sel.getValue());
    }

    /** Botão Excluir: exclui o item selecionado após confirmação. */
    @FXML
    private void handleExcluirItemSelecionado() {
        TreeItem<ItemArvore> sel = treeViewArquivos.getSelectionModel().getSelectedItem();
        if (sel != null) handleExcluir(sel.getValue());
    }

    /** Botão Imprimir: exporta a nota ou PDF selecionado para impressão. */
    @FXML
    private void handleImprimirItemSelecionado() {
        TreeItem<ItemArvore> sel = treeViewArquivos.getSelectionModel().getSelectedItem();
        if (sel != null) handleImprimir(sel.getValue());
    }

    /**
     * Exporta o item para impressão:
     * <ul>
     *   <li>NOTA — converte o Markdown para HTML com CSS de impressão e abre no navegador padrão</li>
     *   <li>PDF  — abre o arquivo no visualizador de PDF padrão do sistema</li>
     *   <li>CADERNO — ignorado</li>
     * </ul>
     *
     * @param item item a exportar/imprimir
     */
    private void handleImprimir(ItemArvore item) {
        if (item == null || item.getTipo() == TipoItem.CADERNO) return;

        if (item.getTipo() == TipoItem.PDF) {
            new Thread(() -> {
                try {
                    PdfDocumento pdf = arquivoService.buscarPdf(item.getId());
                    if (pdf != null) {
                        java.awt.Desktop.getDesktop().open(new java.io.File(pdf.getCaminhoArquivo()));
                    }
                } catch (Exception ex) {
                    Platform.runLater(() -> mostrarErro("Erro ao abrir PDF: " + ex.getMessage()));
                }
            }).start();
            return;
        }

        // NOTA: converte Markdown → HTML e abre no navegador para impressão
        new Thread(() -> {
            try {
                Nota nota = notaService.carregar(item.getId());
                String html = gerarHtmlParaImpressao(item.getNome(), nota.getConteudo());
                java.io.File temp = java.io.File.createTempFile("studyapp_print_", ".html");
                temp.deleteOnExit();
                try (java.io.FileWriter fw = new java.io.FileWriter(temp, java.nio.charset.StandardCharsets.UTF_8)) {
                    fw.write(html);
                }
                java.awt.Desktop.getDesktop().browse(temp.toURI());
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro ao preparar impressão: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Gera HTML otimizado para impressão com margens, fonte serifada e CSS {@code @media print}.
     * Inclui o título da nota como cabeçalho H1.
     *
     * @param titulo  nome da nota (usado como título do documento)
     * @param markdown conteúdo da nota em Markdown
     * @return string com o documento HTML completo
     */
    private String gerarHtmlParaImpressao(String titulo, String markdown) {
        String htmlConteudo = rendererMarkdown.render(parserMarkdown.parse(converterImagensParaBase64(markdown)));
        String tituloEscapado = titulo.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><title>" + tituloEscapado + "</title><style>" +
            "body{font-family:Georgia,serif;background:white;color:#1A1A2E;" +
            "max-width:800px;margin:0 auto;padding:48px 40px;line-height:1.8;font-size:14px;}" +
            "h1{font-size:26px;font-weight:bold;border-bottom:2px solid #1B3A6B;padding-bottom:10px;margin-bottom:24px;}" +
            "h2,h3,h4{border-bottom:1px solid #ddd;padding-bottom:4px;margin-top:20px;}" +
            "pre{background:#f6f8fa;padding:14px;border-radius:4px;overflow-x:auto;border:1px solid #ddd;}" +
            "code{background:#f6f8fa;padding:2px 5px;border-radius:3px;font-family:'Courier New',monospace;font-size:13px;}" +
            "pre code{background:none;padding:0;border:none;}" +
            "blockquote{border-left:4px solid #1B3A6B;margin:0 0 0 16px;padding-left:16px;color:#555;font-style:italic;}" +
            "table{border-collapse:collapse;width:100%;margin:14px 0;}" +
            "th,td{border:1px solid #ccc;padding:8px 12px;text-align:left;}" +
            "th{background:#f0f4fa;font-weight:bold;}" +
            "img{max-width:100%;border-radius:4px;}" +
            "a{color:#1B3A6B;}" +
            "@media print{" +
            "  body{max-width:100%;padding:0;margin:0;}" +
            "  h1{page-break-after:avoid;}" +
            "  pre,blockquote,table,img{page-break-inside:avoid;}" +
            "}" +
            "</style></head><body>" +
            "<h1>" + tituloEscapado + "</h1>" +
            htmlConteudo +
            "</body></html>";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // EDITOR MARKDOWN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Configura o auto-save com debounce de 1,5 segundo usando {@link PauseTransition}.
     */
    private void configurarAutoSave() {
        pausaAutoSave = new PauseTransition(Duration.seconds(1.5));
        pausaAutoSave.setOnFinished(e -> salvarNotaAtual());
        textAreaEditor.textProperty().addListener((obs, anterior, novo) -> {
            if (itemAtual != null && itemAtual.getTipo() == TipoItem.NOTA) {
                labelStatusSave.setText("Salvando...");
                pausaAutoSave.playFromStart();
            }
        });
    }

    /**
     * Abre uma nota no editor, carregando seu conteúdo do banco de dados em background.
     *
     * @param item item do tipo NOTA a ser aberto
     */
    private void abrirNota(ItemArvore item) {
        fecharPdfAtual();
        mostrarPainel(painelEditor);
        labelNomeNota.setText(item.getNome());
        if (!emModoVisualizacao) {
            toolbarEditor.setVisible(true);
            toolbarEditor.setManaged(true);
        }
        new Thread(() -> {
            try {
                Nota nota = notaService.carregar(item.getId());
                Platform.runLater(() -> {
                    pausaAutoSave.stop();
                    textAreaEditor.setText(nota.getConteudo());
                    labelStatusSave.setText("Salvo");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro ao carregar nota: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Salva o conteúdo atual do editor no banco de dados em background.
     */
    private void salvarNotaAtual() {
        if (itemAtual == null || itemAtual.getTipo() != TipoItem.NOTA) return;
        String conteudo = textAreaEditor.getText();
        int itemId = itemAtual.getId();
        new Thread(() -> {
            try {
                notaService.salvar(itemId, conteudo);
                Platform.runLater(() -> labelStatusSave.setText("Salvo"));
            } catch (Exception ex) {
                Platform.runLater(() -> labelStatusSave.setText("Erro ao salvar"));
            }
        }).start();
    }

    /**
     * Alterna entre o modo de edição (TextArea) e o modo de visualização (WebView).
     */
    @FXML
    private void handleToggleEditor() {
        emModoVisualizacao = !emModoVisualizacao;
        if (emModoVisualizacao) {
            btnToggleEditor.setText("Editar");
            textAreaEditor.setVisible(false);
            textAreaEditor.setManaged(false);
            toolbarEditor.setVisible(false);
            toolbarEditor.setManaged(false);
            webViewPreview.setVisible(true);
            webViewPreview.setManaged(true);
            atualizarPreview();
        } else {
            btnToggleEditor.setText("Visualizar");
            textAreaEditor.setVisible(true);
            textAreaEditor.setManaged(true);
            toolbarEditor.setVisible(true);
            toolbarEditor.setManaged(true);
            webViewPreview.setVisible(false);
            webViewPreview.setManaged(false);
        }
    }

    /**
     * Atualiza o conteúdo do WebView com o HTML gerado a partir do Markdown atual.
     */
    private void atualizarPreview() {
        String md = textAreaEditor.getText();
        String html = gerarHtml(md);
        webViewPreview.getEngine().loadContent(html, "text/html");
    }

    /**
     * Converte texto Markdown para HTML completo com estilos inline adequados ao tema atual.
     *
     * @param markdown texto em formato Markdown
     * @return string com o documento HTML completo
     */
    private String gerarHtml(String markdown) {
        String htmlConteudo = rendererMarkdown.render(parserMarkdown.parse(converterImagensParaBase64(markdown)));
        boolean escuro = "ESCURO".equals(ThemeManager.getInstance().getTemaAtual());
        String bgColor = escuro ? "#1E1E2E" : "#ffffff";
        String textColor = escuro ? "#E8E8F0" : "#1A1A2E";
        String codeColor = escuro ? "#2A2A3C" : "#f6f8fa";
        String borderColor = escuro ? "#3A3A50" : "#ddd";

        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><style>" +
                "body{font-family:system-ui,sans-serif;background:" + bgColor + ";color:" + textColor +
                ";max-width:860px;margin:0 auto;padding:24px;line-height:1.7;font-size:15px;}" +
                "h1,h2,h3,h4{border-bottom:1px solid " + borderColor + ";padding-bottom:6px;}" +
                "pre{background:" + codeColor + ";padding:16px;border-radius:6px;overflow-x:auto;}" +
                "code{background:" + codeColor + ";padding:2px 5px;border-radius:3px;font-family:monospace;font-size:13px;}" +
                "pre code{background:none;padding:0;}" +
                "blockquote{border-left:4px solid #1B3A6B;margin:0;padding-left:16px;color:#888;}" +
                "table{border-collapse:collapse;width:100%;margin:12px 0;}" +
                "th,td{border:1px solid " + borderColor + ";padding:8px 12px;text-align:left;}" +
                "th{background:" + codeColor + ";}" +
                "img{max-width:100%;border-radius:4px;}" +
                "a{color:#4A78C4;}" +
                "</style></head><body>" + htmlConteudo + "</body></html>";
    }

    /**
     * Pré-processa o Markdown convertendo imagens referenciadas por {@code file://} para
     * data-URIs base64, contornando a restrição de segurança do WebView ao carregar conteúdo
     * via {@code loadContent()} sem URL de base.
     *
     * @param markdown texto Markdown original
     * @return Markdown com as referências de imagem locais substituídas por data-URIs
     */
    private String converterImagensParaBase64(String markdown) {
        Pattern p = Pattern.compile("!\\[([^\\]]*)\\]\\(file://(/[^)]+)\\)");
        Matcher m = p.matcher(markdown);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String alt = m.group(1);
            String path = m.group(2);
            try {
                java.io.File arquivo = new java.io.File(path);
                if (arquivo.exists()) {
                    byte[] bytes = java.nio.file.Files.readAllBytes(arquivo.toPath());
                    String base64 = Base64.getEncoder().encodeToString(bytes);
                    String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
                    String mime = switch (ext) {
                        case "jpg", "jpeg" -> "image/jpeg";
                        case "gif" -> "image/gif";
                        case "webp" -> "image/webp";
                        default -> "image/png";
                    };
                    m.appendReplacement(sb, Matcher.quoteReplacement(
                        "![" + alt + "](data:" + mime + ";base64," + base64 + ")"));
                    continue;
                }
            } catch (Exception ignored) {}
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ── Botões de formatação ─────────────────────────────────────────────────

    /** Insere formatação de negrito ao redor do texto selecionado ou no cursor. */
    @FXML private void handleFmtNegrito() { inserirFormatacao("**", "**", "texto"); }
    /** Insere formatação de itálico ao redor do texto selecionado ou no cursor. */
    @FXML private void handleFmtItalico() { inserirFormatacao("*", "*", "texto"); }
    /** Insere marcação de título H1 no início da linha atual. */
    @FXML private void handleFmtH1() { inserirNoInicioDaLinha("# "); }
    /** Insere marcação de título H2 no início da linha atual. */
    @FXML private void handleFmtH2() { inserirNoInicioDaLinha("## "); }
    /** Insere marcação de título H3 no início da linha atual. */
    @FXML private void handleFmtH3() { inserirNoInicioDaLinha("### "); }
    /** Insere bloco de código delimitado por crases triplas. */
    @FXML private void handleFmtCodigo() { inserirFormatacao("```\n", "\n```", "código"); }
    /** Insere template de link Markdown no cursor. */
    @FXML private void handleFmtLink() { inserirNoCursor("[texto](url)"); }
    /** Abre seletor de imagem, copia o arquivo e insere a referência Markdown. */
    @FXML private void handleFmtImagem() { inserirImagem(); }
    /** Insere item de lista não ordenada no início da linha. */
    @FXML private void handleFmtLista() { inserirNoInicioDaLinha("- "); }
    /** Insere item de lista ordenada no início da linha. */
    @FXML private void handleFmtListaNum() { inserirNoInicioDaLinha("1. "); }
    /** Abre diálogo de criação de tabela Markdown. */
    @FXML private void handleFmtTabela() { inserirTabela(); }
    /** Insere delimitadores de fórmula matemática LaTeX inline. */
    @FXML private void handleFmtFormula() { inserirFormatacao("$", "$", "formula"); }

    /**
     * Insere formatação ao redor do texto selecionado ou texto padrão se nada estiver selecionado.
     *
     * @param prefixo    string a inserir antes do conteúdo
     * @param sufixo     string a inserir depois do conteúdo
     * @param padrao     texto padrão a usar quando não há seleção
     */
    private void inserirFormatacao(String prefixo, String sufixo, String padrao) {
        String sel = textAreaEditor.getSelectedText();
        String texto = sel.isBlank() ? padrao : sel;
        int inicio = textAreaEditor.getSelection().getStart();
        textAreaEditor.replaceSelection(prefixo + texto + sufixo);
        if (sel.isBlank()) {
            textAreaEditor.selectRange(inicio + prefixo.length(), inicio + prefixo.length() + texto.length());
        }
        textAreaEditor.requestFocus();
    }

    /**
     * Insere um texto no início da linha onde está o cursor.
     *
     * @param marcacao string a inserir no início da linha
     */
    private void inserirNoInicioDaLinha(String marcacao) {
        int pos = textAreaEditor.getCaretPosition();
        String texto = textAreaEditor.getText();
        int inicioLinha = texto.lastIndexOf('\n', pos - 1) + 1;
        textAreaEditor.insertText(inicioLinha, marcacao);
        textAreaEditor.requestFocus();
    }

    /**
     * Insere texto diretamente na posição atual do cursor.
     *
     * @param texto texto a inserir
     */
    private void inserirNoCursor(String texto) {
        textAreaEditor.replaceSelection(texto);
        textAreaEditor.requestFocus();
    }

    /**
     * Abre seletor de imagem, copia o arquivo para o diretório de dados e insere a referência Markdown.
     */
    private void inserirImagem() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecionar Imagem");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp"));
        File arquivo = chooser.showOpenDialog(StudyApplication.getStagePrincipal());
        if (arquivo == null) return;

        new Thread(() -> {
            try {
                int usuarioId = SessionManager.getInstance().getUsuarioLogado().getId();
                String dirImagens = com.leonhardsen.studyapp.database.DatabaseManager.getDirApp()
                        + "/images/" + usuarioId;
                new java.io.File(dirImagens).mkdirs();
                String ext = arquivo.getName().substring(arquivo.getName().lastIndexOf('.'));
                String nomeDestino = UUID.randomUUID().toString() + ext;
                java.io.File destino = new java.io.File(dirImagens + "/" + nomeDestino);
                java.nio.file.Files.copy(arquivo.toPath(), destino.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                String markdown = "![" + arquivo.getName() + "](file://" + destino.getAbsolutePath() + ")";
                Platform.runLater(() -> inserirNoCursor(markdown));
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro ao inserir imagem: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Exibe diálogo para definir dimensões de uma tabela Markdown e a insere no editor.
     */
    private void inserirTabela() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Inserir Tabela");
        dialog.setHeaderText("Dimensões da tabela");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ThemeManager.getInstance().aplicarTemaAoDialogo(dialog);

        Spinner<Integer> spColunas = new Spinner<>(1, 10, 3);
        Spinner<Integer> spLinhas = new Spinner<>(1, 20, 2);

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(12));
        grid.add(new Label("Colunas:"), 0, 0);
        grid.add(spColunas, 1, 0);
        grid.add(new Label("Linhas de dados:"), 0, 1);
        grid.add(spLinhas, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            int cols = spColunas.getValue();
            int rows = spLinhas.getValue();
            StringBuilder sb = new StringBuilder("\n");
            // Cabeçalho
            for (int c = 0; c < cols; c++) sb.append("| Coluna ").append(c + 1).append(" ");
            sb.append("|\n");
            // Separador
            sb.append("|---".repeat(cols)).append("|\n");
            // Linhas
            for (int r = 0; r < rows; r++) {
                sb.append("|   ".repeat(cols)).append("|\n");
            }
            inserirNoCursor(sb.toString());
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VISUALIZADOR DE PDF
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Abre um PDF no visualizador, carregando o documento via PDFBox em background.
     *
     * @param item item do tipo PDF a ser aberto
     */
    private void abrirPdf(ItemArvore item) {
        fecharPdfAtual();
        mostrarPainel(painelPdf);
        new Thread(() -> {
            try {
                PdfDocumento pdf = arquivoService.buscarPdf(item.getId());
                if (pdf == null) {
                    Platform.runLater(() -> mostrarErro("Arquivo PDF não encontrado."));
                    return;
                }
                PDDocument doc = Loader.loadPDF(new File(pdf.getCaminhoArquivo()));
                documentoPdfAberto = doc;
                renderizadorPdf = new PDFRenderer(doc);
                paginaPdfAtual = 0;
                Platform.runLater(() -> {
                    renderizarPaginaPdf();
                    atualizarInfoPdf(doc.getNumberOfPages());
                });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro ao abrir PDF: " + ex.getMessage()));
            }
        }).start();
    }

    /**
     * Renderiza a página atual do PDF como imagem e a exibe no ImageView.
     * Captura referências locais antes de iniciar a thread para evitar condição de corrida
     * quando o documento é fechado enquanto a renderização ainda está em andamento.
     */
    private void renderizarPaginaPdf() {
        if (renderizadorPdf == null) return;
        // Captura local para evitar que fecharPdfAtual() anule os campos durante a renderização
        PDFRenderer renderer = renderizadorPdf;
        int pagina = paginaPdfAtual;
        float dpi = 150 * zoomPdf;
        new Thread(() -> {
            try {
                BufferedImage bi = renderer.renderImageWithDPI(pagina, dpi);
                Image fxImage = bufferedImageParaFxImage(bi);
                Platform.runLater(() -> imagePdf.setImage(fxImage));
            } catch (Exception ex) {
                // Ignora silenciosamente: pode ocorrer ao fechar o PDF durante a renderização
            }
        }).start();
    }

    /**
     * Converte um {@link BufferedImage} para um {@link Image} JavaFX sem dependência do módulo Swing.
     *
     * @param bi imagem no formato AWT
     * @return imagem no formato JavaFX, ou {@code null} em caso de falha
     */
    private Image bufferedImageParaFxImage(BufferedImage bi) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bi, "png", baos);
            return new Image(new ByteArrayInputStream(baos.toByteArray()));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Atualiza o label de informação de página do PDF.
     *
     * @param totalPaginas total de páginas do documento aberto
     */
    private void atualizarInfoPdf(int totalPaginas) {
        labelPdfPagina.setText("Página " + (paginaPdfAtual + 1) + " de " + totalPaginas);
        labelPdfZoom.setText(Math.round(zoomPdf * 100) + "%");
    }

    /** Navega para a página anterior do PDF aberto. */
    @FXML
    private void handlePdfAnterior() {
        if (documentoPdfAberto != null && paginaPdfAtual > 0) {
            paginaPdfAtual--;
            renderizarPaginaPdf();
            atualizarInfoPdf(documentoPdfAberto.getNumberOfPages());
        }
    }

    /** Navega para a próxima página do PDF aberto. */
    @FXML
    private void handlePdfProxima() {
        if (documentoPdfAberto != null && paginaPdfAtual < documentoPdfAberto.getNumberOfPages() - 1) {
            paginaPdfAtual++;
            renderizarPaginaPdf();
            atualizarInfoPdf(documentoPdfAberto.getNumberOfPages());
        }
    }

    /** Aumenta o zoom do visualizador de PDF. */
    @FXML
    private void handlePdfZoomMais() {
        if (zoomPdf < 3.0f) { zoomPdf += 0.25f; renderizarPaginaPdf(); atualizarInfoPdf(documentoPdfAberto.getNumberOfPages()); }
    }

    /** Diminui o zoom do visualizador de PDF. */
    @FXML
    private void handlePdfZoomMenos() {
        if (zoomPdf > 0.5f) { zoomPdf -= 0.25f; renderizarPaginaPdf(); atualizarInfoPdf(documentoPdfAberto.getNumberOfPages()); }
    }

    /**
     * Fecha o documento PDF aberto e libera os recursos de memória.
     * O fechamento do arquivo ocorre em background após 300ms para que threads de renderização
     * em andamento possam concluir sem gerar erros de "already closed" no PDFBox.
     */
    private void fecharPdfAtual() {
        if (documentoPdfAberto != null) {
            PDDocument docAFechar = documentoPdfAberto;
            renderizadorPdf = null;
            documentoPdfAberto = null;
            new Thread(() -> {
                try { Thread.sleep(300); } catch (InterruptedException ignored) {}
                try { docAFechar.close(); } catch (IOException ignored) {}
            }).start();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITÁRIOS DE UI
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Exibe o painel vazio (estado inicial sem seleção) e oculta os demais painéis.
     */
    private void mostrarPainelVazio() {
        itemAtual = null;
        mostrarPainel(painelVazio);
    }

    /**
     * Torna visível apenas o painel especificado, ocultando os outros painéis da StackPane.
     *
     * @param painel painel a ser exibido
     */
    private void mostrarPainel(Region painel) {
        painelVazio.setVisible(false); painelVazio.setManaged(false);
        painelEditor.setVisible(false); painelEditor.setManaged(false);
        painelPdf.setVisible(false); painelPdf.setManaged(false);
        painel.setVisible(true); painel.setManaged(true);
    }

    /**
     * Exibe um alerta de erro para o usuário.
     *
     * @param mensagem texto da mensagem de erro
     */
    private void mostrarErro(String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        ThemeManager.getInstance().aplicarTemaAoDialogo(alert);
        alert.showAndWait();
    }

    /**
     * Para o relógio, o auto-save e a animação de campaninha ao encerrar a tela principal.
     */
    private void pararRecursos() {
        if (relogio != null) relogio.stop();
        if (pausaAutoSave != null) pausaAutoSave.stop();
        pararAnimacaoCampainha();
        fecharPdfAtual();
        if (blocoNotasController != null) blocoNotasController.pararRecursos();
        if (calculadoraController != null) calculadoraController.pararRecursos();
        if (pomodoroController != null) pomodoroController.pararRecursos();
    }
}
