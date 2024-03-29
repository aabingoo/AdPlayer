package com.boilbingo.adplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AdPlayer adPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adPlayer = (AdPlayer) findViewById(R.id.adPlayer);
        List<Bitmap> list = new ArrayList<>();
        Bitmap bitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.p1);
        Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.p2);
        Bitmap bitmap3 = BitmapFactory.decodeResource(getResources(), R.drawable.p3);
        Bitmap bitmap4 = BitmapFactory.decodeResource(getResources(), R.drawable.p4);
        list.add(bitmap1);
        list.add(bitmap2);
        list.add(bitmap3);
        list.add(bitmap4);
//        adPlayer.setAdPictures(list);
        adPlayer.addAdWithTitle("塞法阿道夫", bitmap1);
        adPlayer.addAdWithTitle("塞法阿道夫aafasdfa", bitmap2);
        adPlayer.addAdWithTitle("塞法阿道夫阿斯顿发送到发送到法萨是否是多福多寿发生的阿瑟费大是大非阿斯顿发送地方", bitmap3);
        adPlayer.addAdWithTitle("塞法阿道夫", bitmap4);
        adPlayer.setItemClickListener(new AdPlayer.ItemClickListener() {
            @Override
            public void onItemClick(int index) {
                Toast.makeText(MainActivity.this,
                        "Index:" + index + " is clicked!!!", Toast.LENGTH_SHORT).show();
            }
        });
        adPlayer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        adPlayer.stop();
    }
}
