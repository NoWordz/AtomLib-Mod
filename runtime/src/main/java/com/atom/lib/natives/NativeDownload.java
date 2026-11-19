// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.natives;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

final class NativeDownload {
    private static final Duration IDLE_TIMEOUT = Duration.ofSeconds(5);
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    void fetch(URI uri, Path target, long size, long deadline) throws IOException, InterruptedException {
        var activity = new AtomicLong(System.nanoTime());
        var request = HttpRequest.newBuilder(uri).GET().timeout(IDLE_TIMEOUT).build();
        try (var body = new BoundedFile(target, size, activity)) {
            var transfer = client.sendAsync(request, response -> {
                if (response.statusCode() != 200) {
                    throw new UncheckedIOException(new IOException("HTTP " + response.statusCode()));
                }
                if (response.headers().firstValueAsLong("Content-Length").orElse(size) != size) {
                    throw new UncheckedIOException(new IOException("Unexpected download length"));
                }
                activity.set(System.nanoTime());
                return body;
            });
            try {
                await(transfer, activity, deadline);
            } finally {
                transfer.cancel(true);
            }
        }
    }

    private static void await(CompletableFuture<?> transfer, AtomicLong activity, long deadline)
            throws IOException, InterruptedException {
        try {
            while (true) {
                long now = System.nanoTime();
                long timeLeft = deadline - now;
                if (timeLeft <= 0) throw new IOException("Native download timed out");
                long idleLeft = activity.get() + IDLE_TIMEOUT.toNanos() - now;
                if (idleLeft <= 0) throw new IOException("Native download stalled");
                try {
                    transfer.get(Math.min(timeLeft, idleLeft), TimeUnit.NANOSECONDS);
                    return;
                } catch (TimeoutException ignored) {
                    // Progress after get() began may have extended the idle deadline.
                }
            }
        } catch (ExecutionException failed) {
            Throwable cause = failed.getCause();
            if (cause instanceof UncheckedIOException wrapped) throw wrapped.getCause();
            throw new IOException(cause.getMessage(), cause);
        }
    }

    private static final class BoundedFile implements HttpResponse.BodySubscriber<Void>, AutoCloseable {
        private final CompletableFuture<Void> body = new CompletableFuture<>();
        private final FileChannel file;
        private final AtomicLong activity;
        private long remaining;
        private Flow.Subscription subscription;

        private BoundedFile(Path target, long size, AtomicLong activity) throws IOException {
            file = FileChannel.open(target, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
            remaining = size;
            this.activity = activity;
        }

        @Override public CompletionStage<Void> getBody() { return body; }
        @Override public synchronized void onSubscribe(Flow.Subscription subscription) {
            if (body.isDone()) {
                subscription.cancel();
                return;
            }
            this.subscription = subscription;
            subscription.request(1);
        }
        @Override public synchronized void onNext(List<ByteBuffer> buffers) {
            if (body.isDone()) return;
            try {
                for (ByteBuffer buffer : buffers) {
                    int bytes = buffer.remaining();
                    if (bytes > remaining) throw new IOException("Native download exceeds its expected length");
                    remaining -= bytes;
                    if (bytes > 0) activity.set(System.nanoTime());
                    while (buffer.hasRemaining()) file.write(buffer);
                }
                subscription.request(1);
            } catch (IOException error) {
                subscription.cancel();
                onError(error);
            }
        }
        @Override public synchronized void onError(Throwable error) {
            body.completeExceptionally(error);
        }
        @Override public synchronized void onComplete() {
            if (remaining == 0) body.complete(null);
            else onError(new IOException("Native download ended early"));
        }
        @Override public synchronized void close() throws IOException {
            // Prevent writes from a cancelled response after the caller deletes the temporary file.
            body.cancel(false);
            file.close();
        }
    }
}
