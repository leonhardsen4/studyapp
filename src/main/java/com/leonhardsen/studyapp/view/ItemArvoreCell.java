package com.leonhardsen.studyapp.view;

import com.leonhardsen.studyapp.model.ItemArvore;
import com.leonhardsen.studyapp.model.TipoItem;
import javafx.scene.control.*;

/**
 * Célula customizada para exibição de itens na {@link TreeView} do sistema de arquivos.
 * Exibe ícone apropriado ao tipo do item e monta o menu de contexto com as opções disponíveis.
 * As ações são delegadas ao {@link com.leonhardsen.studyapp.controller.MainController}
 * via callbacks funcionais configurados externamente.
 *
 * @author StudyApp
 * @version 1.0
 */
public class ItemArvoreCell extends TreeCell<ItemArvore> {

    private Runnable onNovoCaderno;
    private Runnable onNovaNota;
    private Runnable onNovoPdf;
    private Runnable onRenomear;
    private Runnable onExcluir;
    private Runnable onMover;
    private Runnable onSubir;
    private Runnable onDescer;
    private Runnable onImprimir;
    private Runnable onArquivar;
    private Runnable onDesarquivar;

    /**
     * Construtor padrão sem argumentos.
     */
    public ItemArvoreCell() {
    }

    /**
     * Atualiza o conteúdo visual da célula com base no item fornecido.
     * Exibe o ícone e nome do item, ou limpa a célula se vazia.
     *
     * @param item  objeto {@link ItemArvore} a ser exibido, ou {@code null} para limpar a célula
     * @param vazio {@code true} se a célula estiver sem item
     */
    @Override
    protected void updateItem(ItemArvore item, boolean vazio) {
        super.updateItem(item, vazio);
        if (vazio || item == null) {
            setText(null);
            setGraphic(null);
            setContextMenu(null);
            getStyleClass().remove("file-tree-cell-arquivado");
        } else {
            setText(icone(item.getTipo()) + "  " + item.getNome());
            setContextMenu(criarMenuContexto(item));
            if (item.isArquivado()) {
                if (!getStyleClass().contains("file-tree-cell-arquivado")) {
                    getStyleClass().add("file-tree-cell-arquivado");
                }
            } else {
                getStyleClass().remove("file-tree-cell-arquivado");
            }
        }
    }

    /**
     * Retorna o ícone Unicode correspondente ao tipo do item.
     *
     * @param tipo tipo do item (CADERNO, NOTA ou PDF)
     * @return string com o caractere de ícone
     */
    private String icone(TipoItem tipo) {
        return switch (tipo) {
            case CADERNO -> "📁"; // 📁
            case NOTA -> "📄";    // 📄
            case PDF -> "📃";     // 📃
        };
    }

