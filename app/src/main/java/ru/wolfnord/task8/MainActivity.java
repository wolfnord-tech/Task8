package ru.wolfnord.task8;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
        Button loadButtonImage = findViewById(R.id.button_image);
        Button buttonTaskWorker = findViewById(R.id.button_task_worker);
        Button buttonParallelTaskWorker = findViewById(R.id.button_parallel_task_worker);

        buttonTaskWorker.setOnClickListener(v -> startSequentialWork());
        buttonParallelTaskWorker.setOnClickListener(v -> startParallelWork());

        // Использование WeakReference для предотвращения утечки контекста:
        loadButtonImage.setOnClickListener(v -> new DownloadImageTask(new WeakReference<>(imageView)).executeOnExecutor(Executors.newFixedThreadPool(2), "https://random.dog/woof.json"));
    }

    private void startSequentialWork() {
        // Создаем рабочий запрос для первой задачи
        OneTimeWorkRequest workRequest1 = new OneTimeWorkRequest.Builder(TaskWorker.class)
                .setInitialDelay(0, TimeUnit.MILLISECONDS)
                .build();

        // Создаем рабочий запрос для второй задачи
        OneTimeWorkRequest workRequest2 = new OneTimeWorkRequest.Builder(TaskWorker.class)
                .setInitialDelay(0, TimeUnit.MILLISECONDS)
                .build();

        // Создаем рабочий запрос для третьей задачи
        OneTimeWorkRequest workRequest3 = new OneTimeWorkRequest.Builder(TaskWorker.class)
                .setInitialDelay(0, TimeUnit.MILLISECONDS)
                .build();

        // Устанавливаем зависимости между задачами, чтобы они выполнялись последовательно
        WorkManager.getInstance(this)
                .beginWith(workRequest1)
                .then(workRequest2)
                .then(workRequest3)
                .enqueue();

        Toast.makeText(this, "Задачи поставлены в очередь последовательно", Toast.LENGTH_SHORT).show();
    }

    private void startParallelWork() {
        // Создаем рабочий запрос для первой параллельной задачи
        OneTimeWorkRequest parallelWorkRequest1 = new OneTimeWorkRequest.Builder(ParallelTaskWorker.class)
                .setInitialDelay(0, TimeUnit.MILLISECONDS)
                .build();

        // Создаем рабочий запрос для второй параллельной задачи
        OneTimeWorkRequest parallelWorkRequest2 = new OneTimeWorkRequest.Builder(ParallelTaskWorker.class)
                .setInitialDelay(0, TimeUnit.MILLISECONDS)
                .build();

        // Ставим обе параллельные задачи в очередь
        WorkManager.getInstance(this)
                .beginWith(Arrays.asList(parallelWorkRequest1, parallelWorkRequest2))
                .enqueue();

        Toast.makeText(this, "Параллельные задачи поставлены в очередь", Toast.LENGTH_SHORT).show();
    }

    // Используем WeakReference для ImageView, чтобы избежать утечек памяти:
    private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        private final WeakReference<ImageView> imageViewWeakReference;
        private HttpURLConnection urlConnection;

        public DownloadImageTask(WeakReference<ImageView> imageViewWeakReference) {
            this.imageViewWeakReference = imageViewWeakReference;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            Bitmap bitmap = null;
            try {
                urlConnection = (HttpURLConnection) new URL(url).openConnection();
                urlConnection.connect();
                if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    InputStream inputStream = urlConnection.getInputStream();
                    String jsonResponse = convertInputStreamToString(inputStream);
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    String imageUrl = jsonObject.getString("url");
                    bitmap = downloadBitmap(imageUrl);
                }
            } catch (IOException | JSONException e) {
                Log.e("DownloadImageTask", "Error: ", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            ImageView imageView = imageViewWeakReference.get();
            if (imageView != null && bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }

        private String convertInputStreamToString(InputStream inputStream) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            return stringBuilder.toString();
        }

        private Bitmap downloadBitmap(String imageUrl) throws IOException {
            urlConnection = (HttpURLConnection) new URL(imageUrl).openConnection();
            urlConnection.connect();
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = urlConnection.getInputStream();
                return BitmapFactory.decodeStream(inputStream);
            }
            return null;
        }
    }
}
