package com.leonhardsen.studyapp.controller;

import com.leonhardsen.studyapp.StudyApplication;
import com.leonhardsen.studyapp.service.UsuarioService;
import com.leonhardsen.studyapp.util.SessionManager;
import com.leonhardsen.studyapp.util.ThemeManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;

/**
 * Controlador da tela de perfil do usuário.
 * Permite alterar e-mail, senha e excluir a conta permanentemente,
 * com confirmação de senha para todas as operações sensíveis.
 *
 * @author StudyApp
 * @version 1.0
 */
public class PerfilController {

    // ── Seção Email ────────────────────────────────────────────────
    @FXML private TextField tfNovoEmail;
    @FXML private PasswordField pfSenhaConfEmail;
    @FXML private Label labelErroEmail;
    @FXML private Label labelSucessoEmail;

    // ── Seção Senha ────────────────────────────────────────────────
    @FXML private PasswordField pfSenhaAtual;
    @FXML private PasswordField pfNovaSenha;
    @FXML private PasswordField pfConfirmarNovaSenha;
    @FXML private Label labelErroSenha;
    @FXML private Label labelSucessoSenha;

    private final UsuarioService usuarioService = new UsuarioService();

    /**
     * Preenche o campo de e-mail com o valor atual do usuário logado ao inicializar a tela.
     */
    @FXML
    public void initialize() {
        var usuario = SessionManager.getInstance().getUsuarioLogado();
        if (usuario != null) {
            tfNovoEmail.setText(usuario.getEmail());
        }
    }

    /**
     * Trata o clique em "Salvar novo e-mail".
     * Exige a senha atual para confirmar a operação antes de persistir.
     */
    @FXML
    private void handleAlterarEmail() {
        String novoEmail = tfNovoEmail.getText().trim();
        String senha = pfSenhaConfEmail.getText();
        ocultarFeedbackEmail();

        if (novoEmail.isBlank() || senha.isBlank()) {
            mostrarErroEmail("Preencha o e-mail e a senha de confirmação.");
            return;
        }

        int usuarioId = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                usuarioService.alterarEmail(usuarioId, senha, novoEmail);
                SessionManager.getInstance().getUsuarioLogado().setEmail(novoEmail);
                Platform.runLater(() -> {
                    mostrarSucessoEmail("E-mail atualizado com sucesso.");
                    pfSenhaConfEmail.clear();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErroEmail(ex.getMessage()));
            }
        }).start();
    }

    /**
     * Trata o clique em "Alterar senha".
     * Valida que a nova senha e a confirmação coincidem, e exige a senha atual.
     */
    @FXML
    private void handleAlterarSenha() {
        String senhaAtual = pfSenhaAtual.getText();
        String novaSenha = pfNovaSenha.getText();
        String confirmar = pfConfirmarNovaSenha.getText();
        ocultarFeedbackSenha();

        if (senhaAtual.isBlank() || novaSenha.isBlank()) {
            mostrarErroSenha("Preencha todos os campos de senha.");
            return;
        }
        if (novaSenha.length() < 6) {
            mostrarErroSenha("A nova senha deve ter pelo menos 6 caracteres.");
            return;
        }
        if (!novaSenha.equals(confirmar)) {
            mostrarErroSenha("A nova senha e a confirmação não coincidem.");
            return;
        }

        int usuarioId = SessionManager.getInstance().getUsuarioLogado().getId();
        new Thread(() -> {
            try {
                usuarioService.alterarSenha(usuarioId, senhaAtual, novaSenha);
                Platform.runLater(() -> {
                    mostrarSucessoSenha("Senha alterada com sucesso.");
                    pfSenhaAtual.clear();
                    pfNovaSenha.clear();
                    pfConfirmarNovaSenha.clear();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> mostrarErroSenha(ex.getMessage()));
            }
        }).start();
    }

    /**
     * Trata o clique em "Excluir minha conta".
     * Exibe diálogo de confirmação solicitando a senha e, se confirmado, exclui a conta e encerra a sessão.
     */
    @FXML
    private void handleExcluirConta() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Excluir Conta");
        confirm.setHeaderText("Esta ação não pode ser desfeita!");
        confirm.setContentText("Todos os seus dados (notas, cadernos, PDFs) serão excluídos permanentemente.\n\nDeseja continuar?");
        ThemeManager.getInstance().aplicarTemaAoDialogo(confirm);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            // Solicita senha para confirmar
            PasswordField pfSenha = new PasswordField();
            pfSenha.setPromptText("Digite sua senha para confirmar");
            Dialog<ButtonType> dialSenha = new Dialog<>();
            dialSenha.setTitle("Confirmar Exclusão");
            dialSenha.setHeaderText("Digite sua senha:");
            dialSenha.getDialogPane().setContent(pfSenha);
            dialSenha.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            ThemeManager.getInstance().aplicarTemaAoDialogo(dialSenha);
            dialSenha.showAndWait().ifPresent(b -> {
                if (b != ButtonType.OK) return;
                String senha = pfSenha.getText();
                if (senha.isBlank()) return;

                int usuarioId = SessionManager.getInstance().getUsuarioLogado().getId();
                new Thread(() -> {
                    try {
                        usuarioService.excluirConta(usuarioId, senha);
                        Platform.runLater(() -> {
                            SessionManager.getInstance().logout();
                            // Fecha o modal e volta ao login
                            pfSenha.getScene().getWindow().hide();
                            try { StudyApplication.mostrarTelaLogin(); } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            Alert err = new Alert(Alert.AlertType.ERROR);
                            err.setTitle("Erro");
                            err.setHeaderText(null);
                            err.setContentText(ex.getMessage());
                            ThemeManager.getInstance().aplicarTemaAoDialogo(err);
                            err.showAndWait();
                        });
                    }
                }).start();
            });
        });
    }

    // ── Feedback visual ─────────────────────────────────────────────────────

    private void mostrarErroEmail(String msg) {
        labelErroEmail.setText(msg);
        labelErroEmail.setVisible(true); labelErroEmail.setManaged(true);
    }

    private void mostrarSucessoEmail(String msg) {
        labelSucessoEmail.setText(msg);
        labelSucessoEmail.setVisible(true); labelSucessoEmail.setManaged(true);
    }

    private void ocultarFeedbackEmail() {
        labelErroEmail.setVisible(false); labelErroEmail.setManaged(false);
        labelSucessoEmail.setVisible(false); labelSucessoEmail.setManaged(false);
    }

    private void mostrarErroSenha(String msg) {
        labelErroSenha.setText(msg);
        labelErroSenha.setVisible(true); labelErroSenha.setManaged(true);
    }

    private void mostrarSucessoSenha(String msg) {
        labelSucessoSenha.setText(msg);
        labelSucessoSenha.setVisible(true); labelSucessoSenha.setManaged(true);
    }

    private void ocultarFeedbackSenha() {
        labelErroSenha.setVisible(false); labelErroSenha.setManaged(false);
        labelSucessoSenha.setVisible(false); labelSucessoSenha.setManaged(false);
    }
}
