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

package com.frostwire.android.gui.fragments;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.*;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.TextView;
import com.andrew.apollo.MusicPlaybackService;
import com.frostwire.android.R;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;
import com.frostwire.android.core.FileDescriptor;
import com.frostwire.android.gui.Peer;
import com.frostwire.android.gui.PeerManager;
import com.frostwire.android.gui.adapters.FileListAdapter;
import com.frostwire.android.gui.util.UIUtils;
import com.frostwire.android.gui.views.AbstractFragment;
import com.frostwire.android.gui.views.BrowsePeerSearchBarView;
import com.frostwire.android.gui.views.BrowsePeerSearchBarView.OnActionListener;
import com.frostwire.android.gui.views.FileTypeRadioButtonSelectorFactory;
import com.frostwire.android.gui.views.OverScrollListener;
import com.frostwire.localpeer.Finger;
import com.frostwire.logging.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author gubatron
 * @author aldenml
 */
public class BrowsePeerFragment extends AbstractFragment implements LoaderCallbacks<Object>, MainFragment {

    private static final Logger LOG = Logger.getLogger(BrowsePeerFragment.class);

    private static final int LOADER_FINGER_ID = 0;
    private static final int LOADER_FILES_ID = 1;

    private final BroadcastReceiver broadcastReceiver;

    private RadioButton buttonAudio;
    private RadioButton buttonRingtones;
    private RadioButton buttonVideos;
    private RadioButton buttonPictures;
    private RadioButton buttonApplications;
    private RadioButton buttonDocuments;

    private BrowsePeerSearchBarView filesBar;
    private com.frostwire.android.gui.views.ListView list;

    private FileListAdapter adapter;

    private Peer peer;
    private Finger finger;

    private View header;

    private long lastAdapterRefresh;

    private Set<FileListAdapter.FileDescriptorItem> previouslyChecked;

    public BrowsePeerFragment() {
        super(R.layout.fragment_browse_peer);
        broadcastReceiver = new LocalBroadcastReceiver();
    }

    public Peer getPeer() {
        if (peer == null) {
            loadPeerFromBundleData();
        }

        if (peer == null) {
            loadPeerFromIntentData();
        }

        return peer;
    }

    public void setPeer(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setRetainInstance(true);

        if (peer == null) {
            getPeer();
        }

        if (peer == null) { // save move
            getActivity().finish();
            return;
        }
    }

