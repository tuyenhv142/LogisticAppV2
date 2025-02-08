package com.example.qr_code_project.data.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qr_code_project.R;
import com.example.qr_code_project.data.modal.ProductModal;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Map;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.HolderPost> {

    private final Context context;
    private final ArrayList<ProductModal> productModals;
    private final Map<Integer, Object> productMap;
    private final OnProductClickListener onProductClickListener;

    public ProductAdapter(Context context, ArrayList<ProductModal> productModals,
                          Map<Integer, Object> productMap, OnProductClickListener listener) {
        this.context = context;
        this.productModals = productModals;
        this.productMap = productMap;
        this.onProductClickListener = listener;
    }

    @NonNull
    @Override
    public ProductAdapter.HolderPost onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_post, parent, false);
        return new HolderPost(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductAdapter.HolderPost holder, int position) {
        ProductModal productModal = productModals.get(position);

        if (productModal != null) {
            int productId = productModal.getId();
            String productCode = productModal.getCode();
            String productName = productModal.getTitle();
            int productQuantity = productModal.getQuantity();
            String productLocation = productModal.getLocation();

            if (productMap.containsKey(productId)) {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.green));
            } else {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.white));
            }

            // Tải hình ảnh sản phẩm
            String imageUrl = productModal.getImage();
            try {
                if (!imageUrl.isEmpty()) {
                    Picasso.get()
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_image)
                            .error(R.drawable.ic_image)
                            .fit()
                            .centerCrop()
                            .into(holder.image);
                } else {
                    holder.image.setImageResource(R.drawable.ic_image);
                }
            } catch (Exception e) {
                Log.e("PicassoError", "Error loading image", e);
            }

            // Gán dữ liệu vào các TextView
            holder.code.setText(String.format("Code: %s", productCode));
            holder.name.setText(productName);
            holder.quantity.setText(String.format("Quantity: %s", productQuantity));
            holder.location.setText(String.format("Location: %s", productLocation));

            // Xử lý khi người dùng click vào item
            holder.itemView.setOnClickListener(v -> {
                if (productMap.containsKey(productId)) {
                    Toast.makeText(context, "This product has already been confirmed!", Toast.LENGTH_SHORT).show();
                } else {
                    onProductClickListener.onProductClick(productModal, productMap);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return productModals.size();
    }

    static class HolderPost extends RecyclerView.ViewHolder {
        ImageButton moreBtn;
        TextView code, name, quantity, location;
        ImageView image;

        public HolderPost(@NonNull View itemView) {
            super(itemView);
            moreBtn = itemView.findViewById(R.id.moreBtn);
            code = itemView.findViewById(R.id.code);
            name = itemView.findViewById(R.id.title);
            quantity = itemView.findViewById(R.id.quantity);
            location = itemView.findViewById(R.id.location);
            image = itemView.findViewById(R.id.image);
        }
    }

    public interface OnProductClickListener {
        void onProductClick(ProductModal product, Map<Integer, Object> productMap);
    }
}
