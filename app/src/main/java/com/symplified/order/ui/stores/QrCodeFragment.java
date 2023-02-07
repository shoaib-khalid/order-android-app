package com.symplified.order.ui.stores;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.symplified.order.R;
import com.symplified.order.interfaces.QrCodeObserver;
import com.symplified.order.models.qrcode.QrCodeRequest;
import com.symplified.order.models.qrcode.QrCodeResponse;
import com.symplified.order.networking.ServiceGenerator;
import com.symplified.order.networking.apis.OrderApi;
import com.symplified.order.services.OrderNotificationService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QrCodeFragment extends Fragment implements QrCodeObserver {

    AppCompatImageView qrCodeImage;
    ProgressBar progressBar;
    ConstraintLayout failureLayout;
    Button retryButton;
    AppCompatImageButton closeButton;
    int screenWidth = 800, screenHeight = 0;

    String storeId;

    OrderApi orderApiService;

    public QrCodeFragment() {
        super(R.layout.fragment_qr_code);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        qrCodeImage = view.findViewById(R.id.qr_code_image);
        progressBar = view.findViewById(R.id.circular_progress_bar);
        failureLayout = view.findViewById(R.id.failure_layout);

//        closeButton = view.findViewById(R.id.btn_close);
//        closeButton.setOnClickListener(v -> closeFragment());

        retryButton = view.findViewById(R.id.btn_retry);
        retryButton.setOnClickListener(v -> requestQrCode());

        storeId = requireArguments().getString("storeId");

        requestQrCode();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        Log.d("qr-code", "onAttach");
        orderApiService = ServiceGenerator.createOrderService(context);
        OrderNotificationService.disableOrderNotifications();
        OrderNotificationService.addQrCodeObserver(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Rect bounds = context.getSystemService(WindowManager.class).getCurrentWindowMetrics()
                    .getBounds();
            screenWidth = bounds.width();
            screenHeight = bounds.height();
        } else if (getActivity() != null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            screenWidth = displayMetrics.widthPixels;
            screenHeight = displayMetrics.heightPixels;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        OrderNotificationService.enableOrderNotifications();
        OrderNotificationService.removeQrCodeObserver(this);
    }

    private void requestQrCode() {
        startLoading();

        orderApiService.generateQrCode(new QrCodeRequest(storeId)).clone().enqueue(new Callback<QrCodeResponse>() {
            @Override
            public void onResponse(@NonNull Call<QrCodeResponse> call,
                                   @NonNull Response<QrCodeResponse> response) {
                if(response.isSuccessful() && response.body() != null) {
                    try {
                        qrCodeImage.setImageBitmap(encodeAsBitmap(response.body().data.url));
                        stopLoadingWithSuccess();
                    } catch (WriterException e) {
                        stopLoadingWithFailure();
                    }
                } else {
                    stopLoadingWithFailure();
                }
            }

            @Override
            public void onFailure(@NonNull Call<QrCodeResponse> call,
                                  @NonNull Throwable t) {
                stopLoadingWithFailure();
            }
        });
    }

    Bitmap encodeAsBitmap(String stringToShow) throws WriterException {
        double fraction = screenWidth > 1500 ? 0.4 : 0.35;
        String output = "Width: " + screenWidth + ", Height: " + screenHeight + ", Fraction: " + fraction;
        QRCodeWriter writer = new QRCodeWriter();
        int dimensions = (int)(fraction * screenWidth);
        BitMatrix bitMatrix = writer.encode(stringToShow, BarcodeFormat.QR_CODE, dimensions, dimensions);

        int w = bitMatrix.getWidth();
        int h = bitMatrix.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixels[y * w + x] = bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }

    private void startLoading() {
        qrCodeImage.setImageBitmap(null);
        failureLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
    }

    private void stopLoadingWithSuccess() {
//        qrCodeImage.setImageBitmap(null);
        failureLayout.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
    }

    private void stopLoadingWithFailure() {
        qrCodeImage.setImageBitmap(null);
        failureLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onRedeemed() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(this::requestQrCode);
        }
    }

    private void closeFragment() {
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager().popBackStack();
        }
    }
}