package com.example.bluetooth;

import android.app.Activity;
import android.os.Bundle;

public class ChatActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        ChatFragment frag = new ChatFragment();
        frag.setArguments(getIntent().getExtras());
        getFragmentManager().beginTransaction().add(R.id.root, frag, "ChatFragment").commit();
    }

}
