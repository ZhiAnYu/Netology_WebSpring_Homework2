package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private final int PORT;
    private final int NUMBER_THREADS = 64;
    private final ExecutorService executorService = Executors.newFixedThreadPool(NUMBER_THREADS);
    private final Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();

    public Server(int port) {
        PORT = port;
    }

    public void listen() {
        try (final var serverSocket = new ServerSocket(PORT)) {
            while (true) {
                var socket = serverSocket.accept();
                executorService.submit(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            executorService.shutdown();
        }
    }

    private void handleConnection(Socket socket) {
        try (socket;
             final var in = new BufferedInputStream(socket.getInputStream());
             final var out = new BufferedOutputStream(socket.getOutputStream());) {

            final var limit = 4096;

            in.mark(limit);
            final var buffer = new byte[limit];
            final var read = in.read(buffer);

           final var parser = new QueryStringParserImpl();
            parser.parseRequestLine(buffer, read);
            if (parser.badRequest == true) {
                sendResponse(out, "400 Bad Request");
            }

            parser.parseHeaders(buffer, read);

            final var method = parser.getMethod();
            final var fullPath = parser.getFullPath();
            final var protocolVerse = parser.getProtocolVerse();
            final var cleanPath = parser.getCleanPath();
            final var headers = parser.getHeaders();
            final  var body = (method.equals("POST"))?parser.getBody():"0";



            var request = new Request(method, fullPath, protocolVerse, headers, body);

            // --- Поиск хендлера по чистому пути ---
            Handler handler = handlers.getOrDefault(method, Collections.emptyMap())
                    .get(cleanPath);
            if (handler == null) {
                sendResponse(out, "404 Not found");
                return;
            }
            try {
                handler.handle(request, out);
            } catch (Exception ex) {
                sendResponse(out, "500 Internet server error");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendResponse(BufferedOutputStream out, String status) throws IOException {
        out.write((
                "HTTP/1.1" + status + "\r\n" +
                        "Content-Length: 0\r\n" +
                        "Connection: close\r\n" +
                        "\r\n"
        ).getBytes());
        out.flush();
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, k -> new ConcurrentHashMap<>())
                .put(path, handler);
    }

}
