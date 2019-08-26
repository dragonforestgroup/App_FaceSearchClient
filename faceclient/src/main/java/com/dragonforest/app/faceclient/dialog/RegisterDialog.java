package com.dragonforest.app.faceclient.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dragonforest.app.faceclient.R;


/**
 * @author 韩龙林
 * @date 2019/8/6 09:33
 */
public class RegisterDialog extends Dialog {

    private EditText ed_name;
    private EditText ed_groupId;
    private EditText ed_sex;
    private Button btn_ok;
    private Button btn_cancel;

    private OnRegisterListener onRegisterListener;

    public RegisterDialog(Context context) {
        super(context);
        init();
    }

    public RegisterDialog(Context context, int themeResId) {
        super(context, themeResId);
        init();
    }

    private void init() {
        setContentView(R.layout.dialog_register);
        initView();
    }

    private void initView() {
        ed_name = findViewById(R.id.ed_name);
        ed_groupId = findViewById(R.id.ed_groupId);
        ed_sex = findViewById(R.id.ed_sex);
        btn_ok = findViewById(R.id.btn_ok);
        btn_cancel = findViewById(R.id.btn_cancel);

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onRegisterListener != null) {
                    onRegisterListener.onCancel();
                }
                dismiss();
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = ed_name.getText().toString();
                String sex = ed_sex.getText().toString();
                String groupIdStr = ed_groupId.getText().toString();
                if (name.equals("") || groupIdStr.equals("")) {
                    Toast.makeText(getContext(), "姓名和组织不能为空值！", Toast.LENGTH_SHORT).show();
                    return;
                }
                int groupId = 0;
                try {
                    groupId = Integer.parseInt(groupIdStr);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (onRegisterListener != null) {
                    onRegisterListener.onRegister(name, sex, groupId);
                }
                dismiss();
            }
        });
    }

    public void setData(String namme, int groupId, String sex) {
        ed_name.setText(namme + "");
        ed_groupId.setText(groupId + "");
        ed_sex.setText(sex + "");
    }

    public void setOnRegisterListener(OnRegisterListener onRegisterListener) {
        this.onRegisterListener = onRegisterListener;
    }

    public interface OnRegisterListener {
        /**
         * 取消回调
         */
        void onCancel();

        /**
         * 注册回调
         *
         * @param name
         * @param sex
         * @param groupId
         */
        void onRegister(String name, String sex, int groupId);
    }
}
