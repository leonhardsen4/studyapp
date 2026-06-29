#!/bin/bash
# ──────────────────────────────────────────────────────────────────
# Empacota o StudyApp como app-image Linux autossuficiente (JRE
# + JavaFX + todas as dependências embutidas) e cria atalho no
# Desktop. Requer: JDK Zulu FX (com JavaFX embutido) e Maven wrapper.
# ──────────────────────────────────────────────────────────────────
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_NAME="StudyApp"
APP_VERSION="1.0"
MAIN_CLASS="com.leonhardsen.studyapp.Launcher"
APP_JAR="${APP_NAME}-1.0-SNAPSHOT.jar"
DIST_DIR="$HOME/${APP_NAME}-dist"

cd "$PROJECT_DIR"

# ── 1. Build ──────────────────────────────────────────────────────
echo "[1/5] Compilando projeto..."
./mvnw clean package -DskipTests -q

# ── 2. Dependências da aplicação (sem JavaFX — JDK já inclui) ─────
echo "[2/5] Coletando dependências..."
./mvnw dependency:copy-dependencies \
    -DoutputDirectory=target/app-libs \
    -DincludeScope=runtime \
    -q

# Copia o JAR principal para a mesma pasta
cp "target/${APP_JAR}" target/app-libs/

# Remove JARs do JavaFX: o JDK Zulu FX já traz javafx.* como módulos
# do JRE — incluí-los no classpath causaria conflito de pacotes.
rm -f target/app-libs/javafx-*.jar

# ── 3. Ícone ─────────────────────────────────────────────────────
echo "[3/5] Gerando ícone..."
ICON_PATH="target/studyapp-icon.png"
python3 - "$ICON_PATH" <<'PYEOF'
import sys, struct, zlib, base64

# Gera um PNG 64x64 com fundo azul marinho e letras "SA" em branco
def write_png(path, size=64):
    def mk_chunk(tag, data):
        b = tag + data
        return struct.pack('>I', len(data)) + b + struct.pack('>I', zlib.crc32(b) & 0xFFFFFFFF)

    # Pixels RGBA
    bg = (27, 58, 107, 255)   # azul marinho
    fg = (255, 255, 255, 255) # branco

    pixels = [[list(bg) for _ in range(size)] for _ in range(size)]

    # "S" e "A" simplificados como blocos de pixels (bitmap 5x7 cada)
    S = [
        "01110",
        "10001",
        "10000",
        "01110",
        "00001",
        "10001",
        "01110",
    ]
    A = [
        "01110",
        "10001",
        "10001",
        "11111",
        "10001",
        "10001",
        "10001",
    ]

    scale = 4
    gap = 4
    total_w = (5 + 1 + 5) * scale
    start_x = (size - total_w) // 2
    start_y = (size - 7 * scale) // 2

    for row, bits in enumerate(S):
        for col, bit in enumerate(bits):
            if bit == '1':
                for dy in range(scale):
                    for dx in range(scale):
                        y = start_y + row * scale + dy
                        x = start_x + col * scale + dx
                        if 0 <= y < size and 0 <= x < size:
                            pixels[y][x] = list(fg)

    offset_x = start_x + (5 + 1) * scale
    for row, bits in enumerate(A):
        for col, bit in enumerate(bits):
            if bit == '1':
                for dy in range(scale):
                    for dx in range(scale):
                        y = start_y + row * scale + dy
                        x = offset_x + col * scale + dx
                        if 0 <= y < size and 0 <= x < size:
                            pixels[y][x] = list(fg)

    raw = b''
    for row in pixels:
        raw += b'\x00'  # filter type
        for px in row:
            raw += bytes(px)

    compressed = zlib.compress(raw, 9)

    ihdr = struct.pack('>IIBBBBB', size, size, 8, 2, 0, 0, 0)
    # RGBA → bit depth 8, color type 2 = RGB (simplify to RGB)
    # Actually use color type 6 = RGBA
    ihdr = struct.pack('>II', size, size) + bytes([8, 6, 0, 0, 0])

    png  = b'\x89PNG\r\n\x1a\n'
    png += mk_chunk(b'IHDR', ihdr)
    png += mk_chunk(b'IDAT', compressed)
    png += mk_chunk(b'IEND', b'')

    with open(path, 'wb') as f:
        f.write(png)

