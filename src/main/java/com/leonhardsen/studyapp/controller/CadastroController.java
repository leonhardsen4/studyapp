package com.leonhardsen.studyapp.controller;

import com.leonhardsen.studyapp.StudyApplication;
import com.leonhardsen.studyapp.model.Usuario;
import com.leonhardsen.studyapp.service.UsuarioService;
import com.leonhardsen.studyapp.util.SessionManager;
import com.leonhardsen.studyapp.util.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controlador da tela de cadastro de novos usuários.
 * Realiza validações de campos, cadastra o usuário via {@link UsuarioService}
 * e redireciona para a tela principal após o cadastro bem-sucedido.
 *
 * @author StudyApp
 * @version 1.0
 */
public class CadastroController {

    @FXML private TextField tfNome;
    @FXML private TextField tfEmail;
    @FXML private PasswordField pfSenha;
    @FXML private PasswordField pfConfirmarSenha;
    @FXML private Label labelErro;
    @FXML private Button btnCadastrar;

    private final UsuarioService usuarioService = new UsuarioService();

    /**
     * Trata o evento de clique no botão "Criar conta".
     * Valida os campos, cadastra o usuário e redireciona para a tela principal.
     */
    @FXML
    private void handleCadastrar() {
        String nome = tfNome.getText().trim();
        String email = tfEmail.getText().trim();
        String senha = pfSenha.getText();
        String confirmar = pfConfirmarSenha.getText();

        // Validações locais
        if (nome.isBlank() || email.isBlank() || senha.isBlank()) {
            mostrarErro("Preencha todos os campos.");
            return;
        }
        if (senha.length() < 6) {
            mostrarErro("A senha deve ter pelo menos 6 caracteres.");
            return;
        }
        if (!senha.equals(confirmar)) {
            mostrarErro("As senhas não coincidem.");
            return;
        }

        btnCadastrar.setDisable(true);
        esconderErro();

        new Thread(() -> {
            try {
                Usuario usuario = usuarioService.cadastrar(nome, email, senha);
                SessionManager.getInstance().login(usuario);
                ThemeManager.getInstance().setTemaAtual(usuario.getTema());
                Platform.runLater(() -> {
                    try {
                        StudyApplication.mostrarTelaPrincipal();
                    } catch (Exception ex) {
                        mostrarErro("Erro ao abrir a aplicação: " + ex.getMessage());
                        btnCadastrar.setDisable(false);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    mostrarErro(ex.getMessage());
                    btnCadastrar.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * Trata o evento de clique no link "Entrar".
     * Retorna para a tela de login.
     */
    @FXML
    private void handleVoltarLogin() {
        try {
            StudyApplication.mostrarTelaLogin();
        } catch (Exception ex) {
            mostrarErro("Erro ao retornar para o login: " + ex.getMessage());
        }
    }

    /**
     * Exibe uma mensagem de erro no label de feedback da tela.
     *
     * @param mensagem texto do erro a ser exibido
     */
    private void mostrarErro(String mensagem) {
        labelErro.setText(mensagem);
        labelErro.setVisible(true);
        labelErro.setManaged(true);
    }

    /**
     * Oculta o label de erro da tela.
     */
    private void esconderErro() {
        labelErro.setVisible(false);
        labelErro.setManaged(false);
    }
}
