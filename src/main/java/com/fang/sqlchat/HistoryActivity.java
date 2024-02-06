package com.fang.sqlchat;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class HistoryActivity extends AppCompatActivity {
    private List<Chat> historyChats;
    private RecyclerView historyRecyclerView;
    private ChatAdapter historyAdapter;
    private String androidID;
    private String dupID;
    private Socket historySocket;
    private ActionBar actionBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#6200EE")));
        androidID = Settings.Secure.getString(this.getContentResolver(),Settings.Secure.ANDROID_ID);
        historyRecyclerView = findViewById(R.id.historyRecyclerView);
        historyChats = new ArrayList<>();
        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new ChatAdapter(historyChats, androidID);
        historyRecyclerView.setAdapter(historyAdapter);
        if (ChatActivity.connected == false){
            Toast.makeText(HistoryActivity.this, "Couldn't retrieve history\nPossibilities\n1.Server is temporarily down.\n2.Server has been moved to a different location(url).\n3.The server url you have entered is not associated with a fang chat server.\nPlease check your server link and try again.", Toast.LENGTH_LONG).show();
            super.onBackPressed();
        }
        try {
            historySocket = IO.socket(MainActivity.url);
            historySocket.on("get_history", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    runOnUiThread(() -> {
                        JSONObject historyMessage = (JSONObject) args[0];
                        try {
                            String loaded = historyMessage.getString("loaded");
                            if(loaded.equals("no")){
                                String message = historyMessage.getString("msg");
                                String pos = historyMessage.getString("pos");
                                if (pos.equals("sent"+MainActivity.receiver)){
                                    dupID = androidID;
                                    addMessageToRecyclerView(message,dupID);
                                }else if(pos.equals("received"+MainActivity.receiver)){
                                    dupID = "";
                                    addMessageToRecyclerView(message,dupID);
                                }
                            }else if(loaded.equals("yes")){
                                Toast.makeText(HistoryActivity.this, "Loaded history", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(HistoryActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        }
//                        Toast.makeText(HistoryActivity.this, historyMessage.toString(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
            historySocket.connect();
            JSONObject getHistory = new JSONObject();
            getHistory.put("sender",MainActivity.sender);
            getHistory.put("receiver",MainActivity.receiver);
            historySocket.emit("get_history",getHistory.toString());
        }catch (Exception e){
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void addMessageToRecyclerView(String text, String id){
        Chat messages = new Chat(text, id);
        historyChats.add(messages);
        historyRecyclerView.scrollToPosition(historyChats.size() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(historySocket != null){
            historySocket.disconnect();
            historySocket.off("get_history");
        }
    }
}
