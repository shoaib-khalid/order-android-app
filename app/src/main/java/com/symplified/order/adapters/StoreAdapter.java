package com.symplified.order.adapters;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.Gson;
import com.symplified.order.App;
import com.symplified.order.Orders;
import com.symplified.order.R;
import com.symplified.order.apis.StoreApi;
import com.symplified.order.firebase.FirebaseHelper;
import com.symplified.order.models.Store.Store;
import com.symplified.order.models.asset.Asset;
import com.symplified.order.services.DownloadImageTask;
import com.symplified.order.utils.ImageUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class StoreAdapter extends RecyclerView.Adapter<StoreAdapter.ViewHolder> {
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
//        Log.e("TAG", "onCreateViewHolder: size = "+getItemCount(),new Error() );
//        listItem.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//            }
//        });
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

                progressDialog.show();
                SharedPreferences sharedPreferences = context.getSharedPreferences(App.SESSION_DETAILS_TITLE, Context.MODE_PRIVATE);
                final SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("storeId", items.get(holder.getAdapterPosition()).id).apply();

                Retrofit retrofitLogo = new Retrofit.Builder().baseUrl(App.PRODUCT_SERVICE_URL).addConverterFactory(GsonConverterFactory.create()).build();
                StoreApi storeApiSerivice = retrofitLogo.create(StoreApi.class);

                Log.e("TAG", "onEnterLogoUrl: "+ sharedPreferences.getAll(), new Error());
                Map<String, String> headers = new HashMap<>();
                headers.put("Authorization", "Bearer Bearer accessToken");

                Call<ResponseBody> responseLogo = storeApiSerivice.getStoreLogo(headers, sharedPreferences.getString("storeId", "McD"));
                Intent intent = new Intent (holder.itemView.getContext(), Orders.class);

                responseLogo.clone().enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            Asset.AssetResponse responseBody = new Gson().fromJson(response.body().string(), Asset.AssetResponse.class);
//                            editor.putString("logoUrl", "https://blog.hubspot.com/hubfs/image8-2.jpg");

                                Bitmap bitmap  = new DownloadImageTask().execute(responseBody.data.logoUrl).get();
                                if(bitmap != null) {
                                    Log.e("TAG", "bitmapLogo: " + bitmap, new Error());
//                                String bitmap64 = ImageUtil.encodeTobase64(bitmap);
//                                Log.e("TAG", "bitmap: "+ bitmap64, new Error() );
                                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
//                                intent.putExtra("logo", byteArrayOutputStream.toByteArray());
                                    String encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);
                                    editor.putString("logoImage", encodedImage);
                                    editor.apply();
                                }
                            FirebaseHelper.initializeFirebase(items.get(holder.getAdapterPosition()).id, view.getContext());
                            Log.e("TAG", "preferences: " + sharedPreferences.getAll(), new Error());
                            Toast.makeText(view.getContext(), "Store id : " + (items.get(holder.getAdapterPosition()).id), Toast.LENGTH_SHORT).show();
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

//                editor.apply();
//                sharedPreferences.edit().putString("logoUrl", logoUrl);
//                FirebaseHelper.initializeFirebase(items.get(holder.getAdapterPosition()).id,view.getContext());
//                String logoUrl = sharedPreferences.getString("logoUrl", null);
//                Intent intent = new Intent (holder.itemView.getContext(), Orders.class);
//                if(logoUrl != null){
//                    try {
//                        Bitmap bitmap  = new DownloadImageTask().execute(sharedPreferences.getString("logoUrl", null)).get();
//                        String bitmap64 = ImageUtil.encodeTobase64(bitmap);
//                        Log.e("TAG", "bitmap: "+ bitmap64 );
//                        intent.putExtra("logo", bitmap64);
//                    } catch (ExecutionException e) {
//                        e.printStackTrace();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }

//                Log.e("TAG", "preferences: "+sharedPreferences.getAll(),new Error() );
//                Toast.makeText(view.getContext(), "Store id : "+ (items.get(holder.getAdapterPosition()).id), Toast.LENGTH_SHORT).show();
//                view.getContext().startActivity(intent);
//                ((Activity) holder.itemView.getContext()).finish();
            }
        });
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    public boolean isEmpty(){return items.isEmpty();}


}