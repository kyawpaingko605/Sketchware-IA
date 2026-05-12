package pro.sketchware.activities.studio.uidesigner.adapter;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public abstract class BaseHolder<T> extends RecyclerView.ViewHolder
{
    public BaseHolder(View itemView)
    {
        super(itemView);
    }

    public abstract void onBindView(T t, BaseAdapter adapter, int position);
}
