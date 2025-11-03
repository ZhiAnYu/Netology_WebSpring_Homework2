package ru.netology;

import java.io.IOException;

public interface QueryStringParser {

    public void parseRequestLine(byte[] buffer, int readBytes);

    public void parseHeaders(byte[] buffer, int readBytes) throws IOException;

    public void parseBody(byte[] buffer) throws IOException;
}
