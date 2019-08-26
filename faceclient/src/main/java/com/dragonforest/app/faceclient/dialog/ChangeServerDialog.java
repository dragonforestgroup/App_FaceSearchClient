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
public class ChangeServerDialog extends Dialog {

    private EditText ed_ip;
    private EditText ed_port;
    private Button btn_ok;
    private Button btn_cancel;

    private OnChangeListener onChangeListener;

    public ChangeServerDialog(Context context) {
        super(context);
        init();
    }

    public ChangeServerDialog(Context context, int themeResId) {
        super(context, themeResId);
        init();
    }

    private void init() {
        setContentView(R.layout.dialog_changeserver);
        initView();
    }

    private void initView() {
        ed_ip = findViewById(R.id.ed_ip);
        ed_port = findViewById(R.id.ed_port);
        btn_ok = findViewById(R.id.btn_ok);
        btn_cancel = findViewById(R.id.btn_cancel);

        btn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onChangeListener != null) {
                    onChangeListener.onCancel();
                }
                dismiss();
            }
        });

        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = ed_ip.getText().toString();
                String port = ed_port.getText().toString();
                if (ip.equals("")) {
                    Toast.makeText(getContext(), "服务ip不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (onChangeListener != null) {
                    onChangeListener.onChange(ip, port);
                }
                dismiss();
            }
        });
    }

    public void setData(String ip, String port) {
        ed_port.setText(port);
        ed_ip.setText(ip);
    }

    public void setOnChangeListener(OnChangeListener onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

    public interface OnChangeListener {
        /**
         * 取消回调
         */
        void onCancel();

        /**
         * 修改回调
         *
         * @param ip
         * @param port
         */
        void onChange(String ip, String port);
    }
}
