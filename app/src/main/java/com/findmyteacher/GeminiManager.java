package com.findmyteacher;

import android.content.Context;
import android.util.Log;
import androidx.core.content.ContextCompat;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.BlockThreshold;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.ai.client.generativeai.type.GenerationConfig;
import com.google.ai.client.generativeai.type.HarmCategory;
import com.google.ai.client.generativeai.type.SafetySetting;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collections;
import java.util.concurrent.Executor;

public class GeminiManager {
    private static GeminiManager instance;
    // תיקון שם המודל לגרסה קיימת ותקינה
    private static final String modelVersion = "gemini-1.5-flash";
    private static final String TAG = "GeminiManager";

    private GeminiManager() {}

    public static GeminiManager getInstance() {
        if (instance == null) {
            instance = new GeminiManager();
        }
        return instance;
    }

    public void sendText(String promptStr, Context context, GeminiCallback callback) {
        String apiKey = BuildConfig.API_KEY;
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError(new Exception("API_KEY missing. Please check local.properties"));
            return;
        }

        try {
            GenerationConfig.Builder configBuilder = new GenerationConfig.Builder();
            configBuilder.temperature = 0.7f;
            GenerationConfig config = configBuilder.build();

            // הגדרות בטיחות למניעת חסימות מיותרות (חשוב לשיחה בעברית)
            SafetySetting safetySetting = new SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.ONLY_HIGH);

            GenerativeModel gm = new GenerativeModel(
                modelVersion, 
                apiKey, 
                config, 
                Collections.singletonList(safetySetting)
            );
            
            GenerativeModelFutures model = GenerativeModelFutures.from(gm);

            Content prompt = new Content.Builder()
                    .addText(promptStr)
                    .build();

            Executor executor = ContextCompat.getMainExecutor(context);
            ListenableFuture<GenerateContentResponse> response = model.generateContent(prompt);

            Futures.addCallback(response, new FutureCallback<GenerateContentResponse>() {
                @Override
                public void onSuccess(GenerateContentResponse result) {
                    if (result != null && result.getText() != null) {
                        callback.onSuccess(result.getText());
                    } else {
                        callback.onError(new Exception("ה-AI חסם את התגובה מטעמי בטיחות. נסה לנסח אחרת."));
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    Log.e(TAG, "Gemini failure: " + t.getMessage());
                    callback.onError(t);
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
