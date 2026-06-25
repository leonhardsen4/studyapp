package com.leonhardsen.studyapp;

/**
 * Classe de entrada da aplicação StudyApp.
 * Necessária como ponto de entrada alternativo para execução sem módulo Java explícito,
 * contornando a restrição do JavaFX que exige que a classe main não estenda Application diretamente.
 *
 * @author StudyApp
 * @version 1.0
 */
public class Launcher {

    /**
     * Ponto de entrada principal da aplicação. Delega para {@link StudyApplication#main(String[])}.
     *
     * @param args argumentos de linha de comando (não utilizados)
     */
    public static void main(String[] args) {
        // Define locale pt-BR antes de qualquer inicialização do JavaFX para que
        // DatePicker, calendários e outros componentes usem formato brasileiro.
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("pt-BR"));
        StudyApplication.main(args);
    }
}