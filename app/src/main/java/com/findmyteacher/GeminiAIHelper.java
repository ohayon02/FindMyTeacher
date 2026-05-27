package com.findmyteacher;

import android.content.Context;
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
    // שימוש בגרסה v1 היציבה במקום v1beta
    private static final String MODEL_NAME = "gemini-3.1-flash-lite";
    private static final String BASE_URL = "https://generativelanguage.googleapis.com/v1/models/" + MODEL_NAME + ":generateContent";

    public interface AICallback {
        void onResponse(String response);
        void onError(Exception e);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void generateReport(Context context, String studentName, String lessonTime, AICallback callback) {
        String prompt = "אתה עוזר הוראה חכם. צור דוח שיעור קצר עבור " + studentName + " שהיה בשעה " + lessonTime + ". תהיה מעודד וענייני, כתוב בעברית.";
        sendRequest(prompt, callback);
    }

    public static void generateStudentProgressReport(Context context, String studentName, String feedback, List<String> lessonDates, AICallback callback) {
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

    public static void chatWithAI(Context context, String userMessage, List<Message> history, AICallback callback) {
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

    public static void continueAiChat(Context context, String studentName, String history, String lastMessage, int questionCount, AICallback callback) {
        StringBuilder prompt = new StringBuilder();
        if (questionCount == 0) {
            prompt.append("אתה עוזר הוראה חכם. פתח בשיחה קצרה וידידותית עם התלמיד ").append(studentName)
                    .append(". המטרה שלך היא להבין איך הוא מרגיש לגבי הלימודים ומה הקשיים שלו. שאל שאלה ראשונה. כתוב בעברית.");
        } else if (questionCount >= 5) {
            prompt.append("זהו סוף השיחה עם ").append(studentName).append(". בהתבסס על ההיסטוריה הבאה:\n")
                    .append(history)
                    .append("\nצור סיכום קצר (עד 3 שורות) של מצבו הלימודי והרגשי. התחל את התשובה במדויק במילה 'SUMMARY:' ואחריה הסיכום.");
        } else {
            prompt.append("המשך שיחה עם התלמיד ").append(studentName).append(".\n")
                    .append("היסטוריית שיחה:\n").append(history)
                    .append("\nהודעה אחרונה מהתלמיד: ").append(lastMessage)
                    .append("\nשאל שאלה אחת קצרה וממוקדת כדי להמשיך את האבחון. כתוב בעברית.");
        }
        sendRequest(prompt.toString(), callback);
    }

    public static void getPriceRecommendation(Context context, String location, String bio, String subjects, List<Integer> otherPrices, AICallback callback) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("אתה אלגוריתם תמחור קשוח עבור מורים פרטיים.\n")
                .append("נתוני המורה:\n")
                .append("- מיקום: ").append(location != null ? location : "לא צוין").append("\n")
                .append("- מקצועות: ").append(subjects != null ? subjects : "לא צוין").append("\n")
                .append("- ביוגרפיה וניסיון: ").append(bio != null ? bio : "אין").append("\n");

        if (otherPrices != null && !otherPrices.isEmpty()) {
            promptBuilder.append("- מחירי מורים אחרים : ").append(otherPrices.toString()).append("\n");
        }

        promptBuilder.append("\nמשימה: קבע את המחיר המומלץ לשעה בשקלים.\n")
                .append("חוק בל יעבור: התשובה שלך חייבת להכיל אך ורק את המספר עצמו (למשל: 120). ")
                .append("אל תכתוב שום מילה, אל תוסיף הסברים, אל תרשום את סימן השקל (₪) ואל תוסיף נקודות. רק מספר נקי בלבד!");

        sendRequest(promptBuilder.toString(), callback);
    }

    private static void sendRequest(String prompt, AICallback callback) {
        executor.execute(() -> {
            try {
                String apiKey = BuildConfig.API_KEY;
                if (apiKey == null || apiKey.isEmpty() || apiKey.equals("null")) {
                    throw new Exception("מפתח API חסר. וודא שהגדרת API_KEY ב-local.properties וביצעת Sync.");
                }

                URL url = new URL(BASE_URL + "?key=" + apiKey);
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

                    // ניקוי רווחים מיותרים או ירידות שורה שה-AI עלול להחזיר בטעות
                    final String cleanText = aiText.trim();
                    mainHandler.post(() -> callback.onResponse(cleanText));
                } else {
                    String errorBody = readStream(conn.getErrorStream());
                    Log.e(TAG, "Server Error Response: " + errorBody);

                    String message = "שגיאה " + responseCode;
                    try {
                        JSONObject errorJson = new JSONObject(errorBody);
                        message = errorJson.getJSONObject("error").getString("message");
                    } catch (Exception ignored) {}

                    throw new Exception("גוגל החזירה שגיאה: " + message);
                }
            } catch (Exception e) {
                Log.e(TAG, "Request Exception: ", e);
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