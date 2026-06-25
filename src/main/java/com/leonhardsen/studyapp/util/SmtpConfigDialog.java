package com.leonhardsen.studyapp.util;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import java.util.Properties;

/**
 * Diálogo de configuração de SMTP para envio de e-mails pelo StudyApp.
 * Lê as configurações atuais e permite ao usuário salvá-las através da interface gráfica,
 * sem precisar editar o arquivo {@code ~/.studyapp/mail.properties} manualmente.
 *
 * @author StudyApp
 * @version 1.0
 */
public class SmtpConfigDialog {

    /**
     * Construtor privado — classe utilitária, não deve ser instanciada.
     */
    private SmtpConfigDialog() {
    }

    /**
     * Exibe o diálogo de configuração SMTP e salva as configurações se o usuário confirmar.
     *
     * @param owner janela pai do diálogo (pode ser {@code null})
     */
    public static void mostrar(Stage owner) {
        Properties atual = EmailService.lerConfiguracao();

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Configurar Envio de E-mail");
        dialog.setHeaderText(
            "Configure o servidor SMTP para recuperação de senha.\n" +
            "Gmail: use uma Senha de App (não sua senha principal).\n" +
            "Acesse: myaccount.google.com → Segurança → Senhas de app"
        );
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ThemeManager.getInstance().aplicarTemaAoDialogo(dialog);
        if (owner != null) dialog.initOwner(owner);

        TextField tfHost    = new TextField(atual.getProperty("smtp.host", "smtp.gmail.com"));
        TextField tfPort    = new TextField(atual.getProperty("smtp.port", "587"));
        TextField tfUser    = new TextField(atual.getProperty("smtp.user", ""));
        PasswordField tfSenha = new PasswordField();
        tfSenha.setText(atual.getProperty("smtp.password", ""));
        tfSenha.setPromptText("Senha de App do Gmail ou senha SMTP");
        CheckBox cbTls = new CheckBox("Usar STARTTLS (recomendado)");
        cbTls.setSelected(!"false".equals(atual.getProperty("smtp.starttls", "true")));

        Label lblDica = new Label("Dica Gmail: em Segurança da Conta Google, crie uma Senha de App\n" +
            "para \"Outro aplicativo\" e use-a aqui.");
        lblDica.setStyle("-fx-text-fill: -fx-text-base-color; -fx-font-size: 11px; -fx-opacity: 0.7;");
        lblDica.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(16));
        grid.add(new Label("Servidor SMTP:"),    0, 0); grid.add(tfHost,  1, 0);
        grid.add(new Label("Porta:"),            0, 1); grid.add(tfPort,  1, 1);
        grid.add(new Label("E-mail remetente:"), 0, 2); grid.add(tfUser,  1, 2);
        grid.add(new Label("Senha de App:"),     0, 3); grid.add(tfSenha, 1, 3);
        grid.add(cbTls,                          1, 4);
        grid.add(lblDica,                        0, 5, 2, 1);

        GridPane.setHgrow(tfHost,  Priority.ALWAYS);
        GridPane.setHgrow(tfUser,  Priority.ALWAYS);
        GridPane.setHgrow(tfSenha, Priority.ALWAYS);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setMinWidth(440);

        dialog.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;
            String host = tfHost.getText().trim();
            if (host.isBlank()) return;
            try {
                EmailService.salvarConfiguracao(
                    host,
                    tfPort.getText().trim(),
                    tfUser.getText().trim(),
                    tfSenha.getText(),
                    cbTls.isSelected()
                );
                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Configuração Salva");
                ok.setHeaderText(null);
                ok.setContentText("Configuração SMTP salva com sucesso!\n" +
                    "Tente novamente recuperar a senha.");
                ThemeManager.getInstance().aplicarTemaAoDialogo(ok);
                ok.showAndWait();
            } catch (Exception ex) {
                Alert erro = new Alert(Alert.AlertType.ERROR);
                erro.setTitle("Erro ao Salvar");
                erro.setHeaderText(null);
                erro.setContentText("Não foi possível salvar: " + ex.getMessage());
                ThemeManager.getInstance().aplicarTemaAoDialogo(erro);
                erro.showAndWait();
            }
        });
    }
}
