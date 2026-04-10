package com.findmyteacher;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeminiAIHelper {

    private static final String TAG = "GeminiAIHelper";
    private static final String API_KEY = BuildConfig.API_KEY;
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    public interface AICallback {
        void onResponse(String response);
        void onError(Exception e);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void generateReport(String studentName, String lessonTime, AICallback callback) {
        String prompt = "אתה עוזר הוראה חכם. צור דוח שיעור קצר עבור " + studentName + " שהיה בשעה " + lessonTime + ". תהיה מעודד וענייני.";
        sendRequest(prompt, callback);
    }

    public static void generateStudentProgressReport(String studentName, String feedback, List<String> lessonDates, AICallback callback) {
        StringBuilder lessons = new StringBuilder();
        if (lessonDates != null) {
            for (String date : lessonDates) lessons.append(date).append(", ");
        }

        String prompt = "אתה מערכת AI לניתוח התקדמות לימודית. נתח את מצבו של התלמיד " + studentName + ".\n" +
                "מידע מהתלמיד: " + (feedback != null ? feedback : "אין משוב עדיין") + "\n" +
                "תאריכי שיעורים אחרונים: " + (lessons.length() > 0 ? lessons : "לא נמצאו שיעורים") + "\n\n" +
                "צור דוח מקיף למורה הכולל:\n" +
                "1. סיכום התקדמות כללי.\n" +
                "2. נקודות חוזק וקושי (לפי המשוב).\n" +
                "3. המלצות פדגוגיות להמשך.\n" +
                "היה מקצועי, כתוב בעברית, ללא סימני עיצוב מיוחדים.";

        sendRequest(prompt, callback);
    }

    public static void getPriceRecommendation(String location, String bio, String subjects, List<Integer> otherPrices, AICallback callback) {
        StringBuilder pricesStr = new StringBuilder();
        if (otherPrices != null && !otherPrices.isEmpty()) {
            for (Integer p : otherPrices) pricesStr.append(p).append(", ");
        }

        String prompt = "אתה מומחה לתימחור שיעורים פרטיים בישראל. עזור למורה לקבוע מחיר הוגן.\n" +
                "מיקום: " + location + "\n" +
                "ביוגרפיה: " + bio + "\n" +
                "מקצועות: " + subjects + "\n" +
                "מחירים של מורים אחרים באזור: " + (pricesStr.length() > 0 ? pricesStr : "אין נתונים") + "\n\n" +
                "בהתבסס על המידע הזה, תן המלצה למחיר לשעה (בשקלים). הסבר קצר למה בחרת במחיר זה.\n" +
                "כתוב בעברית, קצר ולעניין.";

        sendRequest(prompt, callback);
    }

    private static void sendRequest(String prompt, AICallback callback) {
        executor.execute(() -> {
            try {
                if (API_KEY == null || API_KEY.isEmpty()) {
                    throw new Exception("API Key missing");
                }

                URL url = new URL(GEMINI_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject jsonBody = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", prompt);
                parts.put(part);
                content.put("parts", parts);
                contents.put(content);
                jsonBody.put("contents", contents);

                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                writer.write(jsonBody.toString());
                writer.flush();
                writer.close();

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    String responseString = readStream(conn.getInputStream());
                    JSONObject jsonResponse = new JSONObject(responseString);
                    String aiText = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    mainHandler.post(() -> callback.onResponse(aiText));
                } else {
                    throw new Exception("HTTP Error: " + responseCode);
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private static String readStream(InputStream is) {
        if (is == null) return "";
        Scanner scanner = new Scanner(is);
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNextLine()) sb.append(scanner.nextLine());
        scanner.close();
        return sb.toString();
    }
}
