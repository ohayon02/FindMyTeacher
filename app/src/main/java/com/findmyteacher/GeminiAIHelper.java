package com.findmyteacher;

import android.content.Context;
import android.util.Log;
import java.util.List;

public class GeminiAIHelper {

    public interface AICallback {
        void onResponse(String response);
        void onError(Exception e);
    }

    public static void continueAiChat(Context context, String studentName, String history, String lastAnswer, int questionCount, AICallback callback) {
        String prompt;
        if (questionCount == 0) {
            prompt = "אתה מאמן למידה חכם. התחל שיחה עם התלמיד " + studentName + " כדי להבין איך הולך לו בלימודים. " +
                    "שאל שאלה אחת ראשונה, קצרה ומזמינה בעברית.";
        } else if (questionCount < 3) {
            prompt = "זו היסטוריית השיחה שלך עם התלמיד " + studentName + ":\n" + history + "\n" +
                    "התשובה האחרונה שלו הייתה: " + lastAnswer + "\n" +
                    "שאל שאלה אחת נוספת כדי להבין לעומק את מצבו הלימודי. היה מעודד. כתוב בעברית.";
        } else {
            prompt = "זו היסטוריית השיחה:\n" + history + "\n" +
                    "סכם את מצבו של התלמיד " + studentName + " עבור המורה בסיכום קצר ותמציתי של כ-4 שורות בלבד. " +
                    "התחל במילה 'SUMMARY:' ואז כתוב את הניתוח בעברית.";
        }
        sendRequest(context, prompt, callback);
    }

    public static void generateReport(Context context, String studentName, String lessonTime, AICallback callback) {
        String prompt = "צור דוח שיעור קצר (עד 3-4 שורות) עבור התלמיד " + studentName + " שהיה בשעה " + lessonTime + ". תהיה מעודד. כתוב בעברית.";
        sendRequest(context, prompt, callback);
    }

    public static void generateStudentProgressReport(Context context, String name, String feedback, List<String> lessonDates, boolean isForTeacher, AICallback callback) {
        if (feedback == null || feedback.isEmpty()) {
            callback.onResponse("לא נמצא משוב קודם מהתלמיד. על התלמיד לבצע את שיחת ה-AI תחילה.");
            return;
        }

        StringBuilder lessons = new StringBuilder();
        if (lessonDates != null && !lessonDates.isEmpty()) {
            for (String date : lessonDates) lessons.append(date).append(", ");
        } else {
            lessons.append("לא נמצאו שיעורים מתועדים.");
        }

        String prompt;
        if (isForTeacher) {
            prompt = "נתח את מצבו של התלמיד " + name + ".\n" +
                    "מידע מהשיחה עם התלמיד: " + feedback + "\n" +
                    "תאריכי שיעורים: " + lessons.toString() + "\n\n" +
                    "צור דוח למורה הכולל סיכום התקדמות, נקודות חוזק והמלצות. " +
                    "הדוח חייב להיות קצר ותמציתי מאוד, עד 4 שורות. כתוב בעברית.";
        } else {
            prompt = "צור דוח מוטיבציה קצר (עד 4 שורות) לתלמיד " + name + ".\n" +
                    "מידע מהשיחה שלך: " + feedback + "\n\n" +
                    "סכם לו את הלמידה ותן טיפים לשיפור. כתוב בעברית בגובה העיניים.";
        }

        sendRequest(context, prompt, callback);
    }

    public static void getPriceRecommendation(Context context, String location, String bio, String subjects, List<Integer> otherPrices, AICallback callback) {
        String prompt = "המלץ על מחיר לשיעור פרטי בשקלים.\n" +
                "מיקום: " + location + "\n" +
                "מקצועות: " + subjects + "\n" +
                "כתוב בעברית קצר ולעניין.";
        sendRequest(context, prompt, callback);
    }

    private static void sendRequest(Context context, String prompt, AICallback callback) {
        GeminiManager.getInstance().sendText(prompt, context, new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                callback.onResponse(result);
            }

            @Override
            public void onError(Throwable error) {
                Log.e("GeminiAIHelper", "AI Error", error);
                callback.onError(new Exception(error.getMessage()));
            }
        });
    }
}
