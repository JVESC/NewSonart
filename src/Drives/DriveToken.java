package Drives;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DriveToken {

    private static String token;
    private static long expiraEm = 0;

    public static synchronized String token() throws Exception {
        if (token != null && System.currentTimeMillis() < expiraEm - 60_000) return token;

        String json;
        try (InputStream in = DriveToken.class.getResourceAsStream("/credenciais/drive.json")) {
            if (in == null) throw new Exception("drive.json nao encontrado em /credenciais/");
            json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        String email = campo(json, "client_email");
        String uri = campo(json, "token_uri");
        String chave = campo(json, "private_key")
                .replace("\\n", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replace(" ", "");

        long agora = System.currentTimeMillis() / 1000;
        String header = b64("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");
        String claims = b64("{\"iss\":\"" + email + "\","
                + "\"scope\":\"https://www.googleapis.com/auth/drive.readonly\","
                + "\"aud\":\"" + uri + "\","
                + "\"iat\":" + agora + ",\"exp\":" + (agora + 3600) + "}");
        String paraAssinar = header + "." + claims;

        PrivateKey pk = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(chave)));
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(pk);
        sig.update(paraAssinar.getBytes(StandardCharsets.UTF_8));
        String jwt = paraAssinar + "." +
                Base64.getUrlEncoder().withoutPadding().encodeToString(sig.sign());

        String corpo = "grant_type=urn%3Aietf%3Aparams%3Aoauth%3Agrant-type%3Ajwt-bearer&assertion=" + jwt;
        HttpRequest req = HttpRequest.newBuilder(URI.create(uri))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(corpo))
                .build();
        HttpResponse<String> resp = HttpClient.newHttpClient()
                .send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200)
            throw new Exception("Google recusou o token: " + resp.body());

        token = campo(resp.body(), "access_token");
        Matcher m = Pattern.compile("\"expires_in\"\\s*:\\s*(\\d+)").matcher(resp.body());
        long segundos = m.find() ? Long.parseLong(m.group(1)) : 3600;
        expiraEm = System.currentTimeMillis() + segundos * 1000;
        return token;
    }

    private static String b64(String s) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static String campo(String json, String nome) {
    int i = json.indexOf("\"" + nome + "\"");
    if (i < 0) throw new IllegalStateException("Campo " + nome + " nao achado no JSON");
    i = json.indexOf(':', i);
    i = json.indexOf('"', i) + 1;
    StringBuilder sb = new StringBuilder();
    for (; i < json.length(); i++) {
        char c = json.charAt(i);
        if (c == '\\') { sb.append(c).append(json.charAt(++i)); continue; }
        if (c == '"') break;
        sb.append(c);
    }
    return sb.toString();
    }
}