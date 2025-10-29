package pro.sketchware.activities.main.fragments.web_service.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import pro.sketchware.R;
import pro.sketchware.databinding.ViewWebServiceItemBinding;

public class WebServiceAdapter extends RecyclerView.Adapter<WebServiceAdapter.ViewHolder> {

    private final FragmentActivity context;
    
    private static final class WebService {
        String title;
        String url;
        int iconResId;
        
        WebService(String title, String url, int iconResId) {
            this.title = title;
            this.url = url;
            this.iconResId = iconResId;
        }
    }
    
    private static final WebService[] WEB_SERVICES = {
        new WebService("Ebook Maker", "https://ebookmeker.online/", R.drawable.aaa_bbb_v),
        new WebService("Fitness Plan", "https://www.meuplanofit.online/", R.drawable.abc_aaa),
        new WebService("Lovizin", "https://www.lovizin.com.br/", R.drawable.love_icon_123)
    };

    public WebServiceAdapter(FragmentActivity context) {
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        ViewWebServiceItemBinding binding = ViewWebServiceItemBinding.inflate(inflater, parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WebService webService = WEB_SERVICES[position];
        
        holder.binding.webServiceTitle.setText(webService.title);
        holder.binding.webServiceUrl.setText(webService.url);
        holder.binding.webServiceIcon.setImageResource(webService.iconResId);
        
        holder.itemView.setOnClickListener(v -> openUrl(webService.url));
    }

    @Override
    public int getItemCount() {
        return WEB_SERVICES.length;
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse(url));
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "Erro ao abrir link", Toast.LENGTH_SHORT).show();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ViewWebServiceItemBinding binding;

        public ViewHolder(@NonNull ViewWebServiceItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

