package com.leonhardsen.studyapp.controller;

import com.leonhardsen.studyapp.StudyApplication;
import com.leonhardsen.studyapp.model.Usuario;
import com.leonhardsen.studyapp.service.UsuarioService;
import com.leonhardsen.studyapp.util.EmailService;
import com.leonhardsen.studyapp.util.HashUtil;
import com.leonhardsen.studyapp.util.ThemeManager;
import com.leonhardsen.studyapp.util.SessionManager;
import com.leonhardsen.studyapp.util.SmtpConfigDialog;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.Optional;
import java.util.UUID;

/**
 * Controlador da tela de login.
 * Gerencia a autenticação do usuário, o redirecionamento para cadastro
 * e o fluxo de recuperação de senha por e-mail.
 *
 * @author StudyApp
 * @version 1.0
 */
public class LoginController {

    @FXML private TextField tfEmail;
    @FXML private PasswordField pfSenha;
    @FXML private Label labelErro;
    @FXML private Button btnEntrar;

    private final UsuarioService usuarioService = new UsuarioService();

    /**
     * Trata o evento de clique no botão "Entrar".
     * Autentica o usuário e redireciona para a tela principal em caso de sucesso.
     */
    @FXML
    private void handleLogin() {
        String email = tfEmail.getText().trim();
        String senha = pfSenha.getText();

        if (email.isBlank() || senha.isBlank()) {
            mostrarErro("Preencha o e-mail e a senha.");
            return;
        }

        btnEntrar.setDisable(true);
        esconderErro();

        new Thread(() -> {
            try {
                Usuario usuario = usuarioService.autenticar(email, senha);
                SessionManager.getInstance().login(usuario);
                ThemeManager.getInstance().setTemaAtual(usuario.getTema());
                Platform.runLater(() -> {
                    try {
                        StudyApplication.mostrarTelaPrincipal();
                    } catch (Exception exTela) {
                        exTela.printStackTrace();
                        String causa = exTela.getCause() != null
                                ? exTela.getCause().getMessage() : exTela.getMessage();
                        mostrarErro("Erro ao abrir a tela principal: " + causa);
                        btnEntrar.setDisable(false);
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    mostrarErro(ex.getMessage());
                    btnEntrar.setDisable(false);
                });
            }
        }).start();
    }

    /**
     * Trata o evento de clique no link "Cadastre-se".
     * Redireciona para a tela de cadastro de novo usuário.
     */
    @FXML
    private void handleIrParaCadastro() {
        try {
            StudyApplication.mostrarTelaCadastro();
        } catch (Exception ex) {
            mostrarErro("Erro ao abrir tela de cadastro: " + ex.getMessage());
        }
    }

    /**
     * Trata o evento de clique no link "Esqueceu a senha?".
     * Exibe um diálogo solicitando o e-mail e, se encontrado, envia uma senha temporária.
     */
    @FXML
    private void handleEsqueceuSenha() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Recuperar Senha");
        dialog.setHeaderText("Recuperação de senha");
        dialog.setContentText("Digite seu e-mail cadastrado:");
        ThemeManager.getInstance().aplicarTemaAoDialogo(dialog);

        Optional<String> resultado = dialog.showAndWait();
        resultado.ifPresent(email -> {
            if (email.isBlank()) return;
            new Thread(() -> {
                try {
                    // Gera senha temporária de 12 caracteres
                    String senhaTemporal = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
                    String hashTemp = HashUtil.gerarHash(senhaTemporal);

                    // Verifica existência sem revelar se o e-mail existe
                    Usuario usuario = usuarioService.buscarPorEmail(email.trim());
                    if (usuario != null) {
                        // Envia primeiro; só atualiza a senha se o envio não lançar exceção
                        EmailService.enviar(email.trim(), "StudyApp — Senha Temporária",
                                "Sua senha temporária é: " + senhaTemporal +
                                        "\n\nAcesse o sistema e altere-a imediatamente em Configurações > Perfil.");
                        usuarioService.redefinirSenha(usuario.getId(), hashTemp);
                    }
                    Platform.runLater(() -> {
                        Alert info = new Alert(Alert.AlertType.INFORMATION);
                        info.setTitle("Recuperação de Senha");
                        info.setHeaderText(null);
                        info.setContentText("Se o e-mail estiver cadastrado, uma nova senha foi enviada.");
                        ThemeManager.getInstance().aplicarTemaAoDialogo(info);
                        info.showAndWait();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                        if (msg.contains("SMTP não encontrada") || msg.contains("smtp.host")) {
                            // SMTP ainda não configurado — abre o diálogo de configuração
                            Alert aviso = new Alert(Alert.AlertType.WARNING);
                            aviso.setTitle("SMTP não configurado");
                            aviso.setHeaderText("Configuração de e-mail ausente");
                            aviso.setContentText(
                                "O envio de e-mail não está configurado.\n\n" +
                                "Clique em OK para abrir o assistente de configuração SMTP agora.");
                            ThemeManager.getInstance().aplicarTemaAoDialogo(aviso);
                            aviso.showAndWait();
                            SmtpConfigDialog.mostrar(StudyApplication.getStagePrincipal());
                        } else {
                            Alert erro = new Alert(Alert.AlertType.WARNING);
                            erro.setTitle("Erro ao Enviar E-mail");
                            erro.setHeaderText(null);
                            erro.setContentText("Não foi possível enviar o e-mail: " + msg);
                            ThemeManager.getInstance().aplicarTemaAoDialogo(erro);
                            erro.showAndWait();
                        }
                    });
                }
            }).start();
        });
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
