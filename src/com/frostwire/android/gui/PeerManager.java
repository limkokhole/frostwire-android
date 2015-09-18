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

package com.frostwire.android.gui;

import android.util.Log;
import com.frostwire.android.core.ConfigurationManager;
import com.frostwire.android.core.Constants;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of the Peers we know.
 *
 * @author gubatron
 * @author aldenml
 */
public final class PeerManager {

    private static final String TAG = "FW.PeerManager";

    private final Map<String, Peer> addressMap;

    private static PeerManager instance;

    public static PeerManager instance() {
        if (instance == null) {
            instance = new PeerManager();
        }
        return instance;
    }

    private PeerManager() {
        this.addressMap = new ConcurrentHashMap<String, Peer>();
    }

    public Peer getLocalPeer() {
        return new Peer();
    }

    /**
     * This returns a shadow-copy of the peer cache as an ArrayList plus the local peer.
     *
     * @return
     */
    public List<Peer> getPeers() {
        List<Peer> peers = new ArrayList<Peer>();

        peers.addAll(addressMap.values());

        return peers;
    }

    /**
     * @param uuid
     * @return
     */
    public Peer findPeerByKey(String key) {
        if (key == null) {
            return null;
        }

        Peer p = addressMap.get(key);

        return p;
    }

    public void clear() {
        addressMap.clear();
    }

    public void start() {
    }

    public void stop() {
    }
}
