package com.leonhardsen.studyapp;

import com.leonhardsen.studyapp.util.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Classe principal da aplicação StudyApp.
 * Responsável por inicializar o estágio JavaFX, definir o tamanho mínimo da janela
 * e gerenciar a navegação entre as telas da aplicação.
 *
 * @author StudyApp
 * @version 1.0
 */
public class StudyApplication extends Application {

    /** Estágio principal compartilhado entre todas as telas. */
    private static Stage stagePrincipal;

    /**
     * Ponto de entrada da aplicação JavaFX. Configura o estágio e exibe a tela de login.
     *
     * @param stage estágio principal fornecido pelo JavaFX
     * @throws IOException se ocorrer erro ao carregar os arquivos FXML
     */
    @Override
    public void start(Stage stage) throws IOException {
        stagePrincipal = stage;

        // Define tamanho mínimo como metade da resolução da tela
        Rectangle2D limitesTela = Screen.getPrimary().getVisualBounds();
        stagePrincipal.setMinWidth(limitesTela.getWidth() / 2);
        stagePrincipal.setMinHeight(limitesTela.getHeight() / 2);

        stagePrincipal.setTitle("StudyApp");
        mostrarTelaLogin();
    }

    /**
     * Exibe a tela de login. A janela é redimensionada para tamanho fixo e centralizada.
     *
     * @throws IOException se ocorrer erro ao carregar o FXML da tela de login
     */
    public static void mostrarTelaLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(StudyApplication.class.getResource("login-view.fxml"));
        Scene cena = new Scene(loader.load());
        ThemeManager.getInstance().setCena(cena);
        ThemeManager.getInstance().aplicarTemaAtual();
        stagePrincipal.setScene(cena);
        stagePrincipal.setMaximized(false);
        stagePrincipal.setWidth(480);
        stagePrincipal.setHeight(600);
        stagePrincipal.centerOnScreen();
        stagePrincipal.show();
    }

    /**
     * Exibe a tela de cadastro de novo usuário.
     *
     * @throws IOException se ocorrer erro ao carregar o FXML da tela de cadastro
     */
    public static void mostrarTelaCadastro() throws IOException {
        FXMLLoader loader = new FXMLLoader(StudyApplication.class.getResource("cadastro-view.fxml"));
        Scene cena = new Scene(loader.load());
        ThemeManager.getInstance().setCena(cena);
        ThemeManager.getInstance().aplicarTemaAtual();
        stagePrincipal.setScene(cena);
        stagePrincipal.setMaximized(false);
        stagePrincipal.setWidth(480);
        stagePrincipal.setHeight(520);
        stagePrincipal.centerOnScreen();
        stagePrincipal.show();
    }

    /**
     * Exibe a tela principal da aplicação maximizada.
     *
     * @throws IOException se ocorrer erro ao carregar o FXML da tela principal
     */
    public static void mostrarTelaPrincipal() throws IOException {
        FXMLLoader loader = new FXMLLoader(StudyApplication.class.getResource("main-view.fxml"));
        Scene cena = new Scene(loader.load());
        ThemeManager.getInstance().setCena(cena);
        ThemeManager.getInstance().aplicarTemaAtual();
        stagePrincipal.setScene(cena);
        stagePrincipal.setMaximized(true);
        stagePrincipal.show();
    }

    /**
     * Retorna o estágio principal da aplicação.
     *
     * @return estágio principal JavaFX
     */
    public static Stage getStagePrincipal() {
        return stagePrincipal;
    }

    /**
     * Método de entrada padrão Java, delega para o método {@code launch} do JavaFX.
     *
     * @param args argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
