/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.incallui.incall.impl;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.SoundEffectConstants;
import android.widget.Checkable;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.android.dialer.R;
import com.google.android.material.button.MaterialButton;

/**
 * A Material Design 3 button with icon above text for the in-call screen.
 * This is a replacement for CheckableLabeledButton using MaterialButton.
 */
public class MaterialLabeledButton extends MaterialButton implements Checkable {

    private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
    private static final float DISABLED_STATE_OPACITY = 0.38f;

    private boolean broadcasting;
    private boolean mIsChecked;
    private OnCheckedChangeListener onCheckedChangeListener;
    private boolean shouldShowMoreIndicator;
    @DrawableRes private int iconResource = 0;

    public MaterialLabeledButton(@NonNull Context context) {
        this(context, null);
    }

    public MaterialLabeledButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.materialButtonStyle);
    }

    public MaterialLabeledButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        // Configure for icon above text layout
        setIconGravity(MaterialButton.ICON_GRAVITY_TOP);
        setGravity(Gravity.CENTER);
        setInsetTop(0);
        setInsetBottom(0);

        // Read custom attributes
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CheckableLabeledButton);
            Drawable icon = typedArray.getDrawable(R.styleable.CheckableLabeledButton_incall_icon);
            CharSequence labelText = typedArray.getString(R.styleable.CheckableLabeledButton_incall_labelText);
            boolean enabled = typedArray.getBoolean(R.styleable.CheckableLabeledButton_android_enabled, true);
            typedArray.recycle();

            if (icon != null) {
                setIcon(icon);
            }
            if (labelText != null) {
                setText(labelText);
            }
            setEnabled(enabled);
        }

        // Configure text appearance
        setTextAppearance(R.style.Dialer_Incall_TextAppearance_Label);

        // Configure sizing
        int iconSize = getResources().getDimensionPixelSize(R.dimen.incall_labeled_button_size);
        setMinWidth(iconSize);
        setMinHeight(iconSize);

        int iconImageSize = getResources().getDimensionPixelSize(R.dimen.incall_labeled_button_icon_size);
        setIconSize(iconImageSize);

        // Set icon padding for spacing between icon and text
        int iconPadding = getResources().getDimensionPixelOffset(R.dimen.incall_button_label_margin);
        setIconPadding(iconPadding);

        // Make clickable and focusable
        setFocusable(true);
        setClickable(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setAlpha(enabled ? 1.0f : DISABLED_STATE_OPACITY);
    }

    /**
     * Sets the checked color for the icon.
     * @param color The color to apply when checked.
     */
    public void setCheckedColor(@ColorInt int color) {
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{color, getResources().getColor(android.R.color.white, getContext().getTheme())}
        );
        setIconTint(colorStateList);
    }

    /**
     * Gets the current icon drawable.
     * @return The icon drawable.
     */
    public Drawable getIconDrawable() {
        return getIcon();
    }

    /**
     * Sets the icon drawable from a resource ID.
     * @param drawableRes The drawable resource ID.
     */
    public void setIconDrawable(@DrawableRes int drawableRes) {
        if (iconResource != drawableRes) {
            setIconResource(drawableRes);
            iconResource = drawableRes;
        }
    }

    /**
     * Sets the label text from a string resource ID.
     * @param stringRes The string resource ID.
     */
    public void setLabelText(@StringRes int stringRes) {
        setText(stringRes);
    }

    /**
     * Sets the label text from a CharSequence.
     * @param label The label text.
     */
    public void setLabelText(CharSequence label) {
        setText(label);
    }

    /**
     * Shows or hides a little down arrow to indicate that the button will pop up a menu.
     * @param shouldShow Whether to show the more indicator.
     */
    public void setShouldShowMoreIndicator(boolean shouldShow) {
        this.shouldShowMoreIndicator = shouldShow;
        // The more indicator is handled through the background drawable
        // In MD3, we can use a different style or add a badge
        if (shouldShow) {
            setBackgroundTintList(ColorStateList.valueOf(
                    getResources().getColor(R.color.incall_button_background_more_tint, getContext().getTheme())));
        } else {
            setBackgroundTintList(getResources().getColorStateList(
                    R.color.incall_button_background_tint, getContext().getTheme()));
        }
    }

    @Override
    public boolean isChecked() {
        return mIsChecked;
    }

    @Override
    public void setChecked(boolean checked) {
        performSetChecked(checked);
    }

    @Override
    public void toggle() {
        userRequestedSetChecked(!isChecked());
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (isChecked()) {
            mergeDrawableStates(drawableState, CHECKED_STATE_SET);
        }
        return drawableState;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        invalidate();
    }

    /**
     * Sets the listener for checked state changes.
     * @param listener The listener to set.
     */
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        this.onCheckedChangeListener = listener;
    }

    @Override
    public boolean performClick() {
        if (!hasCheckedChangeListener()) {
            return super.performClick();
        }

        toggle();
        final boolean handled = super.performClick();
        if (!handled) {
            playSoundEffect(SoundEffectConstants.CLICK);
        }
        return handled;
    }

    private boolean hasCheckedChangeListener() {
        return onCheckedChangeListener != null;
    }

    @Override
    @NonNull
    public Parcelable onSaveInstanceState() {
        return new SavedState(mIsChecked, super.onSaveInstanceState());
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        performSetChecked(savedState.isChecked);
        requestLayout();
    }

    /**
     * Called when the state of the button should be updated programmatically.
     * @param checked Whether the button should be checked.
     */
    private void performSetChecked(boolean checked) {
        if (isChecked() == checked) {
            return;
        }
        mIsChecked = checked;
        refreshDrawableState();
    }

    /**
     * Called when the user interacts with the button.
     * @param checked Whether the button should be checked.
     */
    private void userRequestedSetChecked(boolean checked) {
        if (isChecked() == checked) {
            return;
        }
        if (broadcasting) {
            return;
        }
        broadcasting = true;
        if (onCheckedChangeListener != null) {
            onCheckedChangeListener.onCheckedChanged(this, checked);
        }
        broadcasting = false;
    }

    /**
     * Callback interface to notify when the button's checked state has changed.
     */
    public interface OnCheckedChangeListener {
        void onCheckedChanged(MaterialLabeledButton button, boolean isChecked);
    }

    private static class SavedState extends BaseSavedState {

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        public final boolean isChecked;

        private SavedState(boolean isChecked, Parcelable superState) {
            super(superState);
            this.isChecked = isChecked;
        }

        protected SavedState(Parcel in) {
            super(in);
            isChecked = in.readByte() != 0;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeByte((byte) (isChecked ? 1 : 0));
        }
    }
}
