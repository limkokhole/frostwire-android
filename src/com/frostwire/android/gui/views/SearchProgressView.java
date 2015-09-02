/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2015, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.android.gui.views;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.frostwire.android.R;
import com.frostwire.android.gui.fragments.SearchFragment;
import com.frostwire.android.gui.adnetworks.Offers;

/**
 * @author gubatron
 * @author aldenml
 */
public class SearchProgressView extends LinearLayout {

    private final FreeAppsListener freeAppsListener;

    private ProgressBar progressbar;
    private Button buttonCancel;
    private Button buttonFreeApps;
    private TextView textNoResults;
    private TextView textTryOtherKeywords;
    private Button[] retryButtons;

    private boolean progressEnabled;
    private CurrentQueryReporter currentQueryReporter;

    public SearchProgressView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.freeAppsListener = new FreeAppsListener(this);
        this.progressEnabled = true;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(state);

        if (currentQueryReporter != null && currentQueryReporter.getCurrentQuery() != null && currentQueryReporter instanceof SearchFragment) {
            ((SearchFragment) currentQueryReporter).setupRetrySuggestions();
        }
    }

    public boolean isProgressEnabled() {
        return progressEnabled;
    }

    public void setProgressEnabled(boolean enabled) {
        if (this.progressEnabled != enabled) {
            this.progressEnabled = enabled;

            if (enabled) {
                startProgress();
            } else {
                stopProgress();
            }
        }
    }

    public void setCancelOnClickListener(OnClickListener l) {
        buttonCancel.setOnClickListener(l);
    }

    public void setupRetrySuggestions(String[] keywords, OnRetryListener retryListener) {
        try {
            int i = 0;
            for (; i < Math.min(keywords.length, retryButtons.length); i++) {
                Button tv = retryButtons[i];
                tv.setText(keywords[i]);
                tv.setVisibility(View.VISIBLE);
                tv.setOnClickListener(new OnRetryAdapter(this, retryListener));
            }
            for (; i < retryButtons.length; i++) {
                Button tv = retryButtons[i];
                tv.setText("");
                tv.setVisibility(View.GONE);
                tv.setOnClickListener(null);
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void hideRetryViews() {
        if (textTryOtherKeywords != null) {
            textTryOtherKeywords.setVisibility(View.GONE);
        }

        if (retryButtons != null) {
            for (Button b : retryButtons) {
                b.setVisibility(View.GONE);
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View.inflate(getContext(), R.layout.view_search_progress, this);

        if (isInEditMode()) {
            return;
        }

        progressbar = (ProgressBar) findViewById(R.id.view_search_progress_progressbar);
        buttonCancel = (Button) findViewById(R.id.view_search_progress_button_cancel);
        buttonFreeApps = (Button) findViewById(R.id.view_search_progress_button_free_apps);
        textNoResults = (TextView) findViewById(R.id.view_search_progress_text_no_results_feedback);
        textTryOtherKeywords = (TextView) findViewById(R.id.view_search_progress_try_other_keywords);

        initRetryTextViews();
        initButtonFreeApps();
    }

    private void initRetryTextViews() {
        retryButtons = new Button[]{
                (Button) findViewById(R.id.view_search_progress_retry_button_1),
                (Button) findViewById(R.id.view_search_progress_retry_button_2),
                (Button) findViewById(R.id.view_search_progress_retry_button_3),
                (Button) findViewById(R.id.view_search_progress_retry_button_4),
        };

        rotateRetryButtonsLayout();
        hideRetryViews();
    }

    private void rotateRetryButtonsLayout() {
        try {
            int layoutOrientation = getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL;
            LinearLayout retryButtonsLayout = (LinearLayout) findViewById(R.id.view_search_progress_retry_buttons_linearlayout);
            retryButtonsLayout.setOrientation(layoutOrientation);
        } catch (Throwable e) {
        }
    }

    private void initButtonFreeApps() {
        buttonFreeApps.setVisibility(View.GONE);
        buttonFreeApps.setOnClickListener(freeAppsListener);
    }

    private void startProgress() {
        progressbar.setVisibility(View.VISIBLE);
        buttonCancel.setText(android.R.string.cancel);
        textNoResults.setVisibility(View.GONE);
        buttonFreeApps.setVisibility(View.GONE);
        hideRetryViews();
    }

    private void stopProgress() {
        progressbar.setVisibility(View.GONE);
        buttonCancel.setText(R.string.retry_search);
        textNoResults.setVisibility(View.VISIBLE);
        buttonFreeApps.setVisibility(Offers.isFreeAppsEnabled() ? View.VISIBLE : View.GONE);

        if (currentQueryReporter.getCurrentQuery() != null) {
            textTryOtherKeywords.setVisibility(View.VISIBLE);
        } else {
            hideRetryViews();
        }
    }

    public void setCurrentQueryReporter(CurrentQueryReporter currentQueryReporter) {
        this.currentQueryReporter = currentQueryReporter;
    }

    private static final class FreeAppsListener extends ClickAdapter<View> {

        public FreeAppsListener(View owner) {
            super(owner);
        }

        @Override
        public void onClick(View owner, View v) {
            Offers.onFreeAppsClick(v.getContext());
        }
    }

    public interface OnRetryListener {
        public void onRetry(SearchProgressView v, String keywords);
    }

    private static final class OnRetryAdapter extends ClickAdapter<SearchProgressView> {

        private final OnRetryListener retryListener;

        public OnRetryAdapter(SearchProgressView owner, OnRetryListener retryListener) {
            super(owner);
            this.retryListener = retryListener;
        }

        @Override
        public void onClick(SearchProgressView owner, View v) {
            Button b = (Button) v;
            if (retryListener != null) {
                retryListener.onRetry(owner, b.getText().toString());
            }
        }
    }

    public interface CurrentQueryReporter {
        String getCurrentQuery();
    }
}
