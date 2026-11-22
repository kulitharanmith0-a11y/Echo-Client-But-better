package dev.dov.hack.echo.client.config;

import com.google.gson.JsonObject;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ProfileIo implements AutoCloseable {
    private final ProfileService profiles;
    private final ExecutorService executor;
    private volatile Thread ioThread;

    
    public ProfileIo(ProfileService profiles) {
        this.profiles = Objects.requireNonNull(profiles, "profiles");
        executor = Executors.newSingleThreadExecutor(this::createThread);
    }

    private Thread createThread(Runnable task) {
        Thread thread = Thread.ofPlatform().name("Worker-IO").daemon(true).unstarted(task);
        ioThread = thread;
        return thread;
    }

    
    public boolean isAvailable() {
        try {
            return Boolean.TRUE.equals(call(profiles::isAvailable));
        } catch (UnsatisfiedLinkError error) {
            return false;
        }
    }

    
    public JsonObject listProfiles() {
        return call(profiles::listProfiles);
    }

    
    public JsonObject getProfile(String id) {
        return call(() -> profiles.getProfile(id));
    }

    
    public JsonObject saveProfile(ProfileStore.SaveRequest request) {
        return call(() -> profiles.saveProfile(request));
    }

    
    public JsonObject selectProfile(String id) {
        return call(() -> profiles.selectProfile(id));
    }

    
    public JsonObject deleteProfile(String id) {
        return call(() -> profiles.deleteProfile(id));
    }

    
    public CompletableFuture<JsonObject> listProfilesAsync() {
        return CompletableFuture.supplyAsync(this::listProfiles, executor);
    }

    
    public void execute(Runnable operation) {
        executor.execute(operation);
    }

    
    public <T> CompletableFuture<T> submit(Supplier<T> operation) {
        return CompletableFuture.supplyAsync(operation, executor);
    }

    private <T> T call(Supplier<T> operation) {
        if (Thread.currentThread() == ioThread) {
            return operation.get();
        }
        try {
            return executor.submit(operation::get).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof UnsatisfiedLinkError error) {
                throw error;
            }
            return null;
        }
    }

    
    @Override
    public void close() {
        executor.shutdown();
        try {
            executor.awaitTermination(2L, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
