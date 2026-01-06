/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.os.Bundle;
import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.dialer.common.Assert;
import com.android.dialer.common.FragmentUtils;
import com.android.incallui.incall.protocol.InCallButtonIds;
import java.util.List;
import java.util.Set;
import com.android.dialer.R;

/** Fragment for the in call buttons (mute, speaker, ect.). */
public class InCallButtonGridFragment extends Fragment {

  private static final int BUTTON_COUNT = 12;
  private static final int BUTTONS_PER_ROW = 4;

  private MaterialLabeledButton[] buttons = new MaterialLabeledButton[BUTTON_COUNT];
  private MaterialLabeledButton moreButton;
  private View extraRowsContainer;
  private boolean isExpanded = false;
  private OnButtonGridCreatedListener buttonGridListener;

  public static Fragment newInstance() {
    return new InCallButtonGridFragment();
  }

  @Override
  public void onCreate(@Nullable Bundle bundle) {
    super.onCreate(bundle);
    buttonGridListener = FragmentUtils.getParent(this, OnButtonGridCreatedListener.class);
    Assert.isNotNull(buttonGridListener);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle bundle) {
    View view = inflater.inflate(R.layout.incall_button_grid, parent, false);

    // Main row buttons (always visible)
    buttons[0] = ((MaterialLabeledButton) view.findViewById(R.id.incall_first_button));   // Mute
    buttons[1] = ((MaterialLabeledButton) view.findViewById(R.id.incall_second_button));  // Keypad
    buttons[2] = ((MaterialLabeledButton) view.findViewById(R.id.incall_third_button));   // Speaker

    // Row 2 buttons (in expandable section)
    buttons[3] = ((MaterialLabeledButton) view.findViewById(R.id.incall_fourth_button));  // Add call
    buttons[4] = ((MaterialLabeledButton) view.findViewById(R.id.incall_fifth_button));   // Hold
    buttons[5] = ((MaterialLabeledButton) view.findViewById(R.id.incall_sixth_button));   // Swap
    buttons[6] = ((MaterialLabeledButton) view.findViewById(R.id.incall_seventh_button)); // Merge

    // Row 3 buttons (in expandable section)
    buttons[7] = ((MaterialLabeledButton) view.findViewById(R.id.incall_eighth_button));  // Record
    buttons[8] = ((MaterialLabeledButton) view.findViewById(R.id.incall_ninth_button));   // Video
    buttons[9] = ((MaterialLabeledButton) view.findViewById(R.id.incall_tenth_button));   // RTT
    buttons[10] = ((MaterialLabeledButton) view.findViewById(R.id.incall_eleventh_button)); // Manage

    // Row 4 (extra)
    buttons[11] = ((MaterialLabeledButton) view.findViewById(R.id.incall_twelfth_button));

    // More button and expandable container
    moreButton = view.findViewById(R.id.incall_more_button);
    extraRowsContainer = view.findViewById(R.id.incall_extra_rows);

    // Set up More button click listener
    if (moreButton != null) {
      moreButton.setOnClickListener(v -> toggleExpandedRows());
    }

    return view;
  }

  private void toggleExpandedRows() {
    isExpanded = !isExpanded;
    if (extraRowsContainer != null) {
      extraRowsContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
    }
    if (moreButton != null) {
      moreButton.setIconResource(isExpanded
          ? R.drawable.quantum_ic_close_vd_theme_24
          : R.drawable.quantum_ic_more_vert_vd_theme_24);
      moreButton.setText(isExpanded
          ? R.string.incall_label_less
          : R.string.incall_label_more);
    }
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle bundle) {
    super.onViewCreated(view, bundle);
    buttonGridListener.onButtonGridCreated(this);
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    buttonGridListener.onButtonGridDestroyed();
  }

  public void onInCallScreenDialpadVisibilityChange(boolean isShowing) {
    for (MaterialLabeledButton button : buttons) {
      if (button != null) {
        button.setImportantForAccessibility(
            isShowing
                ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
      }
    }
    if (moreButton != null) {
      moreButton.setImportantForAccessibility(
          isShowing
              ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
              : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    }
  }

  public int updateButtonStates(
      List<ButtonController> buttonControllers,
      @Nullable ButtonChooser buttonChooser,
      int voiceNetworkType,
      int phoneType) {
    Set<Integer> allowedButtons = new ArraySet<>();
    Set<Integer> disabledButtons = new ArraySet<>();
    for (ButtonController controller : buttonControllers) {
      if (controller.isAllowed()) {
        allowedButtons.add(controller.getInCallButtonId());
        if (!controller.isEnabled()) {
          disabledButtons.add(controller.getInCallButtonId());
        }
      }
    }

    for (ButtonController controller : buttonControllers) {
      controller.setButton(null);
    }

    if (buttonChooser == null) {
      buttonChooser =
          ButtonChooserFactory.newButtonChooser(voiceNetworkType, false /* isWiFi */, phoneType);
    }

    List<Integer> buttonsToPlace =
        buttonChooser.getButtonPlacement(BUTTON_COUNT, allowedButtons, disabledButtons);

    int numVisibleRows = getResources().getInteger(R.integer.incall_num_rows);
    for (int i = 0; i < BUTTON_COUNT; ++i) {
      if (buttons[i] == null) {
        continue;
      }
      int numRow = i / BUTTONS_PER_ROW;
      if (i >= buttonsToPlace.size()) {
        if (numRow >= numVisibleRows) {
          buttons[i].setVisibility(View.GONE);
        } else {
          buttons[i].setVisibility(View.INVISIBLE);
        }
        continue;
      }
      @InCallButtonIds int button = buttonsToPlace.get(i);
      buttonGridListener.getButtonController(button).setButton(buttons[i]);
    }

    return BUTTON_COUNT;
  }

  public void updateButtonColor(@ColorInt int color) {
    for (MaterialLabeledButton button : buttons) {
      if (button != null) {
        button.setCheckedColor(color);
      }
    }
  }

  /** Interface to let the listener know the status of the button grid. */
  public interface OnButtonGridCreatedListener {
    void onButtonGridCreated(InCallButtonGridFragment inCallButtonGridFragment);
    void onButtonGridDestroyed();

    ButtonController getButtonController(@InCallButtonIds int id);
  }
}
