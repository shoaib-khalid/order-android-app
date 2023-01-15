package com.symplified.order.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.symplified.order.R;
import com.symplified.order.models.staff.StaffMember;

import java.util.ArrayList;
import java.util.List;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.ViewHolder> {

    private List<StaffMember> staffMembers = new ArrayList<>();

    public StaffAdapter() {
    }

    public StaffAdapter(List<StaffMember> staffMembers) {
        this.staffMembers = staffMembers;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView, usernameTextView;
        private final ImageButton editButton, deleteButton;

        public ViewHolder(View view) {
            super(view);

            nameTextView = view.findViewById(R.id.name_text_view);
            usernameTextView = view.findViewById(R.id.username_text_view);
            editButton = view.findViewById(R.id.edit_button);
            deleteButton = view.findViewById(R.id.delete_button);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new ViewHolder(
                LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.row_staff_member, viewGroup, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        viewHolder.nameTextView.setText(staffMembers.get(position).name);
        viewHolder.usernameTextView.setText(staffMembers.get(position).username);
        viewHolder.editButton.setOnClickListener(view -> {
        });
        viewHolder.deleteButton.setOnClickListener(view -> {
        });
    }

    @Override
    public int getItemCount() {
        return staffMembers.size();
    }

    public void addStaffMembers(List<StaffMember> staffMembersToAdd) {
        int originalEndPosition = staffMembers.size() > 0 ? staffMembers.size() - 1 : 0;
        if (staffMembers.addAll(staffMembersToAdd)) {
            notifyItemRangeInserted(originalEndPosition, staffMembersToAdd.size());
        }
    }

    public void clear() {
        int originalSize = staffMembers.size();
        staffMembers.clear();
        notifyItemRangeRemoved(0, originalSize);
    }
}
