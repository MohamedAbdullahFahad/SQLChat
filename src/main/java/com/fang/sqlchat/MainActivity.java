package com.fang.sqlchat;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private EditText senderName;
    private EditText receiverName;
    private EditText Url;
    private Button startChat;
    public static String url;
    public static String sender;
    public static String receiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        senderName = findViewById(R.id.senderName);
        Url = findViewById(R.id.url);
        startChat = findViewById(R.id.startChat);
        receiverName = findViewById(R.id.receiverName);
        startChat.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                url = Url.getText().toString();
                sender = senderName.getText().toString();
                receiver = receiverName.getText().toString();
                if(sender.isEmpty() || receiver.isEmpty() || url.isEmpty()){
                    Toast.makeText(MainActivity.this, "Please fill all fields to continue", Toast.LENGTH_SHORT).show();
                }else{
                    Intent chatIntent = new Intent(MainActivity.this,ChatActivity.class);
                    startActivity(chatIntent);
                }
            }
        });
    }
}