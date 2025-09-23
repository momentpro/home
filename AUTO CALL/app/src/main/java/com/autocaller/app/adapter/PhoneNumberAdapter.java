package com.autocaller.app.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.autocaller.app.R;
import com.autocaller.app.model.PhoneNumber;

import java.util.ArrayList;
import java.util.List;

public class PhoneNumberAdapter extends RecyclerView.Adapter<PhoneNumberAdapter.ViewHolder> {
    private List<PhoneNumber> phoneNumbers;

    public PhoneNumberAdapter() {
        this.phoneNumbers = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_phone_number, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PhoneNumber phoneNumber = phoneNumbers.get(position);
        holder.bind(phoneNumber);
    }

    @Override
    public int getItemCount() {
        return phoneNumbers.size();
    }

    public void setPhoneNumbers(List<PhoneNumber> phoneNumbers) {
        this.phoneNumbers = phoneNumbers;
        notifyDataSetChanged();
    }

    public void updatePhoneNumber(int index, PhoneNumber.CallStatus status) {
        if (index >= 0 && index < phoneNumbers.size()) {
            phoneNumbers.get(index).setStatus(status);
            notifyItemChanged(index);
        }
    }

    public List<PhoneNumber> getPhoneNumbers() {
        return phoneNumbers;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPhoneNumber;
        private TextView tvIndex;
        private TextView tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPhoneNumber = itemView.findViewById(R.id.tvPhoneNumber);
            tvIndex = itemView.findViewById(R.id.tvIndex);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }

        public void bind(PhoneNumber phoneNumber) {
            tvPhoneNumber.setText(phoneNumber.getNumber());
            tvIndex.setText((phoneNumber.getIndex() + 1) + "번째");
            tvStatus.setText(phoneNumber.getStatus().getDisplayName());

            // Set status color
            int colorRes;
            switch (phoneNumber.getStatus()) {
                case PENDING:
                    colorRes = R.color.status_pending;
                    break;
                case DIALING:
                    colorRes = R.color.status_dialing;
                    break;
                case COMPLETED:
                    colorRes = R.color.status_completed;
                    break;
                case FAILED:
                    colorRes = R.color.status_failed;
                    break;
                case ANSWERED:
                    colorRes = R.color.status_answered;
                    break;
                default:
                    colorRes = R.color.status_pending;
                    break;
            }

            int color = ContextCompat.getColor(itemView.getContext(), colorRes);
            tvStatus.getBackground().setTint(color);
        }
    }
}