    /**
     * Cria e retorna o menu de contexto adequado para o tipo do item.
     * Cadernos exibem opções de criação de filhos; notas e PDFs apenas renomear/excluir/mover.
     *
     * @param item item ao qual o menu de contexto se refere
     * @return menu de contexto configurado para o tipo do item
     */
    private ContextMenu criarMenuContexto(ItemArvore item) {
        ContextMenu menu = new ContextMenu();

        if (item.getTipo() == TipoItem.CADERNO) {
            MenuItem iNovoCaderno = new MenuItem("Novo Caderno aqui");
            MenuItem iNovaNota = new MenuItem("Nova Nota aqui");
            MenuItem iNovoPdf = new MenuItem("Adicionar PDF");
            MenuItem iArquivarDesarquivar = item.isArquivado()
                ? new MenuItem("📬  Desarquivar")
                : new MenuItem("📦  Arquivar");
            MenuItem separador = new SeparatorMenuItem();

            iNovoCaderno.setOnAction(e -> { if (onNovoCaderno != null) onNovoCaderno.run(); });
            iNovaNota.setOnAction(e -> { if (onNovaNota != null) onNovaNota.run(); });
            iNovoPdf.setOnAction(e -> { if (onNovoPdf != null) onNovoPdf.run(); });
            if (item.isArquivado()) {
                iArquivarDesarquivar.setOnAction(e -> { if (onDesarquivar != null) onDesarquivar.run(); });
            } else {
                iArquivarDesarquivar.setOnAction(e -> { if (onArquivar != null) onArquivar.run(); });
            }

            menu.getItems().addAll(iNovoCaderno, iNovaNota, iNovoPdf, separador, iArquivarDesarquivar, new SeparatorMenuItem());
        }

        MenuItem iSubir = new MenuItem("↑ Subir");
        MenuItem iDescer = new MenuItem("↓ Descer");
        MenuItem iImprimir = new MenuItem("🖨  Imprimir / Exportar PDF");
        MenuItem iRenomear = new MenuItem("✏  Renomear");
        MenuItem iMover = new MenuItem("📂  Mover para...");
        MenuItem iExcluir = new MenuItem("🗑  Excluir");
        iExcluir.setStyle("-fx-text-fill: #c0392b;");

        if (item.getTipo() == TipoItem.CADERNO) {
            iImprimir.setDisable(true);
        }

        iSubir.setOnAction(e -> { if (onSubir != null) onSubir.run(); });
        iDescer.setOnAction(e -> { if (onDescer != null) onDescer.run(); });
        iImprimir.setOnAction(e -> { if (onImprimir != null) onImprimir.run(); });
        iRenomear.setOnAction(e -> { if (onRenomear != null) onRenomear.run(); });
        iMover.setOnAction(e -> { if (onMover != null) onMover.run(); });
        iExcluir.setOnAction(e -> { if (onExcluir != null) onExcluir.run(); });

        menu.getItems().addAll(
            iSubir, iDescer,
            new SeparatorMenuItem(),
            iImprimir, iRenomear, iMover,
            new SeparatorMenuItem(),
            iExcluir
        );
        return menu;
    }

    /**
     * Define o callback chamado ao solicitar criação de novo caderno.
     *
     * @param onNovoCaderno ação a executar
     */
    public void setOnNovoCaderno(Runnable onNovoCaderno) { this.onNovoCaderno = onNovoCaderno; }

    /**
     * Define o callback chamado ao solicitar criação de nova nota.
     *
     * @param onNovaNota ação a executar
     */
    public void setOnNovaNota(Runnable onNovaNota) { this.onNovaNota = onNovaNota; }

    /**
     * Define o callback chamado ao solicitar adição de PDF.
     *
     * @param onNovoPdf ação a executar
     */
    public void setOnNovoPdf(Runnable onNovoPdf) { this.onNovoPdf = onNovoPdf; }

    /**
     * Define o callback chamado ao solicitar renomeação do item.
     *
     * @param onRenomear ação a executar
     */
    public void setOnRenomear(Runnable onRenomear) { this.onRenomear = onRenomear; }

    /**
     * Define o callback chamado ao solicitar exclusão do item.
     *
     * @param onExcluir ação a executar
     */
    public void setOnExcluir(Runnable onExcluir) { this.onExcluir = onExcluir; }

    /**
     * Define o callback chamado ao solicitar movimentação do item.
     *
     * @param onMover ação a executar
     */
    public void setOnMover(Runnable onMover) { this.onMover = onMover; }

    /**
     * Define o callback chamado ao solicitar que o item suba uma posição.
     *
     * @param onSubir ação a executar
     */
    public void setOnSubir(Runnable onSubir) { this.onSubir = onSubir; }

    /**
     * Define o callback chamado ao solicitar que o item desça uma posição.
     *
     * @param onDescer ação a executar
     */
    public void setOnDescer(Runnable onDescer) { this.onDescer = onDescer; }

    /**
     * Define o callback chamado ao solicitar impressão ou exportação do item.
     *
     * @param onImprimir ação a executar
     */
    public void setOnImprimir(Runnable onImprimir) { this.onImprimir = onImprimir; }

    /**
     * Define o callback chamado ao solicitar arquivamento do caderno.
     *
     * @param onArquivar ação a executar
     */
    public void setOnArquivar(Runnable onArquivar) { this.onArquivar = onArquivar; }

    /**
     * Define o callback chamado ao solicitar desarquivamento do caderno.
     *
     * @param onDesarquivar ação a executar
     */
    public void setOnDesarquivar(Runnable onDesarquivar) { this.onDesarquivar = onDesarquivar; }
}
