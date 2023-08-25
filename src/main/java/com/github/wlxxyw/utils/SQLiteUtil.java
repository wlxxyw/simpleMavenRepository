package com.github.wlxxyw.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * sqlite数据库工具 用来储存install记录
 */
public class SQLiteUtil {
    private static boolean enable = false;
    private static Connection con;
    private static final String CREATE_SQL = "CREATE TABLE IF NOT EXISTS UPLOAD_LOG (id INTEGER PRIMARY KEY AUTOINCREMENT, time MEDIUMINT NOT NULL, user TEXT NOT NULL,ip TEXT NOT NULL,lib TEXT NOT NULL, path TEXT NOT NULL)";
    private static final String INSERT_SQL = "INSERT INTO UPLOAD_LOG (time,user,ip,lib,path) values (${time},${user},${ip},${lib},${path})";
    private static final Pattern pattern = Pattern.compile("\\$\\{[^\\}]*\\}");
    static {
        try {
            Class.forName("org.sqlite.JDBC");
            Properties prop = new Properties();
            prop.put("charSet", "utf8");
            con = DriverManager.getConnection("jdbc:sqlite:upload.db",prop);
            con.createStatement().execute(CREATE_SQL);
            enable = true;
        } catch (ClassNotFoundException | SQLException e) {
            Logger.error("fail to init SQLUtil, uploadLog is disable!",e);
        }
    }
    public static boolean uploadLog(String user,String ip,String lib, String path){
        if(!enable)return false;
        StringBuffer sb = new StringBuffer();
        Matcher m = pattern.matcher(INSERT_SQL);
        Iterator<String> iterator = Arrays.asList(new String[]{String.valueOf(System.currentTimeMillis()),encode(user),encode(ip),encode(lib),encode(path)}).iterator();
        try {
            Statement statement = con.createStatement();
            while(m.find()&&iterator.hasNext()){
                m.appendReplacement(sb,iterator.next());
            }
            m.appendTail(sb);
            Logger.info("SQL:"+ sb);
            return statement.execute(sb.toString());
        }catch (SQLException e){
            Logger.error("fail to insert uploadLog");
            return false;
        }
    }
    private static String encode(String str){
        return "\""+(str.replace("\"","\\\""))+"\"";
    }
}
