package com.stock.flow;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class UtangFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        FrameLayout root = new FrameLayout(requireContext());
        root.setBackgroundColor(Color.rgb(245, 245, 245));

        TextView label = new TextView(requireContext());
        label.setText("Utang");
        label.setTextSize(20);
        label.setTextColor(Color.rgb(90, 90, 90));
        label.setTypeface(null, Typeface.BOLD);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;

        root.addView(label, params);

        return root;
    }
}
