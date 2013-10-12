package de.hdodenhof.holoreader.fragments;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.hdodenhof.holoreader.R;

@SuppressLint("ValidFragment")
public interface DynamicDialogFragment {

    public void setTitle(String title);

    public void setMessage(String message);

    public void setPositiveButtonListener(OnClickListener listener);

    public void setPositiveButtonListener(OnClickListener listener, String text);

    public void setNegativeButtonText(String text);

    public void setLayout(int ressourceID);

    public void setView(View view);

    public void setInitialValues(SparseArray<String> map);

    public void setTag(String tag);

    public void show(FragmentManager fm, String tag);

    public interface OnClickListener {
        public void onClick(DialogFragment df, String tag, SparseArray<String> fieldMap);
    }

    public static class Factory {
        public static DynamicDialogFragment getInstance(Context context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                return new DynamicDialogFragment.HCDialogFragment(context);
            } else {
                return new DynamicDialogFragment.PreHCDialogFragment(context);
            }
        }
    }

    public static class Helper {
        public static void writeValues(LinearLayout layout, SparseArray<String> values) {
            if (values != null) {
                for (int i = 0; i < layout.getChildCount(); i++) {
                    View view = layout.getChildAt(i);
                    if (view instanceof EditText) {
                        int viewID = view.getId();
                        if (values.get(viewID) != null) {
                            ((EditText) view).setText(values.get(viewID));
                        }
                    }
                }
            }
        }

        public static SparseArray<String> readValues(LinearLayout layout) {
            SparseArray<String> map = new SparseArray<String>();
            for (int i = 0; i < layout.getChildCount(); i++) {
                View view = layout.getChildAt(i);
                if (view instanceof EditText) {
                    map.put(view.getId(), ((EditText) view).getText().toString());
                }
            }
            return map;
        }
    }

    public abstract class AbstractDynamicDialogFragment extends DialogFragment implements DynamicDialogFragment {

        protected int mContentRessource;
        protected View mView;
        protected OnClickListener mPositiveButtonListener;
        protected String mTitle;
        protected String mMessage;
        protected String mPositiveButtonText;
        protected String mNegativeButtonText;
        protected SparseArray<String> mValues;
        protected String mTag;

        @Override
        public void setTitle(String title) {
            mTitle = title;
        }

        @Override
        public void setMessage(String message) {
            mMessage = message;
        }

        @Override
        public void setPositiveButtonListener(OnClickListener listener) {
            mPositiveButtonListener = listener;
        }

        @Override
        public void setPositiveButtonListener(OnClickListener listener, String text) {
            mPositiveButtonListener = listener;
            mPositiveButtonText = text;
        }

        @Override
        public void setNegativeButtonText(String text) {
            mNegativeButtonText = text;
        }

        @Override
        public void setLayout(int ressourceID) {
            mContentRessource = ressourceID;
        }

        @Override
        public void setView(View view) {
            mView = view;
        }

        @Override
        public void setInitialValues(SparseArray<String> map) {
            mValues = map;
        }

        @Override
        public void setTag(String tag) {
            mTag = tag;
        }

        @Override
        public void show(FragmentManager fm, String tag) {
            // FIXME
            try {
                super.show(fm, tag);
            } catch (IllegalStateException e) {
                // ignore
            }
        }

        // workaround for #17423 in AOSP
        @Override
        public void onDestroyView() {
            if (getDialog() != null && getRetainInstance())
                getDialog().setDismissMessage(null);
            super.onDestroyView();
        }

    }

    public class HCDialogFragment extends AbstractDynamicDialogFragment {

        public HCDialogFragment() {
        }

        public HCDialogFragment(Context context) {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setStyle(STYLE_NO_TITLE, 0);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_dialogframe, container);

            FrameLayout contentFrame = (FrameLayout) rootView.findViewById(R.id.content);
            TextView title = (TextView) rootView.findViewById(R.id.title);
            Button buttonOk = (Button) rootView.findViewById(R.id.buttonOk);
            Button buttonCancel = (Button) rootView.findViewById(R.id.buttonCancel);
            Button buttonNeutral = (Button) rootView.findViewById(R.id.buttonNeutral);

            final View content;
            if (mContentRessource != 0) {
                content = inflater.inflate(mContentRessource, null);
                Helper.writeValues((LinearLayout) content, mValues);
            } else if (mView != null) {
                content = mView;
            } else if (mMessage != null) {
                content = inflater.inflate(R.layout.fragment_dialog_message, null);
                TextView messageView = (TextView) content.findViewById(R.id.message);
                messageView.setText(mMessage);
            } else {
                throw new IllegalArgumentException();
            }

            LayoutParams params = contentFrame.getLayoutParams();
            params.height = LayoutParams.WRAP_CONTENT;
            params.width = LayoutParams.MATCH_PARENT;
            contentFrame.addView(content, params);

            title.setText(mTitle);

            if (mPositiveButtonListener != null) {
                buttonOk.setVisibility(View.VISIBLE);
                buttonCancel.setVisibility(View.VISIBLE);
                buttonNeutral.setVisibility(View.GONE);

                if (mPositiveButtonText != null) {
                    buttonOk.setText(mPositiveButtonText);
                }
                buttonOk.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        SparseArray<String> map = null;
                        if (mContentRessource != 0) {
                            map = Helper.readValues((LinearLayout) content);
                        }
                        mPositiveButtonListener.onClick(HCDialogFragment.this, mTag, map);
                    }
                });
                if (mNegativeButtonText != null) {
                    buttonCancel.setText(mNegativeButtonText);
                }
                buttonCancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HCDialogFragment.this.dismiss();
                    }
                });
            } else {
                buttonNeutral.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        HCDialogFragment.this.dismiss();
                    }
                });
            }

            getDialog().setCanceledOnTouchOutside(false);

            return rootView;
        }
    }

    public class PreHCDialogFragment extends AbstractDynamicDialogFragment {

        private Context mContext;
        private AlertDialog.Builder mAlertDialog;
        private View mRootView;

        public PreHCDialogFragment() {
        }

        public PreHCDialogFragment(Context context) {
            mContext = context;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            mAlertDialog = new AlertDialog.Builder(mContext);
            mAlertDialog.setTitle(mTitle);

            if (mContentRessource != 0) {
                mRootView = ((FragmentActivity) mContext).getLayoutInflater().inflate(mContentRessource, null);
                Helper.writeValues((LinearLayout) mRootView, mValues);
                mAlertDialog.setView(mRootView);
            } else if (mView != null) {
                mRootView = mView;
                mAlertDialog.setView(mRootView);
            } else if (mMessage != null) {
                mAlertDialog.setMessage(mMessage);
            } else {
                throw new IllegalArgumentException();
            }

            if (mPositiveButtonListener != null) {
                mAlertDialog.setPositiveButton(mPositiveButtonText != null ? mPositiveButtonText : getString(R.string.PositiveButton),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SparseArray<String> map = null;
                                if (mContentRessource != 0) {
                                    map = Helper.readValues((LinearLayout) mRootView);
                                }
                                mPositiveButtonListener.onClick(PreHCDialogFragment.this, mTag, map);
                            }
                        });

                mAlertDialog.setNegativeButton(mNegativeButtonText != null ? mNegativeButtonText : getString(R.string.NegativeButton),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                PreHCDialogFragment.this.dismiss();
                            }
                        });
            } else {
                mAlertDialog.setNeutralButton(getString(R.string.NeutralButton), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PreHCDialogFragment.this.dismiss();
                    }
                });
            }

            Dialog dialog = mAlertDialog.create();
            dialog.setCanceledOnTouchOutside(false);
            return dialog;
        }
    }
}
