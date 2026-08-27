package com.stock.flow.adapter;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.stock.flow.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the horizontal "Hot Sale" list. Builds item views programmatically so it doesn't
 * depend on new XML drawables/resources.
 */
public class ProductSaleAdapter extends RecyclerView.Adapter<ProductSaleAdapter.VH> {

    public static class Item {
        public final int rank;
        public final Product product;
        public final long qtySold;

        public Item(int rank, Product product, long qtySold) {
            this.rank = rank;
            this.product = product;
            this.qtySold = qtySold;
        }
    }

    private final List<Item> items = new ArrayList<>();

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ContextWrapper ctx = new ContextWrapper(parent);
        return new VH(ctx.card);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Item it = items.get(position);

        holder.nameView.setText(it.product.getName());
        holder.priceView.setText(String.format(holder.priceView.getContext().getResources().getConfiguration().locale,
                "\u20B1%.2f", it.product.getSellingPrice()));
        holder.statsView.setText(it.qtySold + " sold today");
        holder.rankBadge.setText(String.valueOf(it.rank));

        // placeholder color drawable similar to previous boxBg
        ColorDrawable placeholder = new ColorDrawable(Color.rgb(230, 234, 240));

        if (it.product.getImagePath() != null && !it.product.getImagePath().isEmpty()) {
            Glide.with(holder.imageView)
                    .load(it.product.getImagePath())
                    .placeholder(placeholder)
                    .error(placeholder)
                    .into(holder.imageView);
            holder.imageView.setContentDescription(it.product.getName());
        } else {
            holder.imageView.setImageDrawable(placeholder);
            holder.imageView.setContentDescription(it.product.getName());
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(final List<Item> newItems) {
        final List<Item> old = new ArrayList<>(items);
        DiffUtil.DiffResult dr = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return old.size(); }
            @Override
            public int getNewListSize() { return newItems.size(); }
            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Product a = old.get(oldItemPosition).product;
                Product b = newItems.get(newItemPosition).product;
                String ba = a.getBarcode();
                String bb = b.getBarcode();
                if (ba == null && bb == null) {
                    // fallback to name
                    return a.getName() != null && a.getName().equals(b.getName());
                }
                return ba != null && ba.equals(bb);
            }
            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Item a = old.get(oldItemPosition);
                Item b = newItems.get(newItemPosition);
                return a.rank == b.rank && a.qtySold == b.qtySold;
            }
        });

        items.clear();
        items.addAll(newItems);
        dr.dispatchUpdatesTo(this);
    }

    static class VH extends RecyclerView.ViewHolder {
        FrameLayout imageWrap;
        ImageView imageView;
        TextView rankBadge;
        TextView nameView;
        TextView priceView;
        TextView statsView;

        VH(@NonNull View itemView) {
            super(itemView);
            imageWrap = itemView.findViewById(1);
            imageView = itemView.findViewById(2);
            rankBadge = itemView.findViewById(3);
            nameView = itemView.findViewById(4);
            priceView = itemView.findViewById(5);
            statsView = itemView.findViewById(6);
        }
    }

    // Helper to create the card view programmatically and keep dp conversion localized
    private static class ContextWrapper {
        final LinearLayout card;
        ContextWrapper(ViewGroup parent) {
            android.content.Context context = parent.getContext();
            float density = context.getResources().getDisplayMetrics().density;
            card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            int padding = dp(10, density);
            card.setPadding(padding, padding, padding, dp(12, density));

            GradientDrawableHelper bg = new GradientDrawableHelper(Color.WHITE, dp(16, density));
            card.setBackground(bg.drawable);
            card.setElevation(dp(2, density));

            FrameLayout imageWrap = new FrameLayout(context);
            LinearLayout.LayoutParams wrapParams = new LinearLayout.LayoutParams(dp(118, density), dp(90, density));
            imageWrap.setLayoutParams(wrapParams);

            GradientDrawableHelper boxBg = new GradientDrawableHelper(Color.rgb(230, 234, 240), dp(10, density));

            ImageView imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setBackground(boxBg.drawable);
            int pad = dp(4, density);
            imageView.setPadding(pad, pad, pad, pad);
            FrameLayout.LayoutParams imgLp = new FrameLayout.LayoutParams(dp(118, density), dp(90, density));
            imageWrap.addView(imageView, imgLp);

            TextView rankBadge = new TextView(context);
            rankBadge.setTextColor(Color.WHITE);
            rankBadge.setTextSize(11);
            rankBadge.setGravity(Gravity.CENTER);
            GradientDrawableHelper rankBg = new GradientDrawableHelper(Color.rgb(45, 108, 223), dp(8, density));
            rankBadge.setBackground(rankBg.drawable);
            FrameLayout.LayoutParams rankParams = new FrameLayout.LayoutParams(dp(20, density), dp(20, density));
            rankParams.topMargin = dp(4, density);
            rankParams.leftMargin = dp(4, density);
            imageWrap.addView(rankBadge, rankParams);

            card.addView(imageWrap);
            card.addView(spacer(dp(8, density), context));

            TextView nameView = new TextView(context);
            nameView.setTextSize(13);
            nameView.setTypeface(null, android.graphics.Typeface.BOLD);
            nameView.setTextColor(Color.rgb(27, 42, 74));
            nameView.setMaxLines(1);
            card.addView(nameView);

            TextView priceView = new TextView(context);
            priceView.setTextSize(13);
            priceView.setTypeface(null, android.graphics.Typeface.BOLD);
            priceView.setTextColor(Color.rgb(45, 108, 223));
            card.addView(priceView);

            TextView statsView = new TextView(context);
            statsView.setTextSize(11);
            statsView.setTextColor(Color.rgb(34, 197, 94));
            card.addView(statsView);

            // give view ids for the ViewHolder to find
            imageWrap.setId(1);
            imageView.setId(2);
            rankBadge.setId(3);
            nameView.setId(4);
            priceView.setId(5);
            statsView.setId(6);
        }
    }

    private static View spacer(int hPx, android.content.Context ctx) {
        View v = new View(ctx);
        v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, hPx));
        return v;
    }

    private static int dp(float v, float density) {
        return (int) (v * density + 0.5f);
    }

    // small helper to produce simple rounded background drawable
    private static class GradientDrawableHelper {
        final android.graphics.drawable.GradientDrawable drawable;
        GradientDrawableHelper(int color, int radiusPx) {
            drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setColor(color);
            drawable.setCornerRadius(radiusPx);
        }
    }
}
