package com.github.wlxxyw.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StrTemplate {
    private static final Pattern placeholder = Pattern.compile("[^\\\\]+(\\\\\\\\)*(\\{(\\d+)\\})");
    public static String format(String template, Object... args){
        template = " " + template;
        StringBuffer stringBuffer = new StringBuffer();
        Matcher matcher = placeholder.matcher(template);
        int index = 0;
        while (matcher.find()) {
            String _index = matcher.group(1);
            if (null != _index && !_index.trim().isEmpty()) {
                matcher.appendReplacement(stringBuffer,
                        String.valueOf(args[Integer.parseInt(_index)]).replace(
                                "\\", "\\\\"));
            } else {
                matcher.appendReplacement(stringBuffer, String.valueOf(args[index])
                        .replace("\\", "\\\\"));
            }
            if (index++ == args.length) {
                break;
            }
        }
        matcher.appendTail(stringBuffer);
        return stringBuffer.substring(1);
    }
}
