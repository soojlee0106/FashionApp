package com.example.fashionapp.View;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.example.fashionapp.R;
import com.example.fashionapp.ViewModel.SharedViewModel;
import com.example.fashionapp.databinding.FragmentShowStyleBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class ShowStyleFragment extends Fragment {

    private FragmentShowStyleBinding binding;
    ImageView result_image;
    SharedViewModel sharedViewModel;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentShowStyleBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        File mostRecentFile = null;
        long mostRecentModified = Long.MIN_VALUE;

        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        for (File file : storageDir.listFiles()) {
            if (file.isFile() && file.lastModified() > mostRecentModified) {
                mostRecentFile = file;
                mostRecentModified = file.lastModified();
            }
        }

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        result_image = getView().findViewById(R.id.result_image);

        File finalMostRecentFile = mostRecentFile;

        sharedViewModel.getGenderResult().observe(getViewLifecycleOwner(), gender_result ->{
            sharedViewModel.getDiagnosisResult().observe(getViewLifecycleOwner(), diagnosis_result ->{

        new Thread(() ->
        {
            OkHttpClient client = new OkHttpClient();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("diagnosis", diagnosis_result)
                    .addFormDataPart("gender", gender_result)
                    .addFormDataPart("file","file.jpg", RequestBody.create(finalMostRecentFile, MultipartBody.FORM))
                    .build();

            Request request = new Request.Builder()
                    .url("http://172.23.247.89:5000/pred")
                    .post(requestBody)
                    .build();

            try {
                Response response = client.newCall(request).execute();
                String jsonString = response.body().string();

                JSONObject files = new JSONObject(jsonString);
                String result_photo_encoded = files.getString("result_photo");

                byte[] result_photo_data = Base64.decode(result_photo_encoded, Base64.DEFAULT);

                Bitmap result_image_bitmap = BitmapFactory.decodeByteArray(result_photo_data, 0, result_photo_data.length);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        result_image.setImageBitmap(result_image_bitmap);
                    }
                });

                response.close();

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }).start();

            });
        });

        binding.homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(ShowStyleFragment.this)
                        .navigate(R.id.action_global_toHome);}
        });
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}