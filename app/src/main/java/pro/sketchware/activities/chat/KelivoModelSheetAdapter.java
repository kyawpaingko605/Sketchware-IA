package pro.sketchware.activities.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import pro.sketchware.R;

public class KelivoModelSheetAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int TYPE_HEADER = 0;
    static final int TYPE_MODEL = 1;

    static final class Row {
        final int type;
        final String providerId;
        final String providerLabel;
        final String modelId;
        final boolean selected;
        final boolean pinned;

        Row(String providerId, String providerLabel) {
            type = TYPE_HEADER;
            this.providerId = providerId == null ? "" : providerId;
            this.providerLabel = providerLabel == null ? "" : providerLabel;
            modelId = "";
            selected = false;
            pinned = false;
        }

        Row(String providerId, String providerLabel, String modelId, boolean selected) {
            this(providerId, providerLabel, modelId, selected, false);
        }

        Row(String providerId, String providerLabel, String modelId, boolean selected, boolean pinned) {
            type = TYPE_MODEL;
            this.providerId = providerId;
            this.providerLabel = providerLabel;
            this.modelId = modelId;
            this.selected = selected;
            this.pinned = pinned;
        }
    }

    public interface Listener {
        void onModelSelected(String providerId, String modelId);

        void onFavoriteToggle(String providerId, String modelId);
    }

    private final List<Row> rows = new ArrayList<>();
    private Listener listener;

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Row> items) {
        rows.clear();
        if (items != null) {
            rows.addAll(items);
        }
        notifyDataSetChanged();
    }

    public int findProviderSectionPosition(String providerId) {
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (row.type == TYPE_HEADER && providerId.equals(row.providerId)) {
                return i;
            }
        }
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (row.type == TYPE_MODEL && providerId.equals(row.providerId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getItemViewType(int position) {
        return rows.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderHolder(inflater.inflate(R.layout.item_kelivo_provider_header, parent, false));
        }
        return new ModelHolder(inflater.inflate(R.layout.item_kelivo_model_row, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (holder instanceof HeaderHolder) {
            HeaderHolder headerHolder = (HeaderHolder) holder;
            headerHolder.title.setText(row.providerLabel);
            int iconRes = KelivoModelIconResolver.resolveProvider(row.providerId, row.providerLabel);
            if (iconRes != 0) {
                headerHolder.icon.setVisibility(View.VISIBLE);
                headerHolder.icon.setImageResource(iconRes);
            } else {
                headerHolder.icon.setVisibility(View.GONE);
            }
            return;
        }
        ModelHolder modelHolder = (ModelHolder) holder;
        modelHolder.name.setText(row.modelId);
        int iconRes = KelivoModelIconResolver.resolve(row.providerId, row.modelId);
        if (iconRes != 0) {
            modelHolder.icon.setVisibility(View.VISIBLE);
            modelHolder.icon.setImageResource(iconRes);
            modelHolder.avatar.setVisibility(View.GONE);
        } else {
            modelHolder.icon.setVisibility(View.GONE);
            modelHolder.avatar.setVisibility(View.VISIBLE);
            modelHolder.avatar.setText(KelivoModelIconResolver.initial(row.providerId, row.modelId));
        }
        modelHolder.itemView.setBackgroundResource(row.selected
                ? R.drawable.bg_kelivo_model_selected
                : android.R.color.transparent);
        modelHolder.favorite.setImageResource(row.pinned
                ? R.drawable.ic_kelivo_heart_filled
                : R.drawable.ic_kelivo_heart_outline);
        modelHolder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onModelSelected(row.providerId, row.modelId);
            }
        });
        modelHolder.favorite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteToggle(row.providerId, row.modelId);
            }
        });
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class HeaderHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView title;

        HeaderHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.provider_header_icon);
            title = itemView.findViewById(R.id.provider_header_title);
        }
    }

    static class ModelHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView avatar;
        final TextView name;
        final ImageView favorite;

        ModelHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.model_icon);
            avatar = itemView.findViewById(R.id.model_avatar);
            name = itemView.findViewById(R.id.model_name);
            favorite = itemView.findViewById(R.id.model_favorite);
        }
    }
}
