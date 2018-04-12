package com.marekulip.droidsor.database;

import java.util.List;

public final class PlaceholderMaker {
    public static String makePlaceholders(int len) {
        if (len < 1) {
            // It will lead to an invalid query
            throw new RuntimeException("No placeholders");
        } else {
            StringBuilder sb = new StringBuilder(len * 2 - 1);
            sb.append("?");
            for (int i = 1; i < len; i++) {
                sb.append(",?");
            }
            return sb.toString();
        }
    }
    public static void makeParameters(String[] params,List<Long> idList) {
        if (params.length < 1) {
            // It will lead to an invalid query
            throw new RuntimeException("No placeholders");
        } else {
            for (int i = 0; i < idList.size(); i++) {
                params[i] = String.valueOf(idList.get(i));
            }
        }
    }
}
