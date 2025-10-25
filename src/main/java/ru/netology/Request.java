package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.*;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record Request(String method, String path, String protocolVerse, String[] headersRequest, byte[] bodyRequest) {

    public List<NameValuePair> getQueryParam(String name) {
//фильтрация по имени в момент запроса
        var nameParams = getQueryParams().stream()
                .filter(param -> name.equals(param.getName()))
                .collect(Collectors.toList());

//        for (NameValuePair param : nameParams) {
//            System.out.println(param.getName() + "=" + param.getValue());
//        }

        return nameParams;
    }

    public List<NameValuePair> getQueryParams() {
//возврат коллекции без фильтрации
        //получаем Query String для безопасного парсинга (читерство от Qwen)
        int queryStart = path.indexOf('?');
        if (queryStart == -1 || queryStart + 1 >= path.length()) {
            return Collections.emptyList();
        }
        String query = path.substring(queryStart + 1);

//логирование
        //     for (NameValuePair param : params) {
        //        System.out.println(param.getName() + " = " + param.getValue());
        //    }

        return URLEncodedUtils.parse(query, StandardCharsets.UTF_8);


    }

    public byte[] queryStringGetBytes(List<NameValuePair> params) {
        return URLEncodedUtils
                .format(params, StandardCharsets.UTF_8)
                .getBytes(StandardCharsets.UTF_8);
    }
}