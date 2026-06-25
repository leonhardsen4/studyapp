package com.leonhardsen.studyapp.controller;

import com.leonhardsen.studyapp.database.DatabaseManager;
import com.leonhardsen.studyapp.util.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.function.Function;
import net.objecthunter.exp4j.operator.Operator;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador da Calculadora Científica.
 * Suporta operações matemáticas avançadas (trigonometria, logaritmos, combinatória,
 * potências, raízes, funções hiperbólicas), memória, histórico persistente e modo DEG/RAD.
 * O painel pode ser destacado em janela flutuante separada.
 */
public class CalculadoraController {

    @FXML private VBox       raiz;
    @FXML private TextArea   taExpressao;
    @FXML private Label      lblResultado;
    @FXML private Label      lblModo;
    @FXML private Label      lblMemoria;
    @FXML private GridPane   gridBotoes;
    @FXML private VBox       listaHistorico;
    @FXML private ScrollPane scrollHistorico;
    @FXML private Button     btnDetachar;

    private boolean    isDegMode  = true;
    private double     memoria    = 0.0;
    private boolean    temMemoria = false;
    private Stage      janelaDestacada;
    private BorderPane container;

    private final List<String> historico = new ArrayList<>();

    // ── Funções customizadas para exp4j ───────────────────────────────────────

    private final Function fFact  = new Function("fact",  1) {
        @Override public double apply(double... a) {
            int n = (int) Math.round(a[0]);
            if (n < 0)   throw new ArithmeticException("Fatorial de número negativo");
            if (n > 170) throw new ArithmeticException("Número muito grande para fatorial");
            long r = 1; for (int i = 2; i <= n; i++) r *= i; return r;
        }
    };
    private final Function fNcr   = new Function("ncr", 2) {
        @Override public double apply(double... a) {
            int n = (int) Math.round(a[0]), k = (int) Math.round(a[1]);
            if (k < 0 || k > n) return 0;
            return Math.round(Math.exp(logFat(n) - logFat(k) - logFat(n - k)));
        }
    };
    private final Function fNpr   = new Function("npr", 2) {
        @Override public double apply(double... a) {
            int n = (int) Math.round(a[0]), k = (int) Math.round(a[1]);
            if (k < 0 || k > n) return 0;
            return Math.round(Math.exp(logFat(n) - logFat(n - k)));
        }
    };
    // Trig em graus
    private final Function fSind  = new Function("sind",  1) { @Override public double apply(double... a) { return Math.sin(Math.toRadians(a[0])); } };
    private final Function fCosd  = new Function("cosd",  1) { @Override public double apply(double... a) { return Math.cos(Math.toRadians(a[0])); } };
    private final Function fTand  = new Function("tand",  1) { @Override public double apply(double... a) { return Math.tan(Math.toRadians(a[0])); } };
    private final Function fAsind = new Function("asind", 1) { @Override public double apply(double... a) { return Math.toDegrees(Math.asin(a[0])); } };
    private final Function fAcosd = new Function("acosd", 1) { @Override public double apply(double... a) { return Math.toDegrees(Math.acos(a[0])); } };
    private final Function fAtand = new Function("atand", 1) { @Override public double apply(double... a) { return Math.toDegrees(Math.atan(a[0])); } };

    private final Operator opMod  = new Operator("%", 2, true, Operator.PRECEDENCE_MULTIPLICATION) {
        @Override public double apply(double[] a) {
            if (a[1] == 0) throw new ArithmeticException("Divisão por zero");
            return a[0] % a[1];
        }
    };

    private static double logFat(int n) {
        double r = 0; for (int i = 2; i <= n; i++) r += Math.log(i); return r;
    }

    // ── Inicialização ─────────────────────────────────────────────────────────

