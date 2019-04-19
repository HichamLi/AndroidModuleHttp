package com.itql.module.http;

import androidx.annotation.NonNull;

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
    public static boolean sDebug = false;

    private static final class OkHttpClientHolder {
        private static final OkHttpClient INSTANCE;

        static {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS);
            if (sDebug) {
                builder.addInterceptor(new HttpLogIntercept().setLevel(HttpLogIntercept.Level.BODY));
            }
            INSTANCE = builder.build();
        }
    }

    private static final class LongOkHttpClientHolder {
        private static final OkHttpClient INSTANCE;

        static {
            OkHttpClient.Builder builder = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(300, TimeUnit.SECONDS)
                    .writeTimeout(300, TimeUnit.SECONDS);
            if (sDebug) {
                builder.addInterceptor(new HttpLogIntercept().setLevel(HttpLogIntercept.Level.HEADERS));
            }
            INSTANCE = builder.build();
        }
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

            builder.url(url.toString());
            final Call call = OkHttpClientHolder.INSTANCE.newCall(builder.build());
            if (callback != null) callback.onStart(new HttpCall(call));
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull final IOException e) {
                    if (callback != null) callback.onError(e);
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (callback != null && response.body() != null) {
                        callback.onSuccess(response.body().string());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
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
                for (int i = 0, n = files.size(); i < n; i++) {
                    builder.addFormDataPart("file" + i, files.get(i).getName(), RequestBody.create(MediaType.parse("multipart/otcet-stream"), files.get(i)));
                }
            }

            RequestBody requestBody = builder.build();
            final Request request = new Request.Builder().url(task.getUrl())
                    .post(new ProgressRequestBody(requestBody, upload))
                    .tag(UUID.randomUUID().toString())
                    .build();
            final Call call = LongOkHttpClientHolder.INSTANCE.newCall(request);
            if (callback != null) {
                callback.onStart(new HttpCall(call));
            }

            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull final IOException e) {
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
            e.printStackTrace();
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
                    });
            Request request = new Request.Builder().url(url).build();
            final Call call = builder.build().newCall(request);
            if (callback != null) callback.onStart(new HttpCall(call));
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull final IOException e) {
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
            e.printStackTrace();
            if (callback != null) callback.onError(e);
        }
    }
}
