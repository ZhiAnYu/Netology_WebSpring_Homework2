package ru.netology;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private final int PORT;
    private final int NUMBER_THREADS=64;
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
             final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             final var out = new BufferedOutputStream(socket.getOutputStream());) {

            final var requestLine = in.readLine();

            if (requestLine == null) {
                sendResponse(out, "400 Bad Request");
                return;
            }

            final var parts = requestLine.split(" ");


            if (parts.length != 3) {
                sendResponse(out, "400 Bad Request");
                return;
            }

            final var method = parts[0];
            final var fullPath = parts[1];
            // Извлекаем чистый путь без query-параметров
            String cleanPath;
            int queryStart = fullPath.indexOf('?');
            if (queryStart != -1) {
                cleanPath = fullPath.substring(0, queryStart);
            } else {
                cleanPath = fullPath;
            }
            final var protocolVerse = parts[2];

            var request = new Request(method, fullPath, protocolVerse, null, null);

            //поиск handler - возврат пустой мапы в случае если метода нет,
            // возврат null если нет совпадений по path
            Handler handler = handlers.getOrDefault(method, Collections.emptyMap())
                    .get(cleanPath);
            if (handler == null) {
                sendResponse(out, "404 Not found");
                return;
            }
            try {
                handler.handle(request, out);
                //если ошибка будет в логике у пользователя, то делаем перехват
            } catch (Exception ex) {
                sendResponse(out, "500 Internet server error");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendResponse(BufferedOutputStream out, String status) {
        try {
            String response = "HTTP/1.1 " + status + "\r\n" +
                    "Content-Length: 0" + "\r\n" +
                    "Connection: close\r\n\r\n";
            out.write(response.getBytes());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.computeIfAbsent(method, k -> new ConcurrentHashMap<>())
                .put(path, handler);
    }

}
