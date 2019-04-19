package com.itql.module.http;

import androidx.annotation.NonNull;

import com.itql.module.http.callback.IProgressCallback;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

public class ProgressResponseBody extends ResponseBody {
    private final ResponseBody mResponseBody;
    private final IProgressCallback.download mDownload;
    private BufferedSource mBufferedSource;

    public ProgressResponseBody(ResponseBody responseBody, IProgressCallback.download download) {
        mResponseBody = responseBody;
        mDownload = download;
    }

    @Override
    public MediaType contentType() {
        return mResponseBody.contentType();
    }

    @Override
    public long contentLength() {
        return mResponseBody.contentLength();
    }

    @NonNull
    @Override
    public BufferedSource source() {
        if (mBufferedSource == null) {
            mBufferedSource = Okio.buffer(source(mResponseBody.source()));
        }
        return mBufferedSource;
    }

    private Source source(Source source) {
        return new ForwardingSource(source) {
            long totalLength = 0L;
            long totalBytesRead = 0L;
            long lastTime = 0L;
            long currentTime = 0L;

            @Override
            public long read(@NonNull Buffer sink, long byteCount) throws IOException {
                long bytesRead = super.read(sink, byteCount);
                if (totalLength == 0L) {
                    totalLength = mResponseBody.contentLength();
                }
                totalBytesRead += bytesRead != -1 ? bytesRead : 0;
                currentTime = System.currentTimeMillis();
                if (currentTime - lastTime >= 1000 || totalBytesRead == totalLength || bytesRead == -1) {
                    mDownload.onDownload(totalLength, totalBytesRead, bytesRead == -1);
                    lastTime = currentTime;
                }
                return bytesRead;
            }
        };
    }
}