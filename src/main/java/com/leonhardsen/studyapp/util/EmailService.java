package com.leonhardsen.studyapp.util;

import com.leonhardsen.studyapp.database.DatabaseManager;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.*;
import java.util.Properties;

/**
 * Serviço de envio de e-mails via SMTP usando Jakarta Mail.
 * As configurações de servidor são lidas do arquivo {@code ~/.studyapp/mail.properties}.
 * Se o arquivo não existir, um modelo comentado é criado automaticamente.
 *
 * @author StudyApp
 * @version 1.0
 */
public class EmailService {

    private static final String ARQUIVO_CONFIG = DatabaseManager.getDirApp() + "/mail.properties";

    /**
     * Construtor privado — classe utilitária, não deve ser instanciada.
     */
    private EmailService() {
    }

    /**
     * Envia um e-mail com assunto e corpo definidos.
     * Lê as configurações SMTP de {@code ~/.studyapp/mail.properties}.
     *
     * @param destinatario endereço de e-mail do destinatário
     * @param assunto      assunto da mensagem
     * @param corpo        corpo da mensagem em texto puro
     * @throws Exception se a configuração SMTP estiver ausente ou ocorrer erro de envio
     */
    public static void enviar(String destinatario, String assunto, String corpo) throws Exception {
        Properties config = carregarConfiguracao();
        String host = config.getProperty("smtp.host", "");
        if (host.isBlank()) {
            throw new Exception("Configuração SMTP não encontrada. Configure o arquivo: " + ARQUIVO_CONFIG);
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", config.getProperty("smtp.port", "587"));
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", config.getProperty("smtp.starttls", "true"));

        String usuario = config.getProperty("smtp.user", "");
        String senha = config.getProperty("smtp.password", "");

        Session sessao = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, senha);
            }
        });

        Message mensagem = new MimeMessage(sessao);
        mensagem.setFrom(new InternetAddress(usuario));
        mensagem.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
        mensagem.setSubject(assunto);
        mensagem.setText(corpo);
        Transport.send(mensagem);
    }

    /**
     * Carrega as configurações SMTP do arquivo de propriedades.
     * Se o arquivo não existir, cria um modelo comentado para o usuário preencher.
     *
     * @return objeto {@link Properties} com as configurações SMTP
     * @throws IOException se ocorrer erro ao ler ou criar o arquivo de configuração
     */
    private static Properties carregarConfiguracao() throws IOException {
        File arquivo = new File(ARQUIVO_CONFIG);
        if (!arquivo.exists()) {
            criarArquivoModeloSmtp(arquivo);
        }
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(arquivo)) {
            props.load(fis);
        }
        return props;
    }

    /**
     * Salva as configurações SMTP no arquivo de propriedades.
     * Chamado pela tela de configuração de e-mail da interface gráfica.
     *
     * @param host     endereço do servidor SMTP (ex: smtp.gmail.com)
     * @param port     porta SMTP (ex: 587)
     * @param user     e-mail do remetente
     * @param password senha de aplicativo
     * @param starttls {@code true} para habilitar STARTTLS
     * @throws IOException se ocorrer erro ao gravar o arquivo
     */
    public static void salvarConfiguracao(String host, String port, String user,
                                          String password, boolean starttls) throws IOException {
        File arquivo = new File(ARQUIVO_CONFIG);
        arquivo.getParentFile().mkdirs();
        Properties props = new Properties();
        props.setProperty("smtp.host", host.trim());
        props.setProperty("smtp.port", port.trim());
        props.setProperty("smtp.user", user.trim());
        props.setProperty("smtp.password", password);
        props.setProperty("smtp.starttls", String.valueOf(starttls));
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(arquivo)) {
            props.store(fos, "Configuração SMTP — StudyApp");
        }
    }

    /**
     * Retorna as configurações SMTP atualmente salvas no arquivo de propriedades.
     * Retorna um {@link Properties} vazio se o arquivo não existir ou não puder ser lido.
     *
     * @return configurações SMTP ou objeto vazio
     */
    public static Properties lerConfiguracao() {
        try {
            return carregarConfiguracao();
        } catch (IOException e) {
            return new Properties();
        }
    }

    /**
     * Cria um arquivo de configuração SMTP com valores de exemplo comentados.
     *
     * @param arquivo arquivo a ser criado
     * @throws IOException se ocorrer erro ao criar o arquivo
     */
    private static void criarArquivoModeloSmtp(File arquivo) throws IOException {
        String modelo = """
                # Configuração SMTP para envio de e-mails pelo StudyApp
                # Exemplo com Gmail (use uma senha de aplicativo, não sua senha principal):
                # smtp.host=smtp.gmail.com
                # smtp.port=587
                # smtp.user=seu.email@gmail.com
                # smtp.password=sua_senha_de_aplicativo
                # smtp.starttls=true
                smtp.host=
                smtp.port=587
                smtp.user=
                smtp.password=
                smtp.starttls=true
                """;
        try (FileWriter fw = new FileWriter(arquivo)) {
            fw.write(modelo);
        }
    }
}
