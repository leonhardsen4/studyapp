package com.leonhardsen.studyapp.database;

import com.leonhardsen.studyapp.model.ItemArvore;
import com.leonhardsen.studyapp.model.TipoItem;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Objeto de acesso a dados para a entidade {@link ItemArvore}.
 * Gerencia a persistência da estrutura hierárquica de arquivos do sistema.
 * Todos os métodos filtram os dados pelo {@code usuarioId} para garantir isolamento entre usuários.
 *
 * @author StudyApp
 * @version 1.0
 */
public class ItemArvoreDAO {

    private final DatabaseManager dbManager;

    /**
     * Construtor que recebe a instância do gerenciador de banco de dados.
     *
     * @param dbManager gerenciador de conexão com o banco de dados
     */
    public ItemArvoreDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /**
     * Insere um novo item na árvore de arquivos.
     * O campo {@code id} do objeto é atualizado com o valor gerado automaticamente.
     *
     * @param item objeto com os dados do item a ser inserido
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void inserir(ItemArvore item) throws SQLException {
        String sql = "INSERT INTO item_arvore (usuario_id, pai_id, nome, tipo, posicao) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, item.getUsuarioId());
            if (item.getPaiId() != null) {
                stmt.setInt(2, item.getPaiId());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setString(3, item.getNome());
            stmt.setString(4, item.getTipo().name());
            stmt.setInt(5, item.getPosicao());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    item.setId(rs.getInt(1));
                }
            }
        }
    }

    /**
     * Busca todos os itens da árvore pertencentes a um usuário, ordenados por posição.
     * Retorna a lista plana completa; a hierarquia deve ser reconstruída em memória.
     *
     * @param usuarioId identificador do usuário
     * @return lista de todos os itens do usuário, ordenados por posição
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public List<ItemArvore> buscarTodos(int usuarioId) throws SQLException {
        String sql = "SELECT * FROM item_arvore WHERE usuario_id = ? ORDER BY posicao, id";
        List<ItemArvore> itens = new ArrayList<>();
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    itens.add(mapearItem(rs));
                }
            }
        }
        return itens;
    }

    /**
     * Busca somente os cadernos raiz (sem pai) de um usuário.
     *
     * @param usuarioId identificador do usuário
     * @return lista de cadernos raiz do usuário
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public List<ItemArvore> buscarCadernos(int usuarioId) throws SQLException {
        String sql = "SELECT * FROM item_arvore WHERE usuario_id = ? AND tipo = 'CADERNO' ORDER BY posicao, nome";
        List<ItemArvore> cadernos = new ArrayList<>();
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    cadernos.add(mapearItem(rs));
                }
            }
        }
        return cadernos;
    }

    /**
     * Atualiza o nome de um item na árvore.
     *
     * @param itemId  identificador do item
     * @param novoNome novo nome a ser atribuído
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void atualizarNome(int itemId, String novoNome) throws SQLException {
        String sql = "UPDATE item_arvore SET nome = ?, atualizado_em = datetime('now') WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setString(1, novoNome);
            stmt.setInt(2, itemId);
            stmt.executeUpdate();
        }
    }

    /**
     * Move um item para um novo pai (caderno de destino).
     * Para mover para a raiz, passar {@code null} como novoPaiId.
     *
     * @param itemId    identificador do item a ser movido
     * @param novoPaiId identificador do novo caderno pai, ou {@code null} para raiz
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void mover(int itemId, Integer novoPaiId) throws SQLException {
        String sql = "UPDATE item_arvore SET pai_id = ?, atualizado_em = datetime('now') WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            if (novoPaiId != null) {
                stmt.setInt(1, novoPaiId);
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            stmt.setInt(2, itemId);
            stmt.executeUpdate();
        }
    }

    /**
     * Exclui todos os itens de um usuário. Usado antes de excluir o próprio usuário
     * para garantir compatibilidade com bancos que não possuem ON DELETE CASCADE em usuario_id.
     *
     * @param usuarioId identificador do usuário
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void excluirPorUsuario(int usuarioId) throws SQLException {
        String sql = "DELETE FROM item_arvore WHERE usuario_id = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            stmt.executeUpdate();
        }
    }

    /**
     * Exclui um item e todos os seus filhos em cascata (definido pela chave estrangeira no banco).
     *
     * @param itemId identificador do item a ser excluído
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void excluir(int itemId) throws SQLException {
        String sql = "DELETE FROM item_arvore WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            stmt.executeUpdate();
        }
    }

    /**
     * Conta quantos filhos diretos e indiretos um item possui.
     * Usado para exibir ao usuário antes de confirmar a exclusão de um caderno.
     *
     * @param itemId    identificador do item
     * @param usuarioId identificador do usuário (segurança)
     * @return número total de filhos e descendentes
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public int contarDescendentes(int itemId, int usuarioId) throws SQLException {
        String sql = """
            WITH RECURSIVE descendentes AS (
                SELECT id FROM item_arvore WHERE pai_id = ? AND usuario_id = ?
                UNION ALL
                SELECT ia.id FROM item_arvore ia
                INNER JOIN descendentes d ON ia.pai_id = d.id
            )
            SELECT COUNT(*) FROM descendentes
            """;
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, itemId);
            stmt.setInt(2, usuarioId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * Busca todos os filhos diretos de um item (ou os itens raiz se paiId for null),
     * ordenados por posição. Usado para operações de reordenação.
     *
     * @param usuarioId identificador do usuário
     * @param paiId     id do pai, ou {@code null} para itens raiz
     * @return lista de filhos diretos na ordem de exibição
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public List<ItemArvore> buscarFilhos(int usuarioId, Integer paiId) throws SQLException {
        String sql = paiId == null
            ? "SELECT * FROM item_arvore WHERE usuario_id = ? AND pai_id IS NULL ORDER BY posicao, id"
            : "SELECT * FROM item_arvore WHERE usuario_id = ? AND pai_id = ? ORDER BY posicao, id";
        List<ItemArvore> itens = new ArrayList<>();
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, usuarioId);
            if (paiId != null) stmt.setInt(2, paiId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) itens.add(mapearItem(rs));
            }
        }
        return itens;
    }

    /**
     * Atualiza o campo de posição de um item.
     *
     * @param itemId  identificador do item
     * @param posicao novo valor de posição
     * @throws SQLException se ocorrer erro no banco de dados
     */
    public void atualizarPosicao(int itemId, int posicao) throws SQLException {
        String sql = "UPDATE item_arvore SET posicao = ? WHERE id = ?";
        try (PreparedStatement stmt = dbManager.getConexao().prepareStatement(sql)) {
            stmt.setInt(1, posicao);
            stmt.setInt(2, itemId);
            stmt.executeUpdate();
        }
    }

    /**
     * Converte uma linha do ResultSet em um objeto {@link ItemArvore}.
     *
     * @param rs ResultSet posicionado na linha a ser mapeada
     * @return objeto ItemArvore preenchido com os dados da linha
     * @throws SQLException se ocorrer erro ao ler o ResultSet
     */
    private ItemArvore mapearItem(ResultSet rs) throws SQLException {
        ItemArvore item = new ItemArvore();
        item.setId(rs.getInt("id"));
        item.setUsuarioId(rs.getInt("usuario_id"));
        int paiId = rs.getInt("pai_id");
        item.setPaiId(rs.wasNull() ? null : paiId);
        item.setNome(rs.getString("nome"));
        item.setTipo(TipoItem.valueOf(rs.getString("tipo")));
        item.setPosicao(rs.getInt("posicao"));
        return item;
    }
}
