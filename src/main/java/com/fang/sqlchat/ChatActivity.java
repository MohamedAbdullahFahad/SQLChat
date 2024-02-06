package com.fang.sqlchat;


import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import okhttp3.OkHttpClient;

public class ChatActivity extends AppCompatActivity {
    public static EditText metText;
    public static Button mbtSent;
    public static List<Chat> mChats;
    private RecyclerView mRecyclerView;
    private ChatAdapter mAdapter;
    private String mId;
    private String text;
    public static Socket socket;
    private String socketId;
    private boolean offline = true;
    public static boolean connected = false;
    private ActionBar actionBar;
    private TrustManager[] certManager;
    private OkHttpClient okHttpClient;
    private HostnameVerifier hostnameVerifier;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        actionBar = getSupportActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#6200EE")));
        metText = findViewById(R.id.sendEditText);
        mbtSent = findViewById(R.id.sendButton);
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mChats = new ArrayList<>();
        mId = Settings.Secure.getString(this.getContentResolver(),Settings.Secure.ANDROID_ID);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ChatAdapter(mChats,mId);
        mRecyclerView.setAdapter(mAdapter);
        hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        certManager = new TrustManager[] { new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};

        connectToServer();
        mbtSent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!connected){
                    for (int i = 0; i <=1; i++) {
                        Toast.makeText(ChatActivity.this, "Could not connect to server\nPossibilities\n1.Server is temporarily down.\n2.Server has been moved to a different location(url).\n3.The server url you have entered is not associated with a fang chat server.\nPlease check your server link and try again\nReturning to previous screen in few seconds", Toast.LENGTH_LONG).show();
                    }
                    try {
                        Thread.sleep(3000);
                        ChatActivity.super.onBackPressed();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                text = metText.getText().toString();
                if (!text.trim().isEmpty()) {
                    if (!offline){
                        try {
                            mId = Settings.Secure.getString(ChatActivity.this.getContentResolver(), Settings.Secure.ANDROID_ID);
                            JSONObject message_to_send = new JSONObject();
                            message_to_send.put("from", MainActivity.sender);
                            message_to_send.put("msg", text);
                            message_to_send.put("to", MainActivity.receiver);
                            message_to_send.put("conn_id", socketId);
                            socket.emit("message", message_to_send.toString());
                            Chat messages = new Chat(text, mId);
                            mChats.add(messages);
                            mRecyclerView.scrollToPosition(mChats.size() - 1);
                            metText.setText("");
                            save_msg_in_server(MainActivity.sender, text,"sent"+MainActivity.receiver);
                        } catch (Exception e) {
                            Toast.makeText(ChatActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                } else {
                    Log.d("ChatActivity","Empty text cannot be sent");
                }
            }
        });


    }

    private void connectToServer(){
        try {
            SSLContext tlscontext = SSLContext.getInstance("TLS");
            tlscontext.init(null,certManager,null);
            okHttpClient = new OkHttpClient.Builder().hostnameVerifier(hostnameVerifier).sslSocketFactory(tlscontext.getSocketFactory()).build();
            IO.setDefaultOkHttpWebSocketFactory(okHttpClient);
            IO.setDefaultOkHttpCallFactory(okHttpClient);
            IO.Options socketOptions = new IO.Options();
            socketOptions.callFactory = okHttpClient;
            socketOptions.webSocketFactory = okHttpClient;
            socket = IO.socket(MainActivity.url,socketOptions);
            socket.on("message", messageReceiver);
            socket.on("get_sid", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    runOnUiThread(() -> {
                        try{
                            connected = true;
                            actionBar.setSubtitle("Chatting with "+MainActivity.receiver);
                            Toast.makeText(ChatActivity.this, "Chatting with "+MainActivity.receiver, Toast.LENGTH_SHORT).show();
                            socketId = args[1].toString();
                            JSONObject join_me = new JSONObject();
                            join_me.put("username",MainActivity.sender);
                            join_me.put("conn_id",socketId);
                            join_me.put("sending_to",MainActivity.receiver);
                            socket.emit("join",join_me.toString());
                        }catch (Exception e){
                            Toast.makeText(ChatActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
            socket.on("offline", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    runOnUiThread(() -> {
                        try {
                            offline = true;
                            Toast.makeText(ChatActivity.this, args[0].toString(), Toast.LENGTH_SHORT).show();
                        }catch (Exception e){
                            Toast.makeText(ChatActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            socket.on("online", new Emitter.Listener() {
                @Override
                public void call(Object... args) {
                    try {
                        offline = false;
                        runOnUiThread(() -> {
                            if(args[0].toString().equals("connected")){
                                System.out.print("connected");
                            }else {
                                Toast.makeText(ChatActivity.this, MainActivity.receiver + " is back online", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
            socket.connect();
        } catch (Exception e){
            if(e.getClass().getName().equals("java.lang.RuntimeException")){
                Toast.makeText(this, "Wrong url format", Toast.LENGTH_SHORT).show();
                finish();
            }else{
                Toast.makeText(this, e.getClass().getName(), Toast.LENGTH_SHORT).show();
            }
        }

    }

    private final Emitter.Listener messageReceiver = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(() -> {
                offline = false;
                String message = args[0].toString();
                mId = "received";
                Chat messages = new Chat(message,mId);
                mChats.add(messages);
                mRecyclerView.scrollToPosition(mChats.size() - 1);
                save_msg_in_server(MainActivity.sender,message,"received"+MainActivity.receiver);
            });
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.history_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.historyMenuIcon) {
            Intent historyActivityIntent = new Intent(ChatActivity.this, HistoryActivity.class);
            startActivity(historyActivityIntent);
            return true;
        }else{
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(socket != null) {
            socket.disconnect();
            socket.off("message", messageReceiver);
            socket.off("get_sid");
            socket.off("offline");
            socket.off("online");
        }
    }

    private void save_msg_in_server(String name, String msg, String pos) {
        JSONObject message_to_save = new JSONObject();
        try {
            message_to_save.put("name", name);
            message_to_save.put("msg", msg);
            message_to_save.put("pos", pos);
            socket.emit("save_msg", message_to_save.toString());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        socket.connect();
    }
}