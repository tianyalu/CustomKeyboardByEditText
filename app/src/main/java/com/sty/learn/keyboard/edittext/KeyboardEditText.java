package com.sty.learn.keyboard.edittext;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.Keyboard.Key;
import android.inputmethodservice.KeyboardView;
import android.os.Build;
import android.os.SystemClock;
import android.text.Editable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.PopupWindow;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
/**
 * Created by shity on 2017/6/22/0022.
 */

public class KeyboardEditText extends EditText implements KeyboardView.OnKeyboardActionListener {

    private static final String TAG = "myLog";
    private Keyboard mKeyboard;
    private KeyboardView mKeyboardView;

    private Window mWindow;
    private View mDecorView;
    private View mContentView;

    private PopupWindow mKeyboardWindow;

    private boolean isNeedCustomKeyboard = true; // 是否启用自定义键盘
    private boolean isRandomKeys = false;        // 是否数字按键随机
    /**
     * 输入框在被键盘弹出时，要被推上的距离
     */
    private int mScrollDistance = 0;

    public static int screenWidth = -1; // 未知宽高
    public static int screenHeight = -1;

    /**
     * 不包含导航栏的高度
     */
    public static int screenHeightNoNavBar = -1;
    /**
     * 实际内容高度，  计算公式:屏幕高度-导航栏高度-电量栏高度
     */
    public static int screenContentHeight = -1;
    public static float density = 1.0f;
    public static int densityDpi = 160;

    public KeyboardEditText(Context context) {
        this(context, null);
    }

    public KeyboardEditText(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public KeyboardEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributes(context);
        initKeyboard(context, attrs);
    }

