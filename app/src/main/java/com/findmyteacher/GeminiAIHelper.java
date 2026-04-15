package com.findmyteacher;

import android.content.Context;
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
                    "שאל שאלה אחת ראשונה, קצרה ומזמינה (למשל: מה הנושא שהכי קשה לך בו כרגע?). כתוב בעברית.";
        } else if (questionCount < 3) {
            prompt = "זו היסטוריית השיחה שלך עם התלמיד " + studentName + ":\n" + history + "\n" +
                    "התשובה האחרונה שלו הייתה: " + lastAnswer + "\n" +
                    "בהתבסס על זה, שאל שאלה אחת נוספת כדי להבין לעומק את מצבו הלימודי. היה מעודד. כתוב בעברית.";
        } else {
            prompt = "זו היסטוריית השיחה המלאה:\n" + history + "\n" +
                    "התשובה האחרונה: " + lastAnswer + "\n" +
                    "כעת, סכם את מצבו של התלמיד " + studentName + " עבור המורה שלו. " +
                    "הדוח צריך לכלול: קשיים, הצלחות והמלצה למורה. " +
                    "התחל את התגובה במילה 'SUMMARY:' ואז כתוב את הניתוח בעברית בצורה מקצועית.";
        }
        sendRequest(context, prompt, callback);
    }

    public static void generateReport(Context context, String studentName, String lessonTime, AICallback callback) {
        String prompt = "אתה עוזר הוראה חכם. צור דוח שיעור קצר עבור " + studentName + " שהיה בשעה " + lessonTime + ". תהיה מעודד וענייני. כתוב בעברית.";
        sendRequest(context, prompt, callback);
    }

    public static void generateStudentProgressReport(Context context, String name, String feedback, List<String> lessonDates, boolean isForTeacher, AICallback callback) {
        if (feedback == null || feedback.isEmpty()) {
            if (isForTeacher) {
                callback.onResponse("אין מידע זמין עדיין על תלמיד זה. התלמיד טרם ביצע את שאלון ה-AI.");
                return;
            }
        }

        StringBuilder lessons = new StringBuilder();
        if (lessonDates != null) {
            for (String date : lessonDates) lessons.append(date).append(", ");
        }

        String prompt;
        if (isForTeacher) {
            prompt = "אתה מערכת AI לניתוח התקדמות לימודית. נתח את מצבו של התלמיד " + name + ".\n" +
                    "מידע מהתלמיד: " + feedback + "\n" +
                    "תאריכי שיעורים אחרונים: " + (lessons.length() > 0 ? lessons : "לא נמצאו שיעורים") + "\n\n" +
                    "צור דוח מקיף למורה הכולל:\n" +
                    "1. סיכום התקדמות כללי.\n" +
                    "2. נקודות חוזק וקושי.\n" +
                    "3. המלצות פדגוגיות להמשך.\n" +
                    "היה מקצועי, כתוב בעברית, ללא סימני עיצוב מיוחדים.";
        } else {
            prompt = "אתה מאמן למידה אישי. נתח את ההתקדמות שלך עבור התלמיד " + name + ".\n" +
                    "המשוב שנתת: " + feedback + "\n" +
                    "מספר שיעורים שביצעת: " + (lessonDates != null ? lessonDates.size() : 0) + "\n\n" +
                    "צור דוח מוטיבציה הכולל:\n" +
                    "1. סיכום הלמידה שלך עד כה.\n" +
                    "2. טיפים לשיפור.\n" +
                    "3. מילה טובה לשימור המוטיבציה.\n" +
                    "כתוב בגובה העיניים, בעברית, ללא סימני עיצוב.";
        }

        sendRequest(context, prompt, callback);
    }

    public static void getPriceRecommendation(Context context, String location, String bio, String subjects, List<Integer> otherPrices, AICallback callback) {
        StringBuilder pricesStr = new StringBuilder();
        if (otherPrices != null && !otherPrices.isEmpty()) {
            for (Integer p : otherPrices) pricesStr.append(p).append(", ");
        }

        String prompt = "אתה מומחה לתימחור שיעורים פרטיים בישראל. עזור למורה לקבוע מחיר הוגן.\n" +
                "מיקום: " + location + "\n" +
                "ביוגרפיה: " + bio + "\n" +
                "מקצועות: " + subjects + "\n" +
                "מחירים של מורים אחרים באזור: " + (pricesStr.length() > 0 ? pricesStr : "אין נתונים מספיקים") + "\n\n" +
                "בהתבסס על המידע הזה, תן המלצה למחיר לשעה (בשקלים). הסבר קצר למה בחרת במחיר זה.\n" +
                "כתוב בעברית, קצר ולעניין.";

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
                callback.onError(new Exception(error));
            }
        });
    }
}
