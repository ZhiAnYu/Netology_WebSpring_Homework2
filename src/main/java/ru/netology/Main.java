package ru.netology;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;


public class Main {
    public static void main(String[] args) {

        final var server = new Server(9990);

        server.addHandler("GET", "/hello", (Request request, BufferedOutputStream out) -> {
            var responce = request.getQueryParam("lang");

            try {
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/plain \r\n" +
                                "Content-Length: " + request.queryStringGetBytes(responce).length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(request.queryStringGetBytes(responce));
                out.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        server.addHandler("POST", "/submit", (Request request, BufferedOutputStream out) -> {
            var responce = request.getPostParams();

            try {
                out.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/plain\r\n" +
                                "Content-Length: " + request.queryStringGetBytes(responce).length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                out.write(request.queryStringGetBytes(responce));
                out.flush();

            } catch (IOException e) {
                e.printStackTrace();
            }
        });


        server.listen();
    }
}