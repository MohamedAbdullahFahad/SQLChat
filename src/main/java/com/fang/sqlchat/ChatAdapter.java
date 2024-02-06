package com.fang.sqlchat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {
    private static final int CHAT_START = 2;
    private static final int CHAT_END = 1;
    private List<Chat> mDataSet;
    private String mId;
    public static TextView changeTextColor;
    ChatAdapter(List<Chat> dataSet, String id) {
        mDataSet = dataSet;
        mId = id;
    }

    @Override
    public ChatAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v;
        if (viewType == CHAT_END) {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.sent, parent, false);
        } else {
            v = LayoutInflater.from(parent.getContext()).inflate(R.layout.received, parent, false);
        }

        return new ViewHolder(v);
    }

    @Override
    public int getItemViewType(int position) {
        if (mDataSet.get(position).getId().equals(mId)) {
            return CHAT_END;
        }

        return CHAT_START;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Chat chat = mDataSet.get(position);
        holder.mTextView.setText(chat.getMessage());
    }

    @Override
    public int getItemCount() {
        return mDataSet.size();
    }


    class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTextView;

        ViewHolder(View v) {
            super(v);
            mTextView = (TextView) itemView.findViewById(R.id.Message);
            changeTextColor = mTextView;
        }
    }
}

