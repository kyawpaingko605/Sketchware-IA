package pro.sketchware.activities.studio.uidesigner.adapter;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import pro.sketchware.activities.studio.uidesigner.DesignerApp;
import pro.sketchware.R;
import pro.sketchware.activities.studio.uidesigner.item.WidgetGroup;
import pro.sketchware.activities.studio.uidesigner.util.DesigerUtil;
import pro.sketchware.activities.studio.uidesigner.view.DesignerLayout;
import java.util.List;

public class WidgetsAdapter extends BaseAdapter<WidgetGroup>
{
    public DesignerLayout dl;
    public List<WidgetGroup> list;

    public WidgetsAdapter(DesignerLayout dl, List<WidgetGroup> list)
    {
        this.dl = dl;
        this.list = list;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {
        if (viewType == 0)
        {
            return new WidgetHolder(LayoutInflater.from(parent.getContext())
                                    .inflate(R.layout.item_widget, parent, false));
        }
        else
        {
            return new TitleHolder(LayoutInflater.from(parent.getContext())
                                   .inflate(R.layout.item_widget_title, parent, false));
        }
    }

    @Override
    public int getItemCount()
    {
        return getData().size();
    }

    @Override
    public void onBindViews(RecyclerView.ViewHolder holder, int position)
    {
        if (holder instanceof WidgetHolder)
        {
            WidgetHolder mHolder = (WidgetHolder) holder;
            mHolder.onBindView(mHolder, this, position);
        }
        else if (holder instanceof TitleHolder)
        {
            TitleHolder mHolder = (TitleHolder) holder;
            mHolder.onBindView(mHolder, this, position);
        }
    }

    @Override
    public List<WidgetGroup> getData()
    {
        return list;
    }

    @Override
    public WidgetGroup getItem(int positon)
    {
        return getData().get(positon);
    }

    @Override
    public int getItemViewType(int position)
    {
        return getItem(position).getHolderType();
    }

    public class WidgetHolder extends BaseHolder<WidgetHolder>
    {
        public LinearLayout root;
        public ImageView icon;
        public TextView title;

        public WidgetHolder(View itemView)
        {
            super(itemView);
            root = itemView.findViewById(R.id.root);
            icon = itemView.findViewById(R.id.icon);
            title = itemView.findViewById(R.id.title);
        }

        @Override
        public void onBindView(WidgetHolder holder, BaseAdapter adapter, int position)
        {
            final WidgetGroup item = (WidgetGroup) adapter.getItem(position);
            final String type = item.getWidget() == null || item.getWidget().getWidgetType() == null
                    ? item.getTitle()
                    : item.getWidget().getWidgetType();

            root.setOnClickListener(v -> dl.addWidget(type));
            root.setOnLongClickListener(new View.OnLongClickListener()
                {
                    @Override
                    public boolean onLongClick(View v)
                    {
                        dl.view_location.getLayoutParams().width = DesigerUtil.dip(120);
                        dl.view_location.getLayoutParams().height = DesigerUtil.dip(42);
                        dl.defaultIndex = -1;

                        dl.mWidget = dl.createWidgetContainerFromType(type);
                        if (v.startDrag(null, new View.DragShadowBuilder(v), v, 0))
                        {
                            DesigerUtil.vibrate();
                        }
                        else
                        {
                            dl.mWidget = null;
                        }
                        return true;
                    }
                });
            holder.icon.setImageDrawable(DesigerUtil.setTint(item.getIcon(),
                                                             DesignerApp.getContext().getResources().getColor(R.color.studio_accent)));
            holder.title.setText(item.getTitle());
        }
    }

    public class TitleHolder extends BaseHolder<TitleHolder>
    {
        public TextView title;

        public TitleHolder(View itemView)
        {
            super(itemView);
            title = itemView.findViewById(R.id.title);
        }

        @Override
        public void onBindView(TitleHolder holder, BaseAdapter adapter, int position)
        {
            WidgetGroup item = (WidgetGroup) adapter.getItem(position);
            holder.title.setText(item.getTitle());
        }
    }
}
