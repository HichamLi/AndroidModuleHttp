package com.itql.module.http;

import androidx.annotation.NonNull;

import com.itql.module.http.callback.IProgressCallback;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;


public class ProgressRequestBody extends RequestBody {
    private final RequestBody mRequestBody;
    private final IProgressCallback.upload mUpload;
    private BufferedSink mBufferedSink;

    public ProgressRequestBody(RequestBody requestBody, IProgressCallback.upload upload) {
        mRequestBody = requestBody;
        mUpload = upload;
    }

    @Override
    public MediaType contentType() {
        return mRequestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return mRequestBody.contentLength();
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        if (mBufferedSink == null) {
            mBufferedSink = Okio.buffer(sink(sink));
        }
        mRequestBody.writeTo(mBufferedSink);
        mBufferedSink.flush();
    }

    private Sink sink(Sink sink) {
        return new ForwardingSink(sink) {
            //当前写入字节数
            long bytesWritten = 0L;
            long lastTime = 0L;
            long currentTime = 0L;
            long contentLength = 0L;

            @Override
            public void write(@NonNull Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                if (contentLength == 0) {
                    contentLength = contentLength();
                }
                bytesWritten += byteCount;
                currentTime = System.currentTimeMillis();
                if (currentTime - lastTime >= 1000 || bytesWritten == contentLength) {
                    mUpload.onUpload(contentLength, bytesWritten, bytesWritten >= contentLength);
                    lastTime = currentTime;
                }
            }
        };
    }
}