    /**
     * Inicializa o controlador: constrói a grade de botões, carrega o histórico
     * e configura o listener de pré-visualização do resultado.
     */
    @FXML
    public void initialize() {
        construirGrid();
        carregarHistorico();

        taExpressao.textProperty().addListener((obs, ant, nov) -> avaliarPreview(nov));

        // Enter calcula; Shift+Enter insere quebra de linha
        taExpressao.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) {
                calcular();
                e.consume();
            }
        });
    }

    /**
     * Define o contêiner principal no qual o painel da calculadora está inserido.
     * Necessário para reatachar o painel após destacá-lo em janela separada.
     *
     * @param container painel raiz da tela principal
     */
    public void setContainer(BorderPane container) {
        this.container = container;
    }

    // ── Construção da grade de botões ─────────────────────────────────────────

    private void construirGrid() {
        for (int i = 0; i < 8; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setFillWidth(true);
            cc.setPercentWidth(100.0 / 8.0);
            gridBotoes.getColumnConstraints().add(cc);
        }
        for (int i = 0; i < 7; i++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setFillHeight(true);
            rc.setMinHeight(42);
            gridBotoes.getRowConstraints().add(rc);
        }

        // Linha 0 — Memória + controles
        add("MC",    0, 0, "calc-btn-mem",   () -> { memoria = 0; temMemoria = false; atualizarMem(); },
            "MC — Limpar memória");
        add("MR",    1, 0, "calc-btn-mem",   () -> ins(fmtNum(memoria)),
            "MR — Recuperar valor armazenado na memória");
        add("M+",    2, 0, "calc-btn-mem",   () -> { memoria += resultado(); temMemoria = true; atualizarMem(); },
            "M+ — Somar o resultado atual à memória");
        add("M−",    3, 0, "calc-btn-mem",   () -> { memoria -= resultado(); temMemoria = true; atualizarMem(); },
            "M− — Subtrair o resultado atual da memória");
        add("C",     4, 0, "calc-btn-ctrl",  () -> { taExpressao.clear(); lblResultado.setText(""); },
            "C — Limpar toda a expressão");
        add("CE",    5, 0, "calc-btn-ctrl",  () -> lblResultado.setText(""),
            "CE — Limpar o resultado exibido");
        add("⌫",    6, 0, "calc-btn-ctrl",  this::apagar,
            "⌫ — Apagar o último caractere");
        add("( )",   7, 0, "calc-btn-ctrl",  this::parenteseInt,
            "( ) — Inserir parêntese: abre '(' se houver mais '(' do que ')', fecha ')' caso contrário");

        // Linha 1 — Trig + 7 8 9 ÷
        add("sin",   0, 1, "calc-btn-func",  () -> ins("sin("),
            "sin(x) — Seno de x\nEx: sin(30) = 0.5  [modo DEG]\n    sin(π/6) = 0.5  [modo RAD]");
        add("cos",   1, 1, "calc-btn-func",  () -> ins("cos("),
            "cos(x) — Cosseno de x\nEx: cos(60) = 0.5  [modo DEG]");
        add("tan",   2, 1, "calc-btn-func",  () -> ins("tan("),
            "tan(x) — Tangente de x\nEx: tan(45) = 1  [modo DEG]");
        add("π",     3, 1, "calc-btn-const", () -> ins("π"),
            "π ≈ 3.14159265… (pi)");
        add("7",     4, 1, "calc-btn-num",   () -> ins("7"));
        add("8",     5, 1, "calc-btn-num",   () -> ins("8"));
        add("9",     6, 1, "calc-btn-num",   () -> ins("9"));
        add("÷",     7, 1, "calc-btn-op",    () -> ins("÷"));

        // Linha 2 — Trig inversa + 4 5 6 ×
        add("asin",  0, 2, "calc-btn-func",  () -> ins("asin("),
            "asin(x) — Arco seno: retorna o ângulo cujo seno é x\nEx: asin(0.5) = 30  [modo DEG]");
        add("acos",  1, 2, "calc-btn-func",  () -> ins("acos("),
            "acos(x) — Arco cosseno: retorna o ângulo cujo cosseno é x\nEx: acos(0.5) = 60  [modo DEG]");
        add("atan",  2, 2, "calc-btn-func",  () -> ins("atan("),
            "atan(x) — Arco tangente: retorna o ângulo cuja tangente é x\nEx: atan(1) = 45  [modo DEG]");
        add("e",     3, 2, "calc-btn-const", () -> ins("e"),
            "e ≈ 2.71828… (número de Euler)");
        add("4",     4, 2, "calc-btn-num",   () -> ins("4"));
        add("5",     5, 2, "calc-btn-num",   () -> ins("5"));
        add("6",     6, 2, "calc-btn-num",   () -> ins("6"));
        add("×",     7, 2, "calc-btn-op",    () -> ins("×"));

        // Linha 3 — Log + hiperbólicas + 1 2 3 −
        add("ln",    0, 3, "calc-btn-func",  () -> ins("log("),
            "log(x) — Logaritmo natural (base e) de x\nEx: log(e) = 1\n    log(1) = 0");
        add("log",   1, 3, "calc-btn-func",  () -> ins("log10("),
            "log10(x) — Logaritmo na base 10 de x\nEx: log10(100) = 2\n    log10(1000) = 3");
        add("log₂",  2, 3, "calc-btn-func",  () -> ins("log2("),
            "log2(x) — Logaritmo na base 2 de x\nEx: log2(8) = 3\n    log2(1024) = 10");
        add("sinh",  3, 3, "calc-btn-func",  () -> ins("sinh("),
            "sinh(x) — Seno hiperbólico de x\nEx: sinh(0) = 0\n    sinh(1) ≈ 1.1752");
        add("1",     4, 3, "calc-btn-num",   () -> ins("1"));
        add("2",     5, 3, "calc-btn-num",   () -> ins("2"));
        add("3",     6, 3, "calc-btn-num",   () -> ins("3"));
        add("−",     7, 3, "calc-btn-op",    () -> ins("-"));

        // Linha 4 — Potências/raízes + 0 . ± +
        add("√",     0, 4, "calc-btn-func",  () -> ins("sqrt("),
            "sqrt(x) — Raiz quadrada de x\nEx: sqrt(9) = 3\n    sqrt(2) ≈ 1.4142");
        add("∛",     1, 4, "calc-btn-func",  () -> ins("cbrt("),
            "cbrt(x) — Raiz cúbica de x\nEx: cbrt(27) = 3\n    cbrt(8) = 2");
        add("x²",    2, 4, "calc-btn-func",  () -> ins("^2"),
            "x² — Elevar ao quadrado (insere ^2)\nEx: 5^2 = 25");
        add("xⁿ",   3, 4, "calc-btn-func",  () -> ins("^("),
            "xⁿ — Elevar à potência n (insere ^()\nEx: 2^(10) = 1024");
        add("0",     4, 4, "calc-btn-num",   () -> ins("0"));
        add(".",     5, 4, "calc-btn-num",   () -> ins("."));
        add("±",     6, 4, "calc-btn-ctrl",  this::negar,
            "± — Inverter o sinal da expressão atual");
        add("+",     7, 4, "calc-btn-op",    () -> ins("+"));

        // Linha 5 — Combinatória + floor/ceil/% + = (abrange linhas 5-6)
        add("n!",    0, 5, "calc-btn-func",  () -> ins("fact("),
            "fact(n) — Fatorial de n  (n deve ser inteiro ≥ 0)\nEx: fact(5) = 120\n    fact(0) = 1");
        add("nCr",   1, 5, "calc-btn-func",  () -> ins("ncr("),
            "ncr(n, k) — Combinações: nº de formas de escolher k itens de n (sem ordem)\nEx: ncr(5, 2) = 10\nUso: ncr(n, k) — separe com vírgula");
        add("nPr",   2, 5, "calc-btn-func",  () -> ins("npr("),
            "npr(n, k) — Permutações: nº de formas de arranjar k itens de n (com ordem)\nEx: npr(5, 2) = 20\nUso: npr(n, k) — separe com vírgula");
        add("|x|",   3, 5, "calc-btn-func",  () -> ins("abs("),
            "abs(x) — Valor absoluto de x\nEx: abs(-7) = 7\n    abs(3) = 3");
        add("floor", 4, 5, "calc-btn-func",  () -> ins("floor("),
            "floor(x) — Maior inteiro ≤ x (arredonda para baixo)\nEx: floor(3.9) = 3\n    floor(-1.2) = -2");
        add("ceil",  5, 5, "calc-btn-func",  () -> ins("ceil("),
            "ceil(x) — Menor inteiro ≥ x (arredonda para cima)\nEx: ceil(3.1) = 4\n    ceil(-1.8) = -1");
        add("%",     6, 5, "calc-btn-op",    () -> ins("%"),
            "a % b — Resto da divisão de a por b\nEx: 10 % 3 = 1\n    17 % 5 = 2");
        // = abrange linhas 5 e 6
        Button btnIgual = mkBtn("=", "calc-btn-igual", this::calcular);
        gridBotoes.add(btnIgual, 7, 5, 1, 2);

        // Linha 6 — Hiperbólicas extra + DEG/RAD + ( ) ,
        add("cosh",  0, 6, "calc-btn-func",  () -> ins("cosh("),
            "cosh(x) — Cosseno hiperbólico de x\nEx: cosh(0) = 1\n    cosh(1) ≈ 1.5431");
        add("tanh",  1, 6, "calc-btn-func",  () -> ins("tanh("),
            "tanh(x) — Tangente hiperbólica de x\nEx: tanh(0) = 0\n    tanh(1) ≈ 0.7616");
        add("round", 2, 6, "calc-btn-func",  () -> ins("round("),
            "round(x) — Arredondar para o inteiro mais próximo\nEx: round(3.5) = 4\n    round(2.4) = 2");

        ToggleButton tglDeg = new ToggleButton("DEG");
        tglDeg.setSelected(true);
        tglDeg.getStyleClass().addAll("calc-btn", "calc-btn-modo");
        tglDeg.setMaxWidth(Double.MAX_VALUE);
        tglDeg.setMaxHeight(Double.MAX_VALUE);
        tglDeg.setTooltip(new Tooltip("DEG/RAD — Alternar entre graus e radianos para funções trigonométricas\nDEG: sin(90) = 1\nRAD: sin(π/2) = 1"));
        tglDeg.setOnAction(e -> {
            isDegMode = tglDeg.isSelected();
            tglDeg.setText(isDegMode ? "DEG" : "RAD");
            lblModo.setText(isDegMode ? "DEG" : "RAD");
            avaliarPreview(taExpressao.getText());
        });
        gridBotoes.add(tglDeg, 3, 6);

        add("(",     4, 6, "calc-btn-ctrl",  () -> ins("("));
        add(")",     5, 6, "calc-btn-ctrl",  () -> ins(")"));
        add(",",     6, 6, "calc-btn-ctrl",  () -> ins(","));
        // col 7, linha 6: coberta pelo = que abrange duas linhas
    }

    private void add(String txt, int col, int row, String estilo, Runnable acao) {
        gridBotoes.add(mkBtn(txt, estilo, acao), col, row);
    }

    private void add(String txt, int col, int row, String estilo, Runnable acao, String dica) {
        Button b = mkBtn(txt, estilo, acao);
        Tooltip t = new Tooltip(dica);
        t.setWrapText(true);
        t.setMaxWidth(230);
        b.setTooltip(t);
        gridBotoes.add(b, col, row);
    }

    private Button mkBtn(String txt, String estilo, Runnable acao) {
        Button b = new Button(txt);
        b.getStyleClass().addAll("calc-btn", estilo);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setMaxHeight(Double.MAX_VALUE);
        b.setOnAction(e -> { acao.run(); taExpressao.requestFocus(); });
        return b;
    }

    // ── Edição da expressão ───────────────────────────────────────────────────

    private void ins(String texto) {
        if (!taExpressao.getSelectedText().isEmpty()) {
            taExpressao.replaceSelection(texto);
        } else {
            taExpressao.insertText(taExpressao.getCaretPosition(), texto);
        }
    }

    private void apagar() {
        if (!taExpressao.getSelectedText().isEmpty()) {
            taExpressao.replaceSelection("");
            return;
        }
        int pos = taExpressao.getCaretPosition();
        if (pos > 0) taExpressao.deleteText(pos - 1, pos);
    }

    private void parenteseInt() {
        String t = taExpressao.getText();
        long ab = t.chars().filter(c -> c == '(').count();
        long fe = t.chars().filter(c -> c == ')').count();
        ins(ab > fe ? ")" : "(");
    }

    private void negar() {
        String t = taExpressao.getText();
        if (t.startsWith("-")) {
            taExpressao.setText(t.substring(1));
        } else {
            taExpressao.setText("-" + t);
        }
        taExpressao.positionCaret(taExpressao.getText().length());
    }

    // ── Cálculo ───────────────────────────────────────────────────────────────

    private void calcular() {
        String expr = taExpressao.getText().trim();
        if (expr.isEmpty()) return;
        try {
            double r = avaliar(expr);
            String fR = fmtNum(r);
            lblResultado.setText("= " + fR);
            String linha = expr.replace("\n", " ") + " = " + fR;
            historico.add(0, linha);
            listaHistorico.getChildren().add(0, mkItemHist(linha));
            Platform.runLater(() -> scrollHistorico.setVvalue(0));
            salvarHistorico();
        } catch (Exception ex) {
            String msg = ex.getMessage();
            lblResultado.setText("Erro: " + (msg != null && msg.length() > 55 ? msg.substring(0, 55) + "…" : msg));
        }
    }

    private void avaliarPreview(String texto) {
        if (texto == null || texto.isBlank()) { lblResultado.setText(""); return; }
        try {
            double r = avaliar(texto.trim());
            lblResultado.setText("= " + fmtNum(r));
        } catch (Exception ignored) {
            lblResultado.setText("");
        }
    }

    private double avaliar(String expressao) throws Exception {
        String expr = expressao
            .replace("π", "pi")
            .replace("÷", "/")
            .replace("×", "*")
            .replace("−", "-");

        if (isDegMode) {
            // Mais longo primeiro para evitar substituição dupla
            expr = expr
                .replace("asin(", "asind(")
                .replace("acos(", "acosd(")
                .replace("atan(", "atand(")
                .replace("sin(",  "sind(")
                .replace("cos(",  "cosd(")
                .replace("tan(",  "tand(");
        }

        Expression e = new ExpressionBuilder(expr)
            .functions(fFact, fNcr, fNpr,
                       fSind, fCosd, fTand,
                       fAsind, fAcosd, fAtand)
            .operator(opMod)
            .build();

        double resultado = e.evaluate();
        if (Double.isNaN(resultado))      throw new ArithmeticException("Resultado indefinido (NaN)");
        if (Double.isInfinite(resultado)) throw new ArithmeticException("Resultado infinito");
        return resultado;
    }

    private double resultado() {
        try { return avaliar(taExpressao.getText().trim()); } catch (Exception e) { return 0; }
    }

    // ── Memória ───────────────────────────────────────────────────────────────

    private void atualizarMem() {
        if (temMemoria) {
            lblMemoria.setText("M: " + fmtNum(memoria));
            lblMemoria.setVisible(true);
            lblMemoria.setManaged(true);
        } else {
            lblMemoria.setVisible(false);
            lblMemoria.setManaged(false);
        }
    }

    // ── Histórico ─────────────────────────────────────────────────────────────

    @FXML
    private void handleLimparHistorico() {
        historico.clear();
        listaHistorico.getChildren().clear();
        salvarHistorico();
    }

    private VBox mkItemHist(String linha) {
        int idx = linha.lastIndexOf(" = ");
        String expr = idx >= 0 ? linha.substring(0, idx) : linha;
        String res  = idx >= 0 ? linha.substring(idx + 3) : "";

        VBox item = new VBox(3);
        item.getStyleClass().add("calc-hist-item");
        item.setPadding(new Insets(8, 12, 8, 12));
        item.setStyle("-fx-cursor: hand;");

        Label lExpr = new Label(expr);
        lExpr.getStyleClass().add("calc-hist-expr");
        lExpr.setWrapText(true);

        Label lRes = new Label("= " + res);
        lRes.getStyleClass().add("calc-hist-result");

        item.getChildren().addAll(lExpr, lRes);
        item.setOnMouseClicked(e -> {
            if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                taExpressao.setText(expr);
                taExpressao.positionCaret(expr.length());
            }
        });

        MenuItem miCopiarRes  = new MenuItem("Copiar resultado");
        MenuItem miCopiarExpr = new MenuItem("Copiar expressão");
        miCopiarRes.setOnAction(e -> copiarTexto(res));
        miCopiarExpr.setOnAction(e -> copiarTexto(expr));
        ContextMenu ctx = new ContextMenu(miCopiarRes, miCopiarExpr);
        item.setOnContextMenuRequested(e -> ctx.show(item, e.getScreenX(), e.getScreenY()));

        return item;
    }

    private void copiarTexto(String texto) {
        ClipboardContent cc = new ClipboardContent();
        cc.putString(texto);
        Clipboard.getSystemClipboard().setContent(cc);
    }

    private void carregarHistorico() {
        new Thread(() -> {
            try {
                Path p = pathHist();
                if (!Files.exists(p)) return;
                List<String> linhas = Files.readAllLines(p, StandardCharsets.UTF_8);
                Platform.runLater(() -> {
                    historico.clear();
                    listaHistorico.getChildren().clear();
                    for (String l : linhas) {
                        if (!l.isBlank()) {
                            historico.add(l);
                            listaHistorico.getChildren().add(mkItemHist(l));
                        }
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    private void salvarHistorico() {
        List<String> copia = new ArrayList<>(historico);
        new Thread(() -> {
            try {
                Files.write(pathHist(), copia, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (Exception ignored) {}
        }).start();
    }

    private Path pathHist() {
        int uid = SessionManager.getInstance().getUsuarioLogado().getId();
        return Path.of(DatabaseManager.getDirApp(), "calc_hist_" + uid + ".txt");
    }

    // ── Formatação numérica ───────────────────────────────────────────────────

    private String fmtNum(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v) && Math.abs(v) < 1e15) {
            return String.valueOf((long) v);
        }
        String s = String.format("%.12g", v).replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    // ── Destacar / Reatachar ──────────────────────────────────────────────────

    @FXML
    private void handleDetachar() {
        if (janelaDestacada != null) reatachar();
        else destacar();
    }

    private void destacar() {
        Scene cenaPrincipal = raiz.getScene();

        VBox placeholder = new VBox(12);
        placeholder.setAlignment(Pos.CENTER);
        Label ico = new Label("🧮");
        ico.setStyle("-fx-font-size: 40px;");
        Label msg = new Label("Calculadora aberta em\njanela separada");
        msg.setStyle("-fx-text-fill: #9090A8; -fx-font-size: 13px;");
        msg.setTextAlignment(TextAlignment.CENTER);
        placeholder.getChildren().addAll(ico, msg);

        container.setCenter(placeholder);

        Scene cenaSep = new Scene(raiz, 940, 640);
        cenaSep.getStylesheets().addAll(cenaPrincipal.getStylesheets());

        janelaDestacada = new Stage();
        janelaDestacada.setTitle("🧮  Calculadora — StudyApp");
        janelaDestacada.setAlwaysOnTop(true);
        janelaDestacada.setScene(cenaSep);
        janelaDestacada.setOnCloseRequest(e -> { e.consume(); reatachar(); });

        btnDetachar.setText("↙ Reintegrar");
        janelaDestacada.show();
    }

    private void reatachar() {
        if (janelaDestacada == null) return;
        janelaDestacada.getScene().setRoot(new Region());
        container.setCenter(raiz);
        btnDetachar.setText("↗ Destacar");
        janelaDestacada.close();
        janelaDestacada = null;
    }

    /**
     * Fecha a janela destacada (se estiver aberta).
     * Deve ser chamado ao encerrar o aplicativo.
     */
    public void pararRecursos() {
        if (janelaDestacada != null) { janelaDestacada.close(); janelaDestacada = null; }
    }
}
