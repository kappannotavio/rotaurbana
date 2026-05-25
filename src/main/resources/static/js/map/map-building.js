const UPDATE_INTERVAL = 1000; // 1s

let h2 = document.getElementById("coords");

let map;
let marker;

// ================= ICON =================
const busIcon = L.icon({
    iconUrl: "images/busImage.png",
    iconSize: [40, 40],
    iconAnchor: [20, 40],
    popupAnchor: [0, -35]
});

// ================= START =================
function startLocationTracking(updateCallback) {

    setInterval(() => {
        fetchLocation(updateCallback);
    }, UPDATE_INTERVAL);

}

// ================= FETCH BACKEND =================
function fetchLocation(updateCallback) {

    fetch(`${API_URL}map/1`)
        .then(res => {
            if (!res.ok) return null;
            return res.json();
        })
        .then(data => {

            if (!data) return;

            const lat = Number(data.latitude);
            const lng = Number(data.longitude);

            // se vier null, undefined, string, etc
            if (!isFinite(lat) || !isFinite(lng)) return;

            // ignora coordenadas inválidas
            if (lat === 0 && lng === 0) return;

            updateCallback(lat, lng);

        })
        .catch(() => {

        });

}

// ================= ATUALIZA MAPA =================
function updateMap(lat, lng) {

    if (!isFinite(lat) || !isFinite(lng)) return;

    if (h2) {
        h2.textContent = `Latitude: ${lat.toFixed(5)} | Longitude: ${lng.toFixed(5)}`;
    }

    if (!map) {

        map = L.map("mapid").setView([lat, lng], 16);

        L.tileLayer("https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png", {
            attribution: "© OpenStreetMap © CARTO",
            subdomains: "abcd",
            maxZoom: 19
        }).addTo(map);

        marker = L.marker([lat, lng], { icon: busIcon })
            .addTo(map)
            .bindPopup("🚌 Ônibus está aqui")
            .openPopup();

    } else {

        if (marker) {
            marker.setLatLng([lat, lng]);
        }

        map.flyTo([lat, lng], 16);

    }
}