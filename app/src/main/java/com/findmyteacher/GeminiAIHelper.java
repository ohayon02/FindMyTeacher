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
    // Try v1 instead of v1beta for stability if possible, but keeping v1beta for now as it supports flash
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    public interface AICallback {
        void onResponse(String response);
        void onError(Exception e);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void generateReport(String studentName, String lessonTime, AICallback callback) {
        String prompt = "אתה עוזר הוראה חכם למורה פרטי. " +
                "צור דוח התקדמות קצר ומעודד (בעברית) עבור התלמיד " + studentName + 
                " שהשתתף בשיעור בשעה " + lessonTime + ". " +
                "הדו\"ח צריך לכלול: \n" +
                "1. התרשמות כללית מההתקדמות.\n" +
                "2. נושא אחד לחיזוק.\n" +
                "3. טיפ לימודי קטן.\n" +
                "היה תמציתי ומקצועי. אל תשתמש בסימני עיצוב כמו כוכביות.";

        sendRequest(prompt, callback);
    }

    public static void getPriceRecommendation(String location, String bio, String subjects, List<Integer> otherPrices, AICallback callback) {
        StringBuilder pricesStr = new StringBuilder();
        if (otherPrices != null) {
            for (Integer p : otherPrices) pricesStr.append(p).append(", ");
        }

        String prompt = "אתה יועץ עסקי למורים פרטיים. " +
                "מורה רוצה לקבוע מחיר לשעה. הנה הפרטים שלו:\n" +
                "- מיקום: " + location + "\n" +
                "- תחומי לימוד: " + subjects + "\n" +
                "- ניסיון/ביוגרפיה: " + bio + "\n" +
                "- מחירים של מורים אחרים באזור: " + (pricesStr.length() > 0 ? pricesStr : "אין נתונים") + "\n\n" +
                "תן המלצה למחיר מפורטת בעברית. הסבר למה זה המחיר המתאים בהתבסס על רמת הידע והתחרות. " +
                "היה תמציתי ואל תשתמש בכוכביות.";

        sendRequest(prompt, callback);
    }

    private static void sendRequest(String prompt, AICallback callback) {
        executor.execute(() -> {
            try {
                if (API_KEY == null || API_KEY.isEmpty()) {
                    throw new Exception("API Key is missing. Check local.properties.");
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
                    String errorBody = readStream(conn.getErrorStream());
                    Log.e(TAG, "Gemini Error Body: " + errorBody);
                    throw new Exception("HTTP Error: " + responseCode + " - " + errorBody);
                }
            } catch (Exception e) {
                Log.e(TAG, "Gemini API Error", e);
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private static String readStream(InputStream is) {
        if (is == null) return "";
        Scanner scanner = new Scanner(is);
        StringBuilder sb = new StringBuilder();
        while (scanner.hasNextLine()) {
            sb.append(scanner.nextLine());
        }
        scanner.close();
        return sb.toString();
    }
}
