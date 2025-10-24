package pro.sketchware.activities.main.fragments.projects_store.classes;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import pro.sketchware.utility.TranslationFunction;

public class HorizontalItemDecoration extends RecyclerView.ItemDecoration {
    private final int spacing;

    public HorizontalItemDecoration(int spacing) {
        this.spacing = spacing / 2;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        outRect.left = spacing;
        outRect.right = spacing;
    }
}
