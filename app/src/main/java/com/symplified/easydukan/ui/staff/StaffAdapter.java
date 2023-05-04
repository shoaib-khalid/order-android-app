package com.symplified.easydukan.ui.staff;

import android.app.AlertDialog;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.symplified.easydukan.R;
import com.symplified.easydukan.models.staff.StaffMember;
import com.symplified.easydukan.networking.ServiceGenerator;
import com.symplified.easydukan.utils.EmptyCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffAdapter extends RecyclerView.Adapter<StaffAdapter.ViewHolder> {

    public interface OnShowPasswordChangeDialogListener {
        void onShowChangePasswordDialog(StaffMember staffMember);
    }

    private List<StaffMember> staffMembers = new ArrayList<>();
    private Map<String, String> stores = new HashMap<>();
    private final OnShowPasswordChangeDialogListener listener;

    public StaffAdapter(OnShowPasswordChangeDialogListener listener) {
        this.listener = listener;
    }

    public StaffAdapter(
            List<StaffMember> staffMembers,
            OnShowPasswordChangeDialogListener listener
    ) {
        this.staffMembers = staffMembers;
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView, usernameTextView, storeNameTextView;
        private final ImageButton editButton, deleteButton;
        private final ProgressBar progressBar;

        public ViewHolder(View view) {
            super(view);

            nameTextView = view.findViewById(R.id.name_text_view);
            usernameTextView = view.findViewById(R.id.username_text_view);
            storeNameTextView = view.findViewById(R.id.store_name_text_view);
            editButton = view.findViewById(R.id.edit_button);
            deleteButton = view.findViewById(R.id.delete_button);
            progressBar = view.findViewById(R.id.progress_bar);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                .inflate(
                        R.layout.row_staff_member,
                        viewGroup,
                        false
                ));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        viewHolder.nameTextView.setText(staffMembers.get(position).name);
        viewHolder.usernameTextView.setText(staffMembers.get(position).username);
        viewHolder.storeNameTextView.setText(stores.get(staffMembers.get(position).storeId));
        viewHolder.editButton.setOnClickListener(view -> {
            listener.onShowChangePasswordDialog(staffMembers.get(viewHolder.getAdapterPosition()));
        });
        viewHolder.deleteButton.setOnClickListener(view -> {
            StaffMember selectedStaffMember = staffMembers.get(viewHolder.getAdapterPosition());

            new AlertDialog.Builder(view.getContext())
                    .setTitle("Delete Staff Member")
                    .setMessage("Are you sure you want to delete staff member " + selectedStaffMember.name + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        ServiceGenerator
                                .createStaffService(view.getContext().getApplicationContext())
                                .deleteStaffMember(selectedStaffMember.storeId, selectedStaffMember.id)
                                .clone().enqueue(new EmptyCallback());

                        staffMembers.remove(viewHolder.getAdapterPosition());
                        notifyItemRemoved(viewHolder.getAdapterPosition());
                    })
                    .setNegativeButton("No", null)
                    .setIcon(R.drawable.ic_delete)
                    .show();
        });

        if (Build.VERSION.SDK_INT < 26) {
            viewHolder.editButton.setOnLongClickListener(v -> {
                Toast.makeText(v.getContext(), v.getContext().getString(R.string.change_password), Toast.LENGTH_SHORT).show();
                return true;
            });
            viewHolder.deleteButton.setOnLongClickListener(v -> {
                Toast.makeText(v.getContext(), v.getContext().getString(R.string.delete_staff_member), Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        viewHolder.progressBar.setVisibility(staffMembers.get(position).isLoading ? View.VISIBLE : View.GONE);
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

    public void addStaffMember(StaffMember staffMemberToAdd) {
        if (staffMembers.add(staffMemberToAdd)) {
            notifyItemInserted(staffMembers.size() - 1);
        }
    }

    public void clear() {
        int originalSize = staffMembers.size();
        staffMembers.clear();
        notifyItemRangeRemoved(0, originalSize);
    }

    public void setLoadingStatus(StaffMember selectedStaffMember, Boolean isLoading) {
        for (StaffMember staffMember : staffMembers) {
            if (selectedStaffMember == staffMember) {
                staffMember.isLoading = isLoading;
                notifyItemChanged(staffMembers.indexOf(staffMember));
            }
        }
    }

    public void setStores(Map<String, String> stores) {
        this.stores = stores;
    }
}
