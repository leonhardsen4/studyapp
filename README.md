# StudyApp

Aplicação desktop de produtividade pessoal voltada para estudantes, desenvolvida com JavaFX 21. Reúne em uma única janela as ferramentas mais usadas no dia a dia de quem estuda: anotações, tarefas, agenda, calculadora e mais.

---

## Funcionalidades

### Sistema de Arquivos
- Cadernos aninhados (hierarquia ilimitada), notas Markdown e PDFs
- Editor Markdown com barra de ferramentas (negrito, itálico, títulos, código, links, imagens, tabelas, LaTeX via KaTeX)
- Preview renderizado em WebView; toggle editar / visualizar
- Visualizador de PDF com navegação por páginas e zoom
- Auto-save com debounce de 1,5 s
- Impressão de notas e PDFs

### Gerenciador de Tarefas
- Prioridades (BAIXA / MÉDIA / ALTA / URGENTE) e status (PENDENTE / EM ANDAMENTO / CONCLUÍDA)
- Até 3 etiquetas por tarefa; criação e renomeação de etiquetas no painel lateral
- Coloração de linhas por prazo (vencida, hoje, em breve)
- Filtros em tempo real: texto, status, etiqueta
- Ordenação e ocultação de colunas via botão ☰ no cabeçalho da tabela
- Alertas de prazo ao abrir a aplicação; badge de notificação atualizado em tempo real

### Agenda
- Grade mensal interativa com chips de eventos por dia
- Eventos com hora de início/fim opcionais (dia inteiro ou com horário)
- Notificações dos eventos do dia no badge do sino

### Temporizador Pomodoro
- Timer com fases configuráveis: Foco (25 min), Pausa Curta (5 min), Pausa Longa (15 min)
- Ciclo automático de 4 sessões; indicadores visuais 🍅; alarme sonoro ao fim de cada fase
- Sistema de **metas de estudo**: Disciplina → Assunto com nível de dificuldade, sessões mínimas e data limite opcional
- Contagem de sessões por assunto com botões `[−]` e `[+]` para ajuste manual
- Estatísticas diárias (sessões e tempo de foco acumulados no dia)
- Configurações de duração persistidas entre sessões
- Janela destacável (sempre no topo), com botão "↙ Reintegrar" na própria janela

### Bloco de Notas Rápido
- Texto simples persistido por usuário
- Botões Salvar .txt, Limpar e Destacar (abre em janela flutuante)

### Calculadora
- Operações aritméticas, trigonométricas (DEG/RAD), hiperbólicas, logarítmos, raízes, potências, fatorial, combinatória e arredondamento
- Memória: MC, MR, M+, M−
- Histórico persistente com menu de contexto (copiar resultado / expressão)
- Tooltips em todos os botões de função com sintaxe e exemplos
- Janela destacável (sempre no topo)

### Autenticação e Perfil
- Cadastro com senha hasheada (BCrypt cost 12)
- Recuperação de senha por e-mail (SMTP configurável pela própria interface)
- Editar e-mail, alterar senha e excluir conta no perfil

### Temas
- Modo Claro e Modo Escuro, selecionáveis pelo usuário e persistidos por conta
- Todos os diálogos, alertas, menus de contexto e popups (DatePicker, ComboBox) respeitam o tema ativo

---

## Stack

| Tecnologia       | Versão   | Uso                                  |
|------------------|----------|--------------------------------------|
| Java             | 17       | Linguagem principal                  |
| JavaFX           | 21.0.6   | Interface gráfica                    |
| SQLite (JDBC)    | 3.46.1.3 | Banco de dados local                 |
| BCrypt           | 0.10.2   | Hash de senhas                       |
| Flexmark         | 0.64.8   | Renderização de Markdown             |
| Apache PDFBox    | 3.0.3    | Visualização de PDFs                 |
| exp4j            | 0.4.8    | Avaliador de expressões matemáticas  |
| Jakarta Mail     | 2.0.1    | Envio de e-mail via SMTP             |
| JUnit Jupiter    | 5.12.1   | Testes unitários (99 testes)         |

---

## Requisitos

- **Java 17** ou superior com suporte a JavaFX (`JAVA_HOME` configurado)
- **Maven 3.8+** (ou use o wrapper `./mvnw` incluso no projeto)

---

## Como executar

```bash
# Clonar o repositório
git clone https://github.com/leonhardsen4/studyapp.git
cd studyapp

# Compilar e executar
./mvnw javafx:run
```

O banco de dados e os arquivos de dados são criados automaticamente em `~/.studyapp/` no primeiro uso.

### Configurar envio de e-mail (opcional)

Para usar a recuperação de senha, acesse **Configurações → Configurar E-mail SMTP** e informe os dados do seu servidor SMTP (ex.: Gmail com Senha de App).

---

## Estrutura do projeto

```
src/
├── main/
│   ├── java/com/leonhardsen/studyapp/
│   │   ├── controller/     # Controllers JavaFX de cada módulo
│   │   ├── database/       # DAOs e DatabaseManager (SQLite)
│   │   ├── model/          # Classes de domínio (Usuario, Tarefa, Evento…)
│   │   ├── service/        # Lógica de negócio e validações
│   │   ├── util/           # ThemeManager, SessionManager, EmailService…
│   │   └── view/           # Células customizadas de TableView/TreeView
│   └── resources/com/leonhardsen/studyapp/
│       ├── css/            # dark-theme.css, light-theme.css
│       └── *.fxml          # Layouts das telas
└── test/                   # Testes JUnit 5
```

---

## Banco de dados

Todas as tabelas são criadas automaticamente na primeira execução:

```
usuario · item_arvore · nota · pdf_documento
etiqueta · tarefa · tarefa_etiqueta
evento
disciplina · assunto · sessao_pomodoro
```

Os dados ficam em `~/.studyapp/studyapp.db` e são isolados por usuário logado.

---

## Licença

Este projeto está licenciado sob a [MIT License](LICENSE).
