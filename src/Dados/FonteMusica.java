/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dados;

import com.google.gson.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/**
 *
 * @author jvitor
 */
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

    // Passo 1: lista de candidatos para o admin escolher
    public static List<MetadadosMusica> buscar(String artista, String titulo) throws Exception {
    List<MetadadosMusica> lista = buscarNaDeezer("artist:\"" + artista + "\" track:\"" + titulo + "\"");
    if (lista.isEmpty()) {
        lista = buscarNaDeezer(artista + " " + titulo);      // plano B: busca solta
    }
    return lista;
    }

    private static List<MetadadosMusica> buscarNaDeezer(String q) throws Exception {
        String url = "https://api.deezer.com/search?limit=5&q=" + enc(q);
        JsonElement raiz = get(url);
        List<MetadadosMusica> lista = new ArrayList<>();

        if (raiz == null) {
            System.err.println("Deezer não respondeu (rede bloqueada ou erro HTTP): " + url);
            return lista;
        }
        JsonObject obj = raiz.getAsJsonObject();
        if (!obj.has("data")) {
            System.err.println("Resposta sem 'data': " + obj);          // mostra o erro da Deezer
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

    // Passo 2: depois que o admin escolheu, busca ano e letra
    public static void completar(MetadadosMusica m) {
        try {   // ano de lancamento (vem do album)
            JsonElement al = get("https://api.deezer.com/album/" + m.idAlbumDeezer);
            if (al != null) {
                String data = texto(al.getAsJsonObject(), "release_date");   // AAAA-MM-DD
                if (data != null && data.length() >= 4) m.anoLancamento = Integer.parseInt(data.substring(0, 4));
            }
        } catch (Exception ignorado) { }

        try {   // letras (simples e sincronizada)
            JsonElement l = get("https://lrclib.net/api/get?artist_name=" + enc(m.artista)
                    + "&track_name=" + enc(m.titulo)
                    + "&album_name=" + enc(m.album)
                    + "&duration=" + m.duracaoSeg);
            if (l != null) {
                m.letra = texto(l.getAsJsonObject(), "plainLyrics");
                m.letraSincronizada = texto(l.getAsJsonObject(), "syncedLyrics");
            }
        } catch (Exception ignorado) { }
    }
}
