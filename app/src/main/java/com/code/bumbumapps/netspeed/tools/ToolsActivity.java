package com.code.bumbumapps.netspeed.tools;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.code.bumbumapps.netspeed.R;

/** Hub for the extra network tools, opened from the main screen. */
public class ToolsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tools);
        ToolsUi.header(this, R.string.tools_title);
        findViewById(R.id.card_network).setOnClickListener(
                v -> startActivity(new Intent(this, NetworkDetailsActivity.class)));
        findViewById(R.id.card_latency).setOnClickListener(
                v -> startActivity(new Intent(this, LatencyMonitorActivity.class)));
        findViewById(R.id.card_stats).setOnClickListener(
                v -> startActivity(new Intent(this, HistoryStatsActivity.class)));
    }
}
