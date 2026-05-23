/**
 * EmberGPS — Leaflet map logic
 *
 * Reads the server base URL from the meta tag or defaults to the current origin.
 * Authentication key is read from the #apiKeyInput field (admin or device key).
 */

'use strict';

// ---------------------------------------------------------------------------
// Configuration
// ---------------------------------------------------------------------------

const API_BASE        = '/api/v1';
const REFRESH_MS      = 30_000;   // auto-refresh every 30 s
const MAX_HISTORY_PTS = 500;       // max points to load for trail polyline
const DEVICE_COLORS   = [
    '#e94560', '#3b82f6', '#10b981', '#f59e0b',
    '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16'
];

// ---------------------------------------------------------------------------
// State
// ---------------------------------------------------------------------------

let map;
let markers       = {};     // deviceId -> L.Marker
let polylines     = {};     // deviceId -> L.Polyline
let deviceColors  = {};     // deviceId -> color string
let colorIndex    = 0;
let selectedDevice = null;
let showHistory   = true;
let refreshTimer  = null;

// ---------------------------------------------------------------------------
// Init
// ---------------------------------------------------------------------------

document.addEventListener('DOMContentLoaded', () => {
    initMap();
    addHistoryToggleButton();
    loadData();
    refreshTimer = setInterval(loadData, REFRESH_MS);
});

function initMap() {
    map = L.map('map', {
        center: [39.5, -98.35],
        zoom: 4,
        zoomControl: true
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '© <a href="https://openstreetmap.org/copyright">OpenStreetMap</a> contributors',
        maxZoom: 19
    }).addTo(map);
}

function addHistoryToggleButton() {
    const btn = document.createElement('button');
    btn.id = 'historyToggle';
    btn.textContent = '🗺 Hide Trails';
    btn.onclick = () => {
        showHistory = !showHistory;
        btn.textContent = showHistory ? '🗺 Hide Trails' : '🗺 Show Trails';
        Object.values(polylines).forEach(pl =>
            showHistory ? pl.addTo(map) : pl.remove());
    };
    document.querySelector('.map-container').appendChild(btn);
}

// ---------------------------------------------------------------------------
// Data loading
// ---------------------------------------------------------------------------

async function loadData() {
    const key = document.getElementById('apiKeyInput').value.trim();
    if (!key) {
        showToast('Enter your API key above to load data.');
        return;
    }

    try {
        const latest = await apiFetch('/gps/latest', key);
        renderDevices(latest, key);
        document.getElementById('lastUpdated').textContent =
            'Updated: ' + new Date().toLocaleTimeString();
    } catch (err) {
        showToast('Error loading data: ' + err.message);
    }
}

async function loadHistory(deviceId, key) {
    const since = new Date(Date.now() - 24 * 3600 * 1000).toISOString();
    const url = `/gps/history/${encodeURIComponent(deviceId)}?from=${since}&size=${MAX_HISTORY_PTS}`;
    try {
        const data = await apiFetch(url, key);
        return data.positions || [];
    } catch {
        return [];
    }
}

// ---------------------------------------------------------------------------
// Rendering
// ---------------------------------------------------------------------------

async function renderDevices(positions, key) {
    const deviceListEl = document.getElementById('deviceList');
    deviceListEl.innerHTML = '';

    if (!positions || positions.length === 0) {
        deviceListEl.innerHTML = '<li style="color:#888;font-size:0.8rem">No positions yet.</li>';
        return;
    }

    for (const pos of positions) {
        const color = getDeviceColor(pos.deviceId);

        // Sidebar entry
        const li = document.createElement('li');
        li.className = 'device-item' + (selectedDevice === pos.deviceId ? ' active' : '');
        li.style.borderLeftColor = color;
        li.innerHTML = `
            <span class="device-name">${escHtml(pos.deviceId)}</span>
            <span class="device-meta">${formatTime(pos.capturedAt)}</span>
            <span class="device-speed">${formatSpeed(pos.speed)}</span>
        `;
        li.onclick = () => selectDevice(pos.deviceId, pos, key);
        deviceListEl.appendChild(li);

        // Map marker
        updateMarker(pos, color);

        // Polyline trail
        if (showHistory) {
            const history = await loadHistory(pos.deviceId, key);
            updatePolyline(pos.deviceId, history, color);
        }
    }
}

