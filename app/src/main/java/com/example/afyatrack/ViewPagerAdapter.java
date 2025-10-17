package com.example.afyatrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;
import java.util.List;

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder> {

    private List<ViewPagerItem> viewPagerItems;

    public ViewPagerAdapter(List<ViewPagerItem> viewPagerItems) {
        this.viewPagerItems = viewPagerItems;
    }

    @NonNull
    @Override
    public ViewPagerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_viewpager, parent, false);
        return new ViewPagerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewPagerViewHolder holder, int position) {
        ViewPagerItem item = viewPagerItems.get(position);

        if (item.getLottieRawRes() != 0) {
            // Use Lottie Animation
            holder.lottieAnimation.setVisibility(View.VISIBLE);
            holder.imageView.setVisibility(View.GONE);
            holder.lottieAnimation.setAnimation(item.getLottieRawRes());
        } else if (item.getImageRes() != 0) {
            // Use Static Image
            holder.lottieAnimation.setVisibility(View.GONE);
            holder.imageView.setVisibility(View.VISIBLE);
            holder.imageView.setImageResource(item.getImageRes());
        }

        holder.titleText.setText(item.getTitle());
        holder.descriptionText.setText(item.getDescription());
    }

    @Override
    public int getItemCount() {
        return viewPagerItems.size();
    }

    public static class ViewPagerViewHolder extends RecyclerView.ViewHolder {
        LottieAnimationView lottieAnimation;
        ImageView imageView;
        TextView titleText;
        TextView descriptionText;

        public ViewPagerViewHolder(@NonNull View itemView) {
            super(itemView);
            lottieAnimation = itemView.findViewById(R.id.lottieAnimation);
            imageView = itemView.findViewById(R.id.imageView);
            titleText = itemView.findViewById(R.id.titleText);
            descriptionText = itemView.findViewById(R.id.descriptionText);
        }
    }
}