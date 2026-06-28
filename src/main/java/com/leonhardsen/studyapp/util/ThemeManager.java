package com.leonhardsen.studyapp.util;

import javafx.scene.Scene;
import javafx.scene.control.Dialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gerenciador de temas e escala de fonte da aplicação.
 * Controla qual arquivo CSS está ativo, alterna entre os três temas disponíveis
 * (Claro, Escuro e Lúdico) e aplica um overlay de escala de fonte sem alterar
 * o layout nem o tamanho dos elementos gráficos.
 *
 * <p>A escala de fonte é aplicada gerando um arquivo CSS temporário com os mesmos
 * seletores do tema ativo e os tamanhos de fonte multiplicados pelo fator de escala.
 * Por ter a mesma especificidade e ser carregado por último, esse overlay prevalece.</p>
 *
 * @author StudyApp
 * @version 2.0
 */
public class ThemeManager {

    private static ThemeManager instancia;
    private Scene cenaAtiva;
    private String temaAtual = "CLARO";

    private double escalaFonte = 1.0;
    private static final double ESCALA_MIN   = 0.85;
    private static final double ESCALA_MAX   = 1.25;
    private static final double ESCALA_PASSO = 0.05;

    private static final String DIR_APP    = System.getProperty("user.home") + "/.studyapp";
    private static final String PROPS_PATH = DIR_APP + "/ui.properties";

    private boolean escalaCarregada = false;
    private String urlCssEscala = null;

    private static final String CSS_CLARO  = "/com/leonhardsen/studyapp/css/light-theme.css";
    private static final String CSS_ESCURO = "/com/leonhardsen/studyapp/css/dark-theme.css";
    private static final String CSS_LUDICO = "/com/leonhardsen/studyapp/css/ludic-theme.css";

    /**
     * Construtor privado para impedir instanciação direta.
     */
    private ThemeManager() {
    }

    /**
     * Retorna a instância única do gerenciador de temas.
     *
     * @return instância do ThemeManager
     */
    public static ThemeManager getInstance() {
        if (instancia == null) {
            instancia = new ThemeManager();
        }
        return instancia;
    }

    /**
     * Define a cena JavaFX na qual os temas serão aplicados.
     * Deve ser chamado sempre que uma nova cena for carregada.
     *
     * @param cena nova cena ativa da aplicação
     */
    public void setCena(Scene cena) {
        if (!escalaCarregada) {
            carregarEscala();
            escalaCarregada = true;
        }
        this.cenaAtiva = cena;
        aplicarEscala();
    }

    /**
     * Aplica o tema especificado à cena ativa e re-aplica o overlay de escala de fonte.
     *
     * @param tema nome do tema a aplicar: "CLARO", "ESCURO" ou "LUDICO"
     */
    public void aplicarTema(String tema) {
        if (cenaAtiva == null) return;
        this.temaAtual = tema;
        urlCssEscala = null; // será recriado em aplicarEscala()
        cenaAtiva.getStylesheets().clear();
        String caminhoCss = cssPorTema(tema);
        var url = getClass().getResource(caminhoCss);
        if (url != null) {
            cenaAtiva.getStylesheets().add(url.toExternalForm());
        }
        aplicarEscala();
    }

    private String cssPorTema(String tema) {
        return switch (tema) {
            case "ESCURO" -> CSS_ESCURO;
            case "LUDICO" -> CSS_LUDICO;
            default       -> CSS_CLARO;
        };
    }

    /**
     * Reaplica o tema atual à cena ativa. Útil ao trocar de cena sem mudar o tema.
     */
    public void aplicarTemaAtual() {
        aplicarTema(temaAtual);
    }

    /**
     * Alterna ciclicamente entre os temas Claro → Escuro → Lúdico → Claro.
     *
     * @return o nome do novo tema aplicado
     */
    public String alternarTema() {
        temaAtual = switch (temaAtual) {
            case "CLARO"  -> "ESCURO";
            case "ESCURO" -> "LUDICO";
            default       -> "CLARO";
        };
        aplicarTema(temaAtual);
        return temaAtual;
    }

    /**
     * Retorna o nome do tema atualmente ativo.
     *
     * @return "CLARO", "ESCURO" ou "LUDICO"
     */
    public String getTemaAtual() {
        return temaAtual;
    }

    /**
     * Define o tema atual sem aplicá-lo à cena (usado ao restaurar preferência do banco).
     *
     * @param tema nome do tema
     */
    public void setTemaAtual(String tema) {
        this.temaAtual = tema;
    }

    /**
     * Retorna a lista de URLs de stylesheets ativos (tema + overlay de escala, se houver).
     * Usada para propagar o tema aos diálogos.
     *
     * @return lista imutável de URLs de CSS
     */
    public List<String> getStylesheets() {
        var url = getClass().getResource(cssPorTema(temaAtual));
        List<String> list = new ArrayList<>();
        if (url != null) list.add(url.toExternalForm());
        if (urlCssEscala != null) list.add(urlCssEscala);
        return Collections.unmodifiableList(list);
    }

    /**
     * Retorna o fator de escala de fonte atual.
     *
     * @return escala entre {@value #ESCALA_MIN} e {@value #ESCALA_MAX}
     */
    public double getEscalaFonte() { return escalaFonte; }

