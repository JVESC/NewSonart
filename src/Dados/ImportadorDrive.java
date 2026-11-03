package Dados;

import Drives.DriveBiblioteca;
import Drives.DriveBiblioteca.ArquivoDrive;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Lê uma pasta do Drive e cadastra no banco cada música ainda não cadastrada.
 *
 * Os arquivos precisam se chamar "Artista - Título.mp3" (um número na frente,
 * como "01 - Artista - Título.mp3", também é aceito).
 * Os dados (álbum, capa, duração, ano, letra) vêm da Deezer e da LRCLIB.
 */
public class ImportadorDrive {

    public static class Resultado {
        public int importadas = 0;
        public int jaExistiam = 0;
        public final List<String> pendentes = new ArrayList<>();       // precisam de cadastro manual
        public final List<String> naoSuportados = new ArrayList<>();   // formato que o player não toca
        public final List<String> erros = new ArrayList<>();           // falha ao gravar
    }
    
    // true  = "Título - Artista.mp3"
    // false = "Artista - Título.mp3"
    private static final boolean TITULO_PRIMEIRO = true;
    
    private static final String[] INDESEJADOS = {
        "radio edit", "remix", "live", "karaoke", "tribute", "instrumental", "acoustic", "cover"
    };
    private static final int PONTUACAO_MINIMA = 6;
    private static final long PAUSA_ENTRE_MUSICAS_MS = 800;   // não estourar o limite da Deezer

    public static Resultado importar(Connection c, String pastaId, Consumer<String> log) throws Exception {
        Resultado r = new Resultado();

        log.accept("Lendo a pasta do Drive...");
        DriveBiblioteca.Varredura v = DriveBiblioteca.varrer(pastaId);
        int total = v.suportados.size();
        log.accept(total + " arquivo(s) de áudio encontrado(s).");

        for (ArquivoDrive a : v.naoSuportados) {
            r.naoSuportados.add(a.caminho + a.nome);
        }

        Set<String> existentes = carregarExistentes(c);
        int n = 0;

        for (ArquivoDrive a : v.suportados) {
            n++;
            String prefixo = "[" + n + "/" + total + "] ";
            String rotulo = a.caminho + a.nome;

            if (existentes.contains(a.id)) {
                r.jaExistiam++;
                log.accept(prefixo + "já cadastrada: " + a.nome);
                continue;
            }

            try {
                String[] par = interpretarNome(a.nome);
                String artista = par[0];
                String titulo = par[1];

                if (artista.isEmpty()) {
                    r.pendentes.add(rotulo + "  [id " + a.id + "]  -> nome fora do padrão 'Artista - Título'");
                    log.accept(prefixo + "PENDENTE (nome fora do padrão): " + a.nome);
                    continue;
                }

                List<MetadadosMusica> candidatos = FonteMusica.buscar(artista, titulo);
                MetadadosMusica m = escolher(candidatos, artista, titulo);

                if (m == null) {
                    r.pendentes.add(rotulo + "  [id " + a.id + "]  -> sem resultado confiável na Deezer");
                    log.accept(prefixo + "PENDENTE (não encontrei com segurança): " + a.nome);
                    pausa();
                    continue;
                }

                FonteMusica.completar(m);

                if (m.anoLancamento <= 0) {
                    r.pendentes.add(rotulo + "  [id " + a.id + "]  -> álbum sem ano de lançamento");
                    log.accept(prefixo + "PENDENTE (sem ano de lançamento): " + a.nome);
                    pausa();
                    continue;
                }

                MusicaDAO.inserir(c, m, a.id);
                existentes.add(a.id);
                r.importadas++;
                log.accept(prefixo + "OK: " + m.artista + " - " + m.titulo + "  [" + m.album + "]"
                        + (m.letraSincronizada != null ? "  (letra sincronizada)" : ""));
                pausa();

            } catch (SQLException e) {
                r.erros.add(rotulo + " -> " + e.getMessage());
                log.accept(prefixo + "ERRO ao gravar: " + a.nome + " (" + e.getMessage() + ")");
            } catch (Exception e) {
                r.erros.add(rotulo + " -> " + e.getMessage());
                log.accept(prefixo + "ERRO: " + a.nome + " (" + e.getMessage() + ")");
            }
        }

        log.accept("");
        log.accept("Concluído: " + r.importadas + " nova(s), " + r.jaExistiam + " já existia(m), "
                + r.pendentes.size() + " pendente(s), " + r.naoSuportados.size()
                + " em formato não suportado, " + r.erros.size() + " erro(s).");
        return r;
    }

    // ---------------------------------------------------------------

