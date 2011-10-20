package leoliang.android.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.database.Cursor;

public class CursorHelper {

    public static String getResultSet(Cursor cursor) {
        JSONArray results = new JSONArray();
        String[] columnNames = cursor.getColumnNames();
        if (cursor.moveToFirst()) {
            do {
                JSONObject row = new JSONObject();
                for (String columnName : columnNames) {
                    int index = cursor.getColumnIndex(columnName);
                    String value = cursor.getString(index);
                    try {
                        row.put(columnName, value);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
                results.put(row);
            } while (cursor.moveToNext());
        }
        return results.toString();
    }

}
