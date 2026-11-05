package com.code.bumbumapps.netspeed.tools;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.code.bumbumapps.netspeed.CustomAdapter.MyDatabaseHelper;
import com.code.bumbumapps.netspeed.R;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Locale;

/** Summary statistics over the saved speed tests, plus CSV export through the share sheet. */
public class HistoryStatsActivity extends AppCompatActivity {
    private final ArrayList<String> time = new ArrayList<>(), type = new ArrayList<>(),
            ping = new ArrayList<>(), down = new ArrayList<>(), up = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tool_list);
        ToolsUi.header(this, R.string.tool_stats_title);
        LinearLayout content = findViewById(R.id.tool_content);
        Button action = findViewById(R.id.tool_action);
        action.setText(R.string.stats_export);

        MyDatabaseHelper db = new MyDatabaseHelper(this);
        Cursor c = null;
        try {
            c = db.readAllData();
            while (c != null && c.moveToNext()) {
                time.add(c.getString(1));
                type.add(c.getString(2));
                ping.add(c.getString(3));
                down.add(c.getString(4));
                up.add(c.getString(5));
            }
        } finally {
            if (c != null) c.close();
            db.close();
        }

        if (time.isEmpty()) {
            action.setEnabled(false);
            ToolsUi.message(this, content, getString(R.string.stats_empty));
            return;
        }
        HistoryStats s = HistoryStats.of(type, ping, down, up);
        LinearLayout c1 = ToolsUi.card(this, content, getString(R.string.stats_overview));
        ToolsUi.row(this, c1, getString(R.string.stats_tests), String.valueOf(s.count));
        ToolsUi.row(this, c1, "Wi-Fi", String.valueOf(s.wifiCount));
        ToolsUi.row(this, c1, getString(R.string.network_mobile), String.valueOf(s.mobileCount));
        LinearLayout c2 = ToolsUi.card(this, content, getString(R.string.download));
        ToolsUi.row(this, c2, getString(R.string.stats_average), mbps(s.avgDownload));
        ToolsUi.row(this, c2, getString(R.string.stats_best), mbps(s.bestDownload));
        ToolsUi.row(this, c2, getString(R.string.stats_worst), mbps(s.worstDownload));
        LinearLayout c3 = ToolsUi.card(this, content, getString(R.string.upload));
        ToolsUi.row(this, c3, getString(R.string.stats_average), mbps(s.avgUpload));
        ToolsUi.row(this, c3, getString(R.string.stats_best), mbps(s.bestUpload));
        LinearLayout c4 = ToolsUi.card(this, content, getString(R.string.ping));
        ToolsUi.row(this, c4, getString(R.string.stats_average), ms(s.avgPing));
        ToolsUi.row(this, c4, getString(R.string.stats_best), ms(s.bestPing));

        action.setOnClickListener(v -> export());
    }

    private static String mbps(double v) {
        return v < 0 ? "—" : String.format(Locale.US, "%.2f Mbps", v);
    }

    private static String ms(double v) {
        return v < 0 ? "—" : String.format(Locale.US, "%.1f ms", v);
    }

    private void export() {
        try {
            File dir = new File(getCacheDir(), "shared");
            if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("mkdir");
            File f = new File(dir, "speed-test-history.csv");
            try (FileOutputStream o = new FileOutputStream(f)) {
                o.write(HistoryStats.toCsv(time, type, ping, down, up).getBytes(StandardCharsets.UTF_8));
            }
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/csv");
            send.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this, getPackageName() + ".provider", f));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(send, getString(R.string.stats_export)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.stats_export_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
