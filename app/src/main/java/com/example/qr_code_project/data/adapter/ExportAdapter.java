package com.example.qr_code_project.data.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qr_code_project.R;
import com.example.qr_code_project.data.modal.ExportModal;

import java.util.ArrayList;

public class ExportAdapter extends RecyclerView.Adapter<ExportAdapter.ExportViewHolder> {
    private final Context context;
    private final ArrayList<ExportModal> exportList;
    private final OnExportClickListener onExportClickListener;

    public ExportAdapter(Context context, ArrayList<ExportModal> exportList, OnExportClickListener onExportClickListener) {
        this.context = context;
        this.exportList = exportList;
        this.onExportClickListener = onExportClickListener;
    }

    @NonNull
    @Override
    public ExportAdapter.ExportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_export, parent, false);
        return new ExportViewHolder(view);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void onBindViewHolder(@NonNull ExportAdapter.ExportViewHolder holder, int position) {
        ExportModal exportModal = exportList.get(position);

        holder.codeEp.setText(String.format(exportModal.getCodeEp()));
        holder.items.setText(String.format(String.valueOf(exportModal.getItems())));
        holder.totalItem.setText(String.format(String.valueOf(exportModal.getTotalItem())));

        holder.itemView.setOnClickListener(v -> onExportClickListener.onExportClick(exportModal));
    }


    @Override
    public int getItemCount() {
        return exportList != null ? exportList.size() : 0;
    }


    static class ExportViewHolder extends RecyclerView.ViewHolder {
        TextView codeEp, items, totalItem;

        public ExportViewHolder(@NonNull View itemView) {
            super(itemView);
            codeEp = itemView.findViewById(R.id.code);
            items = itemView.findViewById(R.id.items);
            totalItem = itemView.findViewById(R.id.totalItems);
        }
    }

    public interface OnExportClickListener {
        void onExportClick(ExportModal exportModal);
    }
}
