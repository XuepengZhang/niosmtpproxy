package com.sailing.filter;

import org.apache.commons.codec.binary.Base64;
import org.apache.james.protocols.api.handler.CommandDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class KeyWordFilter {
    //现在实现的访问控制的有
    private final static Logger log = LoggerFactory.getLogger(CommandDispatcher.class);


    private static List<String> keywords = new ArrayList<String>();

    //匹配上了返回true
    //没有匹配上返回
    public static Boolean keyWordFilter(String content){
        try {
            if (keywords == null || keywords.size() == 0){
                log.debug("keywords.size()等于0，不进行过滤");
            }

            if (content !=null && content.trim().length()>0) {
                content = content.trim();
                String head = getHead(content);
                String body = getBody(content);
                body = new String(Base64.decodeBase64(body.getBytes("UTF-8")),"UTF-8");
                for (String keyword : keywords) {
                    if (head.matches("^.*" + keyword + ".*$") ||
                            body.matches("^.*" + keyword + ".*$")) {
                        return true;
                    }

                }
            }
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
            return false;
        }

        return false;
    }

    //    public static void main(String[] args) {
//        String a = "adadasfasffchina";
//        String b = "china";
//        System.out.println(a.matches("^.*"+b+".*$"));
//        String x = new String(Base64.encodeBase64(b.getBytes()));
//        System.out.println(x);
//        String content  = "Message-Id: <B418279C6C3C4E2AE542D913D6B83227@test.com>\n" +
//                "From: \"abc\" <abc@test.com>\n" +
//                "Subject: title\n" +
//                "To: \"b\" <b@test.com>\n" +
//                "Content-Type: multipart/alternative; boundary=\"T9DhG5ro8XfLXaWUIJ2=_rIqCxFx9h9YdM\"\n" +
//                "MIME-Version: 1.0\n" +
//                "Reply-To: abc@test.com\n" +
//                "Date: Sun, 27 Jan 2019 10:16:32 +0800\n" +
//                "X-Mailer: DreamMail5 [GB - 5.16.1008.1030]\n" +
//                "\n" +
//                "This is a multi-part message in MIME format\n" +
//                "\n" +
//                "--T9DhG5ro8XfLXaWUIJ2=_rIqCxFx9h9YdM\n" +
//                "Content-Type: text/plain; charset=\"utf-8\"\n" +
//                "Content-Transfer-Encoding: base64\n" +
//                "Content-Disposition: inline\n" +
//                "\n" +
//                "DQoNCnRoaXMgaXMgYSBjb250ZW50DQo=\n" +
//                "\n" +
//                "--T9DhG5ro8XfLXaWUIJ2=_rIqCxFx9h9YdM\n" +
//                "Content-Type: text/html; charset=\"utf-8\"\n" +
//                "Content-Transfer-Encoding: base64\n" +
//                "Content-Disposition: inline\n" +
//                "\n" +
//                "PEhUTUw+PEhFQUQ+DQo8U1RZTEUgdHlwZT10ZXh0L2Nzcz4gQk9EWSB7IGZvbnQtc2l6ZTogMC44\n" +
//                "NzVlbTsgbGluZS1oZWlnaHQ6IDEuNSAgfSA8L1NUWUxFPg0KDQo8TUVUQSBjb250ZW50PSJ0ZXh0\n" +
//                "L2h0bWw7IGNoYXJzZXQ9dXRmLTgiIGh0dHAtZXF1aXY9Q29udGVudC1UeXBlPg0KPFNUWUxFPg0K\n" +
//                "Ym9keXsNCm1hcmdpbi10b3A6NXB4OyBtYXJnaW4tcmlnaHQ6MnB4IDsgbWFyZ2luLWJvdHRvbTow\n" +
//                "cHg7IG1hcmdpbi1sZWZ0OjJweDsNCmxpbmUtaGVpZ2h0OjEuNTsgIA0KfQ0KcHttYXJnaW46MH0N\n" +
//                "CjwvU1RZTEU+DQoNCjxNRVRBIG5hbWU9R0VORVJBVE9SIGNvbnRlbnQ9Ik1TSFRNTCAxMS4wMC4x\n" +
//                "MDU3MC4xMDAxIj48L0hFQUQ+DQo8Qk9EWSBzdHlsZT0iRk9OVC1TSVpFOiA5cHQ7IEZPTlQtRkFN\n" +
//                "SUxZOiDlvq7ova/pm4Xpu5EiPg0KPERJVj4mbmJzcDs8L0RJVj4NCjxESVY+Jm5ic3A7PC9ESVY+\n" +
//                "DQo8RElWPjwhLS1CRUdJTl84MTI0MjY4RUJDNEVEMThERkQ4NjJGQ0Y5QTQ0OUE0OS0tPjwhLS1F\n" +
//                "TkRfODEyNDI2OEVCQzRFRDE4REZEODYyRkNGOUE0NDlBNDktLT48L0RJVj4NCjxESVY+dGhpcyBp\n" +
//                "cyBhIGNvbnRlbnQ8IS0tQkVHSU5fQkU0RTY5OTQ4OTBCMTkzMUY4MzY5NEFGRTM3NTVFRjEtLT48\n" +
//                "IS0tRU5EX0JFNEU2OTk0ODkwQjE5MzFGODM2OTRBRkUzNzU1RUYxLS0+PC9ESVY+PC9CT0RZPjwv\n" +
//                "SFRNTD4NCg==\n" +
//                "\n" +
//                "--T9DhG5ro8XfLXaWUIJ2=_rIqCxFx9h9YdM--";
////        System.out.println(t);
////        String x1 = new String(Base64.decodeBase64(t.getBytes()));
////        System.out.println(x1);
//
//
////        System.out.println(body);
//    }
    private static String getHead(String content) {
        String boundary = getBoundary(content);
        boundary = "--" + boundary;
        String head = content.substring(0,content.indexOf(boundary));
        return head;
    }
    private static String getBody(String content) {
        String boundary = getBoundary(content);

        //2.获取body
        //2.1定位到大体位置
        boundary = "--" + boundary;
        //+1是为了去除末尾的\n
        String body = content.substring(content.indexOf(boundary)+boundary.length() +1);
        //2.2 会有三行head头需要去除
        body = body.substring(body.indexOf("\n",
                body.indexOf("\n",
                        body.indexOf("\n")+1)+1));
        //2.3 去除尾部字符串

        body = body.substring(0,body.indexOf(boundary)).trim();
        return body;
    }

    private static String getBoundary(String content) {
        //1获取boundary
        //1.1boundary的起始位置
        int startFlag = content.indexOf("boundary=\"") + "boundary=\"".length();
        //1.2boundary的终点位置
        int endFlag = content.indexOf("\"",startFlag);
        return content.substring(startFlag,endFlag);
    }

    public static void setKeyWordFilter(List<String> keywords){
        log.info("接收到{}个关键字",keywords.size());
        log.debug("接收到关键字为{}",keywords);
        KeyWordFilter.keywords = keywords;
    }


}
