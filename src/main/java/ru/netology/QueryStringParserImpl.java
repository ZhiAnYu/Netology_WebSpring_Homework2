package ru.netology;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

public class QueryStringParserImpl implements QueryStringParser {
    private String fullPath;
    private String method;
    private String protocolVerse;
    private String cleanPath;
    private Map<String, String> headers;
    private String body;
    public boolean badRequest = false;
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
//        byte[] headersBytes = Arrays.copyOfRange(buffer, headersStart, headersEnd);
        String headersStr = new String(headersBytes, StandardCharsets.UTF_8);
        List<String> headerLines = Arrays.asList(headersStr.split("\r\n"));

        this.headers = headerLines.stream()
                .filter(line -> line.contains(":"))
                .collect(Collectors.toMap(
                        line -> line.substring(0, line.indexOf(':')).trim().toLowerCase(),
                        line -> line.substring(line.indexOf(':') + 1).trim(),
                        (oldVal, newVal) -> newVal, // последнее значение побеждает
                        LinkedHashMap::new
                ));

//        final var headers = Arrays.asList(new String(headersBytes).split("\r\n"));
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

    private Optional<String> extractHeader(Map<String, String> headers, String headerName) {
        String value = headers.get(headerName.toLowerCase());
        return Optional.ofNullable(value);
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

    public String getMethod() {
        return method;
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getProtocolVerse() {
        return protocolVerse;
    }

    public String getCleanPath() {
        return cleanPath;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }


}