function updateMarker(pos, color) {
    const latlng = [pos.latitude, pos.longitude];

    const icon = createDeviceIcon(color, pos.heading);
    const popupHtml = buildPopup(pos);

    if (markers[pos.deviceId]) {
        markers[pos.deviceId].setLatLng(latlng).setIcon(icon).setPopupContent(popupHtml);
    } else {
        const marker = L.marker(latlng, { icon })
            .addTo(map)
            .bindPopup(popupHtml);
        markers[pos.deviceId] = marker;
    }
}

function updatePolyline(deviceId, history, color) {
    if (polylines[deviceId]) {
        polylines[deviceId].remove();
        delete polylines[deviceId];
    }
    if (!history || history.length < 2) return;

    const latlngs = history.map(p => [p.latitude, p.longitude]).reverse();
    const pl = L.polyline(latlngs, { color, weight: 3, opacity: 0.7, smoothFactor: 1 });
    if (showHistory) pl.addTo(map);
    polylines[deviceId] = pl;
}

function selectDevice(deviceId, pos, key) {
    selectedDevice = deviceId;
    if (markers[deviceId]) {
        map.setView([pos.latitude, pos.longitude], 13, { animate: true });
        markers[deviceId].openPopup();
    }
    // Reload to highlight active item
    loadData();
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function createDeviceIcon(color, heading) {
    const rotation = heading != null ? heading : 0;
    const svg = `
        <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" viewBox="0 0 32 32">
            <g transform="rotate(${rotation}, 16, 16)">
                <polygon points="16,2 26,28 16,22 6,28"
                         fill="${color}" stroke="#fff" stroke-width="2"/>
            </g>
        </svg>`;
    return L.divIcon({
        className: '',
        html: svg,
        iconSize:   [32, 32],
        iconAnchor: [16, 16],
        popupAnchor: [0, -16]
    });
}

function buildPopup(pos) {
    return `
        <strong>${escHtml(pos.deviceId)}</strong><br/>
        <b>Lat:</b> ${pos.latitude.toFixed(6)}<br/>
        <b>Lon:</b> ${pos.longitude.toFixed(6)}<br/>
        ${pos.altitude  != null ? `<b>Alt:</b> ${pos.altitude.toFixed(1)} m<br/>` : ''}
        ${pos.speed     != null ? `<b>Speed:</b> ${msToKph(pos.speed)} km/h<br/>` : ''}
        ${pos.heading   != null ? `<b>Heading:</b> ${pos.heading.toFixed(1)}°<br/>` : ''}
        ${pos.fixType   != null ? `<b>Fix:</b> ${fixTypeLabel(pos.fixType)}<br/>` : ''}
        ${pos.numSatellites != null ? `<b>Sats:</b> ${pos.numSatellites}<br/>` : ''}
        <b>Time:</b> ${formatTime(pos.capturedAt)}
    `;
}

function getDeviceColor(deviceId) {
    if (!deviceColors[deviceId]) {
        deviceColors[deviceId] = DEVICE_COLORS[colorIndex % DEVICE_COLORS.length];
        colorIndex++;
    }
    return deviceColors[deviceId];
}

async function apiFetch(path, key) {
    const headers = {};
    if (key) {
        // Try admin key header; server accepts both
        headers['X-Admin-Key']  = key;
        headers['X-API-Key']    = key;
    }
    const resp = await fetch(API_BASE + path, { headers });
    if (!resp.ok) {
        const err = await resp.json().catch(() => ({}));
        throw new Error(err.message || `HTTP ${resp.status}`);
    }
    return resp.json();
}

function formatTime(iso) {
    if (!iso) return '—';
    return new Date(iso).toLocaleString();
}

function formatSpeed(mps) {
    if (mps == null) return '—';
    return msToKph(mps) + ' km/h';
}

function msToKph(mps) {
    return (mps * 3.6).toFixed(1);
}

function fixTypeLabel(ft) {
    return { 0: 'No fix', 2: '2-D fix', 3: '3-D fix' }[ft] || ft;
}

function escHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

let toastTimer;
function showToast(msg) {
    let el = document.querySelector('.toast');
    if (!el) {
        el = document.createElement('div');
        el.className = 'toast';
        document.body.appendChild(el);
    }
    el.textContent = msg;
    el.style.display = 'block';
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.style.display = 'none'; }, 4000);
}
