package com.findmyteacher;

import android.content.Context;
import android.util.Log;
import androidx.core.content.ContextCompat;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;

public class GeminiManager {
    private static GeminiManager instance;
    // תיקון: חזרה למודל gemini-pro שהוא היציב ביותר עבור גרסה 0.9.0 ומונע שגיאות 404
    private static final String modelVersion = "gemini-pro"; 
    private static final String TAG = "GeminiManager";
//
    private GeminiManager() {}

    public static GeminiManager getInstance() {
        if (instance == null) {
            instance = new GeminiManager();
        }
        return instance;
    }

    public void sendText(String promptStr, Context context, GeminiCallback callback) {
        // משיכת המפתח וניקוי שלו
        String apiKey = BuildConfig.API_KEY.replace("\"", "").trim();
        
        if (apiKey.isEmpty()) {
            callback.onError(new Exception("מפתח API חסר. בדוק את local.properties"));
            return;
        }

        try {
            // הגדרה בסיסית שעובדת ב-100% קומפילציה
            GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
            configBuilder.temperature = 0.7f;
            GenerationConfig config = configBuilder.build();

            // יצירת המודל בצורה הכי פשוטה
            GenerativeModel gm = new GenerativeModel(modelVersion, apiKey, config);
            GenerativeModelFutures model = GenerativeModelFutures.from(gm);

            Content prompt = new Content.Builder().addText(promptStr).build();

            Executor executor = ContextCompat.getMainExecutor(context);
            ListenableFuture<GenerateContentResponse> response = model.generateContent(prompt);

            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    if (result != null && result.getText() != null) {
                        callback.onSuccess(result.getText());
                    } else {
                        callback.onError(new Exception("לא התקבלה תשובה מה-AI"));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Gemini failure: " + t.getMessage());
                    callback.onError(new Exception("שגיאת AI: " + t.getMessage()));
                }
            }, executor);

        } catch (Exception e) {
            callback.onError(e);
        }
    }

    public interface GeminiCallback {
        void onSuccess(String result);
        void onError(Throwable error);
    }
}
