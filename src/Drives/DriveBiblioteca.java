package Drives;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Varre uma pasta do Google Drive (e as subpastas) e devolve os arquivos de áudio.
 * Usa o mesmo DriveToken do player (conta de serviço, só leitura).
 */
public class DriveBiblioteca {

    public static class ArquivoDrive {
        public String id;
        public String nome;
        public String caminho;      // pasta onde está, ex.: "Rock/Daft Punk/"
    }

    public static class Varredura {
        public final List<ArquivoDrive> suportados = new ArrayList<>();      // o JavaFX toca
        public final List<ArquivoDrive> naoSuportados = new ArrayList<>();   // FLAC, OGG etc.
    }

    private static final String MIME_PASTA = "application/vnd.google-apps.folder";
    private static final Set<String> EXT_SUPORTADAS = Set.of("mp3", "m4a", "aac", "wav", "aif", "aiff");
    private static final Set<String> EXT_NAO_SUPORTADAS = Set.of("flac", "ogg", "oga", "opus", "wma");
    private static final Set<String> MIME_SUPORTADOS = Set.of(
            "audio/mpeg", "audio/mp3", "audio/mp4", "audio/x-m4a",
            "audio/aac", "audio/wav", "audio/x-wav", "audio/aiff", "audio/x-aiff");

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    /** pastaId = a parte depois de /folders/ no link da pasta. */
    public static Varredura varrer(String pastaId) throws Exception {
        Varredura v = new Varredura();
        varrer(pastaId, "", v);
        return v;
    }

    private static void varrer(String pastaId, String caminho, Varredura v) throws Exception {
        String pagina = null;
        do {
            String q = "'" + pastaId + "' in parents and trashed = false";
            String url = "https://www.googleapis.com/drive/v3/files"
                    + "?pageSize=1000"
                    + "&supportsAllDrives=true&includeItemsFromAllDrives=true"
                    + "&fields=" + enc("nextPageToken,files(id,name,mimeType)")
                    + "&q=" + enc(q)
                    + (pagina != null ? "&pageToken=" + enc(pagina) : "");

            JsonObject raiz = get(url);
            if (raiz.has("files")) {
                for (JsonElement e : raiz.getAsJsonArray("files")) {
                    JsonObject o = e.getAsJsonObject();
                    String id = o.get("id").getAsString();
                    String nome = o.get("name").getAsString();
                    String mime = o.has("mimeType") ? o.get("mimeType").getAsString() : "";

                    if (MIME_PASTA.equals(mime)) {
                        varrer(id, caminho + nome + "/", v);
                        continue;
                    }

                    ArquivoDrive a = new ArquivoDrive();
                    a.id = id;
                    a.nome = nome;
                    a.caminho = caminho;

                    String ext = extensao(nome);
                    if (EXT_SUPORTADAS.contains(ext) || (ext.isEmpty() && MIME_SUPORTADOS.contains(mime))) {
                        v.suportados.add(a);
                    } else if (EXT_NAO_SUPORTADAS.contains(ext) || mime.startsWith("audio/")) {
                        v.naoSuportados.add(a);
                    }
                    // qualquer outro tipo (imagem, PDF...) é ignorado
                }
            }
            pagina = raiz.has("nextPageToken") ? raiz.get("nextPageToken").getAsString() : null;
        } while (pagina != null);
    }

    private static JsonObject get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + DriveToken.token())
                .build();
        HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) {
            throw new Exception("Drive respondeu " + r.statusCode() + ": " + r.body());
        }
        return JsonParser.parseString(r.body()).getAsJsonObject();
    }

    private static String extensao(String nome) {
        int i = nome.lastIndexOf('.');
        return i < 0 ? "" : nome.substring(i + 1).toLowerCase(Locale.ROOT);
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}