package com.example.qr_code_project.data.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qr_code_project.R;
import com.example.qr_code_project.data.modal.SwapModal;

import java.util.ArrayList;

public class SwapLocationAdapter extends RecyclerView.Adapter<SwapLocationAdapter.HolderPost> {

    private final Context context;
    private final ArrayList<SwapModal> swapModals;
//    private final Map<Integer, Object> realQuantitiesMap;
//    private final SwapLocationAdapter.OnProductClickListener onProductClickListener;
    private final OnSwapClickListener listener;

    public SwapLocationAdapter(Context context, ArrayList<SwapModal> swapModals
            ,OnSwapClickListener listener) {
        this.context = context;
        this.swapModals = swapModals;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SwapLocationAdapter.HolderPost onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_swap_location, parent, false);
        return new SwapLocationAdapter.HolderPost(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SwapLocationAdapter.HolderPost holder, int position) {

        SwapModal swapModal = swapModals.get(position);

        if (swapModal != null) {
            int swapId = swapModal.getId();
            String swapTitle = swapModal.getTitle();
            String locationOldCode = swapModal.getLocationOldCode();
            String locationNewCode = swapModal.getLocationNewCode();
            String warehouseOld = swapModal.getWarehouseOld();
            String areaOld = swapModal.getAreaOld();
            String floorOld = swapModal.getFloorOld();
            String warehouse = swapModal.getWarehouse();
            String area = swapModal.getArea();
            String floor = swapModal.getFloor();
            String shelfOld = swapModal.getShelfOld();
            String shelfNew = swapModal.getShelfNew();

            // Gán dữ liệu vào các TextView
            holder.id.setText(String.valueOf(swapId));
            holder.locationOldCode.setText(String.valueOf(locationOldCode));
            holder.locationNewCode.setText(String.valueOf(locationNewCode));
//            holder.warehouseOld.setText(String.valueOf(warehouseOld));
            holder.areaOld.setText(String.valueOf(areaOld));
            holder.floorOld.setText(String.valueOf(floorOld));
//            holder.warehouseNew.setText(String.valueOf(warehouse));
            holder.floorNew.setText(String.valueOf(floor));
            holder.areaNew.setText(String.valueOf(area));
            holder.shelfOld.setText(String.valueOf(shelfOld));
            holder.shelfNew.setText(String.valueOf(shelfNew));

            // Xử lý khi người dùng click vào item
            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSwapItemClick(swapId);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return swapModals.size();
    }


    static class HolderPost extends RecyclerView.ViewHolder {
        ImageButton moreSwapBtn;
        TextView locationOldCode, warehouseOld, floorOld, areaOld,shelfOld;
        TextView locationNewCode, warehouseNew, floorNew, areaNew,shelfNew,id;
//        ImageView swapImage;

        public HolderPost(@NonNull View itemView) {
            super(itemView);
            moreSwapBtn = itemView.findViewById(R.id.moreSwapBtn);
            locationOldCode = itemView.findViewById(R.id.locationOldCode);
//            warehouseOld = itemView.findViewById(R.id.warehouseOld);
            floorOld = itemView.findViewById(R.id.floorOld);
            areaOld = itemView.findViewById(R.id.areaOld);
            locationNewCode = itemView.findViewById(R.id.locationNewCode);
//            warehouseNew = itemView.findViewById(R.id.warehouseNew);
            floorNew = itemView.findViewById(R.id.floorNew);
            areaNew = itemView.findViewById(R.id.areaNew);
            shelfOld = itemView.findViewById(R.id.shelfOld);
            shelfNew = itemView.findViewById(R.id.shelfNew);
            id = itemView.findViewById(R.id.id);
        }
    }

    public interface OnSwapClickListener {
        void onSwapItemClick(int swapId);
    }

}
