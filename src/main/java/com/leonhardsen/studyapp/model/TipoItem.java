package com.leonhardsen.studyapp.model;

/**
 * Enumeração dos tipos de itens suportados pelo sistema de arquivos.
 * Cada valor representa uma categoria distinta de item na árvore de arquivos.
 *
 * @author StudyApp
 * @version 1.0
 */
public enum TipoItem {

    /** Contêiner hierárquico que pode conter outros cadernos, notas e PDFs. */
    CADERNO,

    /** Documento de texto editável no formato Markdown. */
    NOTA,

    /** Arquivo PDF importado para o sistema. */
    PDF
}
