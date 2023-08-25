package com.github.wlxxyw.utils;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HeadersFilter extends Filter {

    @Override
    public void doFilter(HttpExchange httpExchange, Chain chain) throws IOException {
        Headers responseHeaders = httpExchange.getResponseHeaders();
        List<Map.Entry<String,List<String>>> list = new ArrayList<>(responseHeaders.entrySet());
        list.forEach(one -> {
            if (null == one.getKey() || !one.getKey().contains("-")) {
                return;
            }
            boolean change = false;
            char[] cs = one.getKey().toCharArray();
            if (cs[0] >= 'a' && cs[0] <= 'z') {
                change = true;
                cs[0] = (char) (cs[0] - ('a' - 'A'));
            }
            for (int i = 1; i < cs.length; i++) {
                if (cs[i - 1] == '-' && cs[i] >= 'a' && cs[i] <= 'z') {
                    change = true;
                    cs[i] = (char) (cs[i] - ('a' - 'A'));
                } else if (cs[i] >= 'A' && cs[i] <= 'Z') {
                    change = true;
                    cs[i] = (char) (cs[i] + ('a' - 'A'));
                }
            }
            if (change) {
                responseHeaders.put(new String(cs), one.getValue());
            }
        });
        chain.doFilter(httpExchange);
    }

    @Override
    public String description() {
        return getClass().getSimpleName();
    }
}