    /**
     * Aumenta a escala de fonte em um passo, até o máximo permitido.
     */
    public void aumentarFonte() {
        if (escalaFonte < ESCALA_MAX - 0.001) {
            escalaFonte = Math.min(ESCALA_MAX, Math.round((escalaFonte + ESCALA_PASSO) * 100.0) / 100.0);
            aplicarEscala();
            salvarEscala();
        }
    }

    /**
     * Reduz a escala de fonte em um passo, até o mínimo permitido.
     */
    public void reduzirFonte() {
        if (escalaFonte > ESCALA_MIN + 0.001) {
            escalaFonte = Math.max(ESCALA_MIN, Math.round((escalaFonte - ESCALA_PASSO) * 100.0) / 100.0);
            aplicarEscala();
            salvarEscala();
        }
    }

    /**
     * Aplica o overlay de escala de fonte à cena ativa.
     * Gera um arquivo CSS temporário com os tamanhos de fonte do tema atual
     * multiplicados pelo fator de escala e o adiciona como último stylesheet.
     * Ao escala = 1,0 o overlay é removido e não é criado nenhum arquivo.
     */
    private void aplicarEscala() {
        if (cenaAtiva == null) return;

        // Remove overlay anterior
        if (urlCssEscala != null) {
            cenaAtiva.getStylesheets().remove(urlCssEscala);
            excluirArquivoEscala(urlCssEscala);
            urlCssEscala = null;
        }

        if (Math.abs(escalaFonte - 1.0) < 0.001) return; // Sem overlay na escala neutra

        String cssContent = gerarCssEscala();
        if (cssContent.isEmpty()) return;

        // Nome único para forçar recarga pelo JavaFX (que cacheia por URL)
        String path = DIR_APP + "/font-scale-" + System.currentTimeMillis() + ".css";
        try {
            new File(DIR_APP).mkdirs();
            try (FileWriter fw = new FileWriter(path)) {
                fw.write(cssContent);
            }
            urlCssEscala = new File(path).toURI().toURL().toExternalForm();
            cenaAtiva.getStylesheets().add(urlCssEscala);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void excluirArquivoEscala(String url) {
        try {
            new File(new java.net.URI(url)).delete();
        } catch (Exception ignored) {}
    }

    /**
     * Gera o conteúdo CSS do overlay de escala lendo o arquivo do tema atual,
     * extraindo todos os seletores com {@code -fx-font-size: Xpx} e gerando
     * regras equivalentes com o tamanho multiplicado pelo fator de escala.
     * Por ter a mesma especificidade e ser carregado depois, prevalece sobre o tema.
     *
     * @return string com o conteúdo CSS do overlay, ou vazia se não foi possível gerar
     */
    private String gerarCssEscala() {
        StringBuilder sb = new StringBuilder();
        try {
            String cssPath = cssPorTema(temaAtual);
            try (InputStream is = getClass().getResourceAsStream(cssPath)) {
                if (is == null) return "";
                String css = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                // Remove comentários CSS para evitar captura de seletores incorretos
                css = css.replaceAll("(?s)/\\*.*?\\*/", "");

                Pattern blockPat = Pattern.compile("([^{}]+)\\{([^{}]*)\\}", Pattern.DOTALL);
                Pattern fontPat  = Pattern.compile("-fx-font-size:\\s*(\\d+(?:\\.\\d+)?)px");

                Matcher m = blockPat.matcher(css);
                while (m.find()) {
                    String selector = m.group(1).trim();
                    String body     = m.group(2);
                    if (selector.isEmpty()) continue;

                    Matcher fm = fontPat.matcher(body);
                    if (fm.find()) {
                        double basePx   = Double.parseDouble(fm.group(1));
                        double scalePx  = basePx * escalaFonte;
                        sb.append(selector)
                          .append(" { -fx-font-size: ")
                          .append(String.format("%.1f", scalePx))
                          .append("px; }\n");
                    }
                }
            }
        } catch (Exception ignored) {}
        return sb.toString();
    }

    private void salvarEscala() {
        try {
            Properties props = new Properties();
            props.setProperty("escala_fonte", String.valueOf(escalaFonte));
            new File(DIR_APP).mkdirs();
            try (var out = new FileOutputStream(PROPS_PATH)) {
                props.store(out, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void carregarEscala() {
        try {
            File f = new File(PROPS_PATH);
            if (f.exists()) {
                Properties props = new Properties();
                try (var in = new FileInputStream(f)) {
                    props.load(in);
                }
                double v = Double.parseDouble(props.getProperty("escala_fonte", "1.0"));
                escalaFonte = Math.max(ESCALA_MIN, Math.min(ESCALA_MAX, v));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Aplica o tema ao DialogPane e também à cena (quando disponível),
     * garantindo que popups internos (DatePicker, ComboBox) também sejam estilizados.
     *
     * @param dialog diálogo ao qual o tema deve ser aplicado
     */
    public void aplicarTemaAoDialogo(Dialog<?> dialog) {
        List<String> css = getStylesheets();
        dialog.getDialogPane().getStylesheets().addAll(css);
        dialog.getDialogPane().sceneProperty().addListener((obs, old, newScene) -> {
            if (newScene != null) {
                newScene.getStylesheets().removeAll(css);
                newScene.getStylesheets().addAll(css);
            }
        });
    }
}
