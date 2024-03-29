package com.userinterface.ssuroom;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.type.LatLng;
import com.userinterface.ssuroom.adapter.GridListAdapter;
import com.userinterface.ssuroom.model.ReviewItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton fab_main;
    int[] filter={0,0,0,0,0};
    String myId;
    FirebaseUser user;
    GridListAdapter adapter;

    private Query filterReviews() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Query query=db.collection("reviews");

        if (filter[0] == 2)
            query = query.whereEqualTo("tradeType", "월세");
        else if (filter[0] == 3)
            query = query.whereEqualTo("tradeType", "전세");

        if (filter[1] == 2)
            query = query.whereIn("roomType", Arrays.asList("오픈형 원룸", "분리형 원룸", "복층형 원룸"));
        else if (filter[1] == 3)
            query = query.whereEqualTo("roomType", "투룸");
        else if (filter[1] == 4)
            query = query.whereEqualTo("roomType", "쓰리룸+");

        if (filter[2] == 2)
            query = query.whereLessThan("rentCost", 40);
        else if (filter[2]== 3) {
            query = query.whereLessThan("rentCost", 60)
                    .whereGreaterThanOrEqualTo("rentCost", 40);
        } else if (filter[2]== 4) {
            query = query.whereLessThan("rentCost", 80)
                    .whereGreaterThanOrEqualTo("rentCost", 60);
        } else if (filter[2]== 5)
            query = query.whereGreaterThanOrEqualTo("rentCost", 80);

        if (filter[3] == 2)
            query = query.whereEqualTo("isTrading", "거래중");
        else if (filter[3] == 3)
            query = query.whereEqualTo("isTrading", "거래완료");

        if(filter[4]==1)
            query=query.whereArrayContains("fans",myId);


        Log.d("firebaseFilter", filter[0]+" "+filter[1]+" "+filter[2]+" "+filter[3]+" "+filter[4]);


        return query;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        user=FirebaseAuth.getInstance().getCurrentUser();
        myId=user.getUid();

        fab_main = findViewById(R.id.fab_main);

        fab_main.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), EditActivity1.class);
                startActivity(intent);
            }
        });


        SearchView searchView=findViewById(R.id.search_view);
        Log.d("search_click","init");
        setSearchViewOnClickListener(searchView,new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("search_click","sssss");
                Intent intent = new Intent(getApplicationContext(), MapActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivityForResult(intent,2);
            }
        });

        GridView gridView = findViewById(R.id.gridView);
        adapter = new GridListAdapter(this);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("reviews")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map<String, Object> data = document.getData();
                                ArrayList<String> fans=(ArrayList<String>) data.get("fans");
                                boolean isHeart=fans.contains(user.getUid());

                                ReviewItem rItem=new ReviewItem((String) data.get("tradeType"), (Long) data.get("rentCost"), (Long) data.get("depositCost"), (Long) data.get("area"), (Long) data.get("floor"), (String) data.get("address"), (Double) data.get("star"), (String) data.get("isTrading"), document.getId(),isHeart);
                                adapter.addItem(rItem);
                                Log.d("final_log", "초기 리뷰 데이터 DB에서 가져오기");

                                ArrayList<String> fileName = (ArrayList<String>) data.get("imgs");
                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                StorageReference storageRef = storage.getReference();
                                storageRef.child("image").child(fileName.get(0))
                                        .getBytes(10*1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                            @Override
                                            public void onSuccess(byte[] bytes) {
                                                if(!adapter.items.contains(rItem))
                                                    return;
                                                rItem.setMainImg(bytes);
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                            }

                            gridView.setAdapter(adapter);
                        } else {
                            Log.d("serverLoad", "Error getting documents: ", task.getException());
                        }
                    }
                });

        Spinner spinner1 = findViewById(R.id.spinner1);
        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0)
                    return;
                filter[0]=i;
                adapter.clearItem();
                Query query=filterReviews();
                query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map<String, Object> data = document.getData();
                                ArrayList<String> fans=(ArrayList<String>) data.get("fans");
                                boolean isHeart=fans.contains(user.getUid());
                                ReviewItem rItem=new ReviewItem((String) data.get("tradeType"), (Long) data.get("rentCost"), (Long) data.get("depositCost"), (Long) data.get("area"), (Long) data.get("floor"), (String) data.get("address"), (Double) data.get("star"), (String) data.get("isTrading"), document.getId(),isHeart);
                                adapter.addItem(rItem);
                                Log.d("firebaseSpinner", document.getId() + " => " + document.getData());

                                ArrayList<String> fileName = (ArrayList<String>) data.get("imgs");
                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                StorageReference storageRef = storage.getReference();

                                storageRef.child("image").child(fileName.get(0))
                                        .getBytes(10*1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                            @Override
                                            public void onSuccess(byte[] bytes) {
                                                if(!adapter.items.contains(rItem))
                                                    return;
                                                rItem.setMainImg(bytes);
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                                Log.d("final_log", "필터 적용해서 DB에서 가져오기");
                            }
                        } else {
                            Log.d("firebaseSpinner", "Error getting documents: ", task.getException());
                        }
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Spinner spinner2 = findViewById(R.id.spinner2);
        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0)
                    return;
                filter[1]=i;
                adapter.clearItem();
                Query query=filterReviews();
                query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map<String, Object> data = document.getData();
                                ArrayList<String> fans=(ArrayList<String>) data.get("fans");
                                boolean isHeart=fans.contains(user.getUid());
                                ReviewItem rItem=new ReviewItem((String) data.get("tradeType"), (Long) data.get("rentCost"), (Long) data.get("depositCost"), (Long) data.get("area"), (Long) data.get("floor"), (String) data.get("address"), (Double) data.get("star"), (String) data.get("isTrading"), document.getId(),isHeart);
                                adapter.addItem(rItem);
                                Log.d("firebaseSpinner", document.getId() + " => " + document.getData());


                                ArrayList<String> fileName = (ArrayList<String>) data.get("imgs");
                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                StorageReference storageRef = storage.getReference();
                                storageRef.child("image").child(fileName.get(0))
                                        .getBytes(10*1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                            @Override
                                            public void onSuccess(byte[] bytes) {
                                                if(!adapter.items.contains(rItem))
                                                    return;
                                                rItem.setMainImg(bytes);
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                                Log.d("final_log", "필터 적용해서 DB에서 가져오기");

                            }
                        } else {
                            Log.d("firebaseSpinner", "Error getting documents: ", task.getException());
                        }
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Spinner spinner3 = findViewById(R.id.spinner3);
        spinner3.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0)
                    return;
                filter[2]=i;
                adapter.clearItem();
                Query query=filterReviews();
                query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map<String, Object> data = document.getData();
                                ArrayList<String> fans=(ArrayList<String>) data.get("fans");
                                boolean isHeart=fans.contains(user.getUid());
                                ReviewItem rItem=new ReviewItem((String) data.get("tradeType"), (Long) data.get("rentCost"), (Long) data.get("depositCost"), (Long) data.get("area"), (Long) data.get("floor"), (String) data.get("address"), (Double) data.get("star"), (String) data.get("isTrading"), document.getId(),isHeart);
                                adapter.addItem(rItem);
                                Log.d("firebaseSpinner", document.getId() + " => " + document.getData());

                                ArrayList<String> fileName = (ArrayList<String>) data.get("imgs");
                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                StorageReference storageRef = storage.getReference();
                                storageRef.child("image").child(fileName.get(0))
                                        .getBytes(10*1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                            @Override
                                            public void onSuccess(byte[] bytes) {
                                                if(!adapter.items.contains(rItem))
                                                    return;
                                                rItem.setMainImg(bytes);
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                                Log.d("final_log", "필터 적용해서 DB에서 가져오기");

                            }
                        } else {
                            Log.d("firebaseSpinner", "Error getting documents: ", task.getException());
                        }
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        Spinner spinner4 = findViewById(R.id.spinner4);
        spinner4.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                if (i == 0)
                    return;
                filter[3]=i;
                adapter.clearItem();
                Query query=filterReviews();
                query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map<String, Object> data = document.getData();
                                ArrayList<String> fans=(ArrayList<String>) data.get("fans");
                                boolean isHeart=fans.contains(user.getUid());
                                ReviewItem rItem=new ReviewItem((String) data.get("tradeType"), (Long) data.get("rentCost"), (Long) data.get("depositCost"), (Long) data.get("area"), (Long) data.get("floor"), (String) data.get("address"), (Double) data.get("star"), (String) data.get("isTrading"), document.getId(),isHeart);
                                adapter.addItem(rItem);
                                Log.d("firebaseSpinner", document.getId() + " => " + document.getData());

                                ArrayList<String> fileName = (ArrayList<String>) data.get("imgs");
                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                StorageReference storageRef = storage.getReference();
                                storageRef.child("image").child(fileName.get(0))
                                        .getBytes(10*1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                            @Override
                                            public void onSuccess(byte[] bytes) {
                                                if(!adapter.items.contains(rItem))
                                                    return;
                                                rItem.setMainImg(bytes);
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                                Log.d("final_log", "필터 적용해서 DB에서 가져오기");

                            }
                        } else {
                            Log.d("firebaseSpinner", "Error getting documents: ", task.getException());
                        }
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        CheckBox heart=findViewById(R.id.heart);
        heart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = ((CheckBox) view).isChecked();
                if(checked)
                    filter[4]=1;
                else
                    filter[4]=0;
                adapter.clearItem();
                Query query=filterReviews();
                query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Map<String, Object> data = document.getData();
                                ArrayList<String> fans=(ArrayList<String>) data.get("fans");
                                boolean isHeart=fans.contains(user.getUid());
                                ReviewItem rItem=new ReviewItem((String) data.get("tradeType"), (Long) data.get("rentCost"), (Long) data.get("depositCost"), (Long) data.get("area"), (Long) data.get("floor"), (String) data.get("address"), (Double) data.get("star"), (String) data.get("isTrading"), document.getId(),isHeart);
                                adapter.addItem(rItem);
                                Log.d("firebasecheckbox", document.getId() + " => " + document.getData());

                                ArrayList<String> fileName = (ArrayList<String>) data.get("imgs");
                                FirebaseStorage storage = FirebaseStorage.getInstance();
                                StorageReference storageRef = storage.getReference();
                                storageRef.child("image").child(fileName.get(0))
                                        .getBytes(10*1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                            @Override
                                            public void onSuccess(byte[] bytes) {
                                                if(!adapter.items.contains(rItem))
                                                    return;
                                                rItem.setMainImg(bytes);
                                                adapter.notifyDataSetChanged();
                                            }
                                        });
                                Log.d("final_log", "좋아요 적용해서 DB에서 가져오기");

                            }
                        } else {
                            Log.d("firebasecheckbox", "Error getting documents: ", task.getException());
                        }
                    }
                });
            }
        });
    }

    private static void setSearchViewOnClickListener(View v, View.OnClickListener listener) {
        if (v instanceof ViewGroup) {
            ViewGroup group = (ViewGroup)v;
            int count = group.getChildCount();
            for (int i = 0; i < count; i++) {
                View child = group.getChildAt(i);
                if (child instanceof LinearLayout || child instanceof RelativeLayout) {
                    setSearchViewOnClickListener(child, listener);
                }

                if (child instanceof TextView) {
                    TextView text = (TextView)child;
                    text.setFocusable(false);
                }
                child.setOnClickListener(listener);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode==RESULT_OK){
            ArrayList<String> places=data.getStringArrayListExtra("near");
            adapter.clearItem();
            Query query=filterReviews();
            query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                @Override
                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Map<String, Object> data = document.getData();
                            ArrayList<String> fans=(ArrayList<String>) data.get("fans");
                            boolean isHeart=fans.contains(user.getUid());
                            ReviewItem rItem=new ReviewItem((String) data.get("tradeType"), (Long) data.get("rentCost"), (Long) data.get("depositCost"), (Long) data.get("area"), (Long) data.get("floor"), (String) data.get("address"), (Double) data.get("star"), (String) data.get("isTrading"), document.getId(),isHeart);
                            adapter.addItem(rItem);

                            ArrayList<String> fileName = (ArrayList<String>) data.get("imgs");
                            FirebaseStorage storage = FirebaseStorage.getInstance();
                            StorageReference storageRef = storage.getReference();
                            storageRef.child("image").child(fileName.get(0))
                                    .getBytes(10*1024 * 1024).addOnSuccessListener(new OnSuccessListener<byte[]>() {
                                        @Override
                                        public void onSuccess(byte[] bytes) {
                                            if(!adapter.items.contains(rItem))
                                                return;
                                            rItem.setMainImg(bytes);
                                            adapter.notifyDataSetChanged();
                                        }
                                    });
                            Log.d("final_log", "거리 정보 적용해서 가까운 리뷰만 DB에서 가져오기");
                        }
                        adapter.removeFarReview(places);
                    } else {
                    }
                }
            });
        }
    }
}