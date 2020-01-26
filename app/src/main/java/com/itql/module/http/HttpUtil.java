package com.itql.module.http;

import android.text.TextUtils;
import android.util.Log;

import com.itql.module.http.callback.IHttpCallback;
import com.itql.module.http.callback.IProgressCallback;
import com.itql.module.http.callback.SimpleCallback;
import com.itql.module.http.intercept.HttpLogIntercept;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class HttpUtil {
    private static final String TAG = "HttpUtil";

    private static final class OkHttpClientHolder {
        private static final OkHttpClient INSTANCE;

        static {
            HttpLogIntercept httpLogIntercept = new HttpLogIntercept(new HttpLogIntercept.Logger() {
                @Override
                public void log(String message) {
                    Log.i(TAG, message);
                }
            }).setLevel(HttpLogIntercept.Level.BODY);
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(httpLogIntercept);
            INSTANCE = builder.build();
        }
    }

    private static final class LongOkHttpClientHolder {
        private static final OkHttpClient INSTANCE;
        private static HttpLogIntercept sHttpLogIntercept;

        static {
            sHttpLogIntercept = new HttpLogIntercept(new HttpLogIntercept.Logger() {
                @Override
                public void log(String message) {
                    Log.i(TAG, message);
                }
            }).setLevel(HttpLogIntercept.Level.HEADERS);
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .addInterceptor(sHttpLogIntercept);
            INSTANCE = builder.build();
        }
    }

    public static void execute(HttpTask task) {
        execute(task, null);
    }

    public static void execute(final HttpTask task, final IHttpCallback callback) {
        try {
            Request.Builder builder = new Request.Builder();
            if (task.getTag() == null) {
                task.setTag(UUID.randomUUID().toString());
            }
            builder.tag(task.getTag());

            StringBuilder url = new StringBuilder(task.getUrl());
            boolean hasArg = false;
            Map<String, Object> map = task.getGetParamMap();
            if (map != null && !map.isEmpty()) {
                Set<Map.Entry<String, Object>> set = map.entrySet();
                for (Map.Entry<String, Object> entry : set) {
                    url.append(hasArg ? '&' : '?').append(entry.getKey());
                    url.append('=').append(entry.getValue());
                    hasArg = true;
                }
            }
            map = task.getPostParamMap();
            if (map != null && !map.isEmpty()) {
                Set<Map.Entry<String, Object>> set = map.entrySet();
                FormBody.Builder fbBuilder = new FormBody.Builder();
                for (Map.Entry<String, Object> entry : set) {
                    fbBuilder.add(entry.getKey(), String.valueOf(entry.getValue()));
                }
                builder.post(fbBuilder.build());
            }
            if (task.getRawData() != null) {
                builder.post(new RequestBody() {
                    @Override
                    public MediaType contentType() {
                        return MediaType.get("application/json");
                    }

                    @Override
                    public void writeTo(@NonNull BufferedSink sink) throws IOException {
                        sink.writeUtf8(task.getRawData());
                    }
                });
            }
            if (!TextUtils.isEmpty(task.getSign())) {
                url.append(hasArg ? "&sign=" : "?sign=").append(task.getSign());
            }

            builder.url(url.toString());
            final Call call = OkHttpClientHolder.INSTANCE.newCall(builder.build());
            if (callback != null) {
                callback.onStart(new HttpCall(call, task.getTag()));
            }
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                    if (!call.isCanceled()) call.cancel();
                    if (callback != null) {
                        callback.onError(e);
                    }
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) {
                    try {
                        if (callback != null && response.body() != null) {
                            String s = response.body().string();
                            try {
                                callback.onSuccess(s);
                            } catch (Exception e) {
                                Log.e(TAG, e.toString());
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, e.toString());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            if (callback != null) callback.onError(e);
        }
    }

    public static void upload(final UploadTask task, final SimpleCallback callback) {
        if (task == null) return;
        try {
            final IProgressCallback.upload upload = new IProgressCallback.upload() {
                @Override
                public void onUpload(final long total, final long current, final boolean complete) {
                    if (callback != null) {
                        callback.onUpload(total, current, complete);
                    }
                }
            };

            MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            if (task.getParamMap() != null) {
                Set<Map.Entry<String, Object>> set = task.getParamMap().entrySet();
                for (Map.Entry<String, Object> entry : set) {
                    builder.addFormDataPart(entry.getKey(), String.valueOf(entry.getValue()));
                }
            }

            List<File> files = task.getFiles();
            if (files != null) {
//                File file=files.get(0);
//                if (file != null) {
//                    builder.addFormDataPart("file", file.getName(), RequestBody.create(MediaType.parse("multipart/otcet-stream"), file));
//                }
                for (int i = 0, n = files.size(); i < n; i++) {
                    builder.addFormDataPart("file" + i, files.get(i).getName(), RequestBody.create(MediaType.parse("multipart/otcet-stream"), files.get(i)));
                }
            }

            RequestBody requestBody = builder.build();
            Request.Builder rBuilder = new Request.Builder();
            if (task.getTag() == null) {
                task.setTag(UUID.randomUUID().toString());
            }
            final Request request = rBuilder.url(task.getUrl())
                .post(new ProgressRequestBody(requestBody, upload))
                .tag(task.getTag())
                .build();
            final Call call = LongOkHttpClientHolder.INSTANCE.newCall(request);
            if (callback != null) {
                callback.onStart(new HttpCall(call, task.getTag()));
            }

            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                    if (!call.isCanceled()) call.cancel();
                    if (callback != null) {
                        callback.onError(e);
                    }
                }

                @Override
                public void onResponse(@NonNull final Call call, @NonNull final Response response) throws IOException {
                    if (response.body() != null) {
                        final String s = response.body().string();
                        if (callback != null) {
                            callback.onSuccess(s);
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            if (callback != null) callback.onError(e);
        }
    }

    public static void download(@NonNull String url, @NonNull final String path, final SimpleCallback callback) {
        try {
            final IProgressCallback.download download = new IProgressCallback.download() {
                @Override
                public void onDownload(long total, long current, boolean complete) {
                    if (callback != null) callback.onDownload(total, current, complete);
                }
            };

            OkHttpClient.Builder builder = new OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .addNetworkInterceptor(new Interceptor() {
                    @NonNull
                    @Override
                    public Response intercept(@NonNull Chain chain) throws IOException {
                        Response originalResponse = chain.proceed(chain.request());
                        return originalResponse.newBuilder()
                            .body(new ProgressResponseBody(originalResponse.body(), download))
                            .build();
                    }
                }).addInterceptor(LongOkHttpClientHolder.sHttpLogIntercept);
            String tag = UUID.randomUUID().toString();
            Request request = new Request.Builder().url(url).tag(tag).build();
            final Call call = builder.build().newCall(request);
            if (callback != null) callback.onStart(new HttpCall(call, tag));
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                    if (!call.isCanceled()) call.cancel();
                    if (callback != null) callback.onError(e);
                }

                @Override
                public void onResponse(@NonNull final Call call, @NonNull final Response response) {
                    try {
                        Source source = response.body().source();
                        BufferedSink buffer = Okio.buffer(Okio.sink(new File(path)));
                        buffer.writeAll(source);
                        buffer.flush();
                        source.close();
                        buffer.close();
                        if (callback != null) callback.onSuccess(null);
                    } catch (Exception e) {
                        if (callback != null) callback.onError(e);
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, e.toString());
            if (callback != null) callback.onError(e);
        }
    }
}
