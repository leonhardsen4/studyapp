package com.leonhardsen.studyapp.controller;

import com.leonhardsen.studyapp.database.DatabaseManager;
import com.leonhardsen.studyapp.util.SessionManager;
import com.leonhardsen.studyapp.util.ThemeManager;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class BlocoNotasController {

    @FXML private VBox     raiz;
    @FXML private TextArea textArea;
    @FXML private Label    lblStatus;
    @FXML private Button   btnDetachar;

    private PauseTransition pausaAutoSave;
    private Stage           janelaDestacada;
    private BorderPane      container;

    @FXML
    public void initialize() {
        pausaAutoSave = new PauseTransition(Duration.seconds(1.5));
        pausaAutoSave.setOnFinished(e -> salvarArquivo());

        textArea.textProperty().addListener((obs, ant, nov) -> {
            lblStatus.setText("Salvando...");
            pausaAutoSave.playFromStart();
        });

        carregarArquivo();
    }

    public void setContainer(BorderPane container) {
        this.container = container;
    }

    // ── Persistência ───────────────────────────────────────────────────────────

    private Path getArquivoNota() {
        int uid = SessionManager.getInstance().getUsuarioLogado().getId();
        return Path.of(DatabaseManager.getDirApp(), "bloconotas_" + uid + ".txt");
    }

    private void carregarArquivo() {
        new Thread(() -> {
            try {
                Path p = getArquivoNota();
                String conteudo = Files.exists(p) ? Files.readString(p, StandardCharsets.UTF_8) : "";
                Platform.runLater(() -> {
                    textArea.setText(conteudo);
                    lblStatus.setText("Salvo");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> lblStatus.setText("Erro ao carregar"));
            }
        }).start();
    }

    private void salvarArquivo() {
        String conteudo = textArea.getText();
        new Thread(() -> {
            try {
                Files.writeString(getArquivoNota(), conteudo, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                Platform.runLater(() -> lblStatus.setText("Salvo"));
            } catch (Exception ex) {
                Platform.runLater(() -> lblStatus.setText("Erro ao salvar"));
            }
        }).start();
    }

    // ── Limpar ─────────────────────────────────────────────────────────────────

    @FXML
    private void handleLimpar() {
        textArea.clear();
    }

    // ── Salvar como .txt ───────────────────────────────────────────────────────

    @FXML
    private void handleSalvarTxt() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar como .txt");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivo de texto", "*.txt"));
        chooser.setInitialFileName("notas.txt");

        Stage janelaDono = janelaDestacada != null ? janelaDestacada
                : (Stage) raiz.getScene().getWindow();
        File destino = chooser.showSaveDialog(janelaDono);
        if (destino == null) return;

        String conteudo = textArea.getText();
        new Thread(() -> {
            try {
                Files.writeString(destino.toPath(), conteudo, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                Platform.runLater(() -> lblStatus.setText("Exportado: " + destino.getName()));
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErro("Erro ao exportar: " + ex.getMessage()));
            }
        }).start();
    }

    // ── Destacar / Reatachar ───────────────────────────────────────────────────

    @FXML
    private void handleDetachar() {
        if (janelaDestacada != null) {
            reatachar();
        } else {
            destacar();
        }
    }

    private void destacar() {
        // Captura stylesheets da cena principal antes de mover o nó
        Scene cenaPrincipal = raiz.getScene();

        // Substitui o conteúdo no painel principal por um placeholder
        VBox placeholder = criarPlaceholder();
        container.setCenter(placeholder); // remove raiz do BorderPane automaticamente

        // Cria cena da janela flutuante (raiz agora está sem pai)
        Scene cenaSeparada = new Scene(raiz, 720, 560);
        cenaSeparada.getStylesheets().addAll(cenaPrincipal.getStylesheets());

        janelaDestacada = new Stage();
        janelaDestacada.setTitle("📝  Bloco de Notas — StudyApp");
        janelaDestacada.setAlwaysOnTop(true);
        janelaDestacada.setScene(cenaSeparada);
        janelaDestacada.setOnCloseRequest(e -> {
            e.consume(); // deixa reatachar() controlar o fechamento
            reatachar();
        });

        btnDetachar.setText("↙ Reintegrar");
        janelaDestacada.show();
    }

    private void reatachar() {
        if (janelaDestacada == null) return;

        // Desconecta raiz da cena flutuante antes de mover
        janelaDestacada.getScene().setRoot(new Region());

        // Devolve ao painel principal
        container.setCenter(raiz);
        btnDetachar.setText("↗ Destacar");

        janelaDestacada.close();
        janelaDestacada = null;
    }

    private VBox criarPlaceholder() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);

        Label icone = new Label("📝");
        icone.setStyle("-fx-font-size: 40px;");

        Label msg = new Label("Bloco de notas aberto em\njanela separada");
        msg.setTextAlignment(TextAlignment.CENTER);
        msg.setStyle("-fx-text-fill: #9090A8; -fx-font-size: 13px;");

        box.getChildren().addAll(icone, msg);
        return box;
    }

    // ── Limpeza ────────────────────────────────────────────────────────────────

    public void pararRecursos() {
        if (pausaAutoSave != null) {
            pausaAutoSave.stop();
            salvarArquivo(); // salva imediatamente ao fechar o app
        }
        if (janelaDestacada != null) {
            janelaDestacada.close();
            janelaDestacada = null;
        }
    }

    // ── Utilitários ────────────────────────────────────────────────────────────

    private void mostrarErro(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle("Erro");
        a.setHeaderText(null);
        a.setContentText(msg);
        ThemeManager.getInstance().aplicarTemaAoDialogo(a);
        a.showAndWait();
    }
}
