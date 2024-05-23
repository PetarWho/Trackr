package com.fn2101681010.Trackr;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private List<String> groupList;

    public GroupAdapter(List<String> groupList) {
        this.groupList = groupList;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.group_item_layout, parent, false); // Use your custom layout here
        return new GroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        holder.bind(groupList.get(position));
    }

    @Override
    public int getItemCount() {
        return groupList.size();
    }

    static class GroupViewHolder extends RecyclerView.ViewHolder {
        TextView groupNameTextView;
        ImageButton editGroupItemButton;

        GroupViewHolder(@NonNull View itemView) {
            super(itemView);
            groupNameTextView = itemView.findViewById(R.id.groupNameTextView);
            editGroupItemButton = itemView.findViewById(R.id.editGroupItem);

            editGroupItemButton.setOnClickListener(v -> {
                Context context = itemView.getContext();

                // Get the group name and passcode from the TextView
                String groupName = groupNameTextView.getText().toString().trim();
                String passcode = groupNameTextView.getText().toString().trim();

                Intent intent = new Intent(context, EditGroupActivity.class);
                // Pass the group name and passcode as extras in the intent
                intent.putExtra("groupName", groupName);

                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                context.startActivity(intent);
            });

        }

        void bind(String groupName) {
            groupNameTextView.setText(groupName);
        }
    }

}