    private static Set<String> carregarExistentes(Connection c) throws SQLException {
        Set<String> ids = new HashSet<>();
        try (PreparedStatement ps = c.prepareStatement("SELECT caminho_arquivo FROM musica");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) ids.add(rs.getString(1));
        }
        return ids;
    }

        /**
     * Aceita:
     *   "Get Lucky - Daft Punk.mp3"      (com espaços em volta do hífen)
     *   "GetLucky-DaftPunk.mp3"          (sem espaços, palavras coladas)
     *   "01 - Get Lucky - Daft Punk.mp3" (número da faixa na frente)
     */
    private static String[] interpretarNome(String arquivo) {
        String n = arquivo;
        int ponto = n.lastIndexOf('.');
        if (ponto > 0) n = n.substring(0, ponto);
        n = n.replace('_', ' ').replace(" – ", " - ").replace(" — ", " - ").trim();
        n = n.replaceFirst("^\\d{1,3}\\s*[.)\\-]\\s+", "");      // tira o número da faixa

        // acha o separador: " - " se existir; senão o último "-" sem espaços
        int sep = n.lastIndexOf(" - ");
        int tam = 3;
        if (sep < 0) {
            sep = n.lastIndexOf('-');
            tam = 1;
        }
        if (sep <= 0 || sep + tam >= n.length()) return new String[] { "", n };

        String esq = separarPalavras(n.substring(0, sep).trim());
        String dir = separarPalavras(n.substring(sep + tam).trim());

        return TITULO_PRIMEIRO ? new String[] { dir, esq } : new String[] { esq, dir };
    }

    /** "BillieEilish" -> "Billie Eilish" (só mexe em trechos sem espaço nenhum) */
    private static String separarPalavras(String s) {
        if (s.contains(" ")) return s;
        return s.replaceAll("(?<=[a-z])(?=[A-Z])", " ")
                .replaceAll("(?<=[A-Za-z])(?=\\d)", " ")
                .trim();
    }

    /** Escolhe o melhor candidato da Deezer, ou null se nenhum for confiável. */
    private static MetadadosMusica escolher(List<MetadadosMusica> candidatos, String artista, String titulo) {
        String nArtista = norm(artista);
        String nTitulo = normTitulo(titulo);
        String nArquivo = norm(artista + " " + titulo);

        MetadadosMusica melhor = null;
        int melhorPts = 0;

        for (MetadadosMusica m : candidatos) {
            String cArtista = norm(m.artista);
            String cTitulo = normTitulo(m.titulo);
            int pts = 0;

            if (parecido(cArtista, nArtista)) pts += 4;
            else if (!cArtista.isEmpty() && !nArtista.isEmpty()
                    && (cArtista.contains(nArtista) || nArtista.contains(cArtista))) pts += 2;
            else continue;                                       // artista não tem relação: descarta

            if (parecido(cTitulo, nTitulo)) pts += 4;
            else if (!cTitulo.isEmpty() && !nTitulo.isEmpty()
                    && (cTitulo.contains(nTitulo) || nTitulo.contains(cTitulo))) pts += 1;
            else continue;                                       // título não tem relação: descarta                                     // título não tem relação: descarta

            // versões que o nome do arquivo não pediu (radio edit, remix, ao vivo...)
            String nCandidato = norm(m.titulo + " " + m.album);
            for (String ruim : INDESEJADOS) {
                if (tem(nCandidato, ruim) && !tem(nArquivo, ruim)) pts -= 3;
            }

            if (pts > melhorPts) {
                melhorPts = pts;
                melhor = m;
            }
        }
        return melhorPts >= PONTUACAO_MINIMA ? melhor : null;
    }
    
        /**
     * Igual, ignorando espaços e tolerando erro de digitação pequeno
     * ("belikeawoman" = "be like a woman"; "malcom todd" ~ "malcolm todd").
     */
    private static boolean parecido(String a, String b) {
        String x = a.replace(" ", "");
        String y = b.replace(" ", "");
        if (x.isEmpty() || y.isEmpty()) return false;
        if (x.equals(y)) return true;
        int tolerancia = Math.max(1, Math.min(x.length(), y.length()) / 6);
        return distancia(x, y) <= tolerancia;
    }

    /** Distância de Levenshtein (quantas letras trocar/inserir/apagar). */
    private static int distancia(String a, String b) {
        int[] ant = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) ant[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int custo = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, ant[j] + 1), ant[j - 1] + custo);
            }
            int[] t = ant; ant = cur; cur = t;
        }
        return ant[b.length()];
    }
    
    private static boolean tem(String texto, String palavra) {
        return (" " + texto + " ").contains(" " + palavra + " ");
    }

    /** minúsculas, sem acento, só letras e números separados por espaço */
    private static String norm(String s) {
        if (s == null) return "";
        String t = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return t.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
    }

    /** igual ao norm, mas ignora "(feat. ...)", "[...]" e sufixos depois de " - " */
    private static String normTitulo(String s) {
        if (s == null) return "";
        String t = s.replaceAll("\\(.*?\\)|\\[.*?\\]", " ");
        int i = t.indexOf(" - ");
        if (i > 0) t = t.substring(0, i);
        return norm(t);
    }

    private static void pausa() {
        try {
            Thread.sleep(PAUSA_ENTRE_MUSICAS_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}