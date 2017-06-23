package com.sty.learn.keyboard.edittext;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
/**
 * Created by shity on 2017/6/22/0022.
 */

public class MyKeyboardView extends KeyboardView {

    private Drawable mKeyBgDrawable;
    private Drawable mOpKeyBgDrawable;
    private Resources mRes;

    public MyKeyboardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MyKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initResources(context);
    }

    private void initResources(Context context) {
        mRes = context.getResources();
        mKeyBgDrawable = mRes.getDrawable(R.drawable.btn_keyboard_key);
        mOpKeyBgDrawable = mRes.getDrawable(R.drawable.btn_keyboard_opkey);
    }

    @Override //Implement this to do your drawing.
    public void onDraw(Canvas canvas) {
        List<Keyboard.Key> keys = getKeyboard().getKeys();
        for (Keyboard.Key key : keys) {
            canvas.save();

            int offsetY = 0;
            if (key.y == 0) {
                offsetY = 1;
            }
            int initDrawY = key.y + offsetY;
            Rect rect = new Rect(key.x, initDrawY, key.x + key.width, key.y + key.height);
            canvas.clipRect(rect);

            int primaryCode = -1;
            if (null != key.codes && key.codes.length != 0) {
                primaryCode = key.codes[0];
            }

            Drawable drawable = null;
            if (primaryCode == -3 || key.codes[0] == -5) {
                drawable = mOpKeyBgDrawable;
            } else if (primaryCode != -1) {
                drawable = mKeyBgDrawable;
            }

            if (null != drawable) {
                int[] state = key.getCurrentDrawableState();
                drawable.setState(state);
                drawable.setBounds(rect);
                drawable.draw(canvas);
            }

            Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(50);
            paint.setColor(mRes.getColor(R.color.black));

            if (key.label != null) {
                canvas.drawText(
                        key.label.toString(),
                        key.x + (key.width / 2),
                        initDrawY + (key.height + paint.getTextSize() - paint.descent()) / 2,
                        paint
                );
            } else if (key.icon != null) {
                int intriWidth = key.icon.getIntrinsicWidth();
                int intriHeight = key.icon.getIntrinsicHeight();

                final int drawableX = key.x + (key.width - intriWidth) / 2;
                final int drawableY = initDrawY + (key.height - intriHeight) / 2;

                key.icon.setBounds(drawableX, drawableY,
                        drawableX + intriWidth, drawableY + intriHeight);
                key.icon.draw(canvas);
            }

            canvas.restore();
        }
    }

}