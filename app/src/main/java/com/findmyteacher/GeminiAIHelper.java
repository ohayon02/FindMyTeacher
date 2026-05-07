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
    
    // שימוש בשם המודל היציב ביותר למניעת שגיאות "Not Found"
    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/" + MODEL_NAME + ":generateContent?key=" + API_KEY;

    public interface AICallback {
        void onResponse(String response);
        void onError(Exception e);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void generateReport(String studentName, String lessonTime, AICallback callback) {
        String prompt = "אתה עוזר הוראה חכם. צור דוח שיעור קצר עבור " + studentName + " שהיה בשעה " + lessonTime + ". תהיה מעודד וענייני, כתוב בעברית.";
        sendRequest(prompt, callback);
    }

    public static void generateStudentProgressReport(String studentName, String feedback, List<String> lessonDates, AICallback callback) {
        StringBuilder lessons = new StringBuilder();
        if (lessonDates != null && !lessonDates.isEmpty()) {
            for (String date : lessonDates) lessons.append(date).append(", ");
        }

        String prompt = "נתח התקדמות לימודית עבור: " + studentName + ".\n" +
                "משוב: " + (feedback != null ? feedback : "אין") + "\n" +
                "שיעורים: " + lessons.toString() + "\n" +
                "כתוב דוח קצר בעברית.";

        sendRequest(prompt, callback);
    }

    public static void chatWithAI(String userMessage, List<Message> history, AICallback callback) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("אתה עוזר לימודי. ענה בעברית.\n");
        if (history != null) {
            for (Message msg : history) {
                String role = "AI".equals(msg.getSenderId()) ? "assistant: " : "user: ";
                promptBuilder.append(role).append(msg.getText()).append("\n");
            }
        }
        promptBuilder.append("user: ").append(userMessage);
        sendRequest(promptBuilder.toString(), callback);
    }

    public static void getPriceRecommendation(String location, String bio, String subjects, List<Integer> otherPrices, AICallback callback) {
        String prompt = "המלץ על מחיר למורה ב" + location + ". ביוגרפיה: " + bio;
        sendRequest(prompt, callback);
    }

    private static void sendRequest(String prompt, AICallback callback) {
        executor.execute(() -> {
            try {
                if (API_KEY == null || API_KEY.isEmpty() || API_KEY.length() < 10) {
                    throw new Exception("מפתח API לא תקין. בצע Sync Gradle.");
                }

                URL url = new URL(GEMINI_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);

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
                    Log.e(TAG, "Error " + responseCode + ": " + errorBody);
                    throw new Exception("שגיאת שרת " + responseCode + ". וודא שהמפתח תקין ב-Google Cloud.");
                }
            } catch (Exception e) {
                mainHandler.post(() -> callback.onError(e));
            }
        });
    }

    private static String readStream(InputStream is) {
        if (is == null) return "";
        try (Scanner scanner = new Scanner(is)) {
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) sb.append(scanner.nextLine());
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
