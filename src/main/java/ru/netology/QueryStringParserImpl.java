package ru.netology;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class QueryStringParserImpl implements QueryStringParser {
    private static String fullPath;
    private static String method;
    private static String protocolVerse;
    private static String cleanPath;
    private List<String> headers;
    private String body;
    public static boolean badRequest = false;
    static final List<String> allowedMethods = List.of("GET", "POST");
    private int headersStart;
    private int headersEnd;


    @Override
    public void parseRequestLine(byte[] buffer, int readBytes) {
        // ищем request line
        final var requestLineDelimiter = new byte[]{'\r', '\n'};
        //ищем наш разделитель в прочитаном буффере
        final var requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, readBytes);
        if (requestLineEnd == -1) {
            badRequest = true;
            return;
        }

        headersStart = requestLineEnd + requestLineDelimiter.length;

        // читаем request line
        final var requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            badRequest = true;
            return;
        }

        //получаем версию протокола
        protocolVerse = requestLine[2];
        if (!"HTTP/1.1".equals(protocolVerse)) {
            badRequest = true;
            return;
        }

        //получаем метод и проверяем , что он входит в список разрешенных
        method = requestLine[0];
        if (!allowedMethods.contains(method)) {
            badRequest = true;
            return;
        }

        //получаем fullPath и проверяем, что он начинается со /
        fullPath = requestLine[1];
        if (!fullPath.startsWith("/")) {
            badRequest = true;
            return;
        }

        // Извлекаем чистый путь без query-параметров
        cleanPath = fullPath;
        int queryStart = fullPath.indexOf('?');
        if (queryStart != -1) {
            cleanPath = fullPath.substring(0, queryStart);
        }
    }

    // ищем заголовки
    @Override
    public void parseHeaders(byte[] buffer, int readBytes) throws IOException {
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        headersEnd = indexOf(buffer, headersDelimiter, headersStart, readBytes);
        if (headersEnd == -1) {
            badRequest = true;
            return;
        }

        ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
        final var headersBytes = bis.readNBytes(headersEnd - headersStart);
        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
//        System.out.println(headers);
    }

    @Override
    public void parseBody(byte[] buffer) throws IOException {
        // для GET тела нет
        final var headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
        ByteArrayInputStream bis = new ByteArrayInputStream(buffer);
        bis.skip(headersEnd + headersDelimiter.length);
        // вычитываем Content-Length, чтобы прочитать body
        final var contentLength = extractHeader(headers, "Content-Length");
        if (contentLength.isPresent()) {
            final var length = Integer.parseInt(contentLength.get());
            final var bodyBytes = bis.readNBytes(length);
            body = new String(bodyBytes);
            //    System.out.println(body);
        }


    }

    private static Optional<String> extractHeader(List<String> headers, String header) {
        return headers.stream()
                .filter(o -> o.startsWith(header))
                .map(o -> o.substring(o.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public static String getMethod() {
        return method;
    }

    public static String getFullPath() {
        return fullPath;
    }

    public static String getProtocolVerse() {
        return protocolVerse;
    }

    public static String getCleanPath() {
        return cleanPath;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}