    @Override
    public Loader<Object> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_FINGER_ID) {
            return createLoaderFinger();
        } else if (id == LOADER_FILES_ID) {
            return createLoaderFiles(args.getByte("fileType"));
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Object> loader, Object data) {
        if (data == null) {
            LOG.warn("Something wrong, data is null");
            removePeerAndFinish();
            return;
        }

        if (loader.getId() == LOADER_FINGER_ID) {
            boolean firstCheck = finger == null;
            finger = (Finger) data;

            if (firstCheck) {
                checkNoEmptyButton(finger);
            }
        } else if (loader.getId() == LOADER_FILES_ID) {
            updateFiles((Object[]) data);
        }

        updateHeader();
    }

    @Override
    public void onLoaderReset(Loader<Object> loader) {
    }

    @Override
    public void onResume() {
        super.onResume();
        initBroadcastReceiver();
        getLoaderManager().destroyLoader(LOADER_FINGER_ID);
        getLoaderManager().restartLoader(LOADER_FINGER_ID, null, this);

        if (adapter != null) {
            restorePreviouslyChecked();
            browseFilesButtonClick(adapter.getFileType());
        }
    }

    private void initBroadcastReceiver() {
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_REFRESH_FINGER);
        filter.addAction(Constants.ACTION_MEDIA_PLAYER_PLAY);
        filter.addAction(Constants.ACTION_MEDIA_PLAYER_PAUSED);
        filter.addAction(Constants.ACTION_MEDIA_PLAYER_STOPPED);
        filter.addAction(MusicPlaybackService.META_CHANGED);
        filter.addAction(MusicPlaybackService.PLAYSTATE_CHANGED);
        getActivity().registerReceiver(broadcastReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        savePreviouslyCheckedFileDescriptors();
        getActivity().unregisterReceiver(broadcastReceiver);
    }

    @Override
    public View getHeader(Activity activity) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        header = inflater.inflate(R.layout.view_browse_peer_header, null);

        updateHeader();

        return header;
    }

    @Override
    protected void initComponents(View v) {
        buttonApplications = initRadioButton(v, R.id.fragment_browse_peer_radio_applications, Constants.FILE_TYPE_TORRENTS);
        buttonDocuments = initRadioButton(v, R.id.fragment_browse_peer_radio_documents, Constants.FILE_TYPE_DOCUMENTS);
        buttonPictures = initRadioButton(v, R.id.fragment_browse_peer_radio_pictures, Constants.FILE_TYPE_PICTURES);
        buttonVideos = initRadioButton(v, R.id.fragment_browse_peer_radio_videos, Constants.FILE_TYPE_VIDEOS);
        buttonRingtones = initRadioButton(v, R.id.fragment_browse_peer_radio_ringtones, Constants.FILE_TYPE_RINGTONES);
        buttonAudio = initRadioButton(v, R.id.fragment_browse_peer_radio_audio, Constants.FILE_TYPE_AUDIO);

        filesBar = findView(v, R.id.fragment_browse_peer_files_bar);
        filesBar.setOnActionListener(new OnActionListener() {
            public void onCheckAll(View v, boolean isChecked) {
                if (adapter != null) {
                    if (isChecked) {
                        adapter.checkAll();
                    } else {
                        adapter.clearChecked();
                    }
                }
            }

            public void onFilter(View v, String str) {
                if (adapter != null) {
                    adapter.getFilter().filter(str);
                }
            }
        });

        list = findView(v, R.id.fragment_browse_peer_list);
        list.setOverScrollListener(new OverScrollListener() {
            @Override
            public void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
                long now = SystemClock.elapsedRealtime();
                if (clampedY && (now - lastAdapterRefresh) > 5000) {
                    refreshSelection();
                }
            }
        });
    }

    private void loadPeerFromIntentData() {
        if (peer != null) { // why?
            return;
        }

        Intent intent = getActivity().getIntent();
        if (intent.hasExtra(Constants.EXTRA_PEER_UUID)) {
            String uuid = intent.getStringExtra(Constants.EXTRA_PEER_UUID);

            if (uuid != null) {
                try {
                    peer = PeerManager.instance().findPeerByKey(uuid);
                } catch (Throwable e) {
                    peer = null; // weird situation reported by a strange bug.
                }
            }
        }
    }

    private void loadPeerFromBundleData() {
        if (peer != null) { // why?
            return;
        }

        Bundle bundle = getArguments();

        if (bundle != null && bundle.containsKey(Constants.EXTRA_PEER_UUID)) {
            String uuid = bundle.getString(Constants.EXTRA_PEER_UUID);

            if (uuid != null) {
                try {
                    peer = PeerManager.instance().findPeerByKey(uuid);
                } catch (Throwable e) {
                    peer = null; // weird situation reported by a strange bug.
                }
            }
        }
    }

    private RadioButton initRadioButton(View v, int viewId, final byte fileType) {
        final RadioButton button = findView(v, viewId);
        final Resources r = button.getResources();
        final FileTypeRadioButtonSelectorFactory fileTypeRadioButtonSelectorFactory =
                new FileTypeRadioButtonSelectorFactory(fileType,
                        r,
                        FileTypeRadioButtonSelectorFactory.RadioButtonContainerType.BROWSE);
        fileTypeRadioButtonSelectorFactory.updateButtonBackground(button);

        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (button.isChecked()) {
                    browseFilesButtonClick(fileType);
                }
                fileTypeRadioButtonSelectorFactory.updateButtonBackground(button);
            }
        });
        button.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    browseFilesButtonClick(fileType);
                }
                fileTypeRadioButtonSelectorFactory.updateButtonBackground(button);
            }
        });
        return button;
    }

    private void browseFilesButtonClick(byte fileType) {
        if (adapter != null) {
            savePreviouslyCheckedFileDescriptors();
            saveListViewVisiblePosition(adapter.getFileType());
            adapter.clear();
        }

        filesBar.clearCheckAll();
        filesBar.clearSearch();

        getLoaderManager().destroyLoader(LOADER_FILES_ID);
        Bundle bundle = new Bundle();
        bundle.putByte("fileType", fileType);
        getLoaderManager().restartLoader(LOADER_FILES_ID, bundle, this);
    }

    private void savePreviouslyCheckedFileDescriptors() {
        if (adapter != null) {
            final Set<FileListAdapter.FileDescriptorItem> checked = adapter.getChecked();
            if (checked != null && !checked.isEmpty()) {
                previouslyChecked = new HashSet<FileListAdapter.FileDescriptorItem>(checked);
            } else {
                previouslyChecked = null;
            }
        }
    }

    private Loader<Object> createLoaderFinger() {
        AsyncTaskLoader<Object> loader = new AsyncTaskLoader<Object>(getActivity()) {
            @Override
            public Object loadInBackground() {
                try {
                    return peer.finger();
                } catch (Throwable e) {
                    LOG.error("Error performing finger", e);
                }
                return null;
            }
        };
        loader.forceLoad();
        return loader;
    }

    private Loader<Object> createLoaderFiles(final byte fileType) {
        AsyncTaskLoader<Object> loader = new AsyncTaskLoader<Object>(getActivity()) {
            @Override
            public Object loadInBackground() {
                try {
                    return new Object[]{fileType, peer.browse(fileType)};
                } catch (Throwable e) {
                    LOG.error("Error performing finger", e);
                }
                return null;
            }
        };
        loader.forceLoad();
        return loader;
    }

    private void updateHeader() {
        if (finger == null) {
            if (peer == null) {
                LOG.warn("Something wrong, finger  and peer are null");
                removePeerAndFinish();
                return;
            } else {
                finger = peer.finger();
            }
        }

        if (header != null) {

            byte fileType = adapter != null ? adapter.getFileType() : Constants.FILE_TYPE_AUDIO;

            int numTotal = 0;

            switch (fileType) {
                case Constants.FILE_TYPE_TORRENTS:
                    numTotal = finger.numTotalTorrentFiles;
                    break;
                case Constants.FILE_TYPE_AUDIO:
                    numTotal = finger.numTotalAudioFiles;
                    break;
                case Constants.FILE_TYPE_DOCUMENTS:
                    numTotal = finger.numTotalDocumentFiles;
                    break;
                case Constants.FILE_TYPE_PICTURES:
                    numTotal = finger.numTotalPictureFiles;
                    break;
                case Constants.FILE_TYPE_RINGTONES:
                    numTotal = finger.numTotalRingtoneFiles;
                    break;
                case Constants.FILE_TYPE_VIDEOS:
                    numTotal = finger.numTotalVideoFiles;
                    break;
            }

            String fileTypeStr = getString(R.string.my_filetype, UIUtils.getFileTypeAsString(getResources(), fileType));

            TextView title = (TextView) header.findViewById(R.id.view_browse_peer_header_text_title);
            TextView total = (TextView) header.findViewById(R.id.view_browse_peer_header_text_total);

            title.setText(fileTypeStr);
            total.setText("(" + String.valueOf(numTotal) + ")");

            if (fileType == Constants.FILE_TYPE_AUDIO) {
                buttonAudio.setChecked(true);
            }
        }

        if (adapter == null) {
            browseFilesButtonClick(Constants.FILE_TYPE_AUDIO);
        }

        restoreListViewScrollPosition();
    }

    private void restoreListViewScrollPosition() {
        if (adapter != null) {
            int savedListViewVisiblePosition = getSavedListViewVisiblePosition(adapter.getFileType());
            savedListViewVisiblePosition = (savedListViewVisiblePosition > 0) ? savedListViewVisiblePosition + 1 : 0;
            list.setSelection(savedListViewVisiblePosition);
        }
    }

    private void updateFiles(Object[] data) {
        if (data == null) {
            LOG.warn("Something wrong, data is null");
            removePeerAndFinish();
            return;
        }

        try {
            byte fileType = (Byte) data[0];

            @SuppressWarnings("unchecked")
            List<FileDescriptor> items = (List<FileDescriptor>) data[1];
            adapter = new FileListAdapter(getActivity(), items, fileType) {

                @Override
                protected void onItemChecked(View v, boolean isChecked) {
                    if (!isChecked) {
                        filesBar.clearCheckAll();
                    }
                }

                @Override
                protected void onLocalPlay() {
                    if (adapter != null) {
                        saveListViewVisiblePosition(adapter.getFileType());
                    }
                }
            };
            adapter.setCheckboxesVisibility(true);
            restorePreviouslyChecked();
            list.setAdapter(adapter);

        } catch (Throwable e) {
            LOG.error("Error updating files in list", e);
        }
    }

    private void restorePreviouslyChecked() {
        if (previouslyChecked != null && !previouslyChecked.isEmpty()) {
            adapter.setChecked(previouslyChecked);
        }
    }

    private void checkNoEmptyButton(Finger f) {
        buttonAudio.setChecked(true);
    }

    private void removePeerAndFinish() {
        Activity activity = getActivity();
        if (activity != null) {
            if (peer != null) {
                try {
                    UIUtils.showShortMessage(activity, R.string.is_not_responding, peer.getNickname());
                    PeerManager.instance().removePeer(peer);
                } catch (Throwable e) {
                    // still possible to get an exception since peer is mutable.
                    LOG.error("Error removing a not null peer", e);
                }
            }
            activity.finish();
        }
    }

    private void saveListViewVisiblePosition(byte fileType) {
        int firstVisiblePosition = list.getFirstVisiblePosition();
        ConfigurationManager.instance().setInt(Constants.BROWSE_PEER_FRAGMENT_LISTVIEW_FIRST_VISIBLE_POSITION + fileType, firstVisiblePosition);
    }

    private int getSavedListViewVisiblePosition(byte fileType) {
        //will return 0 if not found.
        return ConfigurationManager.instance().getInt(Constants.BROWSE_PEER_FRAGMENT_LISTVIEW_FIRST_VISIBLE_POSITION + fileType);
    }

    private final class LocalBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Constants.ACTION_MEDIA_PLAYER_PLAY) ||
                    action.equals(Constants.ACTION_MEDIA_PLAYER_STOPPED) ||
                    action.equals(Constants.ACTION_MEDIA_PLAYER_PAUSED) ||
                    action.equals(Constants.ACTION_MEDIA_PLAYER_PAUSED) ||
                    action.equals(MusicPlaybackService.PLAYSTATE_CHANGED) ||
                    action.equals(MusicPlaybackService.META_CHANGED)
                    ) {
                if (adapter != null) {
                    adapter.notifyDataSetChanged();
                }
            } else if (intent.getAction().equals(Constants.ACTION_REFRESH_FINGER)) {
                try {
                    getLoaderManager().restartLoader(LOADER_FINGER_ID, null, BrowsePeerFragment.this);
                } catch (Throwable t) {
                    LOG.error("LocalBroadcastReceiver can't restart loader on ACTION_REFRESH_FINGER, fragment not attached?", t);
                }
            }
        }
    }

    public void refreshSelection() {
        if (adapter != null) {
            lastAdapterRefresh = SystemClock.elapsedRealtime();
            browseFilesButtonClick(adapter.getFileType());
        }
    }
}
