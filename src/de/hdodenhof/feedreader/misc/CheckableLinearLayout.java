package de.hdodenhof.feedreader.misc;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.LinearLayout;

import de.hdodenhof.feedreader.R;

public class CheckableLinearLayout extends LinearLayout implements Checkable {
    private CheckBox mCheckbox;

    public CheckableLinearLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mCheckbox = (CheckBox) findViewById(R.id.list_item_editfeed_checkbox);
    }

    @Override
    public boolean isChecked() {
        return mCheckbox != null ? mCheckbox.isChecked() : false;
    }

    @Override
    public void setChecked(boolean checked) {
        if (mCheckbox != null) {
            mCheckbox.setChecked(checked);
        }
    }

    @Override
    public void toggle() {
        if (mCheckbox != null) {
            mCheckbox.toggle();
        }
    }
}