    private void initKeyboard(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.Keyboard);
        if (array.hasValue(R.styleable.Keyboard_xml)) {
            Log.d(TAG,"hasValue Keyboard_xml");
            isNeedCustomKeyboard = true;
            int xmlId = array.getResourceId(R.styleable.Keyboard_xml, 0);
            mKeyboard = new Keyboard(context, xmlId);

            mKeyboardView = (KeyboardView) LayoutInflater.from(context).inflate(R.layout.my_keyboard_view, null);
            if (array.hasValue(R.styleable.Keyboard_random_keys)) {
                Log.d(TAG,"hasValue Keyboard_random_keys");
                boolean isRandom = array.getBoolean(R.styleable.Keyboard_random_keys, false);
                isRandomKeys = isRandom;
                if (isRandom) {
                    digRandomKey(mKeyboard);
                }
            }

            mKeyboardView.setKeyboard(mKeyboard);
            mKeyboardView.setEnabled(true);
            mKeyboardView.setPreviewEnabled(false);
            mKeyboardView.setOnKeyboardActionListener(this);

            mKeyboardWindow = new PopupWindow(mKeyboardView,
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            mKeyboardWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    if (mScrollDistance > 0) {
                        int temp = mScrollDistance;
                        mScrollDistance = 0;
                        if (null != mContentView) {
                            mContentView.scrollBy(0, -temp);
                        }
                    }
                }
            });
        } else {
            isNeedCustomKeyboard = false;
        }

        array.recycle();
    }

    private void digRandomKey(Keyboard keyboard) {
        if (keyboard == null) {
            return;
        }

        List<Key> keyList = keyboard.getKeys();

        // 查找出0-9的数字键
        List<Key> newKeyList = new ArrayList<Key>();
        for (int i = 0, size = keyList.size(); i < size; i++) {
            Key key = keyList.get(i);
            CharSequence label = key.label;
            if (label != null && isNumber(label.toString())) {
                newKeyList.add(key);
            }
        }

        int count = newKeyList.size();
        List<KeyModel> resultList = new ArrayList<KeyModel>();
        LinkedList<KeyModel> tempList = new LinkedList<KeyModel>();

        for (int i = 0; i < count; i++) {
            tempList.add(new KeyModel(48 + i, i + ""));
        }

        Random rand = new SecureRandom();
        rand.setSeed(SystemClock.currentThreadTimeMillis());

        for (int i = 0; i < count; i++) {
            int num = rand.nextInt(count - i);
            Log.d(TAG, " rand num"+num);
            KeyModel model = tempList.get(num);
            Log.d(TAG, model.toString());
            resultList.add(new KeyModel(model.getCode(), model.getLabel()));
            tempList.remove(num);
        }

        for (int i = 0,size = newKeyList.size(); i < size; i++) {
            Keyboard.Key newKey = newKeyList.get(i);
            KeyModel resultModel = resultList.get(i);
            newKey.label =resultModel.getLabel();
            newKey.codes[0] = resultModel.getCode();
        }
    }

    private boolean isNumber(String str) {
        String wordStr = "0123456789";
        return wordStr.contains(str);
    }

    private void initAttributes(Context context) {
        initScreenParams(context);
        this.setLongClickable(false); // 设置EditText不可长按
        this.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        removeCopyAbility();

        if (this.getText() != null) {
            this.setSelection(this.getText().length());
        }

        this.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    hideKeyboard();
                }
            }
        });
    }

    @TargetApi(11)
    private void removeCopyAbility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            this.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {

                }
            });
        }
    }

    private void initScreenParams(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        display.getMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        density = metrics.density;
        densityDpi = metrics.densityDpi;

        screenHeightNoNavBar = screenHeight;

        int version = Build.VERSION.SDK_INT;
        // 新版本的android 系统有导航栏，造成无法正确获取高度
        if (version == 13) {
            try {
                Method method = display.getClass().getMethod("getRealHeight");
                screenHeightNoNavBar = (Integer) method.invoke(display);
            } catch (Exception e) {
                // do nothing
            }
        } else if (version > 13) {
            try {
                Method method = display.getClass().getMethod("getRawHeight");
                screenHeightNoNavBar = (Integer) method.invoke(display);
            } catch (Exception e) {
                // do nothing
            }
        }

        screenContentHeight = screenHeightNoNavBar - getStatusBarHeight(context);
    }

    /**
     * 获取状态栏高度
     */
    private int getStatusBarHeight(Context context) {
        Class<?> clazz = null;
        Object object = null;
        Field field = null;
        int x = 0;
        int statusBarHeight = 0;
        try {
            clazz = Class.forName("com.android.internal.R$dimen");
            object = clazz.newInstance();
            field = clazz.getField("status_bar_height");
            x = Integer.parseInt(field.get(object).toString());
            statusBarHeight = context.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusBarHeight;
    }


    @Override
    public void onPress(int primaryCode) {

    }

    @Override
    public void onRelease(int primaryCode) {

    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        Editable editable = this.getText();
        int start = this.getSelectionStart();
        if (primaryCode == Keyboard.KEYCODE_CANCEL) {// 隐藏键盘
            hideKeyboard();
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {// 回退
            if (editable != null && editable.length() > 0) {
                if (start > 0) {
                    editable.delete(start - 1, start);
                }
            }
        }else if(0x0<=primaryCode&& primaryCode<=0x7f){
            //可以直接输入的字符(如0-9,.)，他们在键盘映射xml中的keycode值必须配置为该字符的ASCII码
            editable.insert(start, Character.toString((char) primaryCode));
        }
        else if(primaryCode >0x7f){
            Keyboard.Key key = getKeyByKeyCode(primaryCode);
            //可以直接输入的字符(如0-9,.)，他们在键盘映射xml中的keycode值必须配置为该字符的ASCII码
            editable.insert(start, key.label);

        }else{
            //其他一些暂未开放的键指令，如next到下一个输入框等指令
        }
    }

    private Keyboard.Key getKeyByKeyCode(int primaryCode) {
        if(null != mKeyboard){
            List<Key> keyList = mKeyboard.getKeys();
            for (int i =0,size= keyList.size(); i < size; i++) {
                Key key = keyList.get(i);

                int codes[] = key.codes;

                if(codes[0] == primaryCode){
                    return key;
                }
            }
        }

        return null;
    }

    @Override
    public void onText(CharSequence text) {

    }

    @Override
    public void swipeLeft() {

    }

    @Override
    public void swipeRight() {

    }

    @Override
    public void swipeDown() {

    }

    @Override
    public void swipeUp() {

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        requestFocus();
        requestFocusFromTouch();

        if (isNeedCustomKeyboard) {
            hideSysInput();
            showKeyboard();
        }


        return true;
    }

    public boolean onKeyDown(int keyCode , KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_BACK){
            if(null != mKeyboardWindow){
                if(mKeyboardWindow.isShowing()){
                    mKeyboardWindow.dismiss();
                    return true;
                }
            }
        }

        return super.onKeyDown(keyCode, event);

    }

    public void onAttachedToWindow(){
        super.onAttachedToWindow();

        this.mWindow = ((Activity)getContext()).getWindow();
        this.mDecorView =this.mWindow .getDecorView();
        this.mContentView =this.mWindow.findViewById(Window.ID_ANDROID_CONTENT);

        hideSysInput();
    }

    public void onDetachedFromWindow(){
        super.onDetachedFromWindow();

        hideKeyboard();

        mKeyboardWindow = null;
        mKeyboardView = null;
        mKeyboard = null;

        mDecorView = null;
        mContentView = null;
        mWindow = null;
    }

    private void showKeyboard() {
        if (null != mKeyboardWindow) {
            if (!mKeyboardWindow.isShowing()) {
                if (isRandomKeys) {
                    digRandomKey(mKeyboard);
                }

                mKeyboardView.setKeyboard(mKeyboard);
                mKeyboardWindow.showAtLocation(this.mDecorView, Gravity.BOTTOM, 0, 0);
                mKeyboardWindow.update();

                if (null != mDecorView && null != mContentView) {
                    int[] pos = new int[2];
                    getLocationOnScreen(pos);
                    float height = dpToPx(getContext(), 240);

                    int[] hsmlpos = new int[2];
                    mDecorView.getLocationOnScreen(hsmlpos);

                    Rect outRect = new Rect();
                    mDecorView.getWindowVisibleDisplayFrame(outRect);

                    int screen = screenContentHeight;
                    mScrollDistance = (int) ((pos[1] + getMeasuredHeight() - outRect.top) - (screen - height));

                    if (mScrollDistance > 0) {
                        //Move the scrolled position of your view
                        mContentView.scrollBy(0,mScrollDistance);
                    }
                }
            }
        }
    }

    private void hideKeyboard() {
        if (null != mKeyboardWindow) {
            if (mKeyboardWindow.isShowing()) {
                mKeyboardWindow.dismiss();
            }
        }
    }

    /**
     * 密度转换为像素值
     */
    private float dpToPx(Context context, int dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    private void hideSysInput() {
        if (this.getWindowToken() != null) {
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(this.getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }



    class KeyModel {
        private Integer code;
        private String label;

        public KeyModel(Integer code, String label) {
            this.code = code;
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return "code:"+code+","+"label:"+label;
        }
    }
}
