let lastSend = 0;

// ================= GEOLOCATION =================
if ("geolocation" in navigator) {

    navigator.geolocation.watchPosition(success, error, {
        enableHighAccuracy: true,
        timeout: 5000,
        maximumAge: 0
    });

} else {
    console.error("Geolocalização não suportada");
}

// ================= SUCCESS =================
function success(pos) {

    const lat = pos.coords.latitude;
    const lon = pos.coords.longitude;

    const now = Date.now();

    // controla intervalo de envio
    if (now - lastSend < 3000) return;

    lastSend = now;

    sendToBackend(lat, lon);
}

// ================= SEND =================
function sendToBackend(lat, lon) {

    fetch(`${API_URL}map/1`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify({
            latitude: lat,
            longitude: lon
        })
    })
        .then(res => {
            if (!res.ok) {
                throw new Error("Erro na requisição");
            }
            return res.text();
        })
        .then(data => {
            console.log("Enviado:", data);
        })
        .catch(err => {
            console.error("Erro ao enviar:", err);
        });

}

// ================= ERROR =================
function error(err) {
    console.error("Erro ao obter localização:", err);
}