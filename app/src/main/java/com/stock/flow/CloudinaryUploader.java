package com.stock.flow;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simpleng Cloudinary unsigned upload gamit ang HttpURLConnection
 * (walang dagdag na library). Tumatakbo sa background thread,
 * ibinabalik ang resulta sa main thread via callback.
 */
public class CloudinaryUploader {

    private static final String CLOUD_NAME = "dcep9imvt";
    private static final String UPLOAD_PRESET = "android_upload_preset";

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface UploadCallback {
        void onSuccess(String secureUrl);
        void onError(String message);
    }

    public static void upload(Context context, Uri imageUri, UploadCallback callback) {

        executor.execute(() -> {
            HttpURLConnection connection = null;
            try {
                String urlStr = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";
                String boundary = "Boundary-" + UUID.randomUUID();

                connection = (HttpURLConnection) new URL(urlStr).openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty(
                        "Content-Type", "multipart/form-data; boundary=" + boundary
                );

                OutputStream out = connection.getOutputStream();

                writeFormField(out, boundary, "upload_preset", UPLOAD_PRESET);

                InputStream imageStream = context.getContentResolver().openInputStream(imageUri);
                if (imageStream == null) {
                    postError(callback, "Hindi mabuksan ang napiling larawan");
                    return;
                }

                out.write(("--" + boundary + "\r\n").getBytes());
                out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"upload.jpg\"\r\n").getBytes());
                out.write(("Content-Type: image/jpeg\r\n\r\n").getBytes());

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = imageStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
                imageStream.close();

                out.write(("\r\n--" + boundary + "--\r\n").getBytes());
                out.flush();
                out.close();

                int responseCode = connection.getResponseCode();
                InputStream responseStream = (responseCode >= 200 && responseCode < 300)
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                StringBuilder response = new StringBuilder();
                byte[] respBuffer = new byte[2048];
                int len;
                while ((len = responseStream.read(respBuffer)) != -1) {
                    response.append(new String(respBuffer, 0, len));
                }
                responseStream.close();

                if (responseCode >= 200 && responseCode < 300) {
                    JSONObject json = new JSONObject(response.toString());
                    String secureUrl = json.getString("secure_url");
                    postSuccess(callback, secureUrl);
                } else {
                    postError(callback, "Cloudinary error (" + responseCode + "): " + response);
                }

            } catch (IOException | org.json.JSONException e) {
                postError(callback, "Upload failed: " + e.getMessage());
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    private static void writeFormField(
            OutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes());
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes());
        out.write((value + "\r\n").getBytes());
    }

    private static void postSuccess(UploadCallback callback, String url) {
        mainHandler.post(() -> callback.onSuccess(url));
    }

    private static void postError(UploadCallback callback, String message) {
        mainHandler.post(() -> callback.onError(message));
    }
}
