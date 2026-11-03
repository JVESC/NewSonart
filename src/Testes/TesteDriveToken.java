package Testes;


import Drives.DriveToken;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author jvitor
 */
public class TesteDriveToken {
    
    public static void main(String[] args) throws Exception {
    Drives.DriveToken DriveToken = new DriveToken();
    String id = "1suOwJXr_zqkf2TJzyykk6yQK9eg-VRP4";
    java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder(
            java.net.URI.create("https://www.googleapis.com/drive/v3/files/" + id + "?fields=name,size,mimeType"))
        .header("Authorization", "Bearer " + DriveToken.token())
        .build();
    System.out.println(java.net.http.HttpClient.newHttpClient()
        .send(req, java.net.http.HttpResponse.BodyHandlers.ofString()).body());
}
}
