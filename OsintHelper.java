package org.telegram.ui;

import android.content.Context;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class OsintHelper {

    private static final String BASE_URL = "https://infinity-search.fun/find.php";
    private static final String API_TOKEN = "QoNm98UeMLIqNjZ198snm98AdGvhqA88";

    // Поиск по номеру телефона (из профиля или ввода)
    public static void searchByPhone(BaseFragment fragment, String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            showSimpleAlert(fragment.getParentActivity(), "Ошибка", "У пользователя скрыт или отсутствует номер телефона.");
            return;
        }
        String cleanPhone = phone.replaceAll("[^0-9]", "");
        runRequest(fragment.getParentActivity(), BASE_URL + "?phone=" + cleanPhone + "&token=" + API_TOKEN);
    }

    // Поиск по Email
    public static void searchByEmail(BaseFragment fragment, String email) {
        try {
            String encoded = URLEncoder.encode(email.trim(), "UTF-8");
            runRequest(fragment.getParentActivity(), BASE_URL + "?email=" + encoded + "&token=" + API_TOKEN);
        } catch (Exception e) {
            showSimpleAlert(fragment.getParentActivity(), "Ошибка", e.getMessage());
        }
    }

    // Поиск по ФИО и дате рождения
    public static void searchByFio(BaseFragment fragment, String fio, String bdate) {
        try {
            String encodedFio = URLEncoder.encode(fio.trim(), "UTF-8");
            String encodedDate = URLEncoder.encode(bdate.trim(), "UTF-8");
            String url = BASE_URL + "?fio=" + encodedFio;
            if (!bdate.trim().isEmpty()) {
                url += "&bdate=" + encodedDate;
            }
            url += "&token=" + API_TOKEN;
            runRequest(fragment.getParentActivity(), url);
        } catch (Exception e) {
            showSimpleAlert(fragment.getParentActivity(), "Ошибка", e.getMessage());
        }
    }

    // Меню ручного поиска (вызывается из настроек)
    public static void showManualSearchDialog(BaseFragment fragment) {
        Context context = fragment.getParentActivity();
        if (context == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("OSINT Поиск");
        String[] options = new String[]{"По номеру телефона", "По Email", "По ФИО"};

        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                showInputDialog(fragment, "Поиск по номеру", "Пример: 79277231370", (val1, val2) -> searchByPhone(fragment, val1));
            } else if (which == 1) {
                showInputDialog(fragment, "Поиск по Email", "Пример: test@mail.ru", (val1, val2) -> searchByEmail(fragment, val1));
            } else if (which == 2) {
                showFioInputDialog(fragment);
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private interface SearchCallback {
        void onSearch(String val1, String val2);
    }

    private static void showInputDialog(BaseFragment fragment, String title, String hint, SearchCallback callback) {
        Context context = fragment.getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        final EditText input = new EditText(context);
        input.setHint(hint);
        input.setPadding(40, 30, 40, 30);
        builder.setView(input);

        builder.setPositiveButton("Искать", (dialog, which) -> {
            String text = input.getText().toString();
            if (!text.isEmpty()) callback.onSearch(text, "");
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private static void showFioInputDialog(BaseFragment fragment) {
        Context context = fragment.getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Поиск по ФИО");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 20);

        final EditText fioInput = new EditText(context);
        fioInput.setHint("ФИО (Иванов Иван Иванович)");
        layout.addView(fioInput);

        final EditText bdateInput = new EditText(context);
        bdateInput.setHint("Дата рождения (15.03.1985, необязательно)");
        layout.addView(bdateInput);

        builder.setView(layout);

        builder.setPositiveButton("Искать", (dialog, which) -> {
            String fio = fioInput.getText().toString();
            String bdate = bdateInput.getText().toString();
            if (!fio.isEmpty()) searchByFio(fragment, fio, bdate);
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }

    private static void runRequest(Context context, String requestUrl) {
        if (context == null) return;

        AlertDialog progressDialog = new AlertDialog(context, 3);
        progressDialog.setMessage("Запрос к базе...");
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        new Thread(() -> {
            String resultText;
            try {
                URL url = new URL(requestUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);

                int code = conn.getResponseCode();
                if (code == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    resultText = sb.toString();
                } else {
                    resultText = "Ошибка сервера API (код ответа: " + code + ")";
                }
            } catch (Exception e) {
                resultText = "Ошибка подключения: " + e.getMessage();
            }

            final String res = resultText;
            AndroidUtilities.runOnUIThread(() -> {
                progressDialog.dismiss();
                showResultDialog(context, res);
            });
        }).start();
    }

    private static void showResultDialog(Context context, String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Результат поиска");

        ScrollView scrollView = new ScrollView(context);
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextIsSelectable(true);
        textView.setPadding(40, 30, 40, 30);
        scrollView.addView(textView);

        builder.setView(scrollView);
        builder.setPositiveButton("Закрыть", null);
        builder.show();
    }

    private static void showSimpleAlert(Context context, String title, String msg) {
        if (context == null) return;
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton("ОК", null)
                .show();
    }
}
