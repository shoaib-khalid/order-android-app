package com.symplified.order.adapters;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.symplified.order.R;
import com.symplified.order.dialogs.SettingsBottomSheet;
import com.symplified.order.models.Store.Store;

import java.util.List;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.ViewHolder> {
    private static final String TAG = "StoreAdapter";
    public List<Store> items;
    public Context context;
    private Dialog progressDialog;

    public StoreAdapter(List<Store> items, Context context, Dialog progressDialog){
        this.items = items;
        this.context = context;
        this.progressDialog = progressDialog;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            name = (TextView) view.findViewById(R.id.store_name);
        }


    }


    @NonNull
    @Override
    public StoreAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View listItem= layoutInflater.inflate(R.layout.store_list_item, parent, false);
        return new StoreAdapter.ViewHolder(listItem);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        progressDialog.setCancelable(false);
        CircularProgressIndicator progressIndicator = progressDialog.findViewById(R.id.progress);
        progressIndicator.setIndeterminate(true);


        holder.name.setText(items.get(position).name);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*
                progressDialog.show();
                SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
                final SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("timezone", items.get(holder.getAdapterPosition()).regionCountry.timezone).apply();
                editor.putString("storeId", items.get(holder.getAdapterPosition()).id).apply();

                String BASE_URL = sharedPreferences.getString("base_url", App.BASE_URL);

                Retrofit retrofitLogo = new Retrofit.Builder().client(new OkHttpClient()).baseUrl(BASE_URL+App.PRODUCT_SERVICE_URL).addConverterFactory(GsonConverterFactory.create()).build();
                StoreApi storeApiSerivice = retrofitLogo.create(StoreApi.class);
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer Bearer accessToken");

                Call<ResponseBody> responseLogo = storeApiSerivice.getStoreLogo(headers, sharedPreferences.getString("storeId", "McD"));
                Intent intent = new Intent (holder.itemView.getContext(), Orders.class);

                responseLogo.clone().enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            Asset.AssetResponse responseBody = new Gson().fromJson(response.body().string(), Asset.AssetResponse.class);

                            if(responseBody.data !=null){
                                Bitmap bitmap  = new DownloadImageTask().execute(responseBody.data.logoUrl).get();
                                if(bitmap != null) {
                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
                                    String encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
                                    editor.putString("logoImage", encodedImage);
                                    editor.apply();
                                }
                            }
                            FirebaseHelper.initializeFirebase(items.get(holder.getAdapterPosition()).id, view.getContext());
                            progressDialog.hide();
                            view.getContext().startActivity(intent);
                            ((Activity) holder.itemView.getContext()).finish();
                        } catch (IOException | ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        progressDialog.hide();

                    }
                });
                */
                BottomSheetDialogFragment bottomSheetDialogFragment = new SettingsBottomSheet();
                bottomSheetDialogFragment.show(((FragmentActivity) context).getSupportFragmentManager(),"bottomSheetDialog");
            }
        });
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    public boolean isEmpty(){return items.isEmpty();}


}