package com.leonhardsen.studyapp.util;

import javafx.scene.Scene;
import javafx.scene.control.Dialog;

import java.util.List;

public class ThemeManager {

    private static ThemeManager instancia;
    private Scene cenaAtiva;
    private String temaAtual = "CLARO";

    private static final String CSS_CLARO = "/com/leonhardsen/studyapp/css/light-theme.css";
    private static final String CSS_ESCURO = "/com/leonhardsen/studyapp/css/dark-theme.css";

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
        this.cenaAtiva = cena;
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
        String caminhoCss = "ESCURO".equals(tema) ? CSS_ESCURO : CSS_CLARO;
        var url = getClass().getResource(caminhoCss);
        if (url != null) {
            cenaAtiva.getStylesheets().add(url.toExternalForm());
        }
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
        temaAtual = "CLARO".equals(temaAtual) ? "ESCURO" : "CLARO";
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
        String caminho = "ESCURO".equals(temaAtual) ? CSS_ESCURO : CSS_CLARO;
        var url = getClass().getResource(caminho);
        return url != null ? List.of(url.toExternalForm()) : List.of();
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
