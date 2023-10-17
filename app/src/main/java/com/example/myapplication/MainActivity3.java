package com.example.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.*;
import okhttp3.Request;
import okhttp3.RequestBody;

import org.json.JSONObject;

public class MainActivity3 extends AppCompatActivity {
    Single_ton_data mydata = Single_ton_data.getInstance();
    String directoryPath;
    File directory;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> cer_cameraLauncher;
    private String ocr_result;
    ImageView imageView;
    ImageButton back;
    Button certified;
    Button submit;
    CheckBox checkBox;
    String name;
    String idNumber;
    String gender;
    String institution;
    String phone_num;
    String identify;
    String password;
    File user_face;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);//다크모드 삭제
        setContentView(R.layout.activity_3_jtm);

        imageView = findViewById(R.id.imageView2);
        back = findViewById(R.id.ibtn_Backlog);
        certified = findViewById(R.id.btn_cer);
        checkBox = findViewById(R.id.cb_MJ);
        submit = findViewById(R.id.btn_loginGo);

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish(); // 현재 액티비티를 종료하고 이전 화면으로 돌아가기
            }
        });
        certified.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {cer_takePicture();}
        });
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                identify = ((EditText) findViewById(R.id.etv_ID)).getText().toString();
                password = ((EditText) findViewById(R.id.etv_pw)).getText().toString();
                phone_num = ((EditText) findViewById(R.id.PhoneNum)).getText().toString();
                if (phone_num.length() == 11) {
                    phone_num = phone_num.substring(0, 3) + "-" + phone_num.substring(3, 7) + "-" + phone_num.substring(7);
                }
                //Sign_in(user_face); //얼굴 사진 나중에 AI에서 비교
                showFinalDialog(MainActivity3.this ,identify, password, name, idNumber, phone_num, gender, institution); //최종확인 다이얼로그
            }
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);
        }

        //얼굴사진카메라
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Bundle extras = result.getData().getExtras();
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            imageView.setImageBitmap(imageBitmap);
                            directoryPath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();
                            directory = new File(directoryPath);

                            if (!directory.exists()) {
                                if (!directory.mkdirs()) {
                                    Log.e("Directory Creation Error", "Failed to create directory.");
                                }
                            }
                            user_face = new File(directory, "Sign_img.jpg");
                            try {
                                FileOutputStream fos = new FileOutputStream(user_face);
                                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                fos.flush();
                                fos.close();
                                Log.e("image storage", "Success: " + user_face.getAbsolutePath());
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e("image storage", "Fail: " + e.getMessage());
                            }
                        }
                    }
                });

        //ocr카메라
        cer_cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            Bundle extras = result.getData().getExtras();
                            Bitmap imageBitmap = (Bitmap) extras.get("data");
                            directoryPath = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString();
                            directory = new File(directoryPath);

                            if (!directory.exists()) {
                                if (!directory.mkdirs()) {
                                    Log.e("Directory Creation Error", "Failed to create directory.");
                                }
                            }

                            // 이미지를 파일로 저장
                            File internalFile = new File(directory, "my_image.jpg");
                            try {
                                FileOutputStream fos = new FileOutputStream(internalFile);
                                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                                fos.flush();
                                fos.close();
                                Log.e("image storage", "Success: " + internalFile.getAbsolutePath());
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e("image storage", "Fail: " + e.getMessage());
                            }


                            // 서버로 이미지를 업로드
                            send2Server(internalFile);
                            //information = ocr_result;
                            //showInfoDialog(MainActivity3.this, "정보 확인", information);
                        }
                    }
                });
    }
    public void takePicture(View view1) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            cameraLauncher.launch(takePictureIntent);
        }
    }
    public void cer_takePicture() {
        Intent cer_takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cer_takePictureIntent.resolveActivity(getPackageManager()) != null) {
            showConfirmationDialog(MainActivity3.this, "부탁드립니다.", "현재 워터마크로 인하여 기술 부족으로 캡쳐시 배율을 높게하여 캡쳐하시기 바랍니다.");
        }
    }
    public void showInfoDialog(Context context, String name, String idNumber, String gender, String institution) { //v2
        LayoutInflater inflater = LayoutInflater.from(context);
        View viewX1 = inflater.inflate(R.layout.activity_3_x1_dialogocrtext, null);
        TextView tvName = viewX1.findViewById(R.id.tv_ocrresult_name);
        TextView tvIdNumber = viewX1.findViewById(R.id.tv_ocrresult_Num);
        TextView tvGender = viewX1.findViewById(R.id.tv_ocrresult_mw);
        TextView tvInstitution = viewX1.findViewById(R.id.tv_ocrresult_ca);

        tvName.setText(name);
        tvIdNumber.setText(idNumber);
        tvGender.setText(gender);
        tvInstitution.setText(institution);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setView(viewX1) // 이 부분을 수정하여 커스텀 뷰를 설정합니다.
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 사용자가 확인 버튼을 눌렀을 때 수행할 동작을 여기에 추가
                        Confirmation();
                    }
                })
                .setNeutralButton("수정", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Editing();
                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Cancellation();
                    }
                })
                .show(); // 다이얼로그 표시
    }
    public void showConfirmationDialog(Context context, String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent cer_takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        if (cer_takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
                            cer_cameraLauncher.launch(cer_takePictureIntent);
                        }
                        dialog.dismiss(); // 다이얼로그를 닫습니다.
                    }
                })
                .show(); // 다이얼로그를 표시합니다.
    }
    public void showEditableDialog(Context context, String edname, String edidNum, //v2
                                   String edgender, String edins, final OnValuesEditedListener listener) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View viewX2 = inflater.inflate(R.layout.activity_3_x2_dialogedit, null);
        EditText etName = viewX2.findViewById(R.id.etv_ocrresult_name);
        EditText etIdNumber = viewX2.findViewById(R.id.etv_ocrresult_num);
        EditText etGender = viewX2.findViewById(R.id.etv_ocrresult_gender);
        EditText etInstitution = viewX2.findViewById(R.id.etv_ocrresult_ca);

        etName.setText(edname);
        etIdNumber.setText(edidNum);
        etGender.setText(edgender);
        etInstitution.setText(edins);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(viewX2)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 사용자가 수정한 값을 가져옵니다.
                        name = etName.getText().toString();
                        idNumber = etIdNumber.getText().toString();
                        gender = etGender.getText().toString();
                        institution = etInstitution.getText().toString();

                        // 리스너를 통해 수정된 값을 전달합니다.
                        if (listener != null) {
                            listener.onValues_Edited(name, idNumber, gender, institution);
                        }
                        Toast.makeText(context, "완료하였습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(context, "취소하였습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show(); // 다이얼로그 표시
    }
    public void showFinalDialog(Context context, String f_id, String f_pw, String f_name, String f_idNum, String f_pNum, String f_gender, String f_ins) { //v2
        LayoutInflater inflater = LayoutInflater.from(context);
        View viewY1 = inflater.inflate(R.layout.activity_3_y1_final_dialogcheck, null);

        TextView tvId = viewY1.findViewById(R.id.tv_final_result_ID);
        TextView tvPw = viewY1.findViewById(R.id.tv_final_result_PW);
        TextView tvName = viewY1.findViewById(R.id.tv_final_result_Name);
        TextView tvGender = viewY1.findViewById(R.id.tv_final_result_Gender);
        TextView tvPNum = viewY1.findViewById(R.id.tv_mypage_rName);
        TextView tvINum = viewY1.findViewById(R.id.tv_final_result_INum);
        TextView tvIns = viewY1.findViewById(R.id.tv_final_result_Ins);

        tvId.setText(f_id);
        tvPw.setText(f_pw);
        tvName.setText(f_name);
        tvINum.setText(f_idNum);
        tvPNum.setText(f_pNum);
        tvGender.setText(f_gender);
        tvIns.setText(f_ins);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setView(viewY1) // 이 부분을 수정하여 커스텀 뷰를 설정합니다.
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity3.this, "가입완료 ", Toast.LENGTH_SHORT).show();
                        Sign_in(user_face);
                        go_Login();
                    }
                })
                .setNeutralButton("수정", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { final_Editing();}
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Cancellation();
                    }
                })
                .show(); // 다이얼로그 표시
    }
    public void showEditable_FinalDialog(Context context, String F_id, String F_pw, String F_name, String F_idNum, String F_pNum, String F_gender, String F_ins, OnValuesFinalEditedListener listener) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View viewY2 = inflater.inflate(R.layout.activity_3_y2_final_dialogedit, null);
        EditText FE_Id = viewY2.findViewById(R.id.etv_final_result_ID); //최종 아이디
        EditText FE_Pw = viewY2.findViewById(R.id.etv_final_result_PW); //최종 비번
        EditText FE_Name = viewY2.findViewById(R.id.etv_final_result_name); //최종 이름
        EditText FE_IdNumber = viewY2.findViewById(R.id.etv_final_result_num); //최종 민증번호
        EditText FE_PNumber = viewY2.findViewById(R.id.etv_final_result_PNum); //최종 폰번호
        EditText FE_Gendaer = viewY2.findViewById(R.id.etv_final_result_gender); //최종 성별
        EditText FE_Institution = viewY2.findViewById(R.id.etv_final_result_ca); //최종 인증 기관

        FE_Id.setText(F_id);
        FE_Pw.setText(F_pw);
        FE_Name.setText(F_name);
        FE_IdNumber.setText(F_idNum);
        FE_PNumber.setText(F_pNum);
        FE_Gendaer.setText(F_gender);
        FE_Institution.setText(F_ins);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(viewY2)
                .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 사용자가 수정한 값을 가져옵니다.
                        identify = FE_Id.getText().toString();
                        password = FE_Pw.getText().toString();
                        name = FE_Name.getText().toString();
                        idNumber = FE_IdNumber.getText().toString();
                        phone_num = FE_PNumber.getText().toString();
                        gender = FE_Gendaer.getText().toString();
                        institution = FE_Institution.getText().toString();

                        // 리스너를 통해 수정된 값을 전달합니다.
                        if (listener != null) {
                            listener.onValues_FinalEdited(identify, password, name, idNumber, phone_num, gender, institution);
                            Sign_in(user_face); //얼굴 사진 나중에 AI에서 비교
                            go_Login();
                        }
                    }
                })
                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(context, "취소하였습니다.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show(); // 다이얼로그 표시
    }
    public interface OnValuesEditedListener {
        void onValues_Edited(String editedName, String editedIdNumber, String editedGender, String editedInstitution);
    }
    public interface OnValuesFinalEditedListener {
        void onValues_FinalEdited(String fe_Id, String fe_Pw, String fe_Name, String fe_IdNum, String fe_Pnum, String fe_Gender, String fe_Ins);
    }
    private void Confirmation() {
        // 안내문자서 확인 클릭시
        // 예: 다음 화면으로 이동, 데이터 저장, 기타 작업 수행
        Toast.makeText(getApplicationContext(), "완료하였습니다.", Toast.LENGTH_SHORT).show();
        checkBox.setChecked(true);
    }
    private void Cancellation() {
        // 안내문자서 취소 클릭시
        Toast.makeText(getApplicationContext(), "취소하였습니다.", Toast.LENGTH_SHORT).show();
        checkBox.setChecked(false);
    }
    private void Editing() {
        // 안내문자서 수정 클릭시
        showEditableDialog(this, name, idNumber, gender, institution,
                new OnValuesEditedListener() {
                    @Override
                    public void onValues_Edited(String editedName, String editedIdNumber, String editedGender, String editedInstitution) {
                        // 사용자가 수정한 값을 사용
                        // 이곳에서 수정된 값을 사용하거나 처리
                        // editedName: 수정된 이름, editedIdNumber: 수정된 주민번호, editedGender: 수정된 성별, editedInstitution: 수정된 인증기관
                        // 예시로 수정된 이름을 Toast 메시지로 출력하는 예시:
                        Toast.makeText(MainActivity3.this, "수정완료 ", Toast.LENGTH_SHORT).show();
                        checkBox.setChecked(true);
                    }
                });
    }
    private void final_Editing() {
        // 안내문자서 수정 클릭시
        showEditable_FinalDialog(this, identify, password, name, idNumber, phone_num, gender, institution,
                new OnValuesFinalEditedListener() {
                    @Override
                    public void onValues_FinalEdited( String fe_Id, String fe_Pw, String fe_Name, String fe_IdNum, String fe_Pnum, String fe_Gender, String fe_Ins) {
                        // 사용자가 수정한 값을 사용
                        // 이곳에서 수정된 값을 사용하거나 처리
                        // editedName: 수정된 이름, editedIdNumber: 수정된 주민번호, editedGender: 수정된 성별, editedInstitution: 수정된 인증기관
                        // 예시로 수정된 이름을 Toast 메시지로 출력하는 예시:
                        Toast.makeText(MainActivity3.this, "가입완료 ", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    public void go_Login() {
        Intent login = new Intent(getApplicationContext(), MainActivity2.class);
        startActivity(login);
    }
    public void send2Server(File file) { // 서버로 보내기
        MediaType MEDIA_TYPE = MediaType.parse("image/jpeg");

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(), RequestBody.create(MEDIA_TYPE, file))
                .build();

        Request request = new Request.Builder()
                .url("http://15.164.120.162:5000/ocr_test")
                .post(requestBody)
                .build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();

                    try {
                        JSONObject json = new JSONObject(responseBody);

                        // 여기에서 JSON 데이터를 처리하고 원하는 정보 추출
                        String status = json.getString("status");
                        String result = json.getString("result");

                        Log.d("JSON Response: ", "status: " + status);
                        Log.d("JSON Response: ", "result: " + result);
                        ocr_result = result;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                filteringData(ocr_result); //데이터 필터링
                                //showInfoDialog(MainActivity3.this,ocr_result);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(getApplicationContext(), "인식오류! 다시 캡쳐해주세요. ", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // 응답이 성공적이지 않은 경우에 대한 처리
                    Log.e("Response Error", "Response Code: " + response.code());
                }
            }
        });
    }
    public void filteringData(String result){
        //String ocrResult = "주민등록증\n홍길동(빠좀께)\n800101-2345678\n세울특별시 가산디지털1로\n서울특별시 금천구청장";
        String ocrResult = result;

        // 이름 추출
        Pattern name_Pattern = Pattern.compile("(.+)\\(");
        Matcher name_Matcher = name_Pattern.matcher(ocrResult);
        name = "이름 미확인";
        if (name_Matcher.find()) { name = name_Matcher.group(1);}

        // 주민번호 추출
        Pattern id_Pattern = Pattern.compile("(\\d{6}-\\d{7})");
        Matcher id_Matcher = id_Pattern.matcher(ocrResult);
        idNumber = id_Matcher.find() ? id_Matcher.group() : "주민번호 미확인";

        // 성별 추출
        int gender_Digit = Integer.parseInt(idNumber.substring(7, 8));
        gender = (gender_Digit % 2 == 1) ? "남자" : "여자";

        // 기관 추출
        String[] words = ocrResult.split("\\s+");
        institution = words[words.length - 2] +" "+ words[words.length - 1];// 마지막 단어를 추출

        //오류
        char targetChar = '(';
        if (institution.startsWith(String.valueOf(targetChar))) {
            Toast.makeText(MainActivity3.this, "인식이 안되었습니다.", Toast.LENGTH_SHORT).show();
            Toast.makeText(MainActivity3.this, "다시 캡쳐해주세요.", Toast.LENGTH_SHORT).show();
        } else if(name.equals("이름 미확인") || idNumber.equals("주민번호 미확인")){
            Toast.makeText(MainActivity3.this, "인식이 안되었습니다.", Toast.LENGTH_SHORT).show();
            Toast.makeText(MainActivity3.this, "다시 캡쳐해주세요.", Toast.LENGTH_SHORT).show();
        }
        else {showInfoDialog(this, name, idNumber, gender, institution);}
    }
    public void Sign_in(File file) { // 서버로 보내기
        MediaType MEDIA_TYPE = MediaType.parse("image/jpeg");

        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", file.getName(), RequestBody.create(MEDIA_TYPE, file))
                .addFormDataPart("name",name)
                .addFormDataPart("jumin",idNumber)
                .addFormDataPart("gender",gender)
                .addFormDataPart("phone_number",phone_num)
                .addFormDataPart("id",identify)
                .addFormDataPart("pw",password)
                .build();

        Request request = new Request.Builder()
                .url("http://15.164.120.162:5000//Sign_up")
                .post(requestBody)
                .build();

        OkHttpClient client = new OkHttpClient();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();

                    try {
                        JSONObject json = new JSONObject(responseBody);

                        // 여기에서 JSON 데이터를 처리하고 원하는 정보 추출
                        String status = json.getString("status");

                        Log.d("JSON Response: ", "status: " + status);

                        if (status=="success"){
                            //Toast.makeText(MainActivity3.this, "가입성공 ", Toast.LENGTH_SHORT).show();
                            Intent login = new Intent(getApplicationContext(), MainActivity2.class);
                            startActivity(login);
                        }else{
                            //Toast.makeText(MainActivity3.this, "가입실패 ", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // 응답이 성공적이지 않은 경우에 대한 처리
                    Log.e("Response Error", "Response Code: " + response.code());
                }
            }
        });
    }
}