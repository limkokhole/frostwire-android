/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011-2014, FrostWire(R). All rights reserved.
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

package com.frostwire.android.gui.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.frostwire.android.R;
import com.frostwire.android.core.Constants;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.ClickAdapter;
import com.frostwire.android.util.Storages;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author gubatron
 * @author aldenml
 * 
 */
public class AboutFragment extends Fragment implements MainFragment {

    public AboutFragment() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        final TextView title = (TextView) view.findViewById(R.id.fragment_about_title);
        final String basicOrPlus = Constants.IS_GOOGLE_PLAY_DISTRIBUTION ? "Basic" : "Plus";
        title.setText("FrostWire " + basicOrPlus + " v" + Constants.FROSTWIRE_VERSION_STRING);

        final TextView buildNumber = (TextView) view.findViewById(R.id.fragment_about_build_number);
        buildNumber.setText("\nbuild " + Constants.FROSTWIRE_BUILD);

        final TextView content = (TextView) view.findViewById(R.id.fragment_about_content);
        content.setText(Html.fromHtml(getAboutText()));
        content.setMovementMethod(LinkMovementMethod.getInstance());

        final Button helpTranslateButton = (Button) view.findViewById(R.id.fragment_about_help_translate_frostwire_button);
        helpTranslateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onHelpTranslate();
            }
        });


        return view;
    }

    private void onHelpTranslate() {
        Storages.openDocumentTree(getActivity());
        //UIUtils.openURL(getActivity(), "https://github.com/frostwire/frostwire-android/wiki/Help-Translate-FrostWire");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Storages.handleDocumentTreeResult(requestCode, resultCode, data);
    }

    @Override
    public View getHeader(Activity activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        TextView header = (TextView) inflater.inflate(R.layout.view_main_fragment_simple_header, null);
        header.setText(R.string.about);

        return header;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private String getAboutText() {
        try {
            InputStream raw = getResources().openRawResource(R.raw.about);
            return IOUtils.toString(raw, "UTF-8");
        } catch (IOException e) {
            return "";
        }
    }
}
