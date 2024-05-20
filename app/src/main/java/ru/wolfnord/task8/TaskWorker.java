package ru.wolfnord.task8;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Random;

public class TaskWorker extends Worker {
    private final Context mContext;
    public TaskWorker(@NonNull Context context, @NonNull WorkerParameters workerParameters) {
        super(context, workerParameters);
        mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        Random random = new Random();
        int randomNumber = random.nextInt(100);

        String startMessage = "Старт последовательной задачи " + randomNumber;

        showToast(startMessage);

        String endMessage = "Завершение последовательной задачи " + randomNumber;
        showToast(endMessage);

        return Result.success();
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(mContext, message, Toast.LENGTH_LONG).show());
    }
}