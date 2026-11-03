package Dados;

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

/** Busca dados de músicas na Deezer (metadados) e na LRCLIB (letras). Sem chave de API. */
public class FonteMusica {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    private static JsonElement get(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "Sonart/1.0 (projeto academico)")
                .build();
        HttpResponse<String> r = HTTP.send(req, HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) return null;
        return JsonParser.parseString(r.body());
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String texto(JsonObject o, String campo) {
        return (o.has(campo) && !o.get(campo).isJsonNull()) ? o.get(campo).getAsString() : null;
    }

    /** Busca por artista + título (estrita primeiro, depois solta). Até 5 resultados. */
    public static List<MetadadosMusica> buscar(String artista, String titulo) throws Exception {
        List<MetadadosMusica> lista = pesquisar("artist:\"" + artista + "\" track:\"" + titulo + "\"", 5);
        if (lista.isEmpty()) {
            lista = pesquisar(artista + " " + titulo, 5);
        }
        return lista;
    }

    /** Pesquisa livre na Deezer (um texto qualquer), com o limite de resultados que você quiser. */
    public static List<MetadadosMusica> pesquisar(String q, int limite) throws Exception {
        String url = "https://api.deezer.com/search?limit=" + limite + "&q=" + enc(q);
        JsonElement raiz = get(url);
        List<MetadadosMusica> lista = new ArrayList<>();

        if (raiz == null) {
            System.err.println("Deezer não respondeu (rede bloqueada ou erro HTTP): " + url);
            return lista;
        }
        JsonObject obj = raiz.getAsJsonObject();
        if (!obj.has("data")) {
            System.err.println("Resposta sem 'data': " + obj);
            return lista;
        }

        for (JsonElement e : obj.getAsJsonArray("data")) {
            JsonObject t = e.getAsJsonObject();
            JsonObject al = t.getAsJsonObject("album");
            MetadadosMusica m = new MetadadosMusica();
            m.titulo = t.get("title").getAsString();
            m.duracaoSeg = t.get("duration").getAsInt();
            m.artista = t.getAsJsonObject("artist").get("name").getAsString();
            m.album = al.get("title").getAsString();
            m.capa = texto(al, "cover_xl");
            m.idAlbumDeezer = al.get("id").getAsLong();
            lista.add(m);
        }
        return lista;
    }

    /** Depois de escolhido o candidato: busca o ano de lançamento e a letra. */
    public static void completar(MetadadosMusica m) {
        try {   // ano de lançamento (vem do álbum)
            JsonElement al = get("https://api.deezer.com/album/" + m.idAlbumDeezer);
            if (al != null) {
                String data = texto(al.getAsJsonObject(), "release_date");   // AAAA-MM-DD
                if (data != null && data.length() >= 4) {
                    m.anoLancamento = Integer.parseInt(data.substring(0, 4));
                }
            }
        } catch (Exception ignorado) { }

        try {   // letras (simples e sincronizada)
            JsonElement l = get("https://lrclib.net/api/get?artist_name=" + enc(m.artista)
                    + "&track_name=" + enc(m.titulo)
                    + "&album_name=" + enc(m.album)
                    + "&duration=" + m.duracaoSeg);
            JsonObject o = (l != null && l.isJsonObject()) ? l.getAsJsonObject() : null;

            if (o == null) {   // plano B: a busca da LRCLIB é mais tolerante com a duração
                JsonElement s = get("https://lrclib.net/api/search?track_name=" + enc(m.titulo)
                        + "&artist_name=" + enc(m.artista));
                if (s != null && s.isJsonArray() && s.getAsJsonArray().size() > 0) {
                    for (JsonElement e : s.getAsJsonArray()) {
                        JsonObject c = e.getAsJsonObject();
                        if (texto(c, "syncedLyrics") != null) {
                            o = c;
                            break;
                        }
                    }
                    if (o == null) o = s.getAsJsonArray().get(0).getAsJsonObject();
                }
            }

            if (o != null) {
                m.letra = texto(o, "plainLyrics");
                m.letraSincronizada = texto(o, "syncedLyrics");
            }
        } catch (Exception ignorado) { }
    }
}