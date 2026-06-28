package com.leonhardsen.studyapp.util;

import javafx.scene.Scene;
import javafx.scene.control.Dialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Properties;

public class ThemeManager {

    private static ThemeManager instancia;
    private Scene cenaAtiva;
    private String temaAtual = "CLARO";

    private double escalaFonte = 1.0;
    private static final double ESCALA_MIN   = 0.85;
    private static final double ESCALA_MAX   = 1.25;
    private static final double ESCALA_PASSO = 0.05;
    private static final String PROPS_PATH   = System.getProperty("user.home") + "/.studyapp/ui.properties";
    private boolean escalaCarregada = false;

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
     * Aplica o tema especificado à cena ativa.
     * Remove o CSS atual e adiciona o novo correspondente ao tema solicitado.
     *
     * @param tema nome do tema a aplicar: "CLARO" ou "ESCURO"
     */
    public void aplicarTema(String tema) {
        if (cenaAtiva == null) return;
        this.temaAtual = tema;
        cenaAtiva.getStylesheets().clear();
        String caminhoCss = cssPorTema(tema);
        var url = getClass().getResource(caminhoCss);
        if (url != null) {
            cenaAtiva.getStylesheets().add(url.toExternalForm());
        }
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
     * Alterna entre os temas claro e escuro e aplica imediatamente à cena ativa.
     *
     * @return o nome do novo tema aplicado ("CLARO" ou "ESCURO")
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
     * @return "CLARO" ou "ESCURO"
     */
    public String getTemaAtual() {
        return temaAtual;
    }

    public void setTemaAtual(String tema) {
        this.temaAtual = tema;
    }

    public List<String> getStylesheets() {
        var url = getClass().getResource(cssPorTema(temaAtual));
        return url != null ? List.of(url.toExternalForm()) : List.of();
    }

    public double getEscalaFonte() { return escalaFonte; }

    public void aumentarFonte() {
        if (escalaFonte < ESCALA_MAX - 0.001) {
            escalaFonte = Math.min(ESCALA_MAX, Math.round((escalaFonte + ESCALA_PASSO) * 100.0) / 100.0);
            aplicarEscala();
            salvarEscala();
        }
    }

    public void reduzirFonte() {
        if (escalaFonte > ESCALA_MIN + 0.001) {
            escalaFonte = Math.max(ESCALA_MIN, Math.round((escalaFonte - ESCALA_PASSO) * 100.0) / 100.0);
            aplicarEscala();
            salvarEscala();
        }
    }

    private void aplicarEscala() {
        if (cenaAtiva == null) return;
        cenaAtiva.getRoot().setScaleX(escalaFonte);
        cenaAtiva.getRoot().setScaleY(escalaFonte);
    }

    private void salvarEscala() {
        try {
            Properties props = new Properties();
            props.setProperty("escala_fonte", String.valueOf(escalaFonte));
            new File(System.getProperty("user.home") + "/.studyapp").mkdirs();
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

    /** Aplica o tema ao DialogPane e também à cena (quando disponível),
     *  garantindo que popups internos (DatePicker, ComboBox) também sejam estilizados. */
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