write_png(sys.argv[1])
print(f"  Ícone gravado em {sys.argv[1]}")
PYEOF

# ── 4. jpackage → app-image ───────────────────────────────────────
# O JDK Zulu FX já contém os módulos javafx.* no runtime:
# não é necessário --module-path externo. O --add-modules garante
# que esses módulos sejam incluídos na JRE bundlada pelo jpackage.
echo "[4/5] Empacotando com jpackage..."
rm -rf target/dist

jpackage \
    --type app-image \
    --name "$APP_NAME" \
    --app-version "$APP_VERSION" \
    --input target/app-libs \
    --main-jar "$APP_JAR" \
    --main-class "$MAIN_CLASS" \
    --add-modules javafx.controls,javafx.fxml,javafx.web,javafx.media,javafx.graphics,javafx.base,java.sql,java.desktop,java.security.sasl,java.logging,java.naming,jdk.crypto.ec \
    --java-options "-Dfile.encoding=UTF-8" \
    --java-options "-Djava.locale.providers=COMPAT,SPI" \
    --java-options "--enable-native-access=javafx.graphics" \
    --dest target/dist \
    --icon "$ICON_PATH" \
    --verbose 2>&1 | grep -E "^\[|^Error|^Warning|Generating|Creating|Adding" || true

# Verifica se o app-image foi criado
if [ ! -f "target/dist/${APP_NAME}/bin/${APP_NAME}" ]; then
    echo "ERRO: jpackage não criou o executável esperado."
    echo "Verifique target/dist/ manualmente."
    exit 1
fi

# ── 5. Instala e cria atalho no Desktop ──────────────────────────
echo "[5/5] Instalando e criando atalho..."

rm -rf "$DIST_DIR"
cp -r "target/dist/${APP_NAME}" "$DIST_DIR"

# Copia o ícone para o diretório de instalação
cp "$ICON_PATH" "$DIST_DIR/"

DESKTOP_DIR="$(xdg-user-dir DESKTOP 2>/dev/null || echo "$HOME/Desktop")"
DESKTOP_FILE="${DESKTOP_DIR}/${APP_NAME}.desktop"
LAUNCHER="$DIST_DIR/bin/${APP_NAME}"

# Conteúdo do .desktop (gerado uma vez, usado em dois locais)
DESKTOP_CONTENT="[Desktop Entry]
Version=1.0
Type=Application
Name=StudyApp
Comment=Aplicativo pessoal de estudos
Exec=${LAUNCHER}
Icon=${DIST_DIR}/studyapp-icon.png
Terminal=false
Categories=Education;Office;
StartupWMClass=StudyApp
StartupNotify=true"

# ── Instala no banco XDG (obrigatório para GNOME 3.36+ confiar no arquivo) ──
XDG_APPS="$HOME/.local/share/applications"
mkdir -p "$XDG_APPS"
printf '%s\n' "$DESKTOP_CONTENT" > "${XDG_APPS}/${APP_NAME}.desktop"
chmod +x "${XDG_APPS}/${APP_NAME}.desktop"
update-desktop-database "$XDG_APPS" 2>/dev/null || true

# ── Cria atalho no Desktop (ativa confiança via metadado e permissão) ──
printf '%s\n' "$DESKTOP_CONTENT" > "$DESKTOP_FILE"
chmod +x "$DESKTOP_FILE"
chmod +x "$LAUNCHER"

# GNOME ≥ 3.28 exige metadado "trusted" no arquivo do Desktop
gio set "$DESKTOP_FILE" metadata::trusted true 2>/dev/null || true

echo ""
echo "╔═══════════════════════════════════════════════════╗"
echo "║  StudyApp empacotado com sucesso!                 ║"
echo "║                                                   ║"
printf "║  Instalado em: %-35s║\n" "$DIST_DIR/"
printf "║  Atalho:       %-35s║\n" "$DESKTOP_FILE"
echo "║                                                   ║"
echo "║  Para abrir: clique duas vezes no atalho do       ║"
echo "║  Desktop, ou execute diretamente:                 ║"
printf "║    %-47s║\n" "$LAUNCHER"
echo "╚═══════════════════════════════════════════════════╝"
